/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.FunctionGenerationStrategy
import org.jetbrains.kotlin.codegen.TransformationMethodVisitor
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.inline.addFakeContinuationConstructorCallMarker
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.JVMConstructorCallNormalizationMode
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.MethodNode

open class SuspendFunctionGenerationStrategy(
        state: GenerationState,
        protected val originalSuspendDescriptor: FunctionDescriptor,
        protected val declaration: KtFunction,
        private val containingClassInternalName: String,
        private val constructorCallNormalizationMode: JVMConstructorCallNormalizationMode
) : FunctionGenerationStrategy.CodegenBased(state) {

    private lateinit var codegen: ExpressionCodegen
    private val languageVersionSettings: LanguageVersionSettings = state.configuration.languageVersionSettings

    private val classBuilderForCoroutineState by lazy {
        state.factory.newVisitor(
            OtherOrigin(declaration, originalSuspendDescriptor),
            CodegenBinding.asmTypeForAnonymousClass(state.bindingContext, originalSuspendDescriptor),
            declaration.containingFile
        ).also {
            val coroutineCodegen =
                    CoroutineCodegenForNamedFunction.create(it, codegen, originalSuspendDescriptor, declaration)
            coroutineCodegen.generate()
        }
    }

    override fun wrapMethodVisitor(mv: MethodVisitor, access: Int, name: String, desc: String): MethodVisitor {
        if (access and Opcodes.ACC_ABSTRACT != 0) return mv

        if (state.bindingContext[CodegenBinding.CAPTURES_CROSSINLINE_SUSPEND_LAMBDA, originalSuspendDescriptor] == true) {
            return AddConstructorCallForCoroutineRegeneration(
                mv, access, name, desc, null, null, this::classBuilderForCoroutineState,
                containingClassInternalName,
                originalSuspendDescriptor.dispatchReceiverParameter != null,
                containingClassInternalNameOrNull(),
                languageVersionSettings
            )
        }
        return CoroutineTransformerMethodVisitor(
            mv, access, name, desc, null, null, containingClassInternalName, this::classBuilderForCoroutineState,
            isForNamedFunction = true,
            lineNumber = CodegenUtil.getLineNumberForElement(declaration, false) ?: 0,
            shouldPreserveClassInitialization = constructorCallNormalizationMode.shouldPreserveClassInitialization,
            needDispatchReceiver = originalSuspendDescriptor.dispatchReceiverParameter != null,
            internalNameForDispatchReceiver = containingClassInternalNameOrNull(),
            languageVersionSettings = languageVersionSettings,
            sourceFile = declaration.containingFile.name
        )
    }

    private fun containingClassInternalNameOrNull() =
            originalSuspendDescriptor.containingDeclaration.safeAs<ClassDescriptor>()?.let(state.typeMapper::mapClass)?.internalName

    override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
        this.codegen = codegen
        codegen.returnExpression(declaration.bodyExpression ?: error("Function has no body: " + declaration.getElementTextWithContext()))
    }

    // When we generate named suspend function for the use as inline site, we do not generate state machine.
    // So, there will be no way to remember the name of generated continuation in such case.
    // In order to keep generated continuation for named suspend function, we just generate construction call, which is going to be
    // removed during inlining.
    // The continuation itself will be regenerated and used as a container for the coroutine's locals.
    private class AddConstructorCallForCoroutineRegeneration(
        delegate: MethodVisitor,
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<out String>?,
        obtainClassBuilderForCoroutineState: () -> ClassBuilder,
        private val containingClassInternalName: String,
        private val needDispatchReceiver: Boolean,
        private val internalNameForDispatchReceiver: String?,
        private val languageVersionSettings: LanguageVersionSettings
    ) : TransformationMethodVisitor(delegate, access, name, desc, signature, exceptions) {
        private val classBuilderForCoroutineState: ClassBuilder by lazy(obtainClassBuilderForCoroutineState)
        override fun performTransformations(methodNode: MethodNode) {
            val objectTypeForState = Type.getObjectType(classBuilderForCoroutineState.thisName)
            methodNode.instructions.insert(withInstructionAdapter {
                addFakeContinuationConstructorCallMarker(this, true)
                generateContinuationConstructorCall(
                    objectTypeForState,
                    methodNode,
                    needDispatchReceiver,
                    internalNameForDispatchReceiver,
                    containingClassInternalName,
                    classBuilderForCoroutineState,
                    languageVersionSettings
                )
                addFakeContinuationConstructorCallMarker(this, false)
            })
        }
    }
}

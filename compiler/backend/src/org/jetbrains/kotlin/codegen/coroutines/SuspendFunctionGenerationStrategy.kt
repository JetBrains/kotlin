/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.inline.addFakeContinuationConstructorCallMarker
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.codegen.inline.preprocessSuspendMarkers
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JVMConstructorCallNormalizationMode
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.inline.isEffectivelyInlineOnly
import org.jetbrains.kotlin.resolve.isInlineClass
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class SuspendFunctionGenerationStrategy(
    state: GenerationState,
    private val originalSuspendDescriptor: FunctionDescriptor,
    private val declaration: KtFunction,
    private val containingClassInternalName: String,
    private val constructorCallNormalizationMode: JVMConstructorCallNormalizationMode,
    private val functionCodegen: FunctionCodegen
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

        if (originalSuspendDescriptor.isEffectivelyInlineOnly()) {
            return SuspendForInlineOnlyMethodVisitor(mv, access, name, desc)
        }
        val stateMachineBuilder = createStateMachineBuilder(mv, access, name, desc)
        if (originalSuspendDescriptor.isInline) {
            return SuspendForInlineCopyingMethodVisitor(stateMachineBuilder, access, name, desc, functionCodegen::newMethod, keepAccess = false)
        }
        if (state.bindingContext[CodegenBinding.CAPTURES_CROSSINLINE_LAMBDA, originalSuspendDescriptor] == true) {
            return AddConstructorCallForCoroutineRegeneration(
                SuspendForInlineCopyingMethodVisitor(stateMachineBuilder, access, name, desc, functionCodegen::newMethod),
                access, name, desc, null, null, this::classBuilderForCoroutineState,
                containingClassInternalName,
                originalSuspendDescriptor.dispatchReceiverParameter != null,
                containingClassInternalNameOrNull(),
                languageVersionSettings
            )
        }
        return stateMachineBuilder
    }

    private fun createStateMachineBuilder(
        mv: MethodVisitor,
        access: Int,
        name: String,
        desc: String
    ): CoroutineTransformerMethodVisitor {
        return CoroutineTransformerMethodVisitor(
            mv, access, name, desc, null, null, containingClassInternalName, this::classBuilderForCoroutineState,
            isForNamedFunction = true,
            reportSuspensionPointInsideMonitor = { reportSuspensionPointInsideMonitor(declaration, state, it) },
            lineNumber = CodegenUtil.getLineNumberForElement(declaration, false) ?: 0,
            sourceFile = declaration.containingKtFile.name,
            shouldPreserveClassInitialization = constructorCallNormalizationMode.shouldPreserveClassInitialization,
            needDispatchReceiver = originalSuspendDescriptor.dispatchReceiverParameter != null,
            internalNameForDispatchReceiver = (originalSuspendDescriptor.containingDeclaration as? ClassDescriptor)?.let {
                if (it.isInlineClass()) state.typeMapper.mapType(it).internalName else null
            } ?: containingClassInternalNameOrNull(),
            languageVersionSettings = languageVersionSettings,
            disableTailCallOptimizationForFunctionReturningUnit = originalSuspendDescriptor.returnType?.isUnit() == true &&
                    originalSuspendDescriptor.overriddenDescriptors.isNotEmpty() &&
                    !originalSuspendDescriptor.allOverriddenFunctionsReturnUnit(),
            useOldSpilledVarTypeAnalysis = state.configuration.getBoolean(JVMConfigurationKeys.USE_OLD_SPILLED_VAR_TYPE_ANALYSIS)
        )
    }

    private fun FunctionDescriptor.allOverriddenFunctionsReturnUnit(): Boolean {
        val visited = mutableSetOf<FunctionDescriptor>()

        fun bfs(descriptor: FunctionDescriptor): Boolean {
            if (!visited.add(descriptor)) return true
            if (descriptor.original.returnType?.isUnit() != true) return false
            for (parent in descriptor.overriddenDescriptors) {
                if (!bfs(parent)) return false
            }
            return true
        }
        return bfs(this)
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
                pop() // Otherwise stack-transformation breaks
            })
        }
    }
}

private class SuspendForInlineOnlyMethodVisitor(delegate: MethodVisitor, access: Int, name: String, desc: String) :
    TransformationMethodVisitor(delegate, access, name, desc, null, null) {
    override fun performTransformations(methodNode: MethodNode) {
        methodNode.preprocessSuspendMarkers(forInline = true, keepFakeContinuation = false)
    }
}

// For named suspend function we generate two methods:
// 1) to use as noinline function, which have state machine
// 2) to use from inliner: private one without state machine
class SuspendForInlineCopyingMethodVisitor(
    delegate: MethodVisitor, access: Int, name: String, desc: String,
    private val newMethod: (JvmDeclarationOrigin, Int, String, String, String?, Array<String>?) -> MethodVisitor,
    private val keepAccess: Boolean = true
) : TransformationMethodVisitor(delegate, access, name, desc, null, null) {
    override fun performTransformations(methodNode: MethodNode) {
        val newMethodNode = with(methodNode) {
            val newAccess = if (keepAccess) access else
                access or Opcodes.ACC_PRIVATE and Opcodes.ACC_PUBLIC.inv() and Opcodes.ACC_PROTECTED.inv()
            MethodNode(newAccess, name + FOR_INLINE_SUFFIX, desc, signature, exceptions.toTypedArray())
        }
        val newMethodVisitor = with(newMethodNode) {
            newMethod(JvmDeclarationOrigin.NO_ORIGIN, access, name, desc, signature, exceptions.toTypedArray())
        }
        methodNode.instructions.resetLabels()
        methodNode.accept(newMethodNode)
        methodNode.preprocessSuspendMarkers(forInline = false, keepFakeContinuation = false)
        newMethodNode.preprocessSuspendMarkers(forInline = true, keepFakeContinuation = true)
        newMethodNode.accept(newMethodVisitor)
    }
}

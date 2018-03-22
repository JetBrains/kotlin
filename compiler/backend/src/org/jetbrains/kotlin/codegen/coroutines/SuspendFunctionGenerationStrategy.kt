/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

class SuspendFunctionGenerationStrategy(
        state: GenerationState,
        private val originalSuspendDescriptor: FunctionDescriptor,
        private val declaration: KtFunction,
        private val containingClassInternalName: String,
        private val constructorCallNormalizationMode: JVMConstructorCallNormalizationMode
) : FunctionGenerationStrategy.CodegenBased(state) {

    private lateinit var codegen: ExpressionCodegen

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
                containingClassInternalNameOrNull()
            )
        }
        return CoroutineTransformerMethodVisitor(
            mv, access, name, desc, null, null, containingClassInternalName, this::classBuilderForCoroutineState,
            isForNamedFunction = true,
            lineNumber = CodegenUtil.getLineNumberForElement(declaration, false) ?: 0,
            shouldPreserveClassInitialization = constructorCallNormalizationMode.shouldPreserveClassInitialization,
            needDispatchReceiver = originalSuspendDescriptor.dispatchReceiverParameter != null,
            internalNameForDispatchReceiver = containingClassInternalNameOrNull()
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
        private val internalNameForDispatchReceiver: String?
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
                    classBuilderForCoroutineState
                )
                addFakeContinuationConstructorCallMarker(this, false)
            })
        }
    }
}

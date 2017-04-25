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

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.FunctionGenerationStrategy
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

class SuspendFunctionGenerationStrategy(
        state: GenerationState,
        private val originalSuspendDescriptor: FunctionDescriptor,
        private val declaration: KtFunction,
        private val containingClassInternalName: String
) : FunctionGenerationStrategy.CodegenBased(state) {
    private val containsNonTailSuspensionCalls = originalSuspendDescriptor.containsNonTailSuspensionCalls(state.bindingContext)

    private val classBuilderForCoroutineState by lazy {
        state.factory.newVisitor(
            OtherOrigin(declaration, originalSuspendDescriptor),
            CodegenBinding.asmTypeForAnonymousClass(state.bindingContext, originalSuspendDescriptor),
            declaration.containingFile
        )
    }

    override fun wrapMethodVisitor(mv: MethodVisitor, access: Int, name: String, desc: String): MethodVisitor {
        if (containsNonTailSuspensionCalls) {
            return CoroutineTransformerMethodVisitor(
                    mv, access, name, desc, null, null, containingClassInternalName, classBuilderForCoroutineState,
                    isForNamedFunction = true
            )
        }

        return super.wrapMethodVisitor(mv, access, name, desc)
    }

    override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
        if (!containsNonTailSuspensionCalls) {
            codegen.returnExpression(declaration.bodyExpression)
            return
        }

        val coroutineCodegen =
                CoroutineForNamedFunctionCodegen.create(classBuilderForCoroutineState, codegen, originalSuspendDescriptor, declaration)
        coroutineCodegen.generate()

        val functionDescriptor = codegen.context.functionDescriptor

        val continuationIndex = codegen.frameMap.getIndex(functionDescriptor.valueParameters.last())
        val objectTypeForState = Type.getObjectType(classBuilderForCoroutineState.thisName)

        val dataIndex = codegen.frameMap.enterTemp(AsmTypes.OBJECT_TYPE)
        val exceptionIndex = codegen.frameMap.enterTemp(AsmTypes.OBJECT_TYPE)

        with(codegen.v) {
            val createStateInstance = Label()
            val storeStateObject = Label()

            // We have to distinguish the following situations:
            // - Our function got called in a common way (e.g. from another function or via recursive call) and we should execute our
            // code from the beginning
            // - We got called from `doResume` of our continuation, i.e. we need to continue from the last suspension point
            //
            // Also in the first case we wrap the completion into a special anonymous class instance (let's call it X$1)
            // that we'll use as a continuation argument for suspension points
            //
            // How we distinguish the cases:
            // - If the continuation is not an instance of X$1 we know exactly it's not the second case, because when resuming
            // the continuation we pass an instance of that class
            // - Otherwise it's still can be a recursive call. To check it's not the case we set the last bit in the label in
            // `doResume` just before calling the suspend function (see kotlin.coroutines.experimental.jvm.internal.CoroutineImplForNamedFunction).
            // So, if it's set we're in continuation.
            visitVarInsn(Opcodes.ALOAD, continuationIndex)
            instanceOf(objectTypeForState)
            ifeq(createStateInstance)

            visitVarInsn(Opcodes.ALOAD, continuationIndex)
            checkcast(objectTypeForState)
            invokevirtual(
                    COROUTINE_IMPL_FOR_NAMED_ASM_TYPE.internalName,
                    "checkAndFlushLastBit", Type.getMethodDescriptor(Type.BOOLEAN_TYPE), false
            )
            ifeq(createStateInstance)

            visitVarInsn(Opcodes.ALOAD, continuationIndex)
            checkcast(objectTypeForState)
            goTo(storeStateObject)

            visitLabel(createStateInstance)
            coroutineCodegen.putInstanceOnStack(codegen, null).put(objectTypeForState, this)

            visitLabel(storeStateObject)
            visitVarInsn(Opcodes.ASTORE, continuationIndex)

            visitVarInsn(Opcodes.ALOAD, continuationIndex)
            checkcast(objectTypeForState)
            getfield(COROUTINE_IMPL_FOR_NAMED_ASM_TYPE.internalName, "data", AsmTypes.OBJECT_TYPE.descriptor)
            visitVarInsn(Opcodes.ASTORE, dataIndex)

            visitVarInsn(Opcodes.ALOAD, continuationIndex)
            checkcast(objectTypeForState)
            getfield(COROUTINE_IMPL_FOR_NAMED_ASM_TYPE.internalName, "exception", AsmTypes.JAVA_THROWABLE_TYPE.descriptor)
            visitVarInsn(Opcodes.ASTORE, exceptionIndex)

            invokestatic(COROUTINE_MARKER_OWNER, ACTUAL_COROUTINE_START_MARKER_NAME, "()V", false)
        }

        codegen.returnExpression(declaration.bodyExpression)
    }
}

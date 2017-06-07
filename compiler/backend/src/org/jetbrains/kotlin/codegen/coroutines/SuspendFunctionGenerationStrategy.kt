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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

class SuspendFunctionGenerationStrategy(
        state: GenerationState,
        private val originalSuspendDescriptor: FunctionDescriptor,
        private val declaration: KtFunction,
        private val containingClassInternalName: String
) : FunctionGenerationStrategy.CodegenBased(state) {

    private lateinit var transformer: CoroutineTransformerMethodVisitor
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

        return CoroutineTransformerMethodVisitor(
                mv, access, name, desc, null, null, containingClassInternalName, this::classBuilderForCoroutineState,
                isForNamedFunction = true,
                element = declaration,
                needDispatchReceiver = originalSuspendDescriptor.dispatchReceiverParameter != null,
                internalNameForDispatchReceiver = containingClassInternalNameOrNull()
        ).also {
            transformer = it
        }
    }

    private fun containingClassInternalNameOrNull() =
            originalSuspendDescriptor.containingDeclaration.safeAs<ClassDescriptor>()?.let(state.typeMapper::mapClass)?.internalName

    override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
        this.codegen = codegen
        codegen.returnExpression(declaration.bodyExpression)
    }
}

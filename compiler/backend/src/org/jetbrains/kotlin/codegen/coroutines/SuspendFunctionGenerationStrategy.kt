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
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type

class SuspendFunctionGenerationStrategy(
        state: GenerationState,
        private val originalSuspendDescriptor: FunctionDescriptor,
        private val declaration: KtFunction
) : FunctionGenerationStrategy.CodegenBased(state) {

    override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
        if (!originalSuspendDescriptor.containsNonTailSuspensionCalls(state.bindingContext)) {
            return codegen.returnExpression(declaration.bodyExpression)
        }

        val coroutineCodegen = CoroutineCodegen.create(codegen, originalSuspendDescriptor, declaration, state)
        coroutineCodegen.generate()

        codegen.putClosureInstanceOnStack(coroutineCodegen, null).put(Type.getObjectType(coroutineCodegen.className), codegen.v)

        with(codegen.v) {
            invokeDoResumeWithUnit(coroutineCodegen.v.thisName)

            codegen.markLineNumber(declaration, true)

            areturn(AsmTypes.OBJECT_TYPE)
        }
    }
}

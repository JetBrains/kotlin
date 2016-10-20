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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver

class ClosureGenerationStrategy(
        state: GenerationState,
        declaration: KtDeclarationWithBody
) : FunctionGenerationStrategy.FunctionDefault(state, declaration) {

    override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
        processDestructuringInLambdaParameters(codegen)

        super.doGenerateBody(codegen, signature)
    }

    private fun processDestructuringInLambdaParameters(codegen: ExpressionCodegen) {
        val savedIsShouldMarkLineNumbers = codegen.isShouldMarkLineNumbers
        // Do not write line numbers until destructuring happens
        // (otherwise destructuring variables will be uninitialized in the beginning of lambda)
        codegen.isShouldMarkLineNumbers = false

        for (parameterDescriptor in codegen.context.functionDescriptor.valueParameters) {
            if (parameterDescriptor !is ValueParameterDescriptorImpl.WithDestructuringDeclaration) continue

            for (entry in parameterDescriptor.destructuringVariables.filterOutDescriptorsWithSpecialNames()) {
                codegen.myFrameMap.enter(entry, codegen.typeMapper.mapType(entry.type))
            }

            val destructuringDeclaration =
                    (DescriptorToSourceUtils.descriptorToDeclaration(parameterDescriptor) as? KtParameter)?.destructuringDeclaration
                    ?: error("Destructuring declaration for descriptor $parameterDescriptor not found")

            codegen.initializeDestructuringDeclarationVariables(
                    destructuringDeclaration,
                    TransientReceiver(parameterDescriptor.type),
                    codegen.findLocalOrCapturedValue(parameterDescriptor) ?: error("Local var not found for parameter $parameterDescriptor")
            )
        }

        codegen.isShouldMarkLineNumbers = savedIsShouldMarkLineNumbers
    }
}

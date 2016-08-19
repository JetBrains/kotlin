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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi2ir.toExpectedType

class FunctionBodyGenerator(override val context: GeneratorContext): IrGenerator {
    fun generateFunctionBody(scopeOwner: DeclarationDescriptor, ktExpression: KtExpression): IrExpression {
        resetInternalContext()
        val irExpression = StatementGenerator(context, scopeOwner, this)
                .generateExpression(ktExpression)
                .toExpectedType(getExpectedTypeForLastInferredCall(ktExpression))
        postprocessFunctionBody()
        return irExpression
    }

    private fun resetInternalContext() {
    }

    private fun postprocessFunctionBody() {
    }
}


/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.contracts.parsing

import org.jetbrains.kotlin.contracts.description.expressions.BooleanConstantReference
import org.jetbrains.kotlin.contracts.description.expressions.ConstantReference
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.types.KotlinType

internal class PsiConstantParser(private val callContext: ContractCallContext) : KtVisitor<ConstantReference?, Unit>() {
    override fun visitKtElement(element: KtElement, data: Unit?): ConstantReference? = null

    override fun visitConstantExpression(expression: KtConstantExpression, data: Unit?): ConstantReference? {
        val type: KotlinType = callContext.bindingContext.getType(expression) ?: return null

        val compileTimeConstant: CompileTimeConstant<*> = callContext.bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expression)
            ?: return null

        val value: Any? = compileTimeConstant.getValue(type)

        return when (value) {
            true -> BooleanConstantReference.TRUE
            false -> BooleanConstantReference.FALSE
            null -> ConstantReference.NULL
            else -> null
        }
    }
}
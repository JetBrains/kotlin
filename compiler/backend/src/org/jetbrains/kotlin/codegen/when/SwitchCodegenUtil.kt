/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.`when`

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.IntegerValueConstant
import org.jetbrains.kotlin.resolve.constants.NullValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.org.objectweb.asm.Type

import java.util.ArrayList

object SwitchCodegenUtil {
    @JvmStatic
    fun checkAllItemsAreConstantsSatisfying(
            expression: KtWhenExpression,
            bindingContext: BindingContext,
            shouldInlineConstVals: Boolean,
            predicate: Function1<ConstantValue<*>, Boolean>
    ): Boolean =
            expression.entries.all { entry ->
                entry.conditions.all { condition ->
                    if (condition !is KtWhenConditionWithExpression) return false
                    val patternExpression = condition.expression ?: return false
                    val constant = ExpressionCodegen.getCompileTimeConstant(patternExpression, bindingContext, shouldInlineConstVals) ?: return false
                    predicate.invoke(constant)
                }
            }

    @JvmStatic
    fun getAllConstants(
            expression: KtWhenExpression,
            bindingContext: BindingContext,
            shouldInlineConstVals: Boolean
    ): Iterable<ConstantValue<*>?> =
            ArrayList<ConstantValue<*>?>().apply {
                for (entry in expression.entries) {
                    addConstantsFromConditions(entry, bindingContext, shouldInlineConstVals)
                }
            }

    @JvmStatic
    fun getConstantsFromEntry(
            entry: KtWhenEntry,
            bindingContext: BindingContext,
            shouldInlineConstVals: Boolean
    ): Iterable<ConstantValue<*>?> =
            ArrayList<ConstantValue<*>?>().apply {
                addConstantsFromConditions(entry, bindingContext, shouldInlineConstVals)
            }

    private fun ArrayList<ConstantValue<*>?>.addConstantsFromConditions(
            entry: KtWhenEntry,
            bindingContext: BindingContext,
            shouldInlineConstVals: Boolean
    ) {
        for (condition in entry.conditions) {
            if (condition !is KtWhenConditionWithExpression) continue
            val patternExpression = condition.expression ?: throw AssertionError("expression in when should not be null")
            add(ExpressionCodegen.getCompileTimeConstant(patternExpression, bindingContext, shouldInlineConstVals))
        }
    }

    @JvmStatic
    fun buildAppropriateSwitchCodegenIfPossible(
            expression: KtWhenExpression,
            isStatement: Boolean,
            isExhaustive: Boolean,
            codegen: ExpressionCodegen
    ): SwitchCodegen? {
        val bindingContext = codegen.bindingContext
        val shouldInlineConstVals = codegen.state.shouldInlineConstVals
        if (!isThereConstantEntriesButNulls(expression, bindingContext, shouldInlineConstVals)) {
            return null
        }

        val subjectType = codegen.expressionType(expression.subjectExpression)

        val mapping = codegen.bindingContext.get(CodegenBinding.MAPPING_FOR_WHEN_BY_ENUM, expression)

        return when {
            mapping != null ->
                EnumSwitchCodegen(expression, isStatement, isExhaustive, codegen, mapping)
            isIntegralConstantsSwitch(expression, subjectType, bindingContext, shouldInlineConstVals) ->
                IntegralConstantsSwitchCodegen(expression, isStatement, isExhaustive, codegen)
            isStringConstantsSwitch(expression, subjectType, bindingContext, shouldInlineConstVals) ->
                StringSwitchCodegen(expression, isStatement, isExhaustive, codegen)
            else -> null
        }

    }

    private fun isThereConstantEntriesButNulls(
            expression: KtWhenExpression,
            bindingContext: BindingContext,
            shouldInlineConstVals: Boolean
    ): Boolean =
            getAllConstants(expression, bindingContext, shouldInlineConstVals).any { it != null && it !is NullValue }

    private fun isIntegralConstantsSwitch(
            expression: KtWhenExpression,
            subjectType: Type,
            bindingContext: BindingContext,
            shouldInlineConstVals: Boolean
    ): Boolean =
            AsmUtil.isIntPrimitive(subjectType) &&
            checkAllItemsAreConstantsSatisfying(expression, bindingContext, shouldInlineConstVals) { it is IntegerValueConstant<*> }

    private fun isStringConstantsSwitch(
            expression: KtWhenExpression,
            subjectType: Type,
            bindingContext: BindingContext,
            shouldInlineConstVals: Boolean
    ): Boolean =
            subjectType.className == String::class.java.name &&
            checkAllItemsAreConstantsSatisfying(expression, bindingContext, shouldInlineConstVals) { it is StringValue || it is NullValue }
}

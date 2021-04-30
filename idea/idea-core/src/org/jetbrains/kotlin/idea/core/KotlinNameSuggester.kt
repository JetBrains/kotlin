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

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getOutermostParenthesizerOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.builtIns

object KotlinNameSuggester : AbstractNameSuggester() {
    fun suggestNamesByExpressionAndType(
        expression: KtExpression,
        type: KotlinType?,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean,
        defaultName: String?
    ): Collection<String> {
        val result = LinkedHashSet<String>()

        result.addNamesByExpression(expression, bindingContext, validator)

        (type ?: bindingContext?.getType(expression))?.let {
            result.addNamesByType(it, validator)
        }

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    fun suggestNamesByType(type: KotlinType, validator: (String) -> Boolean, defaultName: String? = null): List<String> {
        val result = ArrayList<String>()

        result.addNamesByType(type, validator)

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    fun suggestNamesByExpressionOnly(
        expression: KtExpression,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean, defaultName: String? = null
    ): List<String> {
        val result = ArrayList<String>()

        result.addNamesByExpression(expression, bindingContext, validator)

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    fun suggestIterationVariableNames(
        collection: KtExpression,
        elementType: KotlinType,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean, defaultName: String?
    ): Collection<String> {
        val result = LinkedHashSet<String>()

        suggestNamesByExpressionOnly(collection, bindingContext, { true })
            .mapNotNull { StringUtil.unpluralize(it) }
            .mapTo(result) { suggestNameByName(it, validator) }

        result.addNamesByType(elementType, validator)

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    private fun MutableCollection<String>.addNamesByType(type: KotlinType, validator: (String) -> Boolean) {
        val myType = TypeUtils.makeNotNullable(type) // wipe out '?'
        val builtIns = myType.builtIns
        val typeChecker = KotlinTypeChecker.DEFAULT
        if (ErrorUtils.containsErrorType(myType)) return

        when {
            typeChecker.equalTypes(builtIns.booleanType, myType) -> addName("b", validator)
            typeChecker.equalTypes(builtIns.intType, myType) -> addName("i", validator)
            typeChecker.equalTypes(builtIns.byteType, myType) -> addName("byte", validator)
            typeChecker.equalTypes(builtIns.longType, myType) -> addName("l", validator)
            typeChecker.equalTypes(builtIns.floatType, myType) -> addName("fl", validator)
            typeChecker.equalTypes(builtIns.doubleType, myType) -> addName("d", validator)
            typeChecker.equalTypes(builtIns.shortType, myType) -> addName("sh", validator)
            typeChecker.equalTypes(builtIns.charType, myType) -> addName("c", validator)
            typeChecker.equalTypes(builtIns.stringType, myType) -> addName("s", validator)
            KotlinBuiltIns.isArray(myType) || KotlinBuiltIns.isPrimitiveArray(myType) -> {
                val elementType = builtIns.getArrayElementType(myType)
                when {
                    typeChecker.equalTypes(builtIns.booleanType, elementType) -> addName("booleans", validator)
                    typeChecker.equalTypes(builtIns.intType, elementType) -> addName("ints", validator)
                    typeChecker.equalTypes(builtIns.byteType, elementType) -> addName("bytes", validator)
                    typeChecker.equalTypes(builtIns.longType, elementType) -> addName("longs", validator)
                    typeChecker.equalTypes(builtIns.floatType, elementType) -> addName("floats", validator)
                    typeChecker.equalTypes(builtIns.doubleType, elementType) -> addName("doubles", validator)
                    typeChecker.equalTypes(builtIns.shortType, elementType) -> addName("shorts", validator)
                    typeChecker.equalTypes(builtIns.charType, elementType) -> addName("chars", validator)
                    typeChecker.equalTypes(builtIns.stringType, elementType) -> addName("strings", validator)
                    else -> {
                        val classDescriptor = TypeUtils.getClassDescriptor(elementType)
                        if (classDescriptor != null) {
                            val className = classDescriptor.name
                            addName("arrayOf" + StringUtil.capitalize(className.asString()) + "s", validator)
                        }
                    }
                }
            }
            myType.isFunctionType -> addName("function", validator)
            else -> {
                val descriptor = myType.constructor.declarationDescriptor
                if (descriptor != null) {
                    val className = descriptor.name
                    if (!className.isSpecial) {
                        addCamelNames(className.asString(), validator)
                    }
                }
            }
        }
    }

    private fun MutableCollection<String>.addNamesByExpression(
        expression: KtExpression?,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean
    ) {
        if (expression == null) return

        addNamesByValueArgument(expression, bindingContext, validator)
        addNamesByExpressionPSI(expression, validator)
    }

    private fun MutableCollection<String>.addNamesByValueArgument(
        expression: KtExpression,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean
    ) {
        if (bindingContext == null) return
        val argumentExpression = expression.getOutermostParenthesizerOrThis()
        val valueArgument = argumentExpression.parent as? KtValueArgument ?: return
        val resolvedCall = argumentExpression.getParentResolvedCall(bindingContext) ?: return
        val argumentMatch = resolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: return
        val parameter = argumentMatch.valueParameter
        if (parameter.containingDeclaration.hasStableParameterNames()) {
            addName(parameter.name.asString(), validator)
        }
    }

}

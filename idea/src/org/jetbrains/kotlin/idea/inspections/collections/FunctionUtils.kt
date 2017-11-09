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

package org.jetbrains.kotlin.idea.inspections.collections

import org.jetbrains.kotlin.builtins.getFunctionalClassKind
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType

fun KotlinType.isFunctionOfAnyKind() = constructor.declarationDescriptor?.getFunctionalClassKind() != null

fun ResolvedCall<*>.hasLastFunctionalParameterWithResult(context: BindingContext, predicate: (KotlinType) -> Boolean): Boolean {
    val lastParameter = resultingDescriptor.valueParameters.lastOrNull() ?: return false
    val lastArgument = valueArguments[lastParameter]?.arguments?.singleOrNull() ?: return false
    val functionalType = lastArgument.getArgumentExpression()?.getType(context) ?: return false
    // Both Function & KFunction must pass here
    if (!functionalType.isFunctionOfAnyKind()) return false
    val resultType = functionalType.arguments.lastOrNull()?.type ?: return false
    return predicate(resultType)
}
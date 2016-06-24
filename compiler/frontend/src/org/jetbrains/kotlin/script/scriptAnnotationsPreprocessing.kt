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

package org.jetbrains.kotlin.script

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.TypeUtils

internal class KtAnnotationWrapper(val psi: KtAnnotationEntry) {
    val name: String
        get() = (psi.typeReference?.typeElement as? KtUserType)?.referencedName.orAnonymous()

    val valueArguments: List<Pair<String?, Any?>> by lazy {
        psi.valueArguments.map {
            val evaluator = ConstantExpressionEvaluator(DefaultBuiltIns.Instance)
            val trace = BindingTraceContext()
            val result = evaluator.evaluateToConstantValue(it.getArgumentExpression()!!, trace, TypeUtils.NO_EXPECTED_TYPE)
            it.getArgumentName()?.asName.toString() to result?.value
            // TODO: consider inspecting `trace` to find diagnostics reported during the computation (such as division by zero, integer overflow, invalid annotation parameters etc.)
        }
    }

    internal fun String?.orAnonymous(kind: String = ""): String =
            this ?: "<anonymous" + (if (kind.isNotBlank()) " $kind" else "") + ">"
}

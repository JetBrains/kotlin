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
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.tryCreateCallableMappingFromNamedArgs
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.primaryConstructor

internal val KtAnnotationEntry.typeName: String get() = (typeReference?.typeElement as? KtUserType)?.referencedName.orAnonymous()

internal fun String?.orAnonymous(kind: String = ""): String =
        this ?: "<anonymous" + (if (kind.isNotBlank()) " $kind" else "") + ">"

internal fun constructAnnotation(psi: KtAnnotationEntry, targetClass: KClass<out Annotation>): Annotation {

    val valueArguments = psi.valueArguments.map { arg ->
        val evaluator = ConstantExpressionEvaluator(DefaultBuiltIns.Instance, LanguageVersionSettingsImpl.DEFAULT)
        val trace = BindingTraceContext()
        val result = evaluator.evaluateToConstantValue(arg.getArgumentExpression()!!, trace, TypeUtils.NO_EXPECTED_TYPE)
        // TODO: consider inspecting `trace` to find diagnostics reported during the computation (such as division by zero, integer overflow, invalid annotation parameters etc.)
        val argName = arg.getArgumentName()?.asName?.toString()
        argName to result?.value
    }
    val mappedArguments: Map<KParameter, Any?> =
        tryCreateCallableMappingFromNamedArgs(targetClass.constructors.first(), valueArguments)
        ?: return InvalidScriptResolverAnnotation(psi.typeName, valueArguments)

    try {
        return targetClass.primaryConstructor!!.callBy(mappedArguments)
    }
    catch (ex: Exception) {
        return InvalidScriptResolverAnnotation(psi.typeName, valueArguments, ex)
    }
}

class InvalidScriptResolverAnnotation(val name: String, val annParams: List<Pair<String?, Any?>>?, val error: Exception? = null) : Annotation

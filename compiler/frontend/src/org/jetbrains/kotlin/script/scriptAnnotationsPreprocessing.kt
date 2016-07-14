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
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.primaryConstructor

internal val KtAnnotationEntry.typeName: String get() = (typeReference?.typeElement as? KtUserType)?.referencedName.orAnonymous()

internal fun String?.orAnonymous(kind: String = ""): String =
        this ?: "<anonymous" + (if (kind.isNotBlank()) " $kind" else "") + ">"

internal class KtAnnotationWrapper(val psi: KtAnnotationEntry, val targetClass: KClass<out Annotation>) {
    val name: String get() = psi.typeName

    val valueArguments: Map<String, Any?> by lazy {
        var namedStarted = false
        val res = hashMapOf<String, Any?>()
        // TODO: annotation constructors unsupported yet in kotlin reflection, test and correct when they will be ready
        val targetAnnParams = targetClass.primaryConstructor?.parameters
        psi.valueArguments.mapIndexed { i, arg ->
            val evaluator = ConstantExpressionEvaluator(DefaultBuiltIns.Instance)
            val trace = BindingTraceContext()
            val result = evaluator.evaluateToConstantValue(arg.getArgumentExpression()!!, trace, TypeUtils.NO_EXPECTED_TYPE)
            // TODO: consider inspecting `trace` to find diagnostics reported during the computation (such as division by zero, integer overflow, invalid annotation parameters etc.)
            val argName = arg.getArgumentName()?.asName?.toString()
            // TODO: consider reusing arguments mapping logic from compiler code
            val paramName = when {
                argName == null && !namedStarted && targetAnnParams == null -> "$" // TODO: using invalid name here. Drop when annotation constructors will be accessible (se above)
                argName == null && !namedStarted -> targetAnnParams?.get(i)?.name ?: throw IllegalArgumentException("Unnamed argument for $name at $i")
                argName == null && namedStarted -> throw IllegalArgumentException("Invalid argument sequence for $name at arg $i")
                targetAnnParams != null && targetAnnParams.none { it.name == argName } ->
                    throw IllegalArgumentException("Unknown argument $argName for $name")
                else -> {
                    namedStarted = true
                    argName!!
                }
            }
            res.put(paramName, result?.value)
        }
        res
    }

    internal class AnnProxyInvocationHandler<out K: KClass<out Any>>(val targetAnnClass: K, val annParams: Map<String, Any?>) : InvocationHandler {
        override fun invoke(proxy: Any?, method: Method?, params: Array<out Any>?): Any? = method?.let {
            // TODO: the functionality with checking annParams size is here only to workaround missing access to constructors in annotations. Drop as soon as possible (see above)
            annParams[it.name] ?: if (annParams.size == 1) annParams.values.firstOrNull() else null
        }
    }

    fun getProxy(classLoader: ClassLoader): Annotation =
            try {
                Proxy.newProxyInstance(classLoader, arrayOf(targetClass.java), AnnProxyInvocationHandler(targetClass, valueArguments)) as Annotation
            }
            catch (ex: Exception) {
                InvalidScriptResolverAnnotation(name, valueArguments, ex)
            }
}

class InvalidScriptResolverAnnotation(val name: String, val annParams: Map<String, Any?>, val error: Exception? = null) : Annotation

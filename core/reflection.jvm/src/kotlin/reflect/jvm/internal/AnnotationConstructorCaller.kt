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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.load.java.structure.reflect.wrapperByPrimitive
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KotlinReflectionInternalError
import java.lang.reflect.Method as ReflectMethod

internal class AnnotationConstructorCaller(
        private val jClass: Class<*>,
        private val parameterNames: List<String>,
        private val callMode: CallMode,
        methods: List<ReflectMethod> = parameterNames.map { name -> jClass.getDeclaredMethod(name) }
) : FunctionCaller<Nothing?>(
        null, jClass, null, methods.map { it.genericReturnType }.toTypedArray()
) {
    enum class CallMode { CALL_BY_NAME, POSITIONAL_CALL }

    // Transform primitive int to java.lang.Integer because actual arguments passed here will be boxed and Class#isInstance should succeed
    private val erasedParameterTypes: List<Class<*>> = methods.map { method -> method.returnType.let { it.wrapperByPrimitive ?: it } }

    private val defaultValues: List<Any?> = methods.map { method -> method.defaultValue }

    override fun call(args: Array<*>): Any? {
        checkArguments(args)

        val values = args.mapIndexed { index, arg ->
            val value =
                    if (arg == null && callMode == CallMode.CALL_BY_NAME) defaultValues[index]
                    else arg.transformKotlinToJvm(erasedParameterTypes[index])
            value ?: throwIllegalArgumentType(index, parameterNames[index], erasedParameterTypes[index])
        }

        return createAnnotationInstance(jClass, parameterNames.zip(values).toMap())
    }
}

/**
 * Transforms a Kotlin value to the one required by the JVM, e.g. KClass<*> -> Class<*> or Array<KClass<*>> -> Array<Class<*>>.
 * Returns `null` in case when no transformation is possible (an argument of an incorrect type was passed).
 */
private fun Any?.transformKotlinToJvm(expectedType: Class<*>): Any? {
    @Suppress("UNCHECKED_CAST")
    val result = when (this) {
        is Class<*> -> return null
        is KClass<*> -> this.java
        is Array<*> -> when {
            this.isArrayOf<Class<*>>() -> return null
            this.isArrayOf<KClass<*>>() -> (this as Array<KClass<*>>).map(KClass<*>::java).toTypedArray()
            else -> this
        }
        else -> this
    }

    return if (expectedType.isInstance(result)) result else null
}

private fun throwIllegalArgumentType(index: Int, name: String, expectedJvmType: Class<*>): Nothing {
    val kotlinClass = when {
        expectedJvmType == Class::class.java -> KClass::class
        expectedJvmType.isArray && expectedJvmType.componentType == Class::class.java ->
            @Suppress("CLASS_LITERAL_LHS_NOT_A_CLASS") Array<KClass<*>>::class // Workaround KT-13924
        else -> expectedJvmType.kotlin
    }
    // For arrays, also render the type argument in the message, e.g. "... not of the required type kotlin.Array<kotlin.reflect.KClass>"
    val typeString = when {
        kotlinClass.qualifiedName == Array<Any>::class.qualifiedName ->
            "${kotlinClass.qualifiedName}<${kotlinClass.java.componentType.kotlin.qualifiedName}>"
        else -> kotlinClass.qualifiedName
    }
    throw IllegalArgumentException("Argument #$index $name is not of the required type $typeString")
}

private fun createAnnotationInstance(annotationClass: Class<*>, values: Map<String, Any>): Any {
    return Proxy.newProxyInstance(annotationClass.classLoader, arrayOf(annotationClass)) { proxy, method, args ->
        // TODO: support equals, hashCode, toString, annotationType
        values[method.name] ?: throw KotlinReflectionInternalError("Method is not supported: $method (args: ${args.orEmpty().toList()})")
    }
}

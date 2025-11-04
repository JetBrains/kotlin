/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.GenericDeclaration
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation
import kotlin.jvm.internal.KotlinGenericDeclaration
import kotlin.jvm.internal.findMethodBySignature
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.valueParameters

internal interface ReflectKFunction : ReflectKCallable<Any?>, KFunction<Any?>, KotlinGenericDeclaration {
    val signature: String

    val overridden: Collection<ReflectKFunction>

    override fun findJavaDeclaration(): GenericDeclaration? = container.findMethodBySignature(signature)
}

internal fun ReflectKFunction.extractContinuationArgument(): Type? {
    if (isSuspend) {
        // kotlin.coroutines.Continuation<? super java.lang.String>
        val continuationType = caller.parameterTypes.lastOrNull() as? ParameterizedType
        if (continuationType?.rawType == Continuation::class.java) {
            // ? super java.lang.String
            val wildcard = continuationType.actualTypeArguments.single() as? WildcardType
            // java.lang.String
            return wildcard?.lowerBounds?.first()
        }
    }

    return null
}

private const val DefaultConstructorMarkerDescriptor = "Lkotlin/jvm/internal/DefaultConstructorMarker;"

internal class DescriptorPatchingResult(val newDescriptor: String, val boxedIndices: Set<Int>)

// The compiler excessively boxes type of parameter, such that it has inline type and its underlying type is nullable
// Fixing it would break binary backward compatibility, so we mimic compiler behavior here
// See KT-57357
internal fun patchJvmDescriptorByExtraBoxing(function: ReflectKFunction, jvmDescriptor: String): DescriptorPatchingResult {

    val parsedDescriptor = parseJvmDescriptor(jvmDescriptor)
    val hasDefaultMarker = parsedDescriptor.parameters.lastOrNull() == DefaultConstructorMarkerDescriptor
    val valueParamCount = function.valueParameters.size + if (hasDefaultMarker) 1 else 0

    val boxedIndices = mutableSetOf<Int>()
    val newParameters = mutableListOf<String>()

    newParameters.addAll(parsedDescriptor.parameters.take(parsedDescriptor.parameters.size - valueParamCount))
    function.valueParameters.zip(parsedDescriptor.parameters.takeLast(valueParamCount))
        .forEach { (param, paramJvmDescriptor) ->
            if (param.isAlwaysBoxedByCompiler) {
                boxedIndices.add(newParameters.size)
                newParameters.add((param.type.classifier as KClass<*>).toJvmDescriptor())
            } else {
                newParameters.add(paramJvmDescriptor)
            }
        }

    if (hasDefaultMarker) {
        newParameters.add(DefaultConstructorMarkerDescriptor)
    }

    if (boxedIndices.isEmpty()) return DescriptorPatchingResult(jvmDescriptor, emptySet())
    val patchedDescriptor = newParameters.joinToString("", "(", ")") + parsedDescriptor.returnType
    return DescriptorPatchingResult(patchedDescriptor, boxedIndices)
}

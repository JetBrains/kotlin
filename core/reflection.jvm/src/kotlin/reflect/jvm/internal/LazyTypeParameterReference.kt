/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.KTypeParameterBase
import kotlin.reflect.*

internal class LazyTypeParameterReference(
    container: Any, // TypeParameterContainer
    override val name: String,
    override val variance: KVariance,
    override val isReified: Boolean,
) : KTypeParameterBase(container), TypeParameterMarker, TypeConstructorMarker {

    @Volatile
    private var bounds: List<KType>? = null

    override val upperBounds: List<KType>
        get() = bounds ?: listOf(typeOf<Any?>()).also { bounds = it }

    fun setUpperBounds(upperBounds: List<KType>) {
        bounds = upperBounds
    }

    internal val unwrapped: KTypeParameter by lazy(PUBLICATION) {
        val typeParameters: List<KTypeParameter> = when (container) {
            is KClass<*> -> container.typeParameters
            is KCallable<*> -> container.typeParameters
            else -> throw IllegalArgumentException("Type parameter container must be a class or a callable: ${container}")
        }
        typeParameters.firstOrNull { it.name == name } ?: throw IllegalArgumentException("Type parameter $name not found in $container")
    }
}
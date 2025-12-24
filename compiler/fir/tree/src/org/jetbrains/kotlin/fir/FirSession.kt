/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.fir.util.ConeTypeRegistry
import org.jetbrains.kotlin.util.ArrayMapAccessor
import org.jetbrains.kotlin.util.ComponentArrayOwner
import org.jetbrains.kotlin.util.NullableArrayMapAccessor
import org.jetbrains.kotlin.util.TypeRegistry
import kotlin.reflect.KClass

abstract class FirSession @PrivateSessionConstructor constructor(
    val kind: Kind
) : ComponentArrayOwner<FirSessionComponent, FirSessionComponent>(), SessionHolder {
    companion object : ConeTypeRegistry<FirSessionComponent, FirSessionComponent>() {
        inline fun <reified T : FirSessionComponent> sessionComponentAccessor(): ArrayMapAccessor<FirSessionComponent, FirSessionComponent, T> {
            return generateAccessor(T::class)
        }

        @Suppress("INVISIBLE_REFERENCE")
        inline fun <reified T : FirSessionComponent> sessionComponentAccessorWithDefault(
            defaultImplementation: @kotlin.internal.NoInfer T
        ): ArrayMapAccessor<FirSessionComponent, FirSessionComponent, T> {
            return generateAccessor(T::class, defaultImplementation)
        }

        inline fun <reified T : FirSessionComponent> sessionComponentAccessor(id: String): ArrayMapAccessor<FirSessionComponent, FirSessionComponent, T> {
            return generateAccessor(id)
        }

        inline fun <reified T : FirSessionComponent> nullableSessionComponentAccessor(): NullableArrayMapAccessor<FirSessionComponent, FirSessionComponent, T> {
            return generateNullableAccessor(T::class)
        }
    }

    open val builtinTypes: BuiltinTypes = BuiltinTypes()

    override val session: FirSession
        get() = this

    final override val typeRegistry: TypeRegistry<FirSessionComponent, FirSessionComponent> = Companion

    @SessionConfiguration
    fun register(tClass: KClass<out FirSessionComponent>, value: FirSessionComponent) {
        registerComponent(tClass, value)
    }

    @SessionConfiguration
    fun register(keyQualifiedName: String, value: FirSessionComponent) {
        registerComponent(keyQualifiedName, value)
    }

    @SessionConfiguration
    inline fun <reified T : FirComposableSessionComponent<T>> register(value: T) {
        register(T::class, value)
    }

    @SessionConfiguration
    fun <T : FirComposableSessionComponent<T>> register(tClass: KClass<out T>, value: T) {
        @Suppress("UNCHECKED_CAST")
        val existing = getOrNull(tClass) as T?
        val valueToRegister = if (existing != null) {
            val composed = existing.compose(value)
            composed
        } else {
            value
        }
        registerComponent(tClass, valueToRegister)
    }

    override fun toString(): String {
        val moduleData = nullableModuleData ?: return "Libraries session"
        val prefix = when (kind) {
            Kind.Source -> "Source"
            Kind.Library -> "Library"
        }
        return "$prefix session for module ${moduleData.name}"
    }

    enum class Kind {
        Source, Library
    }
}

class BuiltinTypes {
    val unitType: FirImplicitBuiltinTypeRef = FirImplicitUnitTypeRef(null)
    val anyType: FirImplicitBuiltinTypeRef = FirImplicitAnyTypeRef(null)
    val nullableAnyType: FirImplicitBuiltinTypeRef = FirImplicitNullableAnyTypeRef(null)
    val enumType: FirImplicitBuiltinTypeRef = FirImplicitEnumTypeRef(null)
    val annotationType: FirImplicitBuiltinTypeRef = FirImplicitAnnotationTypeRef(null)
    val booleanType: FirImplicitBuiltinTypeRef = FirImplicitBooleanTypeRef(null)
    val numberType: FirImplicitBuiltinTypeRef = FirImplicitNumberTypeRef(null)
    val byteType: FirImplicitBuiltinTypeRef = FirImplicitByteTypeRef(null)
    val shortType: FirImplicitBuiltinTypeRef = FirImplicitShortTypeRef(null)
    val intType: FirImplicitBuiltinTypeRef = FirImplicitIntTypeRef(null)
    val longType: FirImplicitBuiltinTypeRef = FirImplicitLongTypeRef(null)
    val doubleType: FirImplicitBuiltinTypeRef = FirImplicitDoubleTypeRef(null)
    val floatType: FirImplicitBuiltinTypeRef = FirImplicitFloatTypeRef(null)

    val uByteType: FirImplicitUByteTypeRef = FirImplicitUByteTypeRef(null)
    val uShortType: FirImplicitUShortTypeRef = FirImplicitUShortTypeRef(null)
    val uIntType: FirImplicitUIntTypeRef = FirImplicitUIntTypeRef(null)
    val uLongType: FirImplicitULongTypeRef = FirImplicitULongTypeRef(null)

    val nothingType: FirImplicitBuiltinTypeRef = FirImplicitNothingTypeRef(null)
    val nullableNothingType: FirImplicitBuiltinTypeRef = FirImplicitNullableNothingTypeRef(null)
    val charType: FirImplicitBuiltinTypeRef = FirImplicitCharTypeRef(null)
    val stringType: FirImplicitBuiltinTypeRef = FirImplicitStringTypeRef(null)
    val throwableType: FirImplicitThrowableTypeRef = FirImplicitThrowableTypeRef(null)

    val charSequenceType: FirImplicitCharSequenceTypeRef = FirImplicitCharSequenceTypeRef(null)
    val charIteratorType: FirImplicitCharIteratorTypeRef = FirImplicitCharIteratorTypeRef(null)
}

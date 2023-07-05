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

interface FirSessionComponent

abstract class FirSession @PrivateSessionConstructor constructor(
    val sessionProvider: FirSessionProvider?,
    val kind: Kind
) : ComponentArrayOwner<FirSessionComponent, FirSessionComponent>() {
    companion object : ConeTypeRegistry<FirSessionComponent, FirSessionComponent>() {
        inline fun <reified T : FirSessionComponent> sessionComponentAccessor(): ArrayMapAccessor<FirSessionComponent, FirSessionComponent, T> {
            return generateAccessor(T::class)
        }

        inline fun <reified T : FirSessionComponent> sessionComponentAccessor(id: String): ArrayMapAccessor<FirSessionComponent, FirSessionComponent, T> {
            return generateAccessor(id)
        }

        inline fun <reified T : FirSessionComponent> nullableSessionComponentAccessor(): NullableArrayMapAccessor<FirSessionComponent, FirSessionComponent, T> {
            return generateNullableAccessor(T::class)
        }
    }

    open val builtinTypes: BuiltinTypes = BuiltinTypes()

    final override val typeRegistry: TypeRegistry<FirSessionComponent, FirSessionComponent> = Companion

    @SessionConfiguration
    fun register(tClass: KClass<out FirSessionComponent>, value: FirSessionComponent) {
        registerComponent(tClass, value)
    }

    @SessionConfiguration
    fun register(keyQualifiedName: String, value: FirSessionComponent) {
        registerComponent(keyQualifiedName, value)
    }

    override fun toString(): String {
        val moduleData = nullableModuleData ?: return "Libraries session"
        return "Source session for module ${moduleData.name}"
    }

    enum class Kind {
        Source, Library
    }
}

abstract class FirSessionProvider {
    abstract fun getSession(moduleData: FirModuleData): FirSession?
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

    val uIntType: FirImplicitUIntTypeRef = FirImplicitUIntTypeRef(null)

    val nothingType: FirImplicitBuiltinTypeRef = FirImplicitNothingTypeRef(null)
    val nullableNothingType: FirImplicitBuiltinTypeRef = FirImplicitNullableNothingTypeRef(null)
    val charType: FirImplicitBuiltinTypeRef = FirImplicitCharTypeRef(null)
    val stringType: FirImplicitBuiltinTypeRef = FirImplicitStringTypeRef(null)
    val throwableType: FirImplicitThrowableTypeRef = FirImplicitThrowableTypeRef(null)

    val charSequenceType: FirImplicitCharSequenceTypeRef = FirImplicitCharSequenceTypeRef(null)
    val charIteratorType: FirImplicitCharIteratorTypeRef = FirImplicitCharIteratorTypeRef(null)
}

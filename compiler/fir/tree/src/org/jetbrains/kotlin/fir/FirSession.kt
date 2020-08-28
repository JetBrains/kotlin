/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.fir.utils.ArrayMapAccessor
import org.jetbrains.kotlin.fir.utils.ComponentArrayOwner
import org.jetbrains.kotlin.fir.utils.NullableArrayMapAccessor
import org.jetbrains.kotlin.fir.utils.TypeRegistry
import org.jetbrains.kotlin.utils.Jsr305State
import kotlin.reflect.KClass

@RequiresOptIn
annotation class PrivateSessionConstructor

@RequiresOptIn
annotation class SessionConfiguration

interface FirSessionComponent

abstract class FirSession @PrivateSessionConstructor constructor(val sessionProvider: FirSessionProvider?) : ComponentArrayOwner<FirSessionComponent, FirSessionComponent>() {
    companion object : TypeRegistry<FirSessionComponent, FirSessionComponent>() {
        inline fun <reified T : FirSessionComponent> sessionComponentAccessor(): ArrayMapAccessor<FirSessionComponent, FirSessionComponent, T> {
            return generateAccessor(T::class)
        }

        inline fun <reified T : FirSessionComponent> nullableSessionComponentAccessor(): NullableArrayMapAccessor<FirSessionComponent, FirSessionComponent, T> {
            return generateNullableAccessor(T::class)
        }
    }

    open val moduleInfo: ModuleInfo? get() = null

    val jsr305State: Jsr305State? get() = null

    val builtinTypes: BuiltinTypes = BuiltinTypes()

    final override val typeRegistry: TypeRegistry<FirSessionComponent, FirSessionComponent> = Companion

    @SessionConfiguration
    fun register(tClass: KClass<out FirSessionComponent>, value: FirSessionComponent) {
        registerComponent(tClass, value)
    }
}

interface FirSessionProvider {
    val project: Project

    fun getSession(moduleInfo: ModuleInfo): FirSession?
}

class BuiltinTypes {
    val unitType: FirImplicitBuiltinTypeRef = FirImplicitUnitTypeRef(null)
    val anyType: FirImplicitBuiltinTypeRef = FirImplicitAnyTypeRef(null)
    val nullableAnyType: FirImplicitBuiltinTypeRef = FirImplicitNullableAnyTypeRef(null)
    val enumType: FirImplicitBuiltinTypeRef = FirImplicitEnumTypeRef(null)
    val annotationType: FirImplicitBuiltinTypeRef = FirImplicitAnnotationTypeRef(null)
    val booleanType: FirImplicitBuiltinTypeRef = FirImplicitBooleanTypeRef(null)
    val byteType: FirImplicitBuiltinTypeRef = FirImplicitByteTypeRef(null)
    val shortType: FirImplicitBuiltinTypeRef = FirImplicitShortTypeRef(null)
    val intType: FirImplicitBuiltinTypeRef = FirImplicitIntTypeRef(null)
    val longType: FirImplicitBuiltinTypeRef = FirImplicitLongTypeRef(null)
    val doubleType: FirImplicitBuiltinTypeRef = FirImplicitDoubleTypeRef(null)
    val floatType: FirImplicitBuiltinTypeRef = FirImplicitFloatTypeRef(null)
    val nothingType: FirImplicitBuiltinTypeRef = FirImplicitNothingTypeRef(null)
    val nullableNothingType: FirImplicitBuiltinTypeRef = FirImplicitNullableNothingTypeRef(null)
    val charType: FirImplicitBuiltinTypeRef = FirImplicitCharTypeRef(null)
    val stringType: FirImplicitBuiltinTypeRef = FirImplicitStringTypeRef(null)
}

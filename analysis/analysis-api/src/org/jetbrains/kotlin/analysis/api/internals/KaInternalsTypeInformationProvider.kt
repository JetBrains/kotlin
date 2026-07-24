/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaBuiltinFunctionTypeFamilies
import org.jetbrains.kotlin.analysis.api.types.KaFunctionTypeFamily
import org.jetbrains.kotlin.analysis.api.types.KaType

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsTypeInformationProvider {
    public fun isDenotable(type: KaType): Boolean

    public fun isFunctionalInterface(type: KaType): Boolean

    @KaExperimentalApi
    public fun functionTypeFamily(type: KaType): KaFunctionTypeFamily?

    @KaExperimentalApi
    public fun isFunctionType(type: KaType): Boolean

    @KaExperimentalApi
    public fun isKFunctionType(type: KaType): Boolean

    @KaExperimentalApi
    public fun isSuspendFunctionType(type: KaType): Boolean

    @KaExperimentalApi
    public fun isKSuspendFunctionType(type: KaType): Boolean

    public fun isNullable(type: KaType): Boolean

    public fun isMarkedNullable(type: KaType): Boolean

    public fun hasFlexibleNullability(type: KaType): Boolean

    public fun isUnitType(type: KaType): Boolean

    public fun isIntType(type: KaType): Boolean

    public fun isLongType(type: KaType): Boolean

    public fun isShortType(type: KaType): Boolean

    public fun isByteType(type: KaType): Boolean

    public fun isFloatType(type: KaType): Boolean

    public fun isDoubleType(type: KaType): Boolean

    public fun isCharType(type: KaType): Boolean

    public fun isBooleanType(type: KaType): Boolean

    public fun isStringType(type: KaType): Boolean

    public fun isCharSequenceType(type: KaType): Boolean

    public fun isAnyType(type: KaType): Boolean

    public fun isNothingType(type: KaType): Boolean

    public fun isUIntType(type: KaType): Boolean

    public fun isULongType(type: KaType): Boolean

    public fun isUShortType(type: KaType): Boolean

    public fun isUByteType(type: KaType): Boolean

    public fun expandedSymbol(type: KaType): KaClassSymbol?

    public fun fullyExpandedType(type: KaType): KaType

    public fun isArrayOrPrimitiveArray(type: KaType): Boolean

    public fun isNestedArray(type: KaType): Boolean

    public fun isPrimitive(type: KaType): Boolean

    @KaExperimentalApi
    public fun defaultInitializer(type: KaType): String?

    @KaExperimentalApi
    public fun builtinFunctionTypeFamilies(): KaBuiltinFunctionTypeFamilies
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaBuiltinFunctionTypeFamilies
import org.jetbrains.kotlin.analysis.api.components.KaFunctionTypeFamily
import org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseFunctionTypeFamily
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.analysis.api.types.builtinFunctionTypeFamilies as builtinFunctionTypeFamiliesEndpoint
import org.jetbrains.kotlin.analysis.api.types.defaultInitializer as defaultInitializerEndpoint
import org.jetbrains.kotlin.analysis.api.types.expandedSymbol as expandedSymbolEndpoint
import org.jetbrains.kotlin.analysis.api.types.fullyExpandedType as fullyExpandedTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.functionTypeFamily as functionTypeFamilyEndpoint
import org.jetbrains.kotlin.analysis.api.types.hasFlexibleNullability as hasFlexibleNullabilityEndpoint
import org.jetbrains.kotlin.analysis.api.types.isAnyType as isAnyTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isArrayOrPrimitiveArray as isArrayOrPrimitiveArrayEndpoint
import org.jetbrains.kotlin.analysis.api.types.isBooleanType as isBooleanTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isByteType as isByteTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isCharSequenceType as isCharSequenceTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isCharType as isCharTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isDenotable as isDenotableEndpoint
import org.jetbrains.kotlin.analysis.api.types.isDoubleType as isDoubleTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isFloatType as isFloatTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isFunctionType as isFunctionTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isFunctionalInterface as isFunctionalInterfaceEndpoint
import org.jetbrains.kotlin.analysis.api.types.isIntType as isIntTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isKFunctionType as isKFunctionTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isKSuspendFunctionType as isKSuspendFunctionTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isLongType as isLongTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isMarkedNullable as isMarkedNullableEndpoint
import org.jetbrains.kotlin.analysis.api.types.isNestedArray as isNestedArrayEndpoint
import org.jetbrains.kotlin.analysis.api.types.isNothingType as isNothingTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isNullable as isNullableEndpoint
import org.jetbrains.kotlin.analysis.api.types.isPrimitive as isPrimitiveEndpoint
import org.jetbrains.kotlin.analysis.api.types.isShortType as isShortTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isStringType as isStringTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isSuspendFunctionType as isSuspendFunctionTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isUByteType as isUByteTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isUIntType as isUIntTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isULongType as isULongTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isUShortType as isUShortTypeEndpoint
import org.jetbrains.kotlin.analysis.api.types.isUnitType as isUnitTypeEndpoint

/**
 * Routes the legacy [KaTypeInformationProvider] surface through the new public `context(session: KaSession)` type-information endpoints,
 * which in turn reach the [org.jetbrains.kotlin.analysis.api.internals.KaInternalsTypeInformationProvider] proxy.
 *
 * Three members keep their original behavior here instead of routing through a new endpoint:
 *  - [isClassType] ([ClassId]) — intentionally left un-migrated (KT-68881);
 *  - `functionTypeKind` — the deprecated, hidden, reflection-accessed member, kept faithful to its original computation;
 *  - [canBeNull] — keeps its deprecated interface default (`= isNullable`).
 *
 * The moved supporting types ([KaFunctionTypeFamily], [KaBuiltinFunctionTypeFamilies]) are subtype shims of the new
 * `types`-package interfaces, so the endpoint results are narrowed back to the legacy surface with `as`.
 */
internal class KaTypeInformationProviderBridge(
    override val analysisSessionProvider: () -> KaSession,
) : KaBaseSessionComponent<KaSession>(), KaTypeInformationProvider {
    override val KaType.isDenotable: Boolean
        get() = context(analysisSession) { isDenotableEndpoint }

    override val KaType.isFunctionalInterface: Boolean
        get() = context(analysisSession) { isFunctionalInterfaceEndpoint }

    @KaExperimentalApi
    @Deprecated("Use 'functionTypeFamily' instead", level = DeprecationLevel.HIDDEN)
    override val KaType.functionTypeKind: FunctionTypeKind?
        get() = withValidityAssertion {
            @OptIn(KaImplementationDetail::class)
            (functionTypeFamily as? KaBaseFunctionTypeFamily)?.typeKind
        }

    @KaExperimentalApi
    override val KaType.functionTypeFamily: KaFunctionTypeFamily?
        get() = context(analysisSession) { functionTypeFamilyEndpoint as KaFunctionTypeFamily? }

    @KaExperimentalApi
    override val KaType.isFunctionType: Boolean
        get() = context(analysisSession) { isFunctionTypeEndpoint }

    @KaExperimentalApi
    override val KaType.isKFunctionType: Boolean
        get() = context(analysisSession) { isKFunctionTypeEndpoint }

    @KaExperimentalApi
    override val KaType.isSuspendFunctionType: Boolean
        get() = context(analysisSession) { isSuspendFunctionTypeEndpoint }

    @KaExperimentalApi
    override val KaType.isKSuspendFunctionType: Boolean
        get() = context(analysisSession) { isKSuspendFunctionTypeEndpoint }

    override val KaType.isNullable: Boolean
        get() = context(analysisSession) { isNullableEndpoint }

    override val KaType.isMarkedNullable: Boolean
        get() = context(analysisSession) { isMarkedNullableEndpoint }

    override val KaType.hasFlexibleNullability: Boolean
        get() = context(analysisSession) { hasFlexibleNullabilityEndpoint }

    override val KaType.isUnitType: Boolean
        get() = context(analysisSession) { isUnitTypeEndpoint }

    override val KaType.isIntType: Boolean
        get() = context(analysisSession) { isIntTypeEndpoint }

    override val KaType.isLongType: Boolean
        get() = context(analysisSession) { isLongTypeEndpoint }

    override val KaType.isShortType: Boolean
        get() = context(analysisSession) { isShortTypeEndpoint }

    override val KaType.isByteType: Boolean
        get() = context(analysisSession) { isByteTypeEndpoint }

    override val KaType.isFloatType: Boolean
        get() = context(analysisSession) { isFloatTypeEndpoint }

    override val KaType.isDoubleType: Boolean
        get() = context(analysisSession) { isDoubleTypeEndpoint }

    override val KaType.isCharType: Boolean
        get() = context(analysisSession) { isCharTypeEndpoint }

    override val KaType.isBooleanType: Boolean
        get() = context(analysisSession) { isBooleanTypeEndpoint }

    override val KaType.isStringType: Boolean
        get() = context(analysisSession) { isStringTypeEndpoint }

    override val KaType.isCharSequenceType: Boolean
        get() = context(analysisSession) { isCharSequenceTypeEndpoint }

    override val KaType.isAnyType: Boolean
        get() = context(analysisSession) { isAnyTypeEndpoint }

    override val KaType.isNothingType: Boolean
        get() = context(analysisSession) { isNothingTypeEndpoint }

    override val KaType.isUIntType: Boolean
        get() = context(analysisSession) { isUIntTypeEndpoint }

    override val KaType.isULongType: Boolean
        get() = context(analysisSession) { isULongTypeEndpoint }

    override val KaType.isUShortType: Boolean
        get() = context(analysisSession) { isUShortTypeEndpoint }

    override val KaType.isUByteType: Boolean
        get() = context(analysisSession) { isUByteTypeEndpoint }

    override val KaType.expandedSymbol: KaClassSymbol?
        get() = context(analysisSession) { expandedSymbolEndpoint }

    override val KaType.fullyExpandedType: KaType
        get() = context(analysisSession) { fullyExpandedTypeEndpoint }

    override val KaType.isArrayOrPrimitiveArray: Boolean
        get() = context(analysisSession) { isArrayOrPrimitiveArrayEndpoint }

    override val KaType.isNestedArray: Boolean
        get() = context(analysisSession) { isNestedArrayEndpoint }

    override fun KaType.isClassType(classId: ClassId): Boolean = withValidityAssertion {
        if (this !is KaClassType) return false
        return this.classId == classId
    }

    override val KaType.isPrimitive: Boolean
        get() = context(analysisSession) { isPrimitiveEndpoint }

    @KaExperimentalApi
    override val KaType.defaultInitializer: String?
        get() = context(analysisSession) { defaultInitializerEndpoint }

    @KaExperimentalApi
    override val builtinFunctionTypeFamilies: KaBuiltinFunctionTypeFamilies
        get() = context(analysisSession) { builtinFunctionTypeFamiliesEndpoint as KaBuiltinFunctionTypeFamilies }
}

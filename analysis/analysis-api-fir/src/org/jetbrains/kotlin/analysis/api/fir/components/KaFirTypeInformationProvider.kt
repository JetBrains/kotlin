/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirBuiltinFunctionTypeFamilies
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirFunctionTypeFamily
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsTypeInformationProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.resolve.FirSamResolver
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

internal class KaFirTypeInformationProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaFirSessionComponent(), KaInternalsTypeInformationProvider {
    override fun functionTypeFamily(type: KaType): KaFunctionTypeFamily? = type.withValidityAssertion {
        computeFunctionTypeKind(type)?.let(::KaFirFunctionTypeFamily)
    }

    override fun isFunctionType(type: KaType): Boolean = type.withValidityAssertion {
        functionTypeFamily(type) == builtinFunctionTypeFamilies().function
    }

    override fun isKFunctionType(type: KaType): Boolean = type.withValidityAssertion {
        functionTypeFamily(type) == builtinFunctionTypeFamilies().kFunction
    }

    override fun isSuspendFunctionType(type: KaType): Boolean = type.withValidityAssertion {
        functionTypeFamily(type) == builtinFunctionTypeFamilies().suspendFunction
    }

    override fun isKSuspendFunctionType(type: KaType): Boolean = type.withValidityAssertion {
        functionTypeFamily(type) == builtinFunctionTypeFamilies().kSuspendFunction
    }

    override fun expandedSymbol(type: KaType): KaClassSymbol? = type.withValidityAssertion {
        when (type) {
            is KaClassType -> when (val symbol = type.symbol) {
                is KaClassSymbol -> symbol
                is KaTypeAliasSymbol -> expandedSymbol(symbol.expandedType)
            }
            else -> null
        }
    }

    override fun defaultInitializer(type: KaType): String? = type.withValidityAssertion {
        when {
            isMarkedNullable(type) -> "null"
            else -> when (classId(type)) {
                KaStandardTypeClassIds.INT,
                KaStandardTypeClassIds.LONG,
                KaStandardTypeClassIds.SHORT,
                KaStandardTypeClassIds.BYTE,
                    -> "0"
                KaStandardTypeClassIds.FLOAT -> "0.0f"
                KaStandardTypeClassIds.DOUBLE -> "0.0"
                KaStandardTypeClassIds.CHAR -> "'\\u0000'"
                KaStandardTypeClassIds.BOOLEAN -> "false"
                KaStandardTypeClassIds.UNIT -> "Unit"
                KaStandardTypeClassIds.STRING -> "\"\""
                StandardClassIds.UInt -> "0.toUInt()"
                StandardClassIds.ULong -> "0.toULong()"
                StandardClassIds.UShort -> "0.toUShort()"
                StandardClassIds.UByte -> "0.toUByte()"
                else -> null
            }
        }
    }

    override fun builtinFunctionTypeFamilies(): KaBuiltinFunctionTypeFamilies = KaFirBuiltinFunctionTypeFamilies

    override fun classId(type: KaType): ClassId? = withValidityAssertion {
        when (type) {
            is KaClassType -> type.classId
            else -> null
        }
    }

    override fun isFunctionalInterface(type: KaType): Boolean = type.withValidityAssertion {
        val coneType = (type as KaFirType).coneType
        val firSession = analysisSession.firSession
        val samResolver = FirSamResolver(
            firSession,
            analysisSession.getScopeSessionFor(firSession),
        )
        return samResolver.isSamType(coneType)
    }

    private fun computeFunctionTypeKind(type: KaType): FunctionTypeKind? {
        return (type as KaFirType).coneType.functionTypeKind(analysisSession.firSession)
    }

    override fun isNullable(type: KaType): Boolean = type.withValidityAssertion {
        (type as KaFirType).coneType.canBeNull(analysisSession.firSession)
    }

    override fun isMarkedNullable(type: KaType): Boolean = type.withValidityAssertion {
        (type as KaFirType).coneType.isMarkedNullable
    }

    override fun hasFlexibleNullability(type: KaType): Boolean = type.withValidityAssertion {
        val coneType = (type as KaFirType).coneType
        coneType.hasFlexibleMarkedNullability || coneType is ConeErrorType && coneType.nullable == null
    }

    override fun isDenotable(type: KaType): Boolean = type.withValidityAssertion {
        with(analysisSession) {
            type.approximateToDenotableSupertype(allowLocalDenotableTypes = true) == null
        }
    }

    override fun isArrayOrPrimitiveArray(type: KaType): Boolean = type.withValidityAssertion {
        require(type is KaFirType)
        return type.coneType.isArrayOrPrimitiveArray
    }

    override fun isNestedArray(type: KaType): Boolean = type.withValidityAssertion {
        if (!isArrayOrPrimitiveArray(type)) return false
        require(type is KaFirType)
        return type.coneType.arrayElementType()?.isArrayOrPrimitiveArray == true
    }

    override fun fullyExpandedType(type: KaType): KaType = type.withValidityAssertion {
        (type as KaFirType).coneType.fullyExpandedType(analysisSession.firSession).asKaType()
    }
}

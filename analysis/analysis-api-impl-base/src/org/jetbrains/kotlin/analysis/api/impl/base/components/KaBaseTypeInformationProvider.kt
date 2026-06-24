/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsTypeInformationProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.functions.isSuspendOrKSuspendFunction
import org.jetbrains.kotlin.name.ClassId

@KaImplementationDetail
abstract class KaBaseTypeInformationProvider<T : KaSession> : KaBaseSessionComponent<T>(), KaInternalsTypeInformationProvider {
    protected abstract fun computeFunctionTypeKind(type: KaType): FunctionTypeKind?

    override fun functionTypeFamily(type: KaType): KaFunctionTypeFamily? = type.withValidityAssertion {
        computeFunctionTypeKind(type)?.let(::KaBaseFunctionTypeFamily)
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

    override fun isUnitType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(KaStandardTypeClassIds.UNIT) }

    override fun isIntType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(KaStandardTypeClassIds.INT) }

    override fun isLongType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(KaStandardTypeClassIds.LONG) }

    override fun isShortType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(KaStandardTypeClassIds.SHORT) }

    override fun isByteType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(KaStandardTypeClassIds.BYTE) }

    override fun isFloatType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(KaStandardTypeClassIds.FLOAT) }

    override fun isDoubleType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(KaStandardTypeClassIds.DOUBLE) }

    override fun isCharType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(KaStandardTypeClassIds.CHAR) }

    override fun isBooleanType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(KaStandardTypeClassIds.BOOLEAN) }

    override fun isStringType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(KaStandardTypeClassIds.STRING) }

    override fun isCharSequenceType(type: KaType): Boolean =
        type.withValidityAssertion { type.isClassType(KaStandardTypeClassIds.CHAR_SEQUENCE) }

    override fun isAnyType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(KaStandardTypeClassIds.ANY) }

    override fun isNothingType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(KaStandardTypeClassIds.NOTHING) }

    override fun isUIntType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(StandardNames.FqNames.uInt) }

    override fun isULongType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(StandardNames.FqNames.uLong) }

    override fun isUShortType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(StandardNames.FqNames.uShort) }

    override fun isUByteType(type: KaType): Boolean = type.withValidityAssertion { type.isClassType(StandardNames.FqNames.uByte) }

    override fun expandedSymbol(type: KaType): KaClassSymbol? = type.withValidityAssertion {
        when (type) {
            is KaClassType -> when (val symbol = type.symbol) {
                is KaClassSymbol -> symbol
                is KaTypeAliasSymbol -> expandedSymbol(symbol.expandedType)
            }
            else -> null
        }
    }

    override fun isPrimitive(type: KaType): Boolean = type.withValidityAssertion {
        if (type !is KaClassType) return false
        return type.classId in KaStandardTypeClassIds.PRIMITIVES
    }

    override fun defaultInitializer(type: KaType): String? = type.withValidityAssertion {
        when {
            isMarkedNullable(type) -> "null"
            isIntType(type) || isLongType(type) || isShortType(type) || isByteType(type) -> "0"
            isFloatType(type) -> "0.0f"
            isDoubleType(type) -> "0.0"
            isCharType(type) -> "'\\u0000'"
            isBooleanType(type) -> "false"
            isUnitType(type) -> "Unit"
            isStringType(type) -> "\"\""
            isUIntType(type) -> "0.toUInt()"
            isULongType(type) -> "0.toULong()"
            isUShortType(type) -> "0.toUShort()"
            isUByteType(type) -> "0.toUByte()"
            else -> null
        }
    }

    override fun builtinFunctionTypeFamilies(): KaBuiltinFunctionTypeFamilies = KaBaseBuiltinFunctionTypeFamilies

    /**
     * Checks whether the given [KaType] is a class type with the given [ClassId].
     *
     * This mirrors the (non-migrated, KT-68881) `isClassType(classId)` operation of the legacy
     * `KaTypeInformationProvider` and is used by the migrated proxy methods above.
     */
    private fun KaType.isClassType(classId: ClassId): Boolean = withValidityAssertion {
        if (this !is KaClassType) return false
        return this.classId == classId
    }
}

@KaImplementationDetail
class KaBaseFunctionTypeFamily(
    @property:KaImplementationDetail
    val typeKind: FunctionTypeKind,
) : KaFunctionTypeFamily, org.jetbrains.kotlin.analysis.api.components.KaFunctionTypeFamily {
    override val isReflect: Boolean
        get() = typeKind.isReflectType

    override val isSuspend: Boolean
        get() = typeKind.isSuspendOrKSuspendFunction

    override val isInlinable: Boolean
        get() = typeKind.isInlineable

    override val maxArity: Int
        get() = typeKind.maxArity

    override val supportsConversionFromSimpleFunctionType: Boolean
        get() = typeKind.supportsConversionFromSimpleFunctionType

    override val nameBase: String
        get() = typeKind.classNamePrefix

    override fun classId(arity: Int): ClassId =
        typeKind.numberedClassId(arity)

    override fun equals(other: Any?): Boolean =
        other is KaBaseFunctionTypeFamily && typeKind == other.typeKind

    override fun hashCode(): Int = typeKind.hashCode()

    override fun toString(): String =
        typeKind.toString()
}

private object KaBaseBuiltinFunctionTypeFamilies : KaBuiltinFunctionTypeFamilies,
    org.jetbrains.kotlin.analysis.api.components.KaBuiltinFunctionTypeFamilies {
    override val function: org.jetbrains.kotlin.analysis.api.components.KaFunctionTypeFamily =
        KaBaseFunctionTypeFamily(FunctionTypeKind.Function)

    override val suspendFunction: org.jetbrains.kotlin.analysis.api.components.KaFunctionTypeFamily =
        KaBaseFunctionTypeFamily(FunctionTypeKind.SuspendFunction)

    override val kFunction: org.jetbrains.kotlin.analysis.api.components.KaFunctionTypeFamily =
        KaBaseFunctionTypeFamily(FunctionTypeKind.KFunction)

    override val kSuspendFunction: org.jetbrains.kotlin.analysis.api.components.KaFunctionTypeFamily =
        KaBaseFunctionTypeFamily(FunctionTypeKind.KSuspendFunction)
}

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
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.functions.isSuspendOrKSuspendFunction
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

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

    override fun builtinFunctionTypeFamilies(): KaBuiltinFunctionTypeFamilies = KaBaseBuiltinFunctionTypeFamilies

    override fun classId(type: KaType): ClassId? = withValidityAssertion {
        when (type) {
            is KaClassType -> type.classId
            else -> null
        }
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

    override val typeRenderingPrefix: String?
        get() = typeKind.prefixForTypeRender

    override val markerAnnotationClassId: ClassId?
        get() = typeKind.annotationOnInvokeClassId

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

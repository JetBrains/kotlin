/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaBuiltinFunctionTypeFamilies
import org.jetbrains.kotlin.analysis.api.components.KaFunctionTypeFamily
import org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.functions.isSuspendOrKSuspendFunction
import org.jetbrains.kotlin.name.ClassId

@KaImplementationDetail
abstract class KaBaseTypeInformationProvider<T : KaSession> : KaBaseSessionComponent<T>(), KaTypeInformationProvider {
    @KaExperimentalApi
    override val KaType.functionTypeFamily: KaFunctionTypeFamily?
        get() = withValidityAssertion {
            functionTypeKind?.let(::KaBaseFunctionTypeFamily)
        }

    @KaExperimentalApi
    override val builtinFunctionTypeFamilies: KaBuiltinFunctionTypeFamilies
        get() = KaBaseBuiltinFunctionTypeFamilies
}

@KaImplementationDetail
class KaBaseFunctionTypeFamily(
    private val typeKind: FunctionTypeKind,
) : KaFunctionTypeFamily {
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
        other is KaBaseFunctionTypeFamily && typeKind === other.typeKind

    override fun hashCode(): Int =
        System.identityHashCode(typeKind)

    override fun toString(): String =
        typeKind.toString()
}

@KaImplementationDetail
private object KaBaseBuiltinFunctionTypeFamilies : KaBuiltinFunctionTypeFamilies {
    override val function: KaFunctionTypeFamily =
        KaBaseFunctionTypeFamily(FunctionTypeKind.Function)

    override val suspendFunction: KaFunctionTypeFamily =
        KaBaseFunctionTypeFamily(FunctionTypeKind.SuspendFunction)

    override val kFunction: KaFunctionTypeFamily =
        KaBaseFunctionTypeFamily(FunctionTypeKind.KFunction)

    override val kSuspendFunction: KaFunctionTypeFamily =
        KaBaseFunctionTypeFamily(FunctionTypeKind.KSuspendFunction)
}

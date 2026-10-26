/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.types.KaFunctionTypeFamily
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.builtins.functions.isSuspendOrKSuspendFunction
import org.jetbrains.kotlin.name.ClassId

internal class KaFirFunctionTypeFamily(
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
        other is KaFirFunctionTypeFamily && typeKind == other.typeKind

    override fun hashCode(): Int = typeKind.hashCode()

    override fun toString(): String =
        typeKind.toString()
}

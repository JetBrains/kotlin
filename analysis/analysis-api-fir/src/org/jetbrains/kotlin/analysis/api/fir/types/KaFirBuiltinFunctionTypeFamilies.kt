/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.types.KaBuiltinFunctionTypeFamilies
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind

internal object KaFirBuiltinFunctionTypeFamilies : KaBuiltinFunctionTypeFamilies,
    org.jetbrains.kotlin.analysis.api.components.KaBuiltinFunctionTypeFamilies {
    override val function: org.jetbrains.kotlin.analysis.api.components.KaFunctionTypeFamily =
        KaFirFunctionTypeFamily(FunctionTypeKind.Function)

    override val suspendFunction: org.jetbrains.kotlin.analysis.api.components.KaFunctionTypeFamily =
        KaFirFunctionTypeFamily(FunctionTypeKind.SuspendFunction)

    override val kFunction: org.jetbrains.kotlin.analysis.api.components.KaFunctionTypeFamily =
        KaFirFunctionTypeFamily(FunctionTypeKind.KFunction)

    override val kSuspendFunction: org.jetbrains.kotlin.analysis.api.components.KaFunctionTypeFamily =
        KaFirFunctionTypeFamily(FunctionTypeKind.KSuspendFunction)
}

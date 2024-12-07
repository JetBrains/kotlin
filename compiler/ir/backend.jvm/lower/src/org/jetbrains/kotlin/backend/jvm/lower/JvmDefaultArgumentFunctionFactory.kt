/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.jvm.InlineClassAbi
import org.jetbrains.kotlin.ir.types.IrType

internal class JvmDefaultArgumentFunctionFactory(context: CommonBackendContext) : MaskedDefaultArgumentFunctionFactory(context) {
    override fun IrType.hasNullAsUndefinedValue() =
        (InlineClassAbi.unboxType(this) ?: this) !in context.irBuiltIns.primitiveIrTypes
}
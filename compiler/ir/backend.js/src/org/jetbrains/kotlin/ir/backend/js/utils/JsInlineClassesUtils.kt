/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.backend.js.InlineClassesUtils
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.getInlineClassUnderlyingType

class JsInlineClassesUtils(val context: JsIrBackendContext) : InlineClassesUtils {
    override fun isTypeInlined(type: IrType): Boolean {
        return getInlinedClass(type) != null
    }

    override fun getInlinedClass(type: IrType): IrClass? =
        type.getJsInlinedClass()

    override fun isClassInlineLike(klass: IrClass): Boolean =
        klass.isInline

    override val boxIntrinsic: IrSimpleFunctionSymbol
        get() = context.intrinsics.jsBoxIntrinsic

    override val unboxIntrinsic: IrSimpleFunctionSymbol
        get() = context.intrinsics.jsUnboxIntrinsic
}
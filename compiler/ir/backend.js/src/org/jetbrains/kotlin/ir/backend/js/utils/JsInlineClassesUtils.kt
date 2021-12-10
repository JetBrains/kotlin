/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.backend.js.JsCommonInlineClassesUtils
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IdSignatureValues
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isMarkedNullable

class JsInlineClassesUtils(val context: JsIrBackendContext) : JsCommonInlineClassesUtils {

    override fun getInlinedClass(type: IrType): IrClass? {
        if (type is IrSimpleType) {
            val erased = erase(type) ?: return null
            if (isClassInlineLike(erased)) {
                if (type.isMarkedNullable()) {
                    var fieldType: IrType
                    var fieldInlinedClass = erased
                    while (true) {
                        fieldType = getInlineClassUnderlyingType(fieldInlinedClass)
                        if (fieldType.isMarkedNullable()) {
                            return null
                        }

                        fieldInlinedClass = getInlinedClass(fieldType) ?: break
                    }
                }

                return erased
            }
        }
        return null
    }

    // Char is declared as a regular class, but we want to treat it as an inline class.
    // We can't declare it as an inline/value class for compatibility reasons.
    // For example, applying the === operator will stop working if Char becomes an inline class.
    override fun isClassInlineLike(klass: IrClass): Boolean =
        super.isClassInlineLike(klass) || klass.symbol.signature == IdSignatureValues._char

    override val boxIntrinsic: IrSimpleFunctionSymbol
        get() = context.intrinsics.jsBoxIntrinsic

    override val unboxIntrinsic: IrSimpleFunctionSymbol
        get() = context.intrinsics.jsUnboxIntrinsic
}

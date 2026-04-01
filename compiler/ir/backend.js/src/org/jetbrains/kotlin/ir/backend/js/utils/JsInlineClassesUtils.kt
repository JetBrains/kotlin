/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.ir.backend.js.ir.isExported
import org.jetbrains.kotlin.ir.backend.js.JsCommonInlineClassesUtils
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IdSignatureValues
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.isInterface

class JsInlineClassesUtils(val context: JsIrBackendContext) : JsCommonInlineClassesUtils {

    override fun getInlinedClass(type: IrType, includingExported: Boolean): IrClass? {
        if (type is IrSimpleType) {
            val erased = erase(type) ?: return null
            if (isClassInlineLike(erased, includingExported)) {
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

    override fun isClassInlineLike(klass: IrClass): Boolean =
        isClassInlineLike(klass, includingExported = false)

    // Char is declared as a regular class, but we want to treat it as an inline class.
    // We can't declare it as an inline/value class for compatibility reasons.
    // For example, applying the === operator will stop working if Char becomes an inline class.
    // Additionally, within the scope of KT-80734, we haven't found a reasonable design to maintain regular boxing/unboxing
    // for value classes annotated with @JsExport. As a result, we treat them as regular classes and make boxing/unboxing only
    // in place of passing/getting their instances to/from external declarations (see [AutoboxingForExportedValueClassesForExternalsLowering])
    fun isClassInlineLike(klass: IrClass, includingExported: Boolean): Boolean =
        klass.symbol.signature == IdSignatureValues._char || super.isClassInlineLike(klass) && (includingExported || !klass.isExported(context))

    override val boxIntrinsic: IrSimpleFunctionSymbol
        get() = context.symbols.jsBoxIntrinsic

    override val unboxIntrinsic: IrSimpleFunctionSymbol
        get() = context.symbols.jsUnboxIntrinsic

    fun getRuntimeClassFor(type: IrType): IrClass? = type.erasedUpperBound.takeIf { !it.isInterface }
}

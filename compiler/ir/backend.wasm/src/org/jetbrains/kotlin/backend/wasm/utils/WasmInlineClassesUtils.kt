/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.utils

import org.jetbrains.kotlin.backend.wasm.BackendWasmSymbols
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsCommonInlineClassesUtils
import org.jetbrains.kotlin.ir.backend.js.utils.erase
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.eraseIfTypeParameter
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.isEquals
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isTypeParameter

class WasmInlineClassesUtils(private val wasmSymbols: BackendWasmSymbols) : JsCommonInlineClassesUtils {

    override fun getInlinedClass(type: IrType, includingExported: Boolean): IrClass? {
        if (type is IrSimpleType) {
            // TODO: Make inlining less strict
            if (type.isNullable()) return null
            val erased = erase(type) ?: return null
            if (isClassInlineLike(erased)) {
                return erased
            }
        }
        return null
    }

    override fun isClassInlineLike(klass: IrClass): Boolean {
        // TODO: This hook is called from autoboxing lowering so we also handle autoboxing annotation here. In the future it's better
        // to separate autoboxing from the inline class handling.
        return super.isClassInlineLike(klass) || klass.hasWasmAutoboxedAnnotation()
    }

    override val boxIntrinsic: IrSimpleFunctionSymbol
        get() = wasmSymbols.boxIntrinsic

    override val unboxIntrinsic: IrSimpleFunctionSymbol
        get() = wasmSymbols.unboxIntrinsic
}

internal fun IrType.getMostDeepValueClassUnderlyingType(
    context: WasmBackendContext, stopWhenEqualsIsOverridden: Boolean = false
): IrType {
    var currentType = this

    while (true) {
        currentType = currentType.eraseIfTypeParameter()

        if (currentType.isPrimitiveType() || currentType.isNullable()) break

        val currentClass = currentType.classOrNull?.owner ?: break

        if (!context.inlineClassesUtils.isClassInlineLike(currentClass)) break

        if (stopWhenEqualsIsOverridden) {
            if (currentClass.declarations.any {
                    it is IrSimpleFunction &&
                    it.origin != IrDeclarationOrigin.GENERATED_INLINE_CLASS_MEMBER &&
                    it.isEquals()
                }) break
        }

        currentType = context.inlineClassesUtils.getInlineClassUnderlyingType(currentClass)
    }
    return currentType
}

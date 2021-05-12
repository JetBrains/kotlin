/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWithArguments
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Replace kotlin.Function{N} etc super types with  kotlin.wasm.internal.Function{N} super types.
 * This is a workaround for having a concrete IR for these interfaces.
 */
class WasmFunctionInterfaceReplacer(val context: WasmBackendContext) : FileLoweringPass {

    private fun replaceType(type: IrType): IrType {
        if (type.isFunction()) {
            require(type is IrSimpleType)
            val klass: IrClass = type.classifier.owner as IrClass

            // No need to replace just "kotlin.Function"
            if (klass.name.identifier == "Function") {
                return type
            }

            val arity = klass.typeParameters.size - 1
            return context.wasmSymbols.functionN(arity).typeWithArguments(type.arguments)
        }

        return type
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitClassNew(declaration: IrClass): IrStatement {
                if (declaration.superTypes.any { it.isFunction() }) {
                    declaration.superTypes = declaration.superTypes.map { replaceType(it) }
                }
                return super.visitClassNew(declaration)
            }
        })
    }
}
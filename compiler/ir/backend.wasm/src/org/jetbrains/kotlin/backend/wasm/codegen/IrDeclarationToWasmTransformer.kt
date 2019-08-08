/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isAnnotationClass

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class IrDeclarationToWasmTransformer : BaseIrElementToWasmNodeTransformer<List<WasmModuleField>, WasmStaticContext> {
    override fun visitSimpleFunction(declaration: IrSimpleFunction, context: WasmStaticContext): List<WasmModuleField> {
        val function = WasmFunction(
            name = context.getNameForStaticFunction(declaration).ident,
            parameters = declaration.valueParameters.map { context.transformValueParameter(it) },
            returnType = context.transformType(declaration.returnType),
            locals = emptyList(),
            instructions = bodyToWasm(declaration.body!!, context)
        )
        return listOf(function)
    }

    override fun visitConstructor(declaration: IrConstructor, context: WasmStaticContext): List<WasmModuleField> {
        TODO()
    }

    override fun visitClass(declaration: IrClass, context: WasmStaticContext): List<WasmModuleField> {
        if (declaration.isAnnotationClass) return emptyList()
        TODO()
    }

    override fun visitField(declaration: IrField, context: WasmStaticContext): List<WasmModuleField> {
        val global = WasmGlobal(
            name = context.getNameForField(declaration).ident,
            type = context.transformType(declaration.type),
            isMutable = true,
            init = declaration.initializer?.let {
                expressionToWasmInstruction(it.expression, context)
            }
        )
        return listOf(global)
    }
}



fun WasmStaticContext.transformValueParameter(irParameter: IrValueParameter): WasmParameter {
    return WasmParameter(getNameForValueDeclaration(irParameter).ident, transformType(irParameter.type))
}

fun WasmStaticContext.transformType(irType: IrType): WasmValueType =
    when {
        irType.isBoolean() -> WasmI32
        irType.isByte() -> WasmI32
        irType.isShort() -> WasmI32
        irType.isInt() -> WasmI32
        irType.isLong() -> WasmI64
        irType.isChar() -> WasmI32
        irType.isFloat() -> WasmF32
        irType.isDouble() -> WasmF64
        else -> TODO()
    }
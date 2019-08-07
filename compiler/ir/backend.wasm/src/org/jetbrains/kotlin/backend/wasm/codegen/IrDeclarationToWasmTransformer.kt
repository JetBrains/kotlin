/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isInt
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
        TODO()
    }
}



fun WasmStaticContext.transformValueParameter(irParameter: IrValueParameter): WasmParameter {
    return WasmParameter(getNameForValueDeclaration(irParameter).ident, transformType(irParameter.type))
}

fun WasmStaticContext.transformType(irType: IrType): WasmValueType =
    when {
        irType.isInt() -> WasmI32
        else -> TODO()
    }
/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.backend.wasm.utils.getWasmImportAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.getWasmInstructionAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.hasExcludedFromCodegenAnnotation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

class DeclarationTransformer : BaseTransformer<WasmModuleField?, WasmCodegenContext> {
    override fun visitSimpleFunction(declaration: IrSimpleFunction, data: WasmCodegenContext): WasmModuleField? {
        if (declaration.hasExcludedFromCodegenAnnotation())
            return null
        if (declaration.getWasmInstructionAnnotation() != null)
            return null
        if (declaration.isFakeOverride)
            return null
        // Virtual functions are not supported yet
        if (declaration.origin == IrDeclarationOrigin.BRIDGE)
            return null

        // Collect local variables
        val localNames = wasmNameTable<IrValueDeclaration>()

        val wasmName = data.getGlobalName(declaration)

        val irParameters = declaration.run {
            listOfNotNull(dispatchReceiverParameter, extensionReceiverParameter) + valueParameters
        }

        val wasmParameters = irParameters.map { parameter ->
            val name = localNames.declareFreshName(parameter, parameter.name.asString())
            WasmParameter(name, data.transformType(parameter.type))
        }

        val wasmReturnType = when {
            declaration.returnType.isUnit() -> null
            else -> data.transformType(declaration.returnType)
        }

        val importedName = declaration.getWasmImportAnnotation()
        if (importedName != null) {
            data.imports.add(
                WasmFunction(
                    name = wasmName,
                    parameters = wasmParameters,
                    returnType = wasmReturnType,
                    locals = emptyList(),
                    instructions = emptyList(),
                    importPair = importedName
                )
            )
            return null
        }

        val body = declaration.body
            ?: error("Function ${declaration.fqNameWhenAvailable} without a body")

        data.localNames = localNames.names
        val locals = mutableListOf<WasmLocal>()
        body.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitVariable(declaration: IrVariable) {
                val name = localNames.declareFreshName(declaration, declaration.name.asString())
                locals += WasmLocal(name, data.transformType(declaration.type))
                super.visitVariable(declaration)
            }
        })

        return WasmFunction(
            name = wasmName,
            parameters = wasmParameters,
            returnType = wasmReturnType,
            locals = locals,
            instructions = bodyToWasmInstructionList(body, data),
            importPair = null
        )
    }

    override fun visitConstructor(declaration: IrConstructor, data: WasmCodegenContext): WasmModuleField? {
        TODO()
    }

    override fun visitClass(declaration: IrClass, data: WasmCodegenContext): WasmModuleField? {
        if (declaration.isAnnotationClass) return null
        if (declaration.hasExcludedFromCodegenAnnotation()) return null

        val wasmMembers = declaration.declarations.mapNotNull { member ->
            when (member) {
                is IrSimpleFunction -> this.visitSimpleFunction(member, data)
                else -> null
            }
        }

        return WasmModuleFieldList(wasmMembers)
    }

    override fun visitField(declaration: IrField, data: WasmCodegenContext): WasmModuleField {
        return WasmGlobal(
            name = data.getGlobalName(declaration),
            type = data.transformType(declaration.type),
            isMutable = true,
            // TODO: move non-constexpr initializers out
            init = declaration.initializer?.let {
                expressionToWasmInstruction(it.expression, data)
            }
        )
    }
}
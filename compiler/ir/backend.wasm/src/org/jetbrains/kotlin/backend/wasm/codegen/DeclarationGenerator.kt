/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.common.ir.isOverridableOrOverrides
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.backend.wasm.utils.getWasmImportAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.getWasmInstructionAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.hasExcludedFromCodegenAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.hasSkipRTTIAnnotation
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class DeclarationGenerator(
    val backendContext: WasmBackendContext,
    val wasmModuleFragment: WasmCompiledModuleFragment
) : IrElementVisitorVoid {

    val data = WasmCodegenContext(backendContext, wasmModuleFragment)

    override fun visitElement(element: IrElement) {
        TODO("not implemented")
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        return transformFunction(declaration)
    }

    private fun transformFunction(declaration: IrFunction) {
        if (declaration is IrSimpleFunction && declaration.modality == Modality.ABSTRACT) return
        if (declaration.hasExcludedFromCodegenAnnotation())
            return

        val wasmInstruction = declaration.getWasmInstructionAnnotation()
        val isVirtual = declaration is IrSimpleFunction && declaration.isOverridableOrOverrides && !declaration.parentAsClass.hasSkipRTTIAnnotation()

        if (!isVirtual && wasmInstruction != null)
            return
        if (!isVirtual && declaration.isInline)
            return
        if (declaration.isFakeOverride)
            return

        assert(declaration == declaration.realOverrideTarget)

        if (isVirtual) {
            wasmModuleFragment.virtualFunctions += declaration.symbol
        }

        // Collect local variables
        val labels = wasmNameTable<LoopLabel>()

        val irParameters = declaration.getEffectiveValueParameters()

        val typeTransformer = TypeTransformer(data, backendContext.irBuiltIns)

        val localIndicies = mutableMapOf<IrValueSymbol, Int>()
        for ((index, irParameter) in irParameters.withIndex()) {
            localIndicies[irParameter.symbol] = index
        }

        val watName = declaration.fqNameWhenAvailable.toString()

        val functionType = with(typeTransformer) { declaration.toWasmFunctionType() }

        wasmModuleFragment.functionTypes.define(declaration.symbol, functionType)

        val importedName = declaration.getWasmImportAnnotation()
        if (importedName != null) {
            wasmModuleFragment.functions.define(
                declaration.symbol,
                WasmImportedFunction(
                    name = watName,
                    type = functionType,
                    importPair = importedName
                )
            )
            return
        }


        val locals = mutableListOf<WasmLocal>()
        val wasmBody = (if (wasmInstruction != null) {
            if (wasmInstruction == "nop")
                listOf(WasmGetLocal(0))
            else
                listOf(WasmSimpleInstruction(wasmInstruction, irParameters.indices.map { WasmGetLocal(it) }))
        } else {
            val body = declaration.body
                ?: error("Function ${declaration.fqNameWhenAvailable} without a body")

            data.locals = localIndicies
            data.labels = labels.names

            body.acceptChildrenVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitVariable(declaration: IrVariable) {
                    localIndicies[declaration.symbol] = localIndicies.size
                    locals += WasmLocal(declaration.name.asString(), data.transformType(declaration.type))
                    super.visitVariable(declaration)
                }

                override fun visitLoop(loop: IrLoop) {
                    val suggestedLabel = loop.label ?: ""
                    for (labelType in LoopLabelType.values()) {
                        labels.declareFreshName(
                            LoopLabel(loop, labelType),
                            "${labelType.name}_$suggestedLabel"
                        )
                    }
                    super.visitLoop(loop)
                }
            })

            bodyToWasmInstructionList(body, data)
        }).toMutableList()

        if (declaration is IrConstructor)
            wasmBody += WasmReturn(WasmGetLocal(/*implicit this*/0))

        val function = WasmDefinedFunction(
            name = declaration.fqNameWhenAvailable.toString(),
            type = functionType,
            locals = locals,
            instructions = wasmBody
        )

        if (declaration == backendContext.startFunction)
            wasmModuleFragment.startFunction = WasmStart(function)

        wasmModuleFragment.functions.define(declaration.symbol, function)

        if (declaration.isExported(data)) {
            wasmModuleFragment.exports += WasmExport(
                function = function,
                exportedName = declaration.name.identifier,
                kind = WasmExport.Kind.FUNCTION
            )
        }
    }

    override fun visitConstructor(declaration: IrConstructor) {
        transformFunction(declaration)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.isAnnotationClass) return
        if (declaration.hasExcludedFromCodegenAnnotation()) return

        val structType = WasmStructType(
            declaration.fqNameWhenAvailable.toString(),
            declaration.allFields(backendContext.irBuiltIns).map { WasmStructField(it.name.toString(), data.transformType(it.type), true) }
        )

        if (!declaration.hasSkipRTTIAnnotation()) {
            if (declaration.isInterface) {
                wasmModuleFragment.interfaces += declaration.symbol
            } else {
                val classMetadata = data.getClassMetadata(declaration)
                wasmModuleFragment.classes += declaration.symbol
                wasmModuleFragment.typeInfo.define(declaration.symbol, binaryDataStruct(classMetadata))
            }
        }

        wasmModuleFragment.structTypes.define(declaration.symbol, structType)

        data.currentClass = declaration
        for (member in declaration.declarations) {
            member.acceptVoid(this)
        }
    }

    private fun binaryDataStruct(classMetadata: ClassMetadata): BinaryDataStruct {
        val superClass = classMetadata.superClass?.klass

        val superClassSymbol: WasmSymbol<Int> = superClass?.let { data.getClassId(it) } ?: WasmSymbol(-1)

        val lmSuperType = BinaryDataIntField("Super class", superClassSymbol)

        val lmImplementedInterfacesData = BinaryDataIntArray(
            "data",
            classMetadata.interfaces.map { data.getInterfaceId(it) }
        )
        val lmImplementedInterfacesSize = BinaryDataIntField(
            "size",
            lmImplementedInterfacesData.value.size
        )

        val lmImplementedInterfaces = BinaryDataStruct(
            "Implemented interfaces array", listOf(lmImplementedInterfacesSize, lmImplementedInterfacesData)
        )

        val lmVirtualFunctionsSize = BinaryDataIntField(
            "V-table length",
            classMetadata.virtualMethods.size
        )

        val lmVtable = BinaryDataIntArray(
            "V-table",
            classMetadata.virtualMethods.map { data.virtualFunctionId(it.function) }
        )

        // TODO: Function signature id
        val lmSignatures = BinaryDataIntArray(
            "Signatures Stub",
            List(classMetadata.virtualMethods.size) { WasmSymbol(-1) }
        )

        val classLmElements = listOf(lmSuperType, lmVirtualFunctionsSize, lmVtable, lmSignatures, lmImplementedInterfaces)
        val classLmStruct = BinaryDataStruct("Class TypeInfo: ${classMetadata.klass.fqNameWhenAvailable} ", classLmElements)
        return classLmStruct
    }

    override fun visitField(declaration: IrField) {
        if (!declaration.isStatic) return
        val wasmType = data.transformType(declaration.type)

        val global = WasmGlobal(
            name = declaration.fqNameWhenAvailable.toString(), // data.getGlobalName(declaration),
            type = wasmType,
            isMutable = true,
            // All globals are currently initialized in start function
            // TODO: Put const-expr initializers in place
            init = defaultInitializerForType(wasmType)
        )

        wasmModuleFragment.globals.define(declaration.symbol, global)
    }
}

enum class LoopLabelType { BREAK, CONTINUE, LOOP }
data class LoopLabel(val loop: IrLoop, val isBreak: LoopLabelType)

fun defaultInitializerForType(type: WasmValueType): WasmInstruction = when (type) {
    WasmI32 -> WasmI32Const(0)
    WasmI64 -> WasmI64Const(0)
    WasmF32 -> WasmF32Const(0f)
    WasmF64 -> WasmF64Const(0.0)
    WasmAnyRef -> WasmRefNull
    is WasmStructRef -> WasmRefNull
}

fun IrFunction.getEffectiveValueParameters(): List<IrValueParameter> {
    val implicitThis = if (this is IrConstructor) parentAsClass.thisReceiver!! else null
    return listOfNotNull(implicitThis, dispatchReceiverParameter, extensionReceiverParameter) + valueParameters
}
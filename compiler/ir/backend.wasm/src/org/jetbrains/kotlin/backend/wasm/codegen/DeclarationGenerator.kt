/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.common.ir.isOverridableOrOverrides
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.backend.wasm.utils.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class DeclarationGenerator(val context: WasmModuleCodegenContext) : IrElementVisitorVoid {
    private val backendContext: WasmBackendContext = context.backendContext
    private val irBuiltIns: IrBuiltIns = backendContext.irBuiltIns

    override fun visitElement(element: IrElement) {
        if (element !is IrTypeAlias)
            error("Unexpected element $element")
    }

    override fun visitFunction(declaration: IrFunction) {
        // TODO: Exclude these as IR lowering
        if (declaration.hasExcludedFromCodegenAnnotation())
            return

        if (declaration is IrConstructor)
            if (declaration.parentAsClass.isInline)
                return

        // Generate function type
        val watName = declaration.fqNameWhenAvailable.toString()
        val irParameters = declaration.getEffectiveValueParameters()
        val functionType =
            WasmFunctionType(
                watName,
                parameterTypes = irParameters.map { context.transformValueParameterType(it) },
                resultType = context.transformResultType(declaration.returnType)
            )
        context.defineFunctionType(declaration.symbol, functionType)

        // Check if function needs to be generated
        val wasmUnaryOp = declaration.getWasmUnaryOpAnnotation()
        val wasmBinaryOp = declaration.getWasmBinaryOpAnnotation()
        val wasmRefOp = declaration.getWasmRefOpAnnotation()
        val wasmReinterpet = declaration.hasWasmReinterpretAnnotation()

        val isWasmIntrinsic = (wasmReinterpet || (wasmUnaryOp ?: wasmBinaryOp ?: wasmRefOp) != null)

        val isVirtual =
            declaration is IrSimpleFunction && declaration.isOverridableOrOverrides && !declaration.parentAsClass.hasSkipRTTIAnnotation()
        if (declaration is IrSimpleFunction && declaration.modality == Modality.ABSTRACT) return
        if (!isVirtual && isWasmIntrinsic)
            return
        if (!isVirtual && declaration.isInline)
            return
        if (declaration.isFakeOverride)
            return

        assert(declaration == declaration.realOverrideTarget)

        // Register function as virtual, meaning this function
        // will be stored Wasm table and could be called indirectly.
        if (declaration is IrSimpleFunction && isVirtual) {
            context.registerVirtualFunction(declaration.symbol)
        }

        val importedName = declaration.getWasmImportAnnotation()
        if (importedName != null) {
            context.defineFunction(
                declaration.symbol,
                WasmImportedFunction(watName, functionType, importedName)
            )
            // TODO: Support re-export of imported functions.
            return
        }

        val function = WasmDefinedFunction(watName, functionType)
        val functionCodegenContext = WasmFunctionCodegenContextImpl(declaration, backendContext, context, function)

        irParameters.forEach { functionCodegenContext.defineLocal(it.symbol) }

        val wasmBody = function.instructions

        val wasmArguments = function.locals.map { WasmGetLocal(it) }
        val wasmIntrinsic = generateWasmOpIntrinsic(declaration, wasmArguments)

        when {
            isWasmIntrinsic -> {
                wasmBody += wasmIntrinsic!!
            }

            else -> {
                wasmBody += when (val body = declaration.body) {
                    is IrBlockBody ->
                        body.statements.flatMap { statementToWasmInstruction(it, functionCodegenContext) }

                    is IrExpressionBody ->
                        statementToWasmInstruction(body.expression, functionCodegenContext)

                    else -> error("Unexpected body $body")
                }
            }
        }

        // Return implicit this from constructions to avoid extra tmp
        // variables on constructor call sites.
        if (declaration is IrConstructor)
            wasmBody += WasmReturn(WasmGetLocal(/*implicit this*/function.locals[0]))

        if (declaration == backendContext.startFunction)
            context.setStartFunction(function)

        context.defineFunction(declaration.symbol, function)

        if (declaration.isExported(backendContext)) {
            context.addExport(
                WasmExport(
                    function = function,
                    exportedName = declaration.name.identifier,
                    kind = WasmExport.Kind.FUNCTION
                )
            )
        }
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.isAnnotationClass) return
        if (declaration.hasExcludedFromCodegenAnnotation()) return
        val symbol = declaration.symbol

        if (declaration.isInterface) {
            context.registerInterface(symbol)
        } else {
            val structType = WasmStructType(
                declaration.fqNameWhenAvailable.toString(),
                declaration.allFields(irBuiltIns).map {
                    WasmStructField(it.name.toString(), context.transformType(it.type), true)
                }
            )
            context.defineStructType(symbol, structType)

            if (!declaration.hasSkipRTTIAnnotation()) {
                val classMetadata = context.getClassMetadata(symbol)
                context.registerClass(symbol)
                context.generateTypeInfo(symbol, binaryDataStruct(classMetadata))
            }
        }

        for (member in declaration.declarations) {
            member.acceptVoid(this)
        }
    }

    private fun binaryDataStruct(classMetadata: ClassMetadata): ConstantDataStruct {
        val superClass = classMetadata.superClass?.klass

        val superClassSymbol: WasmSymbol<Int> = superClass?.let { context.referenceClassId(it.symbol) } ?: WasmSymbol(-1)

        val superTypeField = ConstantDataIntField("Super class", superClassSymbol)

        val interfacesArray = ConstantDataIntArray(
            "data",
            classMetadata.interfaces.map { context.referenceInterfaceId(it.symbol) }
        )
        val interfacesArraySize = ConstantDataIntField(
            "size",
            interfacesArray.value.size
        )

        val implementedInterfacesArrayWithSize = ConstantDataStruct(
            "Implemented interfaces array",
            listOf(interfacesArraySize, interfacesArray)
        )

        val vtableSizeField = ConstantDataIntField(
            "V-table length",
            classMetadata.virtualMethods.size
        )

        // TODO: Don't generate v-table and for abstract classes
        val vtableArray = ConstantDataIntArray(
            "V-table",
            classMetadata.virtualMethods.map {
                if (it.function.modality == Modality.ABSTRACT) {
                    WasmSymbol(-1)
                } else {
                    context.referenceVirtualFunctionId(it.function.symbol)
                }
            }
        )

        val signaturesArray = ConstantDataIntArray(
            "Signatures",
            classMetadata.virtualMethods.map {
                if (it.function.modality == Modality.ABSTRACT) {
                    WasmSymbol(-1)
                } else {
                    context.referenceSignatureId(it.signature)
                }
            }
        )

        val typeInfoElements = listOf(superTypeField, vtableSizeField, vtableArray, signaturesArray, implementedInterfacesArrayWithSize)
        return ConstantDataStruct("Class TypeInfo: ${classMetadata.klass.fqNameWhenAvailable} ", typeInfoElements)
    }

    override fun visitField(declaration: IrField) {
        // Member fields are generated as part of struct type
        if (!declaration.isStatic) return

        val wasmType = context.transformType(declaration.type)

        val global = WasmGlobal(
            name = declaration.fqNameWhenAvailable.toString(),
            type = wasmType,
            isMutable = true,
            // All globals are currently initialized in start function
            init = defaultInitializerForType(wasmType)
        )

        context.defineGlobal(declaration.symbol, global)
    }
}

enum class LoopLabelType { BREAK, CONTINUE }

fun defaultInitializerForType(type: WasmValueType): WasmInstruction = when (type) {
    WasmI32 -> WasmConstant.I32(0)
    WasmI64 -> WasmConstant.I64(0)
    WasmF32 -> WasmConstant.F32(0f)
    WasmF64 -> WasmConstant.F64(0.0)
    WasmAnyRef -> WasmRefNull
    is WasmStructRef -> WasmRefNull
    WasmNullRefType -> WasmRefNull
    WasmUnreachableType -> error("Unreachable type can't be initialized")
}

fun IrFunction.getEffectiveValueParameters(): List<IrValueParameter> {
    val implicitThis = if (this is IrConstructor) parentAsClass.thisReceiver!! else null
    return listOfNotNull(implicitThis, dispatchReceiverParameter, extensionReceiverParameter) + valueParameters
}

fun IrFunction.isExported(context: WasmBackendContext): Boolean =
    visibility == Visibilities.PUBLIC && fqNameWhenAvailable in context.additionalExportedDeclarations
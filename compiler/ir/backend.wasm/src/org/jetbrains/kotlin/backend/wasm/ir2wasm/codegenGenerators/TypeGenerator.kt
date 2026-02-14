/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.getWasmArrayAnnotation
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.utils.findUnitGetInstanceFunction
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.wasm.ir.*

class TypeGenerator(
    private val backendContext: WasmBackendContext,
    private val typeCodegenContext: WasmTypeCodegenContext,
    private val wasmModuleTypeTransformer: WasmModuleTypeTransformer,
    private val wasmModuleMetadataCache: WasmModuleMetadataCache,
) {
    private val irBuiltIns: IrBuiltIns = backendContext.irBuiltIns
    private val unitGetInstanceFunction: IrSimpleFunction by lazy { backendContext.findUnitGetInstanceFunction() }
    private val unitPrimaryConstructor: IrConstructor? by lazy { backendContext.irBuiltIns.unitClass.owner.primaryConstructor }

    fun generateFunctionType(declaration: IrFunction) {
        val parameterTypes = mutableListOf<WasmType>()
        declaration.forEachEffectiveValueParameters {
            parameterTypes.add(wasmModuleTypeTransformer.transformValueParameterType(it))
        }

        // Generate function type
        val resultType = when (declaration) {
            // Unit_getInstance returns true Unit reference instead of "void"
            unitGetInstanceFunction, unitPrimaryConstructor -> wasmModuleTypeTransformer.transformType(declaration.returnType)
            else -> wasmModuleTypeTransformer.transformResultType(declaration.returnType)
        }

        val wasmFunctionType =
            WasmFunctionType(
                parameterTypes = parameterTypes,
                resultTypes = listOfNotNull(resultType)
            )

        typeCodegenContext.defineFunctionType(declaration.symbol, wasmFunctionType)
    }

    fun generateClassTypes(declaration: IrClass) {
        val symbol = declaration.symbol

        // Handle arrays
        declaration.getWasmArrayAnnotation()?.let { wasmArrayAnnotation ->
            val nameStr = declaration.fqNameWhenAvailable.toString()
            val wasmArrayDeclaration = WasmArrayDeclaration(
                nameStr,
                WasmStructFieldDeclaration(
                    name = "field",
                    type = wasmModuleTypeTransformer.transformFieldType(wasmArrayAnnotation.type),
                    isMutable = wasmArrayAnnotation.isMutable
                )
            )

            typeCodegenContext.defineGcType(symbol, wasmArrayDeclaration)
            return
        }

        val nameStr = declaration.fqNameWhenAvailable.toString()

        if (declaration.isInterface) {
            val vtableStruct = createVirtualTableStruct(
                methods = wasmModuleMetadataCache.getInterfaceMetadata(symbol).methods,
                name = "<classITable>",
                isFinal = true,
                generateSpecialITableField = false,
            )
            typeCodegenContext.defineVTableGcType(symbol, vtableStruct)
        } else {
            val metadata = wasmModuleMetadataCache.getClassMetadata(symbol)

            val vtableRefGcType = WasmRefType(typeCodegenContext.referenceVTableHeapType(symbol))
            val fields = mutableListOf<WasmStructFieldDeclaration>()
            fields.add(WasmStructFieldDeclaration("vtable", vtableRefGcType, false))
            fields.add(WasmStructFieldDeclaration("itable", WasmRefNullType(Synthetics.HeapTypes.wasmAnyArrayType), false))
            fields.add(WasmStructFieldDeclaration("rtti", WasmRefType(Synthetics.HeapTypes.rttiType), isMutable = false))
            declaration.allFields(irBuiltIns).mapTo(fields) {
                WasmStructFieldDeclaration(
                    name = it.name.toString(),
                    type = wasmModuleTypeTransformer.transformFieldType(it.type),
                    isMutable = true
                )
            }

            val superClass = metadata.superClass
            val structType = WasmStructDeclaration(
                name = nameStr,
                fields = fields,
                superType = superClass?.let { typeCodegenContext.referenceHeapType(superClass.klass.symbol) },
                isFinal = declaration.modality == Modality.FINAL
            )
            typeCodegenContext.defineGcType(symbol, structType)

            createVTableType(metadata)
        }
    }

    private fun createVirtualTableStruct(
        methods: List<VirtualMethodMetadata>,
        name: String,
        superType: WasmHeapType.Type.VTableType? = null,
        isFinal: Boolean,
        generateSpecialITableField: Boolean,
    ): WasmStructDeclaration {
        val vtableFields = mutableListOf<WasmStructFieldDeclaration>()
        if (generateSpecialITableField) {
            val specialITableField = WasmStructFieldDeclaration(
                name = "<SpecialITable>",
                type = WasmRefNullType(Synthetics.HeapTypes.specialSlotITableType),
                isMutable = false
            )
            vtableFields.add(specialITableField)
        }

        methods.mapTo(vtableFields) {
            WasmStructFieldDeclaration(
                name = it.signature.name.asString(),
                type = WasmRefNullType(typeCodegenContext.referenceFunctionHeapType(it.function.symbol)),
                isMutable = false
            )
        }

        return WasmStructDeclaration(
            name = name,
            fields = vtableFields,
            superType = superType,
            isFinal = isFinal
        )
    }

    private fun createVTableType(metadata: ClassMetadata) {
        val klass = metadata.klass

        val vtableStruct = createVirtualTableStruct(
            metadata.virtualMethods,
            "<classVTable>",
            superType = metadata.superClass?.klass?.symbol?.let(typeCodegenContext::referenceVTableHeapType),
            isFinal = klass.modality == Modality.FINAL,
            generateSpecialITableField = true,
        )
        typeCodegenContext.defineVTableGcType(metadata.klass.symbol, vtableStruct)
    }
}

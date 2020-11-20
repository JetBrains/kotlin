/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.common.ir.isOverridableOrOverrides
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.lower.wasmSignature
import org.jetbrains.kotlin.backend.wasm.utils.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.wasm.ir.*

class DeclarationGenerator(val context: WasmModuleCodegenContext) : IrElementVisitorVoid {

    // Shortcuts
    private val backendContext: WasmBackendContext = context.backendContext
    private val irBuiltIns: IrBuiltIns = backendContext.irBuiltIns

    override fun visitElement(element: IrElement) {
        error("Unexpected element of type ${element::class}")
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) {
        // Type aliases are not material
    }

    private fun jsCodeName(declaration: IrFunction): String {
        return declaration.fqNameWhenAvailable!!.asString() + "_" + (declaration as IrSimpleFunction).wasmSignature(irBuiltIns).hashCode()
    }

    override fun visitFunction(declaration: IrFunction) {
        // Inline class constructors are currently empty
        if (declaration is IrConstructor && backendContext.inlineClassesUtils.isClassInlineLike(declaration.parentAsClass))
            return

        val jsCode = declaration.getJsFunAnnotation()
        val importedName = if (jsCode != null) {
            val jsCodeName = jsCodeName(declaration)
            context.addJsFun(jsCodeName, jsCode)
            WasmImportPair("js_code", jsCodeName(declaration))
        } else {
            declaration.getWasmImportAnnotation()
        }

        val isIntrinsic = declaration.hasWasmReinterpretAnnotation() || declaration.getWasmOpAnnotation() != null
        if (isIntrinsic) {
            return
        }

        if (declaration.isFakeOverride)
            return

        // Generate function type
        val watName = declaration.fqNameWhenAvailable.toString()
        val irParameters = declaration.getEffectiveValueParameters()
        val wasmFunctionType =
            WasmFunctionType(
                name = watName,
                parameterTypes = irParameters.map {
                    val t = context.transformValueParameterType(it)
                    if (importedName != null && t is WasmRefNullType) {
                        WasmEqRef
                    } else {
                        t
                    }
                },
                resultTypes = listOfNotNull(
                    context.transformResultType(declaration.returnType).let {
                        if (importedName != null && it is WasmRefNullType) WasmEqRef else it
                    }
                )
            )
        context.defineFunctionType(declaration.symbol, wasmFunctionType)

        if (declaration is IrSimpleFunction) {
            if (declaration.modality == Modality.ABSTRACT) return
            if (declaration.isOverridableOrOverrides) {
                // Register function as virtual, meaning this function
                // will be stored Wasm table and could be called indirectly.
                context.registerVirtualFunction(declaration.symbol)
            }
        }

        assert(declaration == declaration.realOverrideTarget) {
            "Sanity check that $declaration is a real function that can be used in calls"
        }

        if (importedName != null) {
            // Imported functions don't have bodies. Declaring the signature:
            context.defineFunction(
                declaration.symbol,
                WasmFunction.Imported(watName, wasmFunctionType, importedName)
            )
            // TODO: Support re-export of imported functions.
            return
        }

        val function = WasmFunction.Defined(watName, wasmFunctionType)
        val functionCodegenContext = WasmFunctionCodegenContextImpl(
            declaration,
            function,
            backendContext,
            context
        )

        for (irParameter in irParameters) {
            functionCodegenContext.defineLocal(irParameter.symbol)
        }

        val exprGen = functionCodegenContext.bodyGen
        val bodyBuilder = BodyGenerator(functionCodegenContext)

        when (val body = declaration.body) {
            is IrBlockBody ->
                for (statement in body.statements) {
                    bodyBuilder.statementToWasmInstruction(statement)
                }

            is IrExpressionBody ->
                bodyBuilder.generateExpression(body.expression)

            else -> error("Unexpected body $body")
        }

        // Return implicit this from constructions to avoid extra tmp
        // variables on constructor call sites.
        // TODO: Redesign construction scheme.
        if (declaration is IrConstructor) {
            exprGen.buildGetLocal(/*implicit this*/ function.locals[0])
            exprGen.buildInstr(WasmOp.RETURN)
        }

        // Add unreachable if function returns something but not as a last instruction.
        if (wasmFunctionType.resultTypes.isNotEmpty() && declaration.body is IrBlockBody) {
            // TODO: Add unreachable only if needed
            exprGen.buildUnreachable()
        }

        context.defineFunction(declaration.symbol, function)

        if (declaration == backendContext.startFunction)
            context.setStartFunction(function)

        if (declaration.isExported(backendContext)) {
            context.addExport(
                WasmExport.Function(
                    field = function,
                    // TODO: Add ability to specify exported name.
                    name = declaration.name.identifier
                )
            )
        }
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.isAnnotationClass) return
        val symbol = declaration.symbol

        if (declaration.isInterface) {
            context.registerInterface(symbol)
        } else {
            val nameStr = declaration.fqNameWhenAvailable.toString()
            val structType = WasmStructDeclaration(
                name = nameStr,
                fields = declaration.allFields(irBuiltIns).map {
                    WasmStructFieldDeclaration(
                        name = it.name.toString(),
                        type = context.transformType(it.type),
                        isMutable = true
                    )
                }
            )

            context.defineStructType(symbol, structType)

            var depth = 2
            val metadata = context.getClassMetadata(symbol)
            var subMetadata = metadata
            while (true) {
                subMetadata = subMetadata.superClass ?: break
                depth++
            }

            val initBody = mutableListOf<WasmInstr>()
            val wasmExpressionGenerator = WasmIrExpressionBuilder(initBody)

            val superClass = metadata.superClass
            if (superClass != null) {
                val superRTT = context.referenceClassRTT(superClass.klass.symbol)
                wasmExpressionGenerator.buildGetGlobal(superRTT)
            } else {
                wasmExpressionGenerator.buildRttCanon(WasmRefType(WasmHeapType.Simple.Eq))
            }

            wasmExpressionGenerator.buildRttSub(
                WasmRefType(WasmHeapType.Type(WasmSymbol(structType)))
            )

            val rtt = WasmGlobal(
                name = "rtt_of_$nameStr",
                isMutable = false,
                type = WasmRtt(depth, WasmHeapType.Type(WasmSymbol(structType))),
                init = initBody
            )

            context.defineRTT(symbol, rtt)
            context.registerClass(symbol)
            context.generateTypeInfo(symbol, binaryDataStruct(metadata))
        }

        for (member in declaration.declarations) {
            member.acceptVoid(this)
        }
    }

    private fun binaryDataStruct(classMetadata: ClassMetadata): ConstantDataStruct {
        val invalidIndex = -1
        val superClass = classMetadata.superClass?.klass

        val superClassSymbol: WasmSymbol<Int> =
            superClass?.let { context.referenceClassId(it.symbol) } ?: WasmSymbol(invalidIndex)

        val superTypeField =
            ConstantDataIntField("Super class", superClassSymbol)

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

        val vtableArray = ConstantDataIntArray(
            "V-table",
            classMetadata.virtualMethods.map {
                if (it.function.modality == Modality.ABSTRACT) {
                    WasmSymbol(invalidIndex)
                } else {
                    context.referenceVirtualFunctionId(it.function.symbol)
                }
            }
        )

        val signaturesArray = ConstantDataIntArray(
            "Signatures",
            classMetadata.virtualMethods.map {
                if (it.function.modality == Modality.ABSTRACT) {
                    WasmSymbol(invalidIndex)
                } else {
                    context.referenceSignatureId(it.signature)
                }
            }
        )

        return ConstantDataStruct(
            "Class TypeInfo: ${classMetadata.klass.fqNameWhenAvailable} ",
            listOf(
                superTypeField,
                vtableSizeField,
                vtableArray,
                signaturesArray,
                implementedInterfacesArrayWithSize,
            )
        )
    }

    override fun visitField(declaration: IrField) {
        // Member fields are generated as part of struct type
        if (!declaration.isStatic) return

        val wasmType = context.transformType(declaration.type)

        val initBody = mutableListOf<WasmInstr>()
        val wasmExpressionGenerator = WasmIrExpressionBuilder(initBody)
        generateDefaultInitializerForType(wasmType, wasmExpressionGenerator)

        val global = WasmGlobal(
            name = declaration.fqNameWhenAvailable.toString(),
            type = wasmType,
            isMutable = true,
            // All globals are currently initialized in start function
            init = initBody
        )

        context.defineGlobal(declaration.symbol, global)
    }
}


fun generateDefaultInitializerForType(type: WasmType, g: WasmExpressionBuilder) = when (type) {
    WasmI32 -> g.buildConstI32(0)
    WasmI1 -> g.buildConstI32(0)
    WasmI64 -> g.buildConstI64(0)
    WasmF32 -> g.buildConstF32(0f)
    WasmF64 -> g.buildConstF64(0.0)
    is WasmRefNullType -> g.buildRefNull(type.heapType)
    is WasmExternRef -> g.buildRefNull(WasmHeapType.Simple.Extern)
    WasmUnreachableType -> error("Unreachable type can't be initialized")
    else -> error("Unknown value type ${type.name}")
}

fun IrFunction.getEffectiveValueParameters(): List<IrValueParameter> {
    val implicitThis = if (this is IrConstructor) parentAsClass.thisReceiver!! else null
    return listOfNotNull(implicitThis, dispatchReceiverParameter, extensionReceiverParameter) + valueParameters
}

fun IrFunction.isExported(context: WasmBackendContext): Boolean =
    visibility == DescriptorVisibilities.PUBLIC && fqNameWhenAvailable in context.additionalExportedDeclarations
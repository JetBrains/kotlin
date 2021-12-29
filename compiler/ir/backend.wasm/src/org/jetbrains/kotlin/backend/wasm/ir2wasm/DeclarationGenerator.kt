/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.common.ir.isOverridableOrOverrides
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.lower.wasmSignature
import org.jetbrains.kotlin.backend.wasm.utils.*
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.isJsExport
import org.jetbrains.kotlin.ir.backend.js.utils.findUnitGetInstanceFunction
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.wasm.ir.*

class DeclarationGenerator(val context: WasmModuleCodegenContext, private val allowIncompleteImplementations: Boolean) : IrElementVisitorVoid {

    // Shortcuts
    private val backendContext: WasmBackendContext = context.backendContext
    private val irBuiltIns: IrBuiltIns = backendContext.irBuiltIns

    private val unitGetInstanceFunction: IrSimpleFunction by lazy { backendContext.findUnitGetInstanceFunction() }

    override fun visitElement(element: IrElement) {
        error("Unexpected element of type ${element::class}")
    }

    override fun visitProperty(declaration: IrProperty) {
        require(declaration.isExternal)
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

        val isIntrinsic = declaration.hasWasmNoOpCastAnnotation() || declaration.getWasmOpAnnotation() != null
        if (isIntrinsic) {
            return
        }

        val jsCode = declaration.getJsFunAnnotation() ?: if (declaration.isExternal) declaration.name.asString() else null
        val importedName = if (jsCode != null) {
            val jsCodeName = jsCodeName(declaration)
            context.addJsFun(jsCodeName, jsCode)
            WasmImportPair("js_code", jsCodeName(declaration))
        } else {
            declaration.getWasmImportAnnotation()
        }


        if (declaration.isFakeOverride)
            return

        // Generate function type
        val watName = declaration.fqNameWhenAvailable.toString()
        val irParameters = declaration.getEffectiveValueParameters()
        val resultType =
            when {
                // Unit_getInstance returns true Unit reference instead of "void"
                declaration == unitGetInstanceFunction -> context.transformType(declaration.returnType)
                else -> context.transformResultType(declaration.returnType)
            }

        val wasmFunctionType =
            WasmFunctionType(
                name = watName,
                parameterTypes = irParameters.map { context.transformValueParameterType(it) },
                resultTypes = listOfNotNull(resultType)
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

        require(declaration.body is IrBlockBody) { "Only IrBlockBody is supported" }
        declaration.body?.acceptVoid(bodyBuilder)

        // Return implicit this from constructions to avoid extra tmp
        // variables on constructor call sites.
        // TODO: Redesign construction scheme.
        if (declaration is IrConstructor) {
            exprGen.buildGetLocal(/*implicit this*/ function.locals[0])
            exprGen.buildInstr(WasmOp.RETURN)
        }

        // Add unreachable if function returns something but not as a last instruction.
        // We can do a separate lowering which adds explicit returns everywhere instead.
        if (wasmFunctionType.resultTypes.isNotEmpty()) {
            exprGen.buildUnreachable()
        }

        context.defineFunction(declaration.symbol, function)

        val initPriority = when (declaration) {
            backendContext.fieldInitFunction -> "0"
            backendContext.mainCallsWrapperFunction -> "1"
            else -> null
        }
        if (initPriority != null)
            context.registerInitFunction(function, initPriority)

        if (declaration.isExported()) {
            context.addExport(
                WasmExport.Function(
                    field = function,
                    name = declaration.getJsNameOrKotlinName().identifier
                )
            )
        }
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.isAnnotationClass) return
        if (declaration.isExternal) return
        val symbol = declaration.symbol


        // Handle arrays
        declaration.getWasmArrayAnnotation()?.let { wasmArrayAnnotation ->
            val nameStr = declaration.fqNameWhenAvailable.toString()
            val wasmArrayDeclaration = WasmArrayDeclaration(
                nameStr,
                WasmStructFieldDeclaration(
                    name = "field",
                    type = context.transformFieldType(wasmArrayAnnotation.type),
                    isMutable = true
                )
            )

            context.defineGcType(symbol, wasmArrayDeclaration)
            return
        }

        if (declaration.isInterface) {
            val metadata = InterfaceMetadata(declaration, irBuiltIns)
            for (method in metadata.methods) {
                val methodSymbol = method.function.symbol
                val table = WasmTable(
                    elementType = WasmRefNullType(WasmHeapType.Type(context.referenceFunctionType(methodSymbol)))
                )
                context.defineInterfaceMethodTable(methodSymbol, table)
            }
            context.registerInterface(symbol)
        } else {
            val nameStr = declaration.fqNameWhenAvailable.toString()
            val structType = WasmStructDeclaration(
                name = nameStr,
                fields = declaration.allFields(irBuiltIns).map {
                    WasmStructFieldDeclaration(
                        name = it.name.toString(),
                        type = context.transformFieldType(it.type),
                        isMutable = true
                    )
                }
            )

            context.defineGcType(symbol, structType)

            var depth = 0
            val metadata = context.getClassMetadata(symbol)
            var subMetadata = metadata
            while (true) {
                subMetadata = subMetadata.superClass ?: break
                depth++
            }

            val initBody = mutableListOf<WasmInstr>()
            val wasmExpressionGenerator = WasmIrExpressionBuilder(initBody)

            val wasmGcType = context.referenceGcType(symbol)
            val superClass = metadata.superClass
            if (superClass != null) {
                val superRTT = context.referenceClassRTT(superClass.klass.symbol)
                wasmExpressionGenerator.buildGetGlobal(superRTT)
                wasmExpressionGenerator.buildRttSub(wasmGcType)
            } else {
                wasmExpressionGenerator.buildRttCanon(wasmGcType)
            }

            val rtt = WasmGlobal(
                name = "rtt_of_$nameStr",
                isMutable = false,
                type = WasmRtt(depth, WasmSymbol(structType)),
                init = initBody
            )

            context.defineRTT(symbol, rtt)
            context.registerClass(symbol)
            context.generateTypeInfo(symbol, binaryDataStruct(metadata))

            // New type info model
            if (declaration.modality != Modality.ABSTRACT) {
                context.generateInterfaceTable(symbol, interfaceTable(metadata))
                for (i in metadata.interfaces) {
                    val interfaceImplementation = InterfaceImplementation(i.symbol, declaration.symbol)
                    // TODO: Cache it
                    val interfaceMetadata = InterfaceMetadata(i, irBuiltIns)
                    val table = interfaceMetadata.methods.associate { method ->
                        val classMethod: VirtualMethodMetadata? = metadata.virtualMethods
                            .find { it.signature == method.signature && it.function.modality != Modality.ABSTRACT }  // TODO: Use map

                        if (classMethod == null && !allowIncompleteImplementations) {
                            error("Cannot find class implementation of method ${method.signature} in class ${declaration.fqNameWhenAvailable}")
                        }
                        val matchedMethod = classMethod?.let { context.referenceFunction(it.function.symbol) }
                        method.function.symbol as IrFunctionSymbol to matchedMethod
                    }

                    context.registerInterfaceImplementationMethod(
                        interfaceImplementation,
                        table
                    )
                }
            }
        }

        for (member in declaration.declarations) {
            member.acceptVoid(this)
        }
    }

    private fun binaryDataStruct(classMetadata: ClassMetadata): ConstantDataStruct {
        val invalidIndex = -1

        val fqnShouldBeEmitted = context.backendContext.configuration.languageVersionSettings.getFlag(allowFullyQualifiedNameInKClass)
        //TODO("FqName for inner classes could be invalid due to topping it out from outer class")
        val packageName = if (fqnShouldBeEmitted) classMetadata.klass.kotlinFqName.parentOrNull()?.asString() ?: "" else ""
        val simpleName = classMetadata.klass.kotlinFqName.shortName().asString()
        val typeInfo = ConstantDataStruct(
            "TypeInfo",
            listOf(
                ConstantDataIntField("TypePackageNameLength", packageName.length),
                ConstantDataIntField("TypePackageNamePtr", context.referenceStringLiteral(packageName)),
                ConstantDataIntField("TypeNameLength", simpleName.length),
                ConstantDataIntField("TypeNamePtr", context.referenceStringLiteral(simpleName))
            )
        )

        val superClass = classMetadata.klass.getSuperClass(context.backendContext.irBuiltIns)
        val superTypeId = superClass?.let {
            ConstantDataIntField("SuperTypeId", context.referenceClassId(it.symbol))
        } ?: ConstantDataIntField("SuperTypeId", -1)

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

        val interfaceTablePtr = ConstantDataIntField(
            "interfaceTablePtr",
            context.referenceInterfaceTableAddress(classMetadata.klass.symbol)
        )

        return ConstantDataStruct(
            "Class TypeInfo: ${classMetadata.klass.fqNameWhenAvailable} ",
            listOf(
                typeInfo,
                superTypeId,
                interfaceTablePtr,
                vtableSizeField,
                vtableArray,
            )
        )
    }


    private fun interfaceTable(classMetadata: ClassMetadata): ConstantDataStruct {
        val interfaces = classMetadata.interfaces
        val size = ConstantDataIntField("size", interfaces.size)
        val interfaceIds = ConstantDataIntArray(
            "interfaceIds",
            interfaces.map { context.referenceInterfaceId(it.symbol) },
        )
        val interfaceImplementationIds = ConstantDataIntArray(
            "interfaceImplementationId",
            interfaces.map {
                context.referenceInterfaceImplementationId(InterfaceImplementation(it.symbol, classMetadata.klass.symbol))
            },
        )

        return ConstantDataStruct(
            "Class interface table: ${classMetadata.klass.fqNameWhenAvailable} ",
            listOf(
                size,
                interfaceIds,
                interfaceImplementationIds,
            )
        )
    }


    override fun visitField(declaration: IrField) {
        // Member fields are generated as part of struct type
        if (!declaration.isStatic) return

        val wasmType = context.transformType(declaration.type)

        val initBody = mutableListOf<WasmInstr>()
        val wasmExpressionGenerator = WasmIrExpressionBuilder(initBody)

        val initValue: IrExpression? = declaration.initializer?.expression
        if (initValue != null) {
            check(initValue is IrConst<*> && initValue.kind !is IrConstKind.String && initValue.kind !is IrConstKind.Null) {
                "Static field initializer should be non-string const or null"
            }
            generateConstExpression(initValue, wasmExpressionGenerator, context)
        } else {
            generateDefaultInitializerForType(wasmType, wasmExpressionGenerator)
        }

        val global = WasmGlobal(
            name = declaration.fqNameWhenAvailable.toString(),
            type = wasmType,
            isMutable = true,
            init = initBody
        )

        context.defineGlobal(declaration.symbol, global)
    }
}


fun generateDefaultInitializerForType(type: WasmType, g: WasmExpressionBuilder) = when (type) {
    WasmI32 -> g.buildConstI32(0)
    WasmI64 -> g.buildConstI64(0)
    WasmF32 -> g.buildConstF32(0f)
    WasmF64 -> g.buildConstF64(0.0)
    is WasmRefNullType -> g.buildRefNull(type.heapType)
    is WasmExternRef, is WasmAnyRef -> g.buildRefNull(WasmHeapType.Simple.Extern)
    WasmUnreachableType -> error("Unreachable type can't be initialized")
    else -> error("Unknown value type ${type.name}")
}

fun IrFunction.getEffectiveValueParameters(): List<IrValueParameter> {
    val implicitThis = if (this is IrConstructor) parentAsClass.thisReceiver!! else null
    return listOfNotNull(implicitThis, dispatchReceiverParameter, extensionReceiverParameter) + valueParameters
}

fun IrFunction.isExported(): Boolean =
    isJsExport()


fun generateConstExpression(expression: IrConst<*>, body: WasmExpressionBuilder, context: WasmBaseCodegenContext) {
    when (val kind = expression.kind) {
        is IrConstKind.Null -> generateDefaultInitializerForType(context.transformType(expression.type), body)
        is IrConstKind.Boolean -> body.buildConstI32(if (kind.valueOf(expression)) 1 else 0)
        is IrConstKind.Byte -> body.buildConstI32(kind.valueOf(expression).toInt())
        is IrConstKind.Short -> body.buildConstI32(kind.valueOf(expression).toInt())
        is IrConstKind.Int -> body.buildConstI32(kind.valueOf(expression))
        is IrConstKind.Long -> body.buildConstI64(kind.valueOf(expression))
        is IrConstKind.Char -> body.buildConstI32(kind.valueOf(expression).code)
        is IrConstKind.Float -> body.buildConstF32(kind.valueOf(expression))
        is IrConstKind.Double -> body.buildConstF64(kind.valueOf(expression))
        is IrConstKind.String -> {
            body.buildConstI32Symbol(context.referenceStringLiteral(kind.valueOf(expression)))
            body.buildConstI32(kind.valueOf(expression).length)
            body.buildCall(context.referenceFunction(context.backendContext.wasmSymbols.stringGetLiteral))
        }
        else -> error("Unknown constant kind")
    }
}
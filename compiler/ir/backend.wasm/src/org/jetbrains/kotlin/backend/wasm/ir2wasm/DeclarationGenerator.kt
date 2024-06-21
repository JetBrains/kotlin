/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.*
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

class DeclarationGenerator(
    private val backendContext: WasmBackendContext,
    private val wasmFileCodegenContext: WasmFileCodegenContext,
    private val wasmModuleTypeTransformer: WasmModuleTypeTransformer,
    private val wasmModuleMetadataCache: WasmModuleMetadataCache,
    private val allowIncompleteImplementations: Boolean,
) : IrElementVisitorVoid {

    // Shortcuts
    private val irBuiltIns: IrBuiltIns = backendContext.irBuiltIns

    private val unitGetInstanceFunction: IrSimpleFunction by lazy { backendContext.findUnitGetInstanceFunction() }
    private val unitPrimaryConstructor: IrConstructor? by lazy { backendContext.irBuiltIns.unitClass.owner.primaryConstructor }

    override fun visitElement(element: IrElement) {
        error("Unexpected element of type ${element::class}")
    }

    override fun visitProperty(declaration: IrProperty) {
        require(declaration.isExternal)
    }

    override fun visitTypeAlias(declaration: IrTypeAlias) {
        // Type aliases are not material
    }

    override fun visitFunction(declaration: IrFunction) {
        // Inline class constructors are currently empty
        if (declaration is IrConstructor && backendContext.inlineClassesUtils.isClassInlineLike(declaration.parentAsClass))
            return

        val isIntrinsic = declaration.hasWasmNoOpCastAnnotation() || declaration.getWasmOpAnnotation() != null
        if (isIntrinsic) {
            return
        }

        val wasmImportModule = declaration.getWasmImportDescriptor()
        val jsCode = declaration.getJsFunAnnotation()
        val importedName = when {
            wasmImportModule != null -> {
                check(declaration.isExternal) { "Non-external fun with @WasmImport ${declaration.fqNameWhenAvailable}"}
                wasmFileCodegenContext.addJsModuleImport(wasmImportModule.moduleName)
                wasmImportModule
            }
            jsCode != null -> {
                // check(declaration.isExternal) { "Non-external fun with @JsFun ${declaration.fqNameWhenAvailable}"}
                require(declaration is IrSimpleFunction)
                val uniqueJsFunName = wasmFileCodegenContext.referenceUniqueJsFunName(declaration.fqNameWhenAvailable.toString())
                wasmFileCodegenContext.addJsFun(uniqueJsFunName, jsCode)
                WasmImportDescriptor("js_code", uniqueJsFunName)
            }
            else -> {
                null
            }
        }

        if (declaration.isFakeOverride)
            return

        // Generate function type
        val watName = declaration.fqNameWhenAvailable.toString()
        val irParameters = declaration.getEffectiveValueParameters()
        val resultType = when (declaration) {
            // Unit_getInstance returns true Unit reference instead of "void"
            unitGetInstanceFunction, unitPrimaryConstructor -> wasmModuleTypeTransformer.transformType(declaration.returnType)
            else -> wasmModuleTypeTransformer.transformResultType(declaration.returnType)
        }

        val wasmFunctionType =
            WasmFunctionType(
                parameterTypes = irParameters.map { wasmModuleTypeTransformer.transformValueParameterType(it) },
                resultTypes = listOfNotNull(resultType)
            )
        wasmFileCodegenContext.defineFunctionType(declaration.symbol, wasmFunctionType)

        if (declaration is IrSimpleFunction && declaration.modality == Modality.ABSTRACT) {
            return
        }

        assert(declaration == declaration.realOverrideTarget) {
            "Sanity check that $declaration is a real function that can be used in calls"
        }

        val functionTypeSymbol = wasmFileCodegenContext.referenceFunctionType(declaration.symbol)

        if (importedName != null) {
            // Imported functions don't have bodies. Declaring the signature:
            wasmFileCodegenContext.defineFunction(
                declaration.symbol,
                WasmFunction.Imported(watName, functionTypeSymbol, importedName)
            )
            // TODO: Support re-export of imported functions.
            return
        }

        val function = WasmFunction.Defined(watName, functionTypeSymbol)
        val functionCodegenContext = WasmFunctionCodegenContext(
            declaration,
            function,
            backendContext,
            wasmFileCodegenContext,
            wasmModuleTypeTransformer
        )

        for (irParameter in irParameters) {
            functionCodegenContext.defineLocal(irParameter.symbol)
        }

        val exprGen = functionCodegenContext.bodyGen
        val bodyBuilder = BodyGenerator(
            backendContext,
            wasmFileCodegenContext,
            functionCodegenContext,
            wasmModuleMetadataCache,
            wasmModuleTypeTransformer,
        )

        if (declaration is IrConstructor) {
            bodyBuilder.generateObjectCreationPrefixIfNeeded(declaration)
        }

        require(declaration.body is IrBlockBody) { "Only IrBlockBody is supported" }
        declaration.body?.acceptVoid(bodyBuilder)

        // Return implicit this from constructions to avoid extra tmp
        // variables on constructor call sites.
        // TODO: Redesign construction scheme.
        if (declaration is IrConstructor) {
            exprGen.buildGetLocal(/*implicit this*/ function.locals[0], SourceLocation.NoLocation("Get implicit dispatch receiver"))
            exprGen.buildInstr(WasmOp.RETURN, SourceLocation.NoLocation("Implicit return from constructor"))
        }

        // Add unreachable if function returns something but not as a last instruction.
        // We can do a separate lowering which adds explicit returns everywhere instead.
        if (wasmFunctionType.resultTypes.isNotEmpty()) {
            exprGen.buildUnreachableForVerifier()
        }

        wasmFileCodegenContext.defineFunction(declaration.symbol, function)

        val nameIfExported = when {
            declaration.isJsExport() -> declaration.getJsNameOrKotlinName().identifier
            else -> declaration.getWasmExportNameIfWasmExport()
        }

        if (nameIfExported != null) {
            wasmFileCodegenContext.addExport(
                WasmExport.Function(
                    field = function,
                    name = nameIfExported
                )
            )
        }
    }

    private fun createVirtualTableStruct(
        methods: List<VirtualMethodMetadata>,
        name: String,
        superType: WasmSymbolReadOnly<WasmTypeDeclaration>? = null,
        isFinal: Boolean,
    ): WasmStructDeclaration {
        val tableFields = methods.map {
            WasmStructFieldDeclaration(
                name = it.signature.name.asString(),
                type = WasmRefNullType(WasmHeapType.Type(wasmFileCodegenContext.referenceFunctionType(it.function.symbol))),
                isMutable = false
            )
        }

        return WasmStructDeclaration(
            name = name,
            fields = tableFields,
            superType = superType,
            isFinal = isFinal
        )
    }

    private fun createVTable(metadata: ClassMetadata) {
        val klass = metadata.klass
        val symbol = klass.symbol
        val vtableName = "${klass.fqNameWhenAvailable}.vtable"
        val vtableStruct = createVirtualTableStruct(
            metadata.virtualMethods,
            vtableName,
            superType = metadata.superClass?.klass?.symbol?.let(wasmFileCodegenContext::referenceVTableGcType),
            isFinal = klass.modality == Modality.FINAL
        )
        wasmFileCodegenContext.defineVTableGcType(metadata.klass.symbol, vtableStruct)

        if (klass.isAbstractOrSealed) return

        val vTableTypeReference = wasmFileCodegenContext.referenceVTableGcType(symbol)
        val vTableRefGcType = WasmRefType(WasmHeapType.Type(vTableTypeReference))

        val initVTableGlobal = buildWasmExpression {
            val location = SourceLocation.NoLocation("Create instance of vtable struct")
            metadata.virtualMethods.forEachIndexed { i, method ->
                if (method.function.modality != Modality.ABSTRACT) {
                    buildInstr(WasmOp.REF_FUNC, location, WasmImmediate.FuncIdx(wasmFileCodegenContext.referenceFunction(method.function.symbol)))
                } else {
                    check(allowIncompleteImplementations) {
                        "Cannot find class implementation of method ${method.signature} in class ${klass.fqNameWhenAvailable}"
                    }
                    //This erased by DCE so abstract version appeared in non-abstract class
                    buildRefNull(vtableStruct.fields[i].type.getHeapType(), location)
                }
            }
            buildStructNew(vTableTypeReference, location)
        }
        wasmFileCodegenContext.defineGlobalVTable(
            irClass = symbol,
            wasmGlobal = WasmGlobal(vtableName, vTableRefGcType, false, initVTableGlobal)
        )
    }

    private fun addClassInterfaceInheritanceStructure(klass: IrClass) {
        if (klass.isExternal) return
        if (klass.getWasmArrayAnnotation() != null) return
        if (klass.isInterface) return
        if (klass.isAbstractOrSealed) return

        val classMetadata = wasmModuleMetadataCache.getClassMetadata(klass.symbol)
        if (classMetadata.interfaces.isNotEmpty()) {
            wasmFileCodegenContext.addInterfaceUnion(classMetadata.interfaces.map { it.symbol })
        }
    }

    private fun createClassITable(metadata: ClassMetadata) {
        val location = SourceLocation.NoLocation("Create instance of itable struct")
        val klass = metadata.klass
        if (klass.isAbstractOrSealed) return
        val supportedInterface = metadata.interfaces.firstOrNull()?.symbol ?: return

        addClassInterfaceInheritanceStructure(klass)

        val classInterfaceType = wasmFileCodegenContext.referenceClassITableGcType(supportedInterface)

        val initITableGlobal = buildWasmExpression {
            buildInstr(WasmOp.MACRO_TABLE, location, WasmImmediate.SymbolI32(wasmFileCodegenContext.referenceClassITableInterfaceTableSize(supportedInterface)))
            for (iFace in metadata.interfaces) {
                buildInstr(WasmOp.MACRO_TABLE_INDEX, location, WasmImmediate.SymbolI32(wasmFileCodegenContext.referenceClassITableInterfaceSlot(iFace.symbol)))

                val iFaceVTableGcType = wasmFileCodegenContext.referenceVTableGcType(iFace.symbol)

                for (method in wasmModuleMetadataCache.getInterfaceMetadata(iFace.symbol).methods) {
                    val classMethod: VirtualMethodMetadata? = metadata.virtualMethods
                        .find { it.signature == method.signature && it.function.modality != Modality.ABSTRACT }  // TODO: Use map

                    if (classMethod == null && !allowIncompleteImplementations && !backendContext.partialLinkageSupport.isEnabled) {
                        error("Cannot find interface implementation of method ${method.signature} in class ${klass.fqNameWhenAvailable}")
                    }

                    if (classMethod != null) {
                        val functionTypeReference = wasmFileCodegenContext.referenceFunction(classMethod.function.symbol)
                        buildInstr(WasmOp.REF_FUNC, location, WasmImmediate.FuncIdx(functionTypeReference))
                    } else {
                        //This erased by DCE so abstract version appeared in non-abstract class
                        buildRefNull(WasmHeapType.Type(wasmFileCodegenContext.referenceFunctionType(method.function.symbol)), location)
                    }
                }
                buildStructNew(iFaceVTableGcType, location)
            }
            buildInstr(WasmOp.MACRO_TABLE_END, location)
            buildStructNew(classInterfaceType, location)
        }

        val wasmClassIFaceGlobal = WasmGlobal(
            name = "${klass.fqNameWhenAvailable.toString()}.classITable",
            type = WasmRefType(WasmHeapType.Type(classInterfaceType)),
            isMutable = false,
            init = initITableGlobal
        )
        wasmFileCodegenContext.defineGlobalClassITable(klass.symbol, wasmClassIFaceGlobal)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.isExternal) return
        val symbol = declaration.symbol

        // Handle arrays
        declaration.getWasmArrayAnnotation()?.let { wasmArrayAnnotation ->
            val nameStr = declaration.fqNameWhenAvailable.toString()
            val wasmArrayDeclaration = WasmArrayDeclaration(
                nameStr,
                WasmStructFieldDeclaration(
                    name = "field",
                    type = wasmModuleTypeTransformer.transformFieldType(wasmArrayAnnotation.type),
                    isMutable = true
                )
            )

            wasmFileCodegenContext.defineGcType(symbol, wasmArrayDeclaration)
            return
        }

        val nameStr = declaration.fqNameWhenAvailable.toString()

        if (declaration.isInterface) {
            val vtableStruct = createVirtualTableStruct(
                methods = wasmModuleMetadataCache.getInterfaceMetadata(symbol).methods,
                name = "$nameStr.itable",
                isFinal = true,
            )
            wasmFileCodegenContext.defineVTableGcType(symbol, vtableStruct)
        } else {
            val metadata = wasmModuleMetadataCache.getClassMetadata(symbol)

            createVTable(metadata)
            createClassITable(metadata)

            val vtableRefGcType = WasmRefType(WasmHeapType.Type(wasmFileCodegenContext.referenceVTableGcType(symbol)))
            val classITableRefGcType = WasmRefNullType(WasmHeapType.Simple.Struct)
            val fields = mutableListOf<WasmStructFieldDeclaration>()
            fields.add(WasmStructFieldDeclaration("vtable", vtableRefGcType, false))
            fields.add(WasmStructFieldDeclaration("itable", classITableRefGcType, false))
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
                superType = superClass?.let { wasmFileCodegenContext.referenceGcType(superClass.klass.symbol) },
                isFinal = declaration.modality == Modality.FINAL
            )
            wasmFileCodegenContext.defineGcType(symbol, structType)
            wasmFileCodegenContext.generateTypeInfo(symbol, binaryDataStruct(metadata))
        }

        for (member in declaration.declarations) {
            member.acceptVoid(this)
        }
    }

    private fun binaryDataStruct(classMetadata: ClassMetadata): ConstantDataStruct {
        val fqnShouldBeEmitted = backendContext.configuration.languageVersionSettings.getFlag(allowFullyQualifiedNameInKClass)
        //TODO("FqName for inner classes could be invalid due to topping it out from outer class")
        val packageName = if (fqnShouldBeEmitted) classMetadata.klass.kotlinFqName.parentOrNull()?.asString() ?: "" else ""
        val simpleName = classMetadata.klass.kotlinFqName.shortName().asString()

        val (packageNameAddress, packageNamePoolId) = wasmFileCodegenContext.referenceStringLiteralAddressAndId(packageName)
        val (simpleNameAddress, simpleNamePoolId) = wasmFileCodegenContext.referenceStringLiteralAddressAndId(simpleName)

        val typeInfo = ConstantDataStruct(
            name = "TypeInfo",
            elements = listOf(
                ConstantDataIntField("TypePackageNameLength", packageName.length),
                ConstantDataIntField("TypePackageNameId", packageNamePoolId),
                ConstantDataIntField("TypePackageNamePtr", packageNameAddress),
                ConstantDataIntField("TypeNameLength", simpleName.length),
                ConstantDataIntField("TypeNameId", simpleNamePoolId),
                ConstantDataIntField("TypeNamePtr", simpleNameAddress)
            )
        )

        val superClass = classMetadata.klass.getSuperClass(backendContext.irBuiltIns)
        val superTypeId = superClass?.let {
            ConstantDataIntField("SuperTypeId", wasmFileCodegenContext.referenceTypeId(it.symbol))
        } ?: ConstantDataIntField("SuperTypeId", -1)

        val typeInfoContent = mutableListOf(typeInfo, superTypeId)
        if (!classMetadata.klass.isAbstractOrSealed) {
            typeInfoContent.add(interfaceTable(classMetadata))
        }

        return ConstantDataStruct(
            name = "Class TypeInfo: ${classMetadata.klass.fqNameWhenAvailable} ",
            elements = typeInfoContent
        )
    }

    private fun interfaceTable(classMetadata: ClassMetadata): ConstantDataStruct {
        val interfaces = classMetadata.interfaces
        val size = ConstantDataIntField("size", interfaces.size)
        val interfaceIds = ConstantDataIntArray(
            "interfaceIds",
            interfaces.map { wasmFileCodegenContext.referenceTypeId(it.symbol) },
        )

        return ConstantDataStruct(
            name = "Class interface table: ${classMetadata.klass.fqNameWhenAvailable} ",
            elements = listOf(size, interfaceIds)
        )
    }


    override fun visitField(declaration: IrField) {
        // Member fields are generated as part of struct type
        if (!declaration.isStatic) return

        val wasmType = wasmModuleTypeTransformer.transformType(declaration.type)

        val initBody = mutableListOf<WasmInstr>()
        val wasmExpressionGenerator = WasmIrExpressionBuilder(initBody)

        val initValue: IrExpression? = declaration.initializer?.expression
        if (initValue != null) {
            if (initValue is IrConst<*> && initValue.kind !is IrConstKind.String) {
                generateConstExpression(
                    initValue,
                    wasmExpressionGenerator,
                    wasmFileCodegenContext,
                    backendContext,
                    declaration.getSourceLocation(declaration.fileOrNull)
                )
            } else {
                val stubFunction = WasmFunction.Defined("static_fun_stub", WasmSymbol())
                val functionCodegenContext = WasmFunctionCodegenContext(
                    null,
                    stubFunction,
                    backendContext,
                    wasmFileCodegenContext,
                    wasmModuleTypeTransformer
                )
                val bodyGenerator = BodyGenerator(
                    backendContext,
                    wasmFileCodegenContext,
                    functionCodegenContext,
                    wasmModuleMetadataCache,
                    wasmModuleTypeTransformer,
                )
                bodyGenerator.generateExpression(initValue)
                wasmFileCodegenContext.addFieldInitializer(declaration.symbol, stubFunction.instructions)
                generateDefaultInitializerForType(wasmType, wasmExpressionGenerator)
            }
        } else {
            generateDefaultInitializerForType(wasmType, wasmExpressionGenerator)
        }

        val global = WasmGlobal(
            name = declaration.fqNameWhenAvailable.toString(),
            type = wasmType,
            isMutable = true,
            init = initBody
        )

        wasmFileCodegenContext.defineGlobalField(declaration.symbol, global)
    }
}

fun generateDefaultInitializerForType(type: WasmType, g: WasmExpressionBuilder) =
    SourceLocation.NoLocation("Default initializer, usually don't require location").let { location ->
        when (type) {
            WasmI32 -> g.buildConstI32(0, location)
            WasmI64 -> g.buildConstI64(0, location)
            WasmF32 -> g.buildConstF32(0f, location)
            WasmF64 -> g.buildConstF64(0.0, location)
            is WasmRefNullType -> g.buildRefNull(type.heapType, location)
            is WasmRefNullrefType -> g.buildRefNull(WasmHeapType.Simple.None, location)
            is WasmRefNullExternrefType -> g.buildRefNull(WasmHeapType.Simple.NoExtern, location)
            is WasmAnyRef -> g.buildRefNull(WasmHeapType.Simple.Any, location)
            is WasmExternRef -> g.buildRefNull(WasmHeapType.Simple.Extern, location)
            WasmUnreachableType -> error("Unreachable type can't be initialized")
            else -> error("Unknown value type ${type.name}")
        }
    }

fun IrFunction.getEffectiveValueParameters(): List<IrValueParameter> {
    val implicitThis = if (this is IrConstructor) parentAsClass.thisReceiver!! else null
    return listOfNotNull(implicitThis, dispatchReceiverParameter, extensionReceiverParameter) + valueParameters
}

fun IrFunction.isExported(): Boolean =
    isJsExport() || getWasmExportNameIfWasmExport() != null

fun generateConstExpression(
    expression: IrConst<*>,
    body: WasmExpressionBuilder,
    context: WasmFileCodegenContext,
    backendContext: WasmBackendContext,
    location: SourceLocation
) =
    when (val kind = expression.kind) {
        is IrConstKind.Null -> {
            val isExternal = expression.type.getClass()?.isExternal ?: expression.type.erasedUpperBound?.isExternal
            val bottomType = if (isExternal == true) WasmRefNullExternrefType else WasmRefNullrefType
            body.buildInstr(WasmOp.REF_NULL, location, WasmImmediate.HeapType(bottomType))
        }
        is IrConstKind.Boolean -> body.buildConstI32(if (kind.valueOf(expression)) 1 else 0, location)
        is IrConstKind.Byte -> body.buildConstI32(kind.valueOf(expression).toInt(), location)
        is IrConstKind.Short -> body.buildConstI32(kind.valueOf(expression).toInt(), location)
        is IrConstKind.Int -> body.buildConstI32(kind.valueOf(expression), location)
        is IrConstKind.Long -> body.buildConstI64(kind.valueOf(expression), location)
        is IrConstKind.Char -> body.buildConstI32(kind.valueOf(expression).code, location)
        is IrConstKind.Float -> body.buildConstF32(kind.valueOf(expression), location)
        is IrConstKind.Double -> body.buildConstF64(kind.valueOf(expression), location)
        is IrConstKind.String -> {
            val stringValue = kind.valueOf(expression)
            val (literalAddress, literalPoolId) = context.referenceStringLiteralAddressAndId(stringValue)
            body.commentGroupStart { "const string: \"$stringValue\"" }
            body.buildConstI32Symbol(literalPoolId, location)
            body.buildConstI32Symbol(literalAddress, location)
            body.buildConstI32(stringValue.length, location)
            body.buildCall(context.referenceFunction(backendContext.wasmSymbols.stringGetLiteral), location)
            body.commentGroupEnd()
        }
        else -> error("Unknown constant kind")
    }

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
import org.jetbrains.kotlin.ir.backend.js.lower.CallableReferenceLowering
import org.jetbrains.kotlin.ir.backend.js.lower.originalFqName
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
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
    private val skipCommentInstructions: Boolean,
    private val useStringPool: Boolean = true,
    private val inlineUnitGetter: Boolean = true,
) : IrVisitorVoid() {

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
        // Constructor of inline class or with `@WasmPrimitiveConstructor` is empty
        if (declaration is IrConstructor &&
            (backendContext.inlineClassesUtils.isClassInlineLike(declaration.parentAsClass) || declaration.hasWasmPrimitiveConstructorAnnotation())
        ) {
            return
        }

        val isIntrinsic = declaration.hasWasmNoOpCastAnnotation() || declaration.getWasmOpAnnotation() != null
        if (isIntrinsic) {
            return
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

        if (wasmFileCodegenContext.handleFunctionWithImport(declaration.symbol)) return

        val functionTypeSymbol = wasmFileCodegenContext.referenceFunctionType(declaration.symbol)
        val wasmImportModule = declaration.getWasmImportDescriptor()
        val jsCode = declaration.getJsFunAnnotation()

        val importedName = when {
            wasmImportModule != null -> {
                check(declaration.isExternal) { "Non-external fun with @WasmImport ${declaration.fqNameWhenAvailable}"}
                wasmFileCodegenContext.addJsModuleImport(declaration.symbol, wasmImportModule.moduleName)
                wasmImportModule
            }
            jsCode != null -> {
                // check(declaration.isExternal) { "Non-external fun with @JsFun ${declaration.fqNameWhenAvailable}"}
                require(declaration is IrSimpleFunction)
                val jsFunName = WasmSymbol(declaration.fqNameWhenAvailable.toString())
                wasmFileCodegenContext.addJsFun(declaration.symbol, jsFunName, jsCode)
                WasmImportDescriptor("js_code", jsFunName)
            }
            else -> {
                null
            }
        }

        if (importedName != null) {
            // Imported functions don't have bodies. Declaring the signature:
            wasmFileCodegenContext.defineFunction(
                declaration.symbol,
                WasmFunction.Imported(watName, functionTypeSymbol, importedName)
            )
            // TODO: Support re-export of imported functions.
            return
        }

        val locationTarget = declaration.locationTarget
        val functionStartLocation = locationTarget.getSourceLocation(declaration.symbol, declaration.fileOrNull)
        val functionEndLocation = locationTarget.getSourceLocation(declaration.symbol, declaration.fileOrNull, LocationType.END)

        val function = WasmFunction.Defined(
            watName,
            functionTypeSymbol,
            startLocation = functionStartLocation,
            endLocation = functionEndLocation
        )
        val functionCodegenContext = WasmFunctionCodegenContext(
            declaration,
            function,
            backendContext,
            wasmFileCodegenContext,
            wasmModuleTypeTransformer,
            skipCommentInstructions
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
            useStringPool,
            inlineUnitGetter,
        )

        val declarationBody = declaration.body
        require(declarationBody is IrBlockBody) { "Only IrBlockBody is supported" }

        if (declaration is IrConstructor) {
            bodyBuilder.generateObjectCreationPrefixIfNeeded(declaration)
        }

        declarationBody.acceptVoid(bodyBuilder)

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
        generateSpecialITableField: Boolean,
    ): WasmStructDeclaration {
        val vtableFields = mutableListOf<WasmStructFieldDeclaration>()
        if (generateSpecialITableField) {
            val specialITableField = WasmStructFieldDeclaration(
                name = "<SpecialITable>",
                type = WasmRefNullType(WasmHeapType.Type(wasmFileCodegenContext.interfaceTableTypes.specialSlotITableType)),
                isMutable = false
            )
            vtableFields.add(specialITableField)
        }

        methods.mapTo(vtableFields) {
            WasmStructFieldDeclaration(
                name = it.signature.name.asString(),
                type = WasmRefNullType(WasmHeapType.Type(wasmFileCodegenContext.referenceFunctionType(it.function.symbol))),
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

    private fun buildSpecialITableInit(metadata: ClassMetadata, builder: WasmExpressionBuilder, location: SourceLocation) {
        val klass = metadata.klass
        if (!klass.hasInterfaceSuperClass()) {
            builder.buildRefNull(WasmHeapType.Simple.None, location)
            return
        }

        val supportedIFaces = metadata.interfaces
        val specialSlotITableTypes = backendContext.specialSlotITableTypes
        val invokeFunctions = supportedIFaces.mapNotNull(::getFunctionInvokeMethod)
        val specialInterfacesIfSupported = specialSlotITableTypes.map { iFace -> iFace.takeIf { it.owner in supportedIFaces } }

        if (invokeFunctions.isEmpty() && specialInterfacesIfSupported.all { it == null }) {
            builder.buildRefNull(WasmHeapType.Simple.None, location)
            return
        }

        //Load special interfaces implementation
        for (supportedSpecialInterface in specialInterfacesIfSupported) {
            if (supportedSpecialInterface != null) {
                for (method in wasmModuleMetadataCache.getInterfaceMetadata(supportedSpecialInterface).methods) {
                    addInterfaceMethod(metadata, builder, method, location)
                }
                builder.buildStructNew(wasmFileCodegenContext.referenceVTableGcType(supportedSpecialInterface), location)
            } else {
                builder.buildRefNull(WasmHeapType.Simple.None, location)
            }
        }

        //Load functional interfaces implementation
        if (invokeFunctions.isNotEmpty()) {
            val functionsITableSize = invokeFunctions.maxOf { it.parameters.size }
            repeat(functionsITableSize) { slotIndex ->
                val currentParameterCount = slotIndex + 1
                val invokeFunction = invokeFunctions.firstOrNull { it.parameters.size == currentParameterCount }
                if (invokeFunction != null) {
                    val method = VirtualMethodMetadata(
                        function = invokeFunction,
                        signature = invokeFunction.wasmSignature(irBuiltIns),
                    )
                    addInterfaceMethod(
                        metadata = metadata,
                        builder = builder,
                        method = method,
                        location = location
                    )
                } else {
                    builder.buildRefNull(WasmHeapType.Simple.Func, location)
                }
            }
            builder.buildInstr(
                WasmOp.ARRAY_NEW_FIXED,
                location,
                WasmImmediate.GcType(wasmFileCodegenContext.interfaceTableTypes.wasmFuncArrayType),
                WasmImmediate.ConstI32(functionsITableSize)
            )
        } else {
            builder.buildRefNull(WasmHeapType.Simple.None, location)
        }
        builder.buildStructNew(wasmFileCodegenContext.interfaceTableTypes.specialSlotITableType, location)
    }

    private fun createVTable(metadata: ClassMetadata) {
        val klass = metadata.klass
        val symbol = klass.symbol

        val vtableStruct = createVirtualTableStruct(
            metadata.virtualMethods,
            "<classVTable>",
            superType = metadata.superClass?.klass?.symbol?.let(wasmFileCodegenContext::referenceVTableGcType),
            isFinal = klass.modality == Modality.FINAL,
            generateSpecialITableField = true,
        )
        wasmFileCodegenContext.defineVTableGcType(metadata.klass.symbol, vtableStruct)

        if (klass.isAbstractOrSealed) return

        if (wasmFileCodegenContext.handleVTableWithImport(symbol)) return

        val vTableTypeReference = wasmFileCodegenContext.referenceVTableGcType(symbol)
        val vTableRefGcType = WasmRefType(WasmHeapType.Type(vTableTypeReference))

        val initVTableGlobal = buildWasmExpression {
            val location = SourceLocation.NoLocation("Create instance of vtable struct")
            buildSpecialITableInit(metadata, this, location)
            metadata.virtualMethods.forEachIndexed { i, method ->
                if (method.function.modality != Modality.ABSTRACT) {
                    buildInstr(WasmOp.REF_FUNC, location, WasmImmediate.FuncIdx(wasmFileCodegenContext.referenceFunction(method.function.symbol)))
                } else {
                    check(allowIncompleteImplementations) {
                        "Cannot find class implementation of method ${method.signature} in class ${klass.fqNameWhenAvailable}"
                    }
                    //This erased by DCE so abstract version appeared in non-abstract class
                    buildRefNull(WasmHeapType.Simple.NoFunc, location)
                }
            }
            buildStructNew(vTableTypeReference, location)
        }
        wasmFileCodegenContext.defineGlobalVTable(
            irClass = symbol,
            wasmGlobal = WasmGlobal("<classVTable>", vTableRefGcType, false, initVTableGlobal)
        )
    }

    internal fun addInterfaceMethod(
        metadata: ClassMetadata,
        builder: WasmExpressionBuilder,
        method: VirtualMethodMetadata,
        location: SourceLocation
    ) {
        val klass = metadata.klass

        val classMethod: VirtualMethodMetadata? = metadata.virtualMethods
            .find { it.signature == method.signature && it.function.modality != Modality.ABSTRACT }  // TODO: Use map

        if (classMethod == null && !allowIncompleteImplementations && !backendContext.partialLinkageSupport.isEnabled) {
            error("Cannot find interface implementation of method ${method.signature} in class ${klass.fqNameWhenAvailable}")
        }

        if (classMethod != null) {
            val functionTypeReference = wasmFileCodegenContext.referenceFunction(classMethod.function.symbol)
            builder.buildInstr(WasmOp.REF_FUNC, location, WasmImmediate.FuncIdx(functionTypeReference))
        } else {
            //This erased by DCE so abstract version appeared in non-abstract class
            builder.buildRefNull(WasmHeapType.Simple.NoFunc, location)
        }
    }

    private fun createRtti(metadata: ClassMetadata) {
        val klass = metadata.klass
        val symbol = klass.symbol
        val superType = klass.getSuperClass(irBuiltIns)?.symbol

        if (wasmFileCodegenContext.handleRTTIWithImport(symbol, superType)) return

        val location = SourceLocation.NoLocation("Create instance of rtti struct")
        val initRttiGlobal = buildWasmExpression {
            interfaceTable(this, metadata, location)
            if (superType != null) {
                buildGetGlobal(wasmFileCodegenContext.referenceRttiGlobal(superType), location)
            } else {
                buildRefNull(WasmHeapType.Simple.None, location)
            }

            if (useStringPool) {
                val fqnShouldBeEmitted = backendContext.configuration.languageVersionSettings.getFlag(allowFullyQualifiedNameInKClass)
                val qualifier =
                    if (fqnShouldBeEmitted) {
                        (klass.originalFqName ?: klass.kotlinFqName).parentOrNull()?.asString() ?: ""
                    } else {
                        ""
                    }
                val simpleName = klass.name.asString()
                val (packageNameAddress, packageNamePoolId) = wasmFileCodegenContext.referenceStringLiteralAddressAndId(qualifier)
                val (simpleNameAddress, simpleNamePoolId) = wasmFileCodegenContext.referenceStringLiteralAddressAndId(simpleName)

                buildConstI32Symbol(packageNameAddress, location)
                buildConstI32(qualifier.length, location)
                buildConstI32Symbol(packageNamePoolId, location)

                buildConstI32Symbol(simpleNameAddress, location)
                buildConstI32(simpleName.length, location)
                buildConstI32Symbol(simpleNamePoolId, location)
            } else {
                buildConstI32(-1, location)
                buildConstI32(-1, location)
                buildConstI32(-1, location)
                buildConstI32(-1, location)
                buildConstI32(-1, location)
                buildConstI32(-1, location)
            }

            buildConstI64(wasmFileCodegenContext.referenceTypeId(symbol), location)

            buildStructNew(wasmFileCodegenContext.rttiType, location)
        }

        val rttiGlobal = WasmGlobal(
            name = "${klass.fqNameWhenAvailable}_rtti",
            type = WasmRefType(WasmHeapType.Type(wasmFileCodegenContext.rttiType)),
            isMutable = false,
            init = initRttiGlobal
        )

        wasmFileCodegenContext.defineRttiGlobal(rttiGlobal, symbol, superType)
    }

    private fun createClassITable(metadata: ClassMetadata) {
        val klass = metadata.klass
        if (klass.isAbstractOrSealed) return
        if (!klass.hasInterfaceSuperClass()) return

        if (wasmFileCodegenContext.handleClassITableWithImport(klass.symbol)) return

        val location = SourceLocation.NoLocation("Create instance of itable struct")

        val initITableGlobal = buildWasmExpression {
            val supportedIFaces = metadata.interfaces
            val regularITableIFaces = supportedIFaces
                .filterNot { it.symbol in backendContext.specialSlotITableTypes || it.symbol.isFunction() }
            for (iFace in regularITableIFaces) {
                for (method in wasmModuleMetadataCache.getInterfaceMetadata(iFace.symbol).methods) {
                    addInterfaceMethod(metadata, this, method, location)
                }
                buildStructNew(wasmFileCodegenContext.referenceVTableGcType(iFace.symbol), location)
            }
            buildInstr(
                WasmOp.ARRAY_NEW_FIXED,
                location,
                WasmImmediate.GcType(wasmFileCodegenContext.interfaceTableTypes.wasmAnyArrayType),
                WasmImmediate.ConstI32(regularITableIFaces.size)
            )
        }

        val wasmClassIFaceGlobal = WasmGlobal(
            name = "<classITable>",
            type = WasmRefType(WasmHeapType.Type(wasmFileCodegenContext.interfaceTableTypes.wasmAnyArrayType)),
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
                    isMutable = wasmArrayAnnotation.isMutable
                )
            )

            wasmFileCodegenContext.defineGcType(symbol, wasmArrayDeclaration)
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
            wasmFileCodegenContext.defineVTableGcType(symbol, vtableStruct)
        } else {
            val metadata = wasmModuleMetadataCache.getClassMetadata(symbol)

            createVTable(metadata)
            createClassITable(metadata)
            createRtti(metadata)

            val vtableRefGcType = WasmRefType(WasmHeapType.Type(wasmFileCodegenContext.referenceVTableGcType(symbol)))
            val fields = mutableListOf<WasmStructFieldDeclaration>()
            fields.add(WasmStructFieldDeclaration("vtable", vtableRefGcType, false))
            fields.add(WasmStructFieldDeclaration("itable", WasmRefNullType(WasmHeapType.Type(wasmFileCodegenContext.interfaceTableTypes.wasmAnyArrayType)), false))
            fields.add(WasmStructFieldDeclaration("rtti", WasmRefType(WasmHeapType.Type(wasmFileCodegenContext.rttiType)), isMutable = false))
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
        }

        for (member in declaration.declarations) {
            member.acceptVoid(this)
        }
    }

    private fun interfaceTable(builder: WasmExpressionBuilder, classMetadata: ClassMetadata, location: SourceLocation) {
        val supportedInterfaces = classMetadata.interfaces

        if (supportedInterfaces.isEmpty()) {
            builder.buildRefNull(WasmHeapType.Simple.None, location)
            return
        }

        val specialSlotIFaces = backendContext.specialSlotITableTypes

        val (forward, back) = supportedInterfaces.partition { it.symbol !in specialSlotIFaces && !it.symbol.isFunction() }
        val supportedPushedBack = forward + back

        for (iFace in supportedPushedBack) {
            builder.buildConstI64(wasmFileCodegenContext.referenceTypeId(iFace.symbol), location)
        }

        builder.buildInstr(
            WasmOp.ARRAY_NEW_FIXED,
            location,
            WasmImmediate.GcType(wasmFileCodegenContext.referenceGcType(backendContext.wasmSymbols.wasmLongImmutableArray)),
            WasmImmediate.ConstI32(supportedPushedBack.size)
        )
    }

    override fun visitField(declaration: IrField) {
        // Member fields are generated as part of struct type
        if (!declaration.isStatic) return
        val wasmType = wasmModuleTypeTransformer.transformType(declaration.type)

        val initBody = mutableListOf<WasmInstr>()
        val wasmExpressionGenerator = WasmExpressionBuilder(initBody, skipCommentInstructions = skipCommentInstructions)

        val initValue: IrExpression? = declaration.initializer?.expression
        if (initValue != null) {
            if (initValue is IrConst && initValue.kind !is IrConstKind.String) {
                generateConstExpression(
                    initValue,
                    wasmExpressionGenerator,
                    wasmFileCodegenContext,
                    backendContext,
                    initValue.getSourceLocation(declaration.symbol, declaration.fileOrNull),
                    useStringPool,
                )
            } else {
                val stubFunction = WasmFunction.Defined("static_fun_stub", WasmSymbol())
                val functionCodegenContext = WasmFunctionCodegenContext(
                    null,
                    stubFunction,
                    backendContext,
                    wasmFileCodegenContext,
                    wasmModuleTypeTransformer,
                    skipCommentInstructions,
                )
                val bodyGenerator = BodyGenerator(
                    backendContext,
                    wasmFileCodegenContext,
                    functionCodegenContext,
                    wasmModuleMetadataCache,
                    wasmModuleTypeTransformer,
                    useStringPool,
                    inlineUnitGetter,
                )
                bodyGenerator.generateExpression(initValue)
                wasmFileCodegenContext.addFieldInitializer(
                    declaration.symbol,
                    stubFunction.instructions,
                    declaration.isObjectInstanceField()
                )
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
    val result = mutableListOf<IrValueParameter>()
    if (this is IrConstructor) {
        result.add(parentAsClass.thisReceiver!!)
    }
    result.addAll(parameters)
    return result
}

fun IrFunction.isExported(): Boolean =
    isJsExport() || getWasmExportNameIfWasmExport() != null

fun generateConstExpression(
    expression: IrConst,
    body: WasmExpressionBuilder,
    context: WasmFileCodegenContext,
    backendContext: WasmBackendContext,
    location: SourceLocation,
    useStringPool: Boolean,
) =
    when (val kind = expression.kind) {
        is IrConstKind.Null -> {
            val isExternal = expression.type.getClass()?.isExternal ?: expression.type.erasedUpperBound.isExternal
            val bottomType = if (isExternal) WasmRefNullExternrefType else WasmRefNullrefType
            body.buildInstr(WasmOp.REF_NULL, location, WasmImmediate.HeapType(bottomType))
        }
        is IrConstKind.Boolean -> body.buildConstI32(if (expression.value as Boolean) 1 else 0, location)
        is IrConstKind.Byte -> body.buildConstI32((expression.value as Byte).toInt(), location)
        is IrConstKind.Short -> body.buildConstI32((expression.value as Short).toInt(), location)
        is IrConstKind.Int -> body.buildConstI32(expression.value as Int, location)
        is IrConstKind.Long -> body.buildConstI64(expression.value as Long, location)
        is IrConstKind.Char -> body.buildConstI32((expression.value as Char).code, location)
        is IrConstKind.Float -> body.buildConstF32(expression.value as Float, location)
        is IrConstKind.Double -> body.buildConstF64(expression.value as Double, location)
        is IrConstKind.String -> {
            val stringValue = expression.value as String

            body.commentGroupStart { "const string: \"$stringValue\"" }

            val (literalAddress, literalPoolId) = context.referenceStringLiteralAddressAndId(stringValue)
            if (useStringPool) {
                body.buildConstI32Symbol(literalPoolId, location)
                body.buildConstI32Symbol(literalAddress, location)
                body.buildConstI32(stringValue.length, location)
                body.buildCall(context.referenceFunction(backendContext.wasmSymbols.stringGetLiteral), location)
            } else {
                body.buildConstI32Symbol(literalAddress, location)
                body.buildConstI32(stringValue.length, location)

                val createString = backendContext.wasmSymbols.createString
                val wasmCharArrayType = createString.owner.parameters[0].type
                val arrayGcType = WasmImmediate.GcType(
                    context.referenceGcType(wasmCharArrayType.getRuntimeClass(backendContext.irBuiltIns).symbol)
                )
                body.buildInstr(WasmOp.ARRAY_NEW_DATA, location, arrayGcType, WasmImmediate.DataIdx(0))
                body.buildCall(context.referenceFunction(createString), location)
            }

            body.commentGroupEnd()
        }
    }

val IrFunction.locationTarget: IrElement
    get() = when (origin) {
        IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER -> this
        IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA -> this
        else -> when (parentClassOrNull?.origin) {
            CallableReferenceLowering.LAMBDA_IMPL,
            IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA -> this
            else -> body ?: this
        }
    }

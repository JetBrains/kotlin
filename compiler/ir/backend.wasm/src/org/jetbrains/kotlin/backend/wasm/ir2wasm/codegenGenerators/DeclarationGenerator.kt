/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("DeclarationGeneratorKt")

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.buildUnreachableForVerifier
import org.jetbrains.kotlin.backend.wasm.utils.fitsLatin1
import org.jetbrains.kotlin.backend.wasm.utils.getFunctionalInterfaceSlot
import org.jetbrains.kotlin.backend.wasm.utils.getJsBuiltinDescriptor
import org.jetbrains.kotlin.backend.wasm.utils.getJsFunAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.getWasmImportDescriptor
import org.jetbrains.kotlin.backend.wasm.utils.hasUnpairedSurrogates
import org.jetbrains.kotlin.backend.wasm.utils.isAbstractOrSealed
import org.jetbrains.kotlin.config.AnalysisFlags.allowFullyQualifiedNameInKClass
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.lower.WebCallableReferenceLowering
import org.jetbrains.kotlin.ir.backend.js.lower.originalFqName
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.backend.js.utils.isExplicitlyExported
import org.jetbrains.kotlin.ir.backend.js.wasm.getWasmExportName
import org.jetbrains.kotlin.ir.backend.js.wasm.isWasmExportDeclaration
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getSourceFile
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isOriginallyLocalDeclaration
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.wasm.ir.WasmAnyRef
import org.jetbrains.kotlin.wasm.ir.WasmExport
import org.jetbrains.kotlin.wasm.ir.WasmExpressionBuilder
import org.jetbrains.kotlin.wasm.ir.WasmExpressionBuilderWithOptimizer
import org.jetbrains.kotlin.wasm.ir.WasmExternRef
import org.jetbrains.kotlin.wasm.ir.WasmF32
import org.jetbrains.kotlin.wasm.ir.WasmF64
import org.jetbrains.kotlin.wasm.ir.WasmFunction
import org.jetbrains.kotlin.wasm.ir.WasmGlobal
import org.jetbrains.kotlin.wasm.ir.WasmHeapType
import org.jetbrains.kotlin.wasm.ir.WasmI32
import org.jetbrains.kotlin.wasm.ir.WasmI64
import org.jetbrains.kotlin.wasm.ir.WasmImmediate
import org.jetbrains.kotlin.wasm.ir.WasmImportDescriptor
import org.jetbrains.kotlin.wasm.ir.WasmInstr
import org.jetbrains.kotlin.wasm.ir.WasmOp
import org.jetbrains.kotlin.wasm.ir.WasmRefNullExternrefType
import org.jetbrains.kotlin.wasm.ir.WasmRefNullType
import org.jetbrains.kotlin.wasm.ir.WasmRefNullrefType
import org.jetbrains.kotlin.wasm.ir.WasmRefType
import org.jetbrains.kotlin.wasm.ir.WasmSymbol
import org.jetbrains.kotlin.wasm.ir.WasmType
import org.jetbrains.kotlin.wasm.ir.WasmUnreachableType
import org.jetbrains.kotlin.wasm.ir.buildWasmExpression
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import kotlin.collections.forEach

private const val MAX_WASM_IMPORT_NAME_LENGTH = 100_000

private const val TYPE_INFO_FLAG_ANONYMOUS_CLASS = 1
private const val TYPE_INFO_FLAG_LOCAL_CLASS = 2

class DeclarationGenerator(
    private val backendContext: WasmBackendContext,
    private val typeCodegenContext: WasmTypeCodegenContext,
    private val declarationCodegenContext: WasmDeclarationCodegenContext,
    private val serviceDataContext: WasmServiceDataCodegenContext,
    private val wasmModuleTypeTransformer: WasmModuleTypeTransformer,
    private val wasmModuleMetadataCache: WasmModuleMetadataCache,
    private val allowIncompleteImplementations: Boolean,
    private val skipCommentInstructions: Boolean,
    skipLocations: Boolean,
    private val enableMultimoduleExports: Boolean,
) {
    private val irBuiltIns: IrBuiltIns = backendContext.irBuiltIns

    private val locationProvider = if (skipLocations) LocationProviderStub else LocationProviderImpl

    private fun multimoduleExportIfNeeded(irFunction: IrFunction, wasmFunction: WasmFunction) {
        if (!enableMultimoduleExports) return
        if (irFunction.isEffectivelyPrivate()) return
        val tag = typeCodegenContext.getDeclarationTag(irFunction)
        serviceDataContext.addExport(
            WasmExport.Function(
                field = wasmFunction,
                name = "${WasmServiceImportExportKind.FUNC.prefix}$tag"
            )
        )
    }

    private fun multimoduleExportIfNeeded(irClass: IrClass, prefix: WasmServiceImportExportKind, global: WasmGlobal) {
        if (!enableMultimoduleExports) return
        if (irClass.isEffectivelyPrivate()) return
        val tag = typeCodegenContext.getDeclarationTag(irClass)
        serviceDataContext.addExport(
            WasmExport.Global(
                name = "${prefix.prefix}$tag",
                field = global
            )
        )
    }

    fun generateFunction(declaration: IrFunction) {
        val functionTypeSymbol = typeCodegenContext.referenceFunctionHeapType(declaration.symbol)
        val wasmImportModule = declaration.getWasmImportDescriptor()
        val jsBuiltin = declaration.getJsBuiltinDescriptor()
        val jsCode = declaration.getJsFunAnnotation()

        val importedName = when {
            wasmImportModule != null -> {
                check(declaration.isExternal) { "Non-external fun with @WasmImport ${declaration.fqNameWhenAvailable}"}
                serviceDataContext.addJsModuleImport(declaration.symbol, wasmImportModule.moduleName)
                wasmImportModule
            }
            jsBuiltin != null -> {
                check(declaration.isExternal) { "Non-external fun with @JsBuiltin ${declaration.fqNameWhenAvailable}"}
                serviceDataContext.addJsModuleImport(declaration.symbol, jsBuiltin.moduleName)
                serviceDataContext.addJsBuiltin(jsBuiltin.declarationName, jsBuiltin.polyfillImpl)
                WasmImportDescriptor(jsBuiltin.moduleName, WasmSymbol(jsBuiltin.declarationName))
            }
            jsCode != null -> {
                // check(declaration.isExternal) { "Non-external fun with @JsFun ${declaration.fqNameWhenAvailable}"}
                require(declaration is IrSimpleFunction)
                val jsFunName = WasmSymbol(declaration.fqNameWhenAvailable.toString())
                serviceDataContext.addJsFun(declaration.symbol, jsFunName, jsCode)
                WasmImportDescriptor("js_code", jsFunName)
            }
            else -> {
                null
            }
        }

        val watName = declaration.fqNameWhenAvailable.toString()

        if (importedName != null) {
            // Imported functions don't have bodies. Declaring the signature:
            val importedFunction = WasmFunction.Imported(watName, functionTypeSymbol, importedName)
            declarationCodegenContext.defineFunction(
                irFunction = declaration.symbol,
                wasmFunction = importedFunction
            )
            // TODO: Support re-export of imported functions.
            multimoduleExportIfNeeded(declaration, importedFunction)
            return
        }

        val sourceFile = declaration.getSourceFile()!!
        val locationTarget = declaration.locationTarget
        val functionStartLocation = locationProvider.getSourceLocation(locationTarget, declaration.symbol, sourceFile)
        val functionEndLocation = locationProvider.getSourceEndLocation(locationTarget, declaration.symbol, sourceFile)

        val expressionBuilder = WasmExpressionBuilderWithOptimizer(skipCommentInstructions)

        val function = WasmFunction.Defined(
            watName,
            functionTypeSymbol,
            instructions = expressionBuilder.expression,
            startLocation = functionStartLocation,
            endLocation = functionEndLocation
        )
        val functionCodegenContext = WasmFunctionCodegenContext(
            declaration,
            function,
            backendContext,
            typeCodegenContext,
            wasmModuleTypeTransformer,
            sourceFile,
        )

        declaration.forEachEffectiveValueParameters { it
            functionCodegenContext.defineLocal(it.symbol)
        }

        val bodyBuilder = BodyGenerator(
            backendContext,
            typeCodegenContext,
            declarationCodegenContext,
            serviceDataContext,
            functionCodegenContext,
            wasmModuleMetadataCache,
            wasmModuleTypeTransformer,
            locationProvider,
            expressionBuilder,
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
            expressionBuilder.buildGetLocal(/*implicit this*/ function.locals[0], SourceLocation.NoLocation("Get implicit dispatch receiver"))
            expressionBuilder.buildInstr(WasmOp.RETURN, SourceLocation.NoLocation("Implicit return from constructor"))
        }

        // Add unreachable if function returns something but not as a last instruction.
        // We can do a separate lowering which adds explicit returns everywhere instead.
        if (wasmModuleTypeTransformer.hasBlockResultType(declaration.returnType)) {
            expressionBuilder.buildUnreachableForVerifier()
        }

        expressionBuilder.complete()

        declarationCodegenContext.defineFunction(declaration.symbol, function)
        multimoduleExportIfNeeded(declaration, function)

        val nameIfExported = when {
            declaration.isExplicitlyExported() -> declaration.getJsNameOrKotlinName().identifier
            declaration.isWasmExportDeclaration() -> declaration.getWasmExportName()
            else -> null
        }

        if (nameIfExported != null) {
            serviceDataContext.addExport(
                WasmExport.Function(
                    field = function,
                    name = nameIfExported
                )
            )
        }
    }

    private fun buildSpecialITableInit(metadata: ClassMetadata, builder: WasmExpressionBuilder, location: SourceLocation) {
        val klass = metadata.klass
        if (!klass.hasInterfaceSuperClass()) {
            builder.buildRefNull(WasmHeapType.Simple.None, location)
            return
        }

        val supportedIFaces = metadata.interfaces
        val specialSlotITableTypes = backendContext.specialSlotITableTypes

        val functionalInterfaces = supportedIFaces.filter { it.symbol.isFunction() }
        val specialInterfacesIfSupported = specialSlotITableTypes.map { iFace -> iFace.takeIf { it.owner in supportedIFaces } }

        if (functionalInterfaces.isEmpty() && specialInterfacesIfSupported.all { it == null }) {
            builder.buildRefNull(WasmHeapType.Simple.None, location)
            return
        }

        //Load special interfaces implementation
        for (supportedSpecialInterface in specialInterfacesIfSupported) {
            if (supportedSpecialInterface != null) {
                for (method in wasmModuleMetadataCache.getInterfaceMetadata(supportedSpecialInterface).methods) {
                    addInterfaceMethod(metadata, builder, method, location)
                }
                builder.buildStructNew(typeCodegenContext.referenceVTableGcType(supportedSpecialInterface), location)
            } else {
                builder.buildRefNull(WasmHeapType.Simple.None, location)
            }
        }

        //Load functional interfaces implementation
        if (functionalInterfaces.isNotEmpty()) {
            val functionInterfaceToSlot = functionalInterfaces.map { it to (getFunctionalInterfaceSlot(it)) }
            val functionsITableSize = functionInterfaceToSlot.maxOf { it.second } + 1

            repeat(functionsITableSize) { slotIndex ->
                val currentInterface = functionInterfaceToSlot.firstOrNull { it.second == slotIndex }?.first?.symbol
                if (currentInterface != null) {
                    for (method in wasmModuleMetadataCache.getInterfaceMetadata(currentInterface).methods) {
                        addInterfaceMethod(metadata, builder, method, location)
                    }
                    builder.buildStructNew(typeCodegenContext.referenceVTableGcType(currentInterface), location)
                } else {
                    builder.buildRefNull(WasmHeapType.Simple.Any, location)
                }
            }
            builder.buildInstr(
                WasmOp.ARRAY_NEW_FIXED,
                location,
                Synthetics.GcTypes.wasmAnyArrayType,
                WasmImmediate.ConstI32(functionsITableSize)
            )
        } else {
            builder.buildRefNull(WasmHeapType.Simple.None, location)
        }
        builder.buildStructNew(Synthetics.GcTypes.specialSlotITableType, location)
    }

    private fun createVTable(metadata: ClassMetadata) {
        val klass = metadata.klass
        val symbol = klass.symbol
        if (klass.isAbstractOrSealed) return

        val vTableRefGcType = WasmRefType(typeCodegenContext.referenceVTableHeapType(symbol))

        val initVTableGlobal = buildWasmExpression {
            val location = SourceLocation.NoLocation("Create instance of vtable struct")
            buildSpecialITableInit(metadata, this, location)
            metadata.virtualMethods.forEachIndexed { i, method ->
                if (method.function.modality != Modality.ABSTRACT) {
                    buildInstr(WasmOp.REF_FUNC, location, declarationCodegenContext.referenceFunction(method.function.symbol))
                } else {
                    check(allowIncompleteImplementations) {
                        "Cannot find class implementation of method ${method.signature} in class ${klass.fqNameWhenAvailable}"
                    }
                    //This erased by DCE so abstract version appeared in non-abstract class
                    buildRefNull(WasmHeapType.Simple.NoFunc, location)
                }
            }
            buildStructNew(typeCodegenContext.referenceVTableGcType(symbol), location)
        }

        val vTableGlobal = WasmGlobal("<classVTable>", vTableRefGcType, false, initVTableGlobal)
        declarationCodegenContext.defineGlobalVTable(symbol, vTableGlobal)
        multimoduleExportIfNeeded(klass, WasmServiceImportExportKind.VTABLE, vTableGlobal)
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
            val functionTypeReference = declarationCodegenContext.referenceFunction(classMethod.function.symbol)
            builder.buildInstr(WasmOp.REF_FUNC, location, functionTypeReference)
        } else {
            //This erased by DCE so abstract version appeared in non-abstract class
            builder.buildRefNull(WasmHeapType.Simple.NoFunc, location)
        }
    }

    private fun createRtti(metadata: ClassMetadata) {
        val klass = metadata.klass
        val symbol = klass.symbol
        val superType = klass.getSuperClass(irBuiltIns)?.symbol

        val fqnShouldBeEmitted = backendContext.configuration.languageVersionSettings.getFlag(allowFullyQualifiedNameInKClass)
        val qualifier =
            if (fqnShouldBeEmitted) {
                (klass.originalFqName ?: klass.kotlinFqName).parentOrNull()?.asString() ?: ""
            } else {
                ""
            }
        val simpleName = klass.name.asString()
        val packageNameStringLiteralId: WasmSymbol<Int>
        val simpleNameStringLiteralId: WasmSymbol<Int>
        if (backendContext.isWasmJsTarget) {
            packageNameStringLiteralId = serviceDataContext.referenceGlobalStringId(qualifier)
            simpleNameStringLiteralId = serviceDataContext.referenceGlobalStringId(simpleName)
        } else {
            packageNameStringLiteralId = serviceDataContext.referenceStringLiteralId(qualifier)
            simpleNameStringLiteralId = serviceDataContext.referenceStringLiteralId(simpleName)
        }

        val location = SourceLocation.NoLocation("Create instance of rtti struct")
        val initRttiGlobal = buildWasmExpression {
            interfaceTable(this, metadata, location)
            if (superType != null) {
                buildGetGlobal(declarationCodegenContext.referenceRttiGlobal(superType), location)
            } else {
                buildRefNull(WasmHeapType.Simple.None, location)
            }

            buildConstI32Symbol(packageNameStringLiteralId, location)
            buildConstI32Symbol(simpleNameStringLiteralId, location)

            buildConstI64(serviceDataContext.referenceTypeId(symbol), location)

            val isAnonymousFlag = if (klass.isAnonymousObject) TYPE_INFO_FLAG_ANONYMOUS_CLASS else 0
            val isLocalFlag = if (klass.isOriginallyLocalDeclaration) TYPE_INFO_FLAG_LOCAL_CLASS else 0
            buildConstI32(isAnonymousFlag or isLocalFlag, location)

            val qualifierStringLoaderRef =
                if (backendContext.isWasmJsTarget)
                    Synthetics.Functions.createStringLiteralJsString
                else if (qualifier.fitsLatin1)
                    Synthetics.Functions.createStringLiteralLatin1
                else
                    Synthetics.Functions.createStringLiteralUtf16

            buildInstr(
                WasmOp.REF_FUNC,
                location,
                qualifierStringLoaderRef,
            )

            val simpleNameStringLoaderRef =
                if (backendContext.isWasmJsTarget)
                    Synthetics.Functions.createStringLiteralJsString
                else if (simpleName.fitsLatin1)
                    Synthetics.Functions.createStringLiteralLatin1
                else
                    Synthetics.Functions.createStringLiteralUtf16

            buildInstr(
                WasmOp.REF_FUNC,
                location,
                simpleNameStringLoaderRef,
            )

            if (backendContext.isWasmJsTarget) {
                val packageNameGlobalReference = serviceDataContext.referenceGlobalStringGlobal(qualifier)
                buildGetGlobal(packageNameGlobalReference, location)

                val simpleNameGlobalReference = serviceDataContext.referenceGlobalStringGlobal(simpleName)
                buildGetGlobal(simpleNameGlobalReference, location)
            }

            buildStructNew(Synthetics.GcTypes.rttiType, location)
        }

        val rttiGlobal = WasmGlobal(
            name = "${klass.fqNameWhenAvailable}_rtti",
            type = WasmRefType(Synthetics.HeapTypes.rttiType),
            isMutable = false,
            init = initRttiGlobal
        )

        declarationCodegenContext.defineRttiGlobal(rttiGlobal, symbol, superType)
        multimoduleExportIfNeeded(klass, WasmServiceImportExportKind.RTTI, rttiGlobal)
    }

    private fun createClassITable(metadata: ClassMetadata) {
        val klass = metadata.klass
        if (klass.isAbstractOrSealed) return
        if (!klass.hasInterfaceSuperClass()) return

        val location = SourceLocation.NoLocation("Create instance of itable struct")

        val initITableGlobal = buildWasmExpression {
            val supportedIFaces = metadata.interfaces
            val regularITableIFaces = supportedIFaces
                .filterNot { it.symbol in backendContext.specialSlotITableTypes || it.symbol.isFunction() }
            for (iFace in regularITableIFaces) {
                for (method in wasmModuleMetadataCache.getInterfaceMetadata(iFace.symbol).methods) {
                    addInterfaceMethod(metadata, this, method, location)
                }
                buildStructNew(typeCodegenContext.referenceVTableGcType(iFace.symbol), location)
            }
            buildInstr(
                WasmOp.ARRAY_NEW_FIXED,
                location,
                Synthetics.GcTypes.wasmAnyArrayType,
                WasmImmediate.ConstI32(regularITableIFaces.size)
            )
        }

        val wasmClassIFaceGlobal = WasmGlobal(
            name = "<classITable>",
            type = WasmRefType(Synthetics.HeapTypes.wasmAnyArrayType),
            isMutable = false,
            init = initITableGlobal
        )
        declarationCodegenContext.defineGlobalClassITable(klass.symbol, wasmClassIFaceGlobal)
        multimoduleExportIfNeeded(klass, WasmServiceImportExportKind.ITABLE, wasmClassIFaceGlobal)
    }

    fun generateClassDeclarations(declaration: IrClass) {
        if (!declaration.isInterface) {
            val metadata = wasmModuleMetadataCache.getClassMetadata(declaration.symbol)
            createVTable(metadata)
            createClassITable(metadata)
            createRtti(metadata)
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
            builder.buildConstI64(serviceDataContext.referenceTypeId(iFace.symbol), location)
        }

        builder.buildInstr(
            WasmOp.ARRAY_NEW_FIXED,
            location,
            typeCodegenContext.referenceGcType(backendContext.wasmSymbols.wasmLongImmutableArray),
            WasmImmediate.ConstI32(supportedPushedBack.size)
        )
    }

    fun generateField(declaration: IrField) {
        // Member fields are generated as part of struct type
        if (!declaration.isStatic) return

        val wasmType = wasmModuleTypeTransformer.transformType(declaration.type)

        val initBody = mutableListOf<WasmInstr>()
        val wasmExpressionGenerator = WasmExpressionBuilder(
            expression = initBody,
            skipCommentInstructions = skipCommentInstructions,
        )

        val initValue: IrExpression? = declaration.initializer?.expression
        if (initValue is IrConst && initValue.kind !is IrConstKind.String) {
            val sourceFile = declaration.getSourceFile()!!
            val location = locationProvider.getSourceLocation(initValue, declaration.symbol, sourceFile)
            generateConstExpression(
                initValue,
                wasmExpressionGenerator,
                serviceDataContext,
                declarationCodegenContext,
                backendContext,
                location
            )
        } else {
            generateDefaultInitializerForType(wasmType, wasmExpressionGenerator)
        }

        val global = WasmGlobal(
            name = declaration.fqNameWhenAvailable.toString(),
            type = wasmType,
            isMutable = true,
            init = initBody
        )

        declarationCodegenContext.defineGlobalField(declaration.symbol, global)
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
            is WasmRefType -> g.buildRefNull(type.heapType, location)
            is WasmRefNullrefType -> g.buildRefNull(WasmHeapType.Simple.None, location)
            is WasmRefNullExternrefType -> g.buildRefNull(WasmHeapType.Simple.NoExtern, location)
            is WasmAnyRef -> g.buildRefNull(WasmHeapType.Simple.Any, location)
            is WasmExternRef -> g.buildRefNull(WasmHeapType.Simple.Extern, location)
            WasmUnreachableType -> error("Unreachable type can't be initialized")
            else -> error("Unknown value type ${type.name}")
        }
    }

inline fun IrFunction.forEachEffectiveValueParameters(body: (IrValueParameter) -> Unit) {
    if (this is IrConstructor) {
        body(parentAsClass.thisReceiver!!)
    }
    parameters.forEach(body)
}

fun IrFunction.isExported(): Boolean =
    isExplicitlyExported() || isWasmExportDeclaration()

fun generateConstExpression(
    expression: IrConst,
    body: WasmExpressionBuilder,
    serviceDataContext: WasmServiceDataCodegenContext,
    declarationCodegenContext: WasmDeclarationCodegenContext,
    backendContext: WasmBackendContext,
    location: SourceLocation
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

            if (backendContext.isWasmJsTarget && !stringValue.hasUnpairedSurrogates) {
                val literalIdToStore = serviceDataContext.referenceGlobalStringId(stringValue)
                body.buildConstI32Symbol(literalIdToStore, location)

                if (stringValue.length > MAX_WASM_IMPORT_NAME_LENGTH) {
                    val stringValueSplits = stringValue.chunked(MAX_WASM_IMPORT_NAME_LENGTH)
                    val jsConcat: FuncSymbol =
                        declarationCodegenContext.referenceFunction(backendContext.wasmSymbols.jsRelatedSymbols.jsConcat)

                    val globalReferenceFirst = serviceDataContext.referenceGlobalStringGlobal(stringValueSplits.first())
                    body.buildGetGlobal(globalReferenceFirst, location)

                    for (stringValueSplit in stringValueSplits.drop(1)) {
                        val globalReference = serviceDataContext.referenceGlobalStringGlobal(stringValueSplit)
                        body.buildGetGlobal(globalReference, location)
                        body.buildCall(jsConcat, location)
                    }
                } else {
                    val globalReferenceFirst = serviceDataContext.referenceGlobalStringGlobal(stringValue)
                    body.buildGetGlobal(globalReferenceFirst, location)
                }

                body.buildCall(Synthetics.Functions.createStringLiteralJsString, location)
            } else {
                val literalId = serviceDataContext.referenceStringLiteralId(stringValue)
                body.buildConstI32Symbol(literalId, location)

                if (stringValue.fitsLatin1) {
                    body.buildCall(Synthetics.Functions.createStringLiteralLatin1, location)
                } else {
                    body.buildCall(Synthetics.Functions.createStringLiteralUtf16, location)
                }
            }
            body.commentGroupEnd()
        }
    }

val IrFunction.locationTarget: IrElement
    get() = when (origin) {
        IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER -> this
        IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA -> this
        else -> when (parentClassOrNull?.origin) {
            WebCallableReferenceLowering.FUNCTION_REFERENCE_IMPL,
            IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA -> this
            else -> body ?: this
        }
    }

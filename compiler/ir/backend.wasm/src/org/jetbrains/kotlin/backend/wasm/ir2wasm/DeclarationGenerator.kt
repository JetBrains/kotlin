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
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

class DeclarationGenerator(
    val context: WasmModuleCodegenContext,
    private val allowIncompleteImplementations: Boolean,
    private val hierarchyDisjointUnions: DisjointUnions<IrClassSymbol>,
) : IrElementVisitorVoid {

    // Shortcuts
    private val backendContext: WasmBackendContext = context.backendContext
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


    private val jsCodeCounter = mutableMapOf<String, Int>()
    private fun jsCodeName(declaration: IrFunction): String {
        require(declaration is IrSimpleFunction)
        val key = declaration.fqNameWhenAvailable.toString()
        // counter is used to resolve fqName clashes
        val counterValue = jsCodeCounter.getOrPut(key, defaultValue = { 0 })
        jsCodeCounter[key] = counterValue + 1
        val counterSuffix = if (counterValue == 0 && key.lastOrNull()?.isDigit() == false) "" else "_$counterValue"
        return "$key$counterSuffix"
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
                context.addJsModuleImport(wasmImportModule.moduleName)
                wasmImportModule
            }
            jsCode != null -> {
                // check(declaration.isExternal) { "Non-external fun with @JsFun ${declaration.fqNameWhenAvailable}"}
                val jsCodeName = jsCodeName(declaration)
                context.addJsFun(jsCodeName, jsCode)
                WasmImportDescriptor("js_code", jsCodeName)
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
            unitGetInstanceFunction, unitPrimaryConstructor -> context.transformType(declaration.returnType)
            else -> context.transformResultType(declaration.returnType)
        }

        val wasmFunctionType =
            WasmFunctionType(
                parameterTypes = irParameters.map { context.transformValueParameterType(it) },
                resultTypes = listOfNotNull(resultType)
            )
        context.defineFunctionType(declaration.symbol, wasmFunctionType)

        if (declaration is IrSimpleFunction && declaration.modality == Modality.ABSTRACT) {
            return
        }

        assert(declaration == declaration.realOverrideTarget) {
            "Sanity check that $declaration is a real function that can be used in calls"
        }

        val functionTypeSymbol = context.referenceFunctionType(declaration.symbol)

        if (importedName != null) {
            // Imported functions don't have bodies. Declaring the signature:
            context.defineFunction(
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
            context
        )

        for (irParameter in irParameters) {
            functionCodegenContext.defineLocal(irParameter.symbol)
        }

        val exprGen = functionCodegenContext.bodyGen
        val bodyBuilder = BodyGenerator(
            context = context,
            functionContext = functionCodegenContext,
            hierarchyDisjointUnions = hierarchyDisjointUnions,
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

    private fun createDeclarationByInterface(iFace: IrClassSymbol) {
        if (context.isAlreadyDefinedClassITableGcType(iFace)) return
        if (iFace !in hierarchyDisjointUnions) return

        val iFacesUnion = hierarchyDisjointUnions[iFace]

        val fields = iFacesUnion.mapIndexed { index, unionIFace ->
            context.defineClassITableInterfaceSlot(unionIFace, index)
            WasmStructFieldDeclaration(
                name = "${unionIFace.owner.fqNameWhenAvailable.toString()}.itable",
                type = WasmRefNullType(WasmHeapType.Type(context.referenceVTableGcType(unionIFace))),
                isMutable = false
            )
        }

        val struct = WasmStructDeclaration(
            name = "classITable",
            fields = fields,
            superType = null
        )

        iFacesUnion.forEach {
            context.defineClassITableGcType(it, struct)
        }
    }

    private fun createVirtualTableStruct(
        methods: List<VirtualMethodMetadata>,
        name: String,
        superType: WasmSymbolReadOnly<WasmTypeDeclaration>? = null,
    ): WasmStructDeclaration {
        val tableFields = methods.map {
            WasmStructFieldDeclaration(
                name = it.signature.name.asString(),
                type = WasmRefNullType(WasmHeapType.Type(context.referenceFunctionType(it.function.symbol))),
                isMutable = false
            )
        }

        return WasmStructDeclaration(
            name = name,
            fields = tableFields,
            superType = superType,
        )
    }

    private fun createVTable(metadata: ClassMetadata) {
        val klass = metadata.klass
        val symbol = klass.symbol
        val vtableName = "${klass.fqNameWhenAvailable}.vtable"
        val vtableStruct = createVirtualTableStruct(
            metadata.virtualMethods,
            vtableName,
            superType = metadata.superClass?.klass?.symbol?.let(context::referenceVTableGcType)
        )
        context.defineVTableGcType(metadata.klass.symbol, vtableStruct)

        if (klass.isAbstractOrSealed) return

        val vTableTypeReference = context.referenceVTableGcType(symbol)
        val vTableRefGcType = WasmRefType(WasmHeapType.Type(vTableTypeReference))

        val initVTableGlobal = buildWasmExpression {
            val location = SourceLocation.NoLocation("Create instance of vtable struct")
            metadata.virtualMethods.forEachIndexed { i, method ->
                if (method.function.modality != Modality.ABSTRACT) {
                    buildInstr(WasmOp.REF_FUNC, location, WasmImmediate.FuncIdx(context.referenceFunction(method.function.symbol)))
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
        context.defineGlobalVTable(
            irClass = symbol,
            wasmGlobal = WasmGlobal(vtableName, vTableRefGcType, false, initVTableGlobal)
        )
    }

    private fun createClassITable(metadata: ClassMetadata) {
        val location = SourceLocation.NoLocation("Create instance of itable struct")
        val klass = metadata.klass
        if (klass.isAbstractOrSealed) return
        val supportedInterface = metadata.interfaces.firstOrNull()?.symbol ?: return

        createDeclarationByInterface(supportedInterface)

        val classInterfaceType = context.referenceClassITableGcType(supportedInterface)
        val interfaceList = hierarchyDisjointUnions[supportedInterface]

        val initITableGlobal = buildWasmExpression {
            for (iFace in interfaceList) {
                val iFaceVTableGcType = context.referenceVTableGcType(iFace)
                val iFaceVTableGcNullHeapType = WasmHeapType.Type(iFaceVTableGcType)

                if (!metadata.interfaces.contains(iFace.owner)) {
                    buildRefNull(iFaceVTableGcNullHeapType, location)
                    continue
                }

                for (method in context.getInterfaceMetadata(iFace).methods) {
                    val classMethod: VirtualMethodMetadata? = metadata.virtualMethods
                        .find { it.signature == method.signature && it.function.modality != Modality.ABSTRACT }  // TODO: Use map

                    if (classMethod == null && !allowIncompleteImplementations) {
                        error("Cannot find interface implementation of method ${method.signature} in class ${klass.fqNameWhenAvailable}")
                    }

                    if (classMethod != null) {
                        val functionTypeReference = context.referenceFunction(classMethod.function.symbol)
                        buildInstr(WasmOp.REF_FUNC, location, WasmImmediate.FuncIdx(functionTypeReference))
                    } else {
                        //This erased by DCE so abstract version appeared in non-abstract class
                        buildRefNull(WasmHeapType.Type(context.referenceFunctionType(method.function.symbol)), location)
                    }
                }
                buildStructNew(iFaceVTableGcType, location)
            }
            buildStructNew(classInterfaceType, location)
        }

        val wasmClassIFaceGlobal = WasmGlobal(
            name = "${klass.fqNameWhenAvailable.toString()}.classITable",
            type = WasmRefType(WasmHeapType.Type(classInterfaceType)),
            isMutable = false,
            init = initITableGlobal
        )
        context.defineGlobalClassITable(klass.symbol, wasmClassIFaceGlobal)
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
                    type = context.transformFieldType(wasmArrayAnnotation.type),
                    isMutable = true
                )
            )

            context.defineGcType(symbol, wasmArrayDeclaration)
            return
        }

        val nameStr = declaration.fqNameWhenAvailable.toString()

        if (declaration.isInterface) {
            if (symbol in hierarchyDisjointUnions) {
                val vtableStruct = createVirtualTableStruct(
                    methods = context.getInterfaceMetadata(symbol).methods,
                    name = "$nameStr.itable"
                )
                context.defineVTableGcType(symbol, vtableStruct)
            }
        } else {
            val metadata = context.getClassMetadata(symbol)

            createVTable(metadata)
            createClassITable(metadata)

            val vtableRefGcType = WasmRefType(WasmHeapType.Type(context.referenceVTableGcType(symbol)))
            val classITableRefGcType = WasmRefNullType(WasmHeapType.Simple.Struct)
            val fields = mutableListOf<WasmStructFieldDeclaration>()
            fields.add(WasmStructFieldDeclaration("vtable", vtableRefGcType, false))
            fields.add(WasmStructFieldDeclaration("itable", classITableRefGcType, false))
            declaration.allFields(irBuiltIns).mapTo(fields) {
                WasmStructFieldDeclaration(
                    name = it.name.toString(),
                    type = context.transformFieldType(it.type),
                    isMutable = true
                )
            }

            val superClass = metadata.superClass
            val structType = WasmStructDeclaration(
                name = nameStr,
                fields = fields,
                superType = superClass?.let { context.referenceGcType(superClass.klass.symbol) }
            )
            context.defineGcType(symbol, structType)
            context.generateTypeInfo(symbol, binaryDataStruct(metadata))
        }

        for (member in declaration.declarations) {
            member.acceptVoid(this)
        }
    }

    private fun binaryDataStruct(classMetadata: ClassMetadata): ConstantDataStruct {
        val fqnShouldBeEmitted = context.backendContext.configuration.languageVersionSettings.getFlag(allowFullyQualifiedNameInKClass)
        //TODO("FqName for inner classes could be invalid due to topping it out from outer class")
        val packageName = if (fqnShouldBeEmitted) classMetadata.klass.kotlinFqName.parentOrNull()?.asString() ?: "" else ""
        val simpleName = classMetadata.klass.kotlinFqName.shortName().asString()

        val (packageNameAddress, packageNamePoolId) = context.referenceStringLiteralAddressAndId(packageName)
        val (simpleNameAddress, simpleNamePoolId) = context.referenceStringLiteralAddressAndId(simpleName)

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

        val superClass = classMetadata.klass.getSuperClass(context.backendContext.irBuiltIns)
        val superTypeId = superClass?.let {
            ConstantDataIntField("SuperTypeId", context.referenceTypeId(it.symbol))
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
            interfaces.map { context.referenceTypeId(it.symbol) },
        )

        return ConstantDataStruct(
            name = "Class interface table: ${classMetadata.klass.fqNameWhenAvailable} ",
            elements = listOf(size, interfaceIds)
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
            check(initValue is IrConst<*> && initValue.kind !is IrConstKind.String) {
                "Static field initializer should be string or const"
            }
            generateConstExpression(
                initValue,
                wasmExpressionGenerator,
                context,
                declaration.getSourceLocation(declaration.fileOrNull?.fileEntry)
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

        context.defineGlobalField(declaration.symbol, global)
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
            is WasmRefNullNoneType -> g.buildRefNull(WasmHeapType.Simple.NullNone, location)
            is WasmRefNullExternrefType -> g.buildRefNull(WasmHeapType.Simple.NullNoExtern, location)
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
    isJsExport()


fun generateConstExpression(
    expression: IrConst<*>,
    body: WasmExpressionBuilder,
    context: WasmModuleCodegenContext,
    location: SourceLocation
) =
    when (val kind = expression.kind) {
        is IrConstKind.Null -> {
            val isExternal = expression.type.getClass()?.isExternal ?: expression.type.erasedUpperBound?.isExternal
            val bottomType = if (isExternal == true) WasmRefNullExternrefType else WasmRefNullNoneType
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
            body.buildCall(context.referenceFunction(context.backendContext.wasmSymbols.stringGetLiteral), location)
            body.commentGroupEnd()
        }
        else -> error("Unknown constant kind")
    }

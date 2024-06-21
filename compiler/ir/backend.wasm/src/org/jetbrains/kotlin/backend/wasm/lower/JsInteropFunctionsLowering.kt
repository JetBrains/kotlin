/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.ir.addDispatchReceiver
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ir2wasm.isBuiltInWasmRefType
import org.jetbrains.kotlin.backend.wasm.ir2wasm.isExternalType
import org.jetbrains.kotlin.backend.wasm.ir2wasm.toJsStringLiteral
import org.jetbrains.kotlin.backend.wasm.utils.getJsFunAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.getWasmImportDescriptor
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArgumentsFromEnvironment
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.backend.js.utils.isJsExport
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

val KOTLIN_TO_JS_CLOSURE_ORIGIN by IrDeclarationOriginImpl

/**
 * Create wrappers for external and @JsExport functions when type adaptation is needed
 */
class JsInteropFunctionsLowering(val context: WasmBackendContext) : DeclarationTransformer {
    val builtIns = context.irBuiltIns
    val symbols = context.wasmSymbols
    val jsRelatedSymbols get() = context.wasmSymbols.jsRelatedSymbols
    val adapters get() = jsRelatedSymbols.jsInteropAdapters

    val additionalDeclarations = mutableListOf<IrDeclaration>()
    lateinit var currentParent: IrDeclarationParent
    lateinit var currentFile: IrFile

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!context.isWasmJsTarget) return null

        if (declaration.isFakeOverride) return null
        if (declaration !is IrSimpleFunction) return null
        val isExported = declaration.isJsExport()
        val isExternal = declaration.isExternal || declaration.getJsFunAnnotation() != null
        if (declaration.isPropertyAccessor) return null
        if (declaration.parent !is IrPackageFragment) return null
        if (!isExported && !isExternal) return null
        if (declaration.getWasmImportDescriptor() != null) return null
        check(!(isExported && isExternal)) { "Exported external declarations are not supported: ${declaration.fqNameWhenAvailable}" }
        check(declaration.parent !is IrClass) { "Interop members are not supported:  ${declaration.fqNameWhenAvailable}" }
        if (context.mapping.wasmNestedExternalToNewTopLevelFunction[declaration] != null) return null

        additionalDeclarations.clear()
        currentParent = declaration.parent
        currentFile = declaration.file

        val newDeclarations = context.irFactory.stageController.restrictTo(declaration) {
            if (isExternal)
                transformExternalFunction(declaration)
            else
                transformExportFunction(declaration)
        }

        return (newDeclarations ?: listOf(declaration)) + additionalDeclarations
    }

    private fun doubleIfNumber(possiblyNumber: IrType): IrType {
        val isNullable = possiblyNumber.isNullable()
        val notNullType = possiblyNumber.makeNotNull()
        if (notNullType != builtIns.numberType) return possiblyNumber
        return if (isNullable) builtIns.doubleType.makeNullable() else builtIns.doubleType
    }

    /**
     *  external fun foo(x: KotlinType): KotlinType
     *
     *  ->
     *
     *  external fun foo(x: JsType): JsType
     *  fun foo__externalAdapter(x: KotlinType): KotlinType = adaptResult(foo(adaptParameter(x)));
     */
    fun transformExternalFunction(function: IrSimpleFunction): List<IrDeclaration>? {
        // External functions with default parameter values are already processed by
        // [ComplexExternalDeclarationsToTopLevelFunctionsLowering]
        if (function.valueParameters.any { it.defaultValue != null })
            return null

        // Patch function types for Number parameters as double
        function.returnType = doubleIfNumber(function.returnType)

        val valueParametersAdapters = function.valueParameters.map { parameter ->
            val varargElementType = parameter.varargElementType
            if (varargElementType != null) {
                CopyToJsArrayAdapter(parameter.type, varargElementType)
            } else {
                parameter.type.kotlinToJsAdapterIfNeeded(isReturn = false)
            }
        }
        val resultAdapter =
            function.returnType.jsToKotlinAdapterIfNeeded(isReturn = true)

        if (resultAdapter == null && valueParametersAdapters.all { it == null })
            return null

        val newFun = context.irFactory.createStaticFunctionWithReceivers(
            function.parent,
            name = Name.identifier(function.name.asStringStripSpecialMarkers() + "__externalAdapter"),
            function,
            remapMultiFieldValueClassStructure = context::remapMultiFieldValueClassStructure
        )

        function.valueParameters.forEachIndexed { index, newParameter ->
            val adapter = valueParametersAdapters[index]
            if (adapter != null) {
                newParameter.type = adapter.toType
            }
        }
        resultAdapter?.let {
            function.returnType = resultAdapter.fromType
        }

        val builder = context.createIrBuilder(newFun.symbol)
        newFun.body = createAdapterFunctionBody(builder, newFun, function, valueParametersAdapters, resultAdapter)
        newFun.annotations = emptyList()

        context.mapping.wasmJsInteropFunctionToWrapper[function] = newFun
        return listOf(function, newFun)
    }

    /**
     *  @JsExport
     *  fun foo(x: KotlinType): KotlinType { <original-body> }
     *
     *  ->
     *
     *  @JsExport
     *  @JsName("foo")
     *  fun foo__JsExportAdapter(x: JsType): JsType =
     *      adaptResult(foo(adaptParameter(x)));
     *
     *  fun foo(x: KotlinType): KotlinType { <original-body> }
     */
    fun transformExportFunction(function: IrSimpleFunction): List<IrDeclaration>? {
        val valueParametersAdapters = function.valueParameters.map {
            it.type.jsToKotlinAdapterIfNeeded(isReturn = false)
        }
        val resultAdapter =
            function.returnType.kotlinToJsAdapterIfNeeded(isReturn = true)

        if (resultAdapter == null && valueParametersAdapters.all { it == null })
            return null

        val newFun = context.irFactory.createStaticFunctionWithReceivers(
            function.parent,
            name = Name.identifier(function.name.asStringStripSpecialMarkers() + "__JsExportAdapter"),
            function,
            remapMultiFieldValueClassStructure = context::remapMultiFieldValueClassStructure
        )

        newFun.valueParameters.forEachIndexed { index, newParameter ->
            val adapter = valueParametersAdapters[index]
            if (adapter != null) {
                newParameter.type = adapter.fromType
            }
        }
        resultAdapter?.let {
            newFun.returnType = resultAdapter.toType
        }

        // Delegate new function to old function:
        val builder: DeclarationIrBuilder = context.createIrBuilder(newFun.symbol)
        newFun.body = createAdapterFunctionBody(builder, newFun, function, valueParametersAdapters, resultAdapter)

        newFun.annotations += builder.irCallConstructor(jsRelatedSymbols.jsNameConstructor, typeArguments = emptyList()).also {
            it.putValueArgument(0, builder.irString(function.getJsNameOrKotlinName().identifier))
        }
        function.annotations = function.annotations.filter { it.symbol != jsRelatedSymbols.jsExportConstructor }

        return listOf(function, newFun)
    }

    private fun createAdapterFunctionBody(
        builder: DeclarationIrBuilder,
        function: IrSimpleFunction,
        functionToCall: IrSimpleFunction,
        valueParametersAdapters: List<InteropTypeAdapter?>,
        resultAdapter: InteropTypeAdapter?
    ) = builder.irBlockBody {
        +irReturn(
            irCall(functionToCall).let { call ->
                for ((index, valueParameter) in function.valueParameters.withIndex()) {
                    val get = irGet(valueParameter)
                    call.putValueArgument(index, valueParametersAdapters[index].adaptIfNeeded(get, builder))
                }
                resultAdapter.adaptIfNeeded(call, builder)
            }
        )
    }

    val primitivesToExternRefAdapters: Map<IrType, InteropTypeAdapter> by lazy {
        mapOf(
            builtIns.byteType to adapters.kotlinByteToExternRefAdapter,
            symbols.uByteType to adapters.kotlinUByteToJsNumber,
            builtIns.shortType to adapters.kotlinShortToExternRefAdapter,
            symbols.uShortType to adapters.kotlinUShortToJsNumber,
            builtIns.charType to adapters.kotlinCharToExternRefAdapter,
            builtIns.intType to adapters.kotlinIntToExternRefAdapter,
            symbols.uIntType to adapters.kotlinUIntToJsNumber,
            builtIns.longType to adapters.kotlinLongToExternRefAdapter,
            symbols.uLongType to adapters.kotlinULongToJsBigInt,
            builtIns.floatType to adapters.kotlinFloatToExternRefAdapter,
            builtIns.doubleType to adapters.kotlinDoubleToExternRefAdapter,
        ).mapValues {
            FunctionBasedAdapter(it.value.owner)
        }
    }

    private fun IrType.kotlinToJsAdapterIfNeeded(isReturn: Boolean): InteropTypeAdapter? {
        if (isReturn && this == builtIns.unitType)
            return null

        if (this == builtIns.nothingType)
            return null

        if (!isNullable()) {
            return kotlinToJsAdapterIfNeededNotNullable(isReturn)
        }

        val notNullType = makeNotNull()

        if (notNullType == builtIns.numberType) {
            return NullOrAdapter(
                CombineAdapter(
                    FunctionBasedAdapter(adapters.kotlinDoubleToExternRefAdapter.owner),
                    FunctionBasedAdapter(adapters.numberToDoubleAdapter.owner)
                )
            )
        }

        val primitiveToExternRefAdapter = primitivesToExternRefAdapters[notNullType]

        val typeAdapter = primitiveToExternRefAdapter
            ?: notNullType.kotlinToJsAdapterIfNeededNotNullable(isReturn)
            ?: return null

        return NullOrAdapter(typeAdapter)
    }

    private fun IrType.kotlinToJsAdapterIfNeededNotNullable(isReturn: Boolean): InteropTypeAdapter? {
        if (isReturn && this == builtIns.unitType)
            return null

        if (this == builtIns.nothingType)
            return null

        when (this) {
            builtIns.stringType -> return FunctionBasedAdapter(adapters.kotlinToJsStringAdapter.owner)
            builtIns.booleanType -> return FunctionBasedAdapter(adapters.kotlinBooleanToExternRefAdapter.owner)
            builtIns.anyType -> return FunctionBasedAdapter(adapters.kotlinToJsAnyAdapter.owner)
            builtIns.numberType -> return FunctionBasedAdapter(adapters.numberToDoubleAdapter.owner)

            symbols.uByteType -> return FunctionBasedAdapter(adapters.kotlinUByteToJsNumber.owner)
            symbols.uShortType -> return FunctionBasedAdapter(adapters.kotlinUShortToJsNumber.owner)
            symbols.uIntType -> return FunctionBasedAdapter(adapters.kotlinUIntToJsNumber.owner)
            symbols.uLongType -> return FunctionBasedAdapter(adapters.kotlinULongToJsBigInt.owner)

            builtIns.byteType,
            builtIns.shortType,
            builtIns.charType,
            builtIns.intType,
            builtIns.longType,
            builtIns.floatType,
            builtIns.doubleType,
            context.wasmSymbols.voidType ->
                return null
            else -> {}
        }

        if (isExternalType(this))
            return null

        if (isBuiltInWasmRefType(this))
            return null

        if (this is IrSimpleType && this.isFunction()) {
            val functionTypeInfo = FunctionTypeInfo(this, toJs = true)

            // Kotlin's closures are objects that implement FunctionN interface.
            // JavaScript can receive opaque reference to them but cannot call them directly.
            // Thus, we export helper "caller" method that JavaScript will use to call kotlin closures:
            //
            //     @JsExport
            //     fun __callFunction_<signatureString>(f: structref, p1: JsType1, p2: JsType2, ...): JsTypeRes {
            //          return adapt(
            //              cast<FunctionN>(f).invoke(
            //                  adapt(p1),
            //                  adapt(p2),
            //                  ...
            //               )
            //          )
            //     }
            //

            context.getFileContext(currentFile).closureCallExports.getOrPut(functionTypeInfo.signatureString) {
                createKotlinClosureCaller(functionTypeInfo)
            }

            // Converter functions creates new JavaScript closures that delegate to Kotlin closures
            // using above-mentioned "caller" export:
            //
            //     @JsFun("""(f) => {
            //        (p1, p2, ...) => <wasm-exports>.__callFunction_<signatureString>(f, p1, p2, ...)
            //     }""")
            //     external fun __convertKotlinClosureToJsClosure_<signatureString>(f: structref): ExternalRef
            //
            val kotlinToJsClosureConvertor =
                context.getFileContext(currentFile).kotlinClosureToJsConverters.getOrPut(functionTypeInfo.signatureString) {
                    createKotlinToJsClosureConvertor(functionTypeInfo)
                }
            return FunctionBasedAdapter(kotlinToJsClosureConvertor)
        }

        return SendKotlinObjectToJsAdapter(this)
    }

    private fun createNullableAdapter(
        notNullType: IrType,
        isPrimitiveOrUnsigned: Boolean,
        valueAdapter: InteropTypeAdapter?
    ): InteropTypeAdapter {
        return if (isPrimitiveOrUnsigned) { //nullable primitive should be checked and adapt to target type
            val externRefToPrimitiveAdapter = when (notNullType) {
                builtIns.floatType -> adapters.externRefToKotlinFloatAdapter.owner
                builtIns.doubleType -> adapters.externRefToKotlinDoubleAdapter.owner
                builtIns.longType -> adapters.externRefToKotlinLongAdapter.owner
                builtIns.booleanType -> adapters.externRefToKotlinBooleanAdapter.owner

                symbols.uByteType -> adapters.externRefToKotlinUByteAdapter.owner
                symbols.uShortType -> adapters.externRefToKotlinUShortAdapter.owner
                symbols.uIntType -> adapters.externRefToKotlinUIntAdapter.owner
                symbols.uLongType -> adapters.externRefToKotlinULongAdapter.owner

                else -> adapters.externRefToKotlinIntAdapter.owner
            }

            val externalToPrimitiveAdapter = FunctionBasedAdapter(externRefToPrimitiveAdapter)

            NullOrAdapter(
                adapter = valueAdapter?.let { CombineAdapter(it, externalToPrimitiveAdapter) } ?: externalToPrimitiveAdapter
            )
        } else { //nullable reference should not be checked
            val nullableValueAdapter = valueAdapter?.let(::NullOrAdapter)
            val undefinedToNullAdapter = FunctionBasedAdapter(adapters.jsCheckIsNullOrUndefinedAdapter.owner)
            nullableValueAdapter
                ?.let { CombineAdapter(it, undefinedToNullAdapter) }
                ?: undefinedToNullAdapter
        }
    }

    private fun createNotNullAdapter(
        notNullType: IrType,
        isPrimitiveOrUnsigned: Boolean,
        valueAdapter: InteropTypeAdapter?
    ): InteropTypeAdapter? {
        // !nullable primitive checked by wasm signature
        if (isPrimitiveOrUnsigned) return valueAdapter

        // !nullable reference should be null checked
        // notNullAdapter((undefined -> null)!!)
        val nullCheckedValueAdapter = valueAdapter?.let(::CheckNotNullAndAdapter)
            ?: CheckNotNullNoAdapter(notNullType)

        // kotlin types could not take undefined value so just take null-checked value
        if (!isExternalType(notNullType)) return nullCheckedValueAdapter

        // js value should convert undefined into null and the null-checked
        return CombineAdapter(
            outerAdapter = nullCheckedValueAdapter,
            innerAdapter = FunctionBasedAdapter(adapters.jsCheckIsNullOrUndefinedAdapter.owner)
        )
    }

    private fun IrType.jsToKotlinAdapterIfNeeded(isReturn: Boolean): InteropTypeAdapter? {
        if (isReturn && this == builtIns.unitType)
            return null

        val notNullType = makeNotNull()
        val valueAdapter = notNullType.jsToKotlinAdapterIfNeededNotNullable(isReturn)
        val isPrimitiveOrUnsigned = (valueAdapter?.fromType ?: notNullType).let { it.isPrimitiveType() || it.isUnsigned() }

        return if (isNullable())
            createNullableAdapter(notNullType, isPrimitiveOrUnsigned, valueAdapter)
        else
            createNotNullAdapter(notNullType, isPrimitiveOrUnsigned, valueAdapter)
    }

    private fun IrType.jsToKotlinAdapterIfNeededNotNullable(isReturn: Boolean): InteropTypeAdapter? {
        if (isReturn && (this == builtIns.unitType || this == builtIns.nothingType))
            return null

        when (this) {
            builtIns.stringType -> return FunctionBasedAdapter(adapters.jsToKotlinStringAdapter.owner)
            builtIns.anyType -> return FunctionBasedAdapter(adapters.jsToKotlinAnyAdapter.owner)
            builtIns.byteType -> return FunctionBasedAdapter(adapters.jsToKotlinByteAdapter.owner)
            builtIns.shortType -> return FunctionBasedAdapter(adapters.jsToKotlinShortAdapter.owner)
            builtIns.charType -> return FunctionBasedAdapter(adapters.jsToKotlinCharAdapter.owner)

            symbols.uByteType,
            symbols.uShortType,
            symbols.uIntType,
            symbols.uLongType,
            builtIns.booleanType,
            builtIns.intType,
            builtIns.longType,
            builtIns.floatType,
            builtIns.doubleType,
            symbols.voidType ->
                return null
            else -> {}
        }

        if (isExternalType(this))
            return null

        if (isBuiltInWasmRefType(this))
            return null

        if (this is IrSimpleType && this.isFunction()) {
            val functionTypeInfo = FunctionTypeInfo(this, toJs = false)

            // JavaScript's closures are external references that cannot be called directly in WebAssembly.
            // Thus, we import helper "caller" method that WebAssembly will use to call JS closures:
            //
            //     @JsFun("(f, p0, p1, ...) => f(p0, p1, ...)")
            //     external fun __callJsClosure_<signatureString>(f: ExternalRef, p0: JsType1, p1: JsType2, ...): JsResType
            //
            val jsClosureCaller = context.getFileContext(currentFile).jsClosureCallers.getOrPut(functionTypeInfo.signatureString) {
                createJsClosureCaller(functionTypeInfo)
            }

            // Converter functions creates new Kotlin closure that delegate to JS closure
            // using above-mentioned "caller" import:
            //
            //     fun __convertJsClosureToKotlinClosure_<signatureString>(f: ExternalRef) : FunctionN<KotlinType1, ..., KotlinResType> =
            //       { p0: KotlinType1, p1: KotlinType2, ... ->
            //          adapt(__callJsClosure_<signatureString>(f, adapt(p0), adapt(p1), ..))
            //       }
            //
            val jsToKotlinClosure = context.getFileContext(currentFile).jsToKotlinClosures.getOrPut(functionTypeInfo.signatureString) {
                createJsToKotlinClosureConverter(functionTypeInfo, jsClosureCaller)
            }
            return FunctionBasedAdapter(jsToKotlinClosure)
        }

        return ReceivingKotlinObjectFromJsAdapter(this)
    }

    private fun createKotlinClosureCaller(info: FunctionTypeInfo): IrSimpleFunction {
        val result = context.irFactory.buildFun {
            name = Name.identifier("$CALL_FUNCTION${info.signatureString}")
            returnType = info.adaptedResultType
            origin = KOTLIN_TO_JS_CLOSURE_ORIGIN
        }
        result.parent = currentParent
        result.addValueParameter {
            name = Name.identifier("f")
            type = context.wasmSymbols.wasmStructRefType
        }
        var count = 0
        info.adaptedParameterTypes.forEach { type ->
            result.addValueParameter {
                this.name = Name.identifier("p" + count++.toString())
                this.type = type
            }
        }
        val builder = context.createIrBuilder(result.symbol)

        result.body = builder.irBlockBody {
            val invokeFun = info.functionType.classOrNull!!.owner.functions.single { it.name == Name.identifier("invoke") }
            val callInvoke = irCall(invokeFun.symbol, info.originalResultType).also { call ->
                call.dispatchReceiver =
                    ReceivingKotlinObjectFromJsAdapter(invokeFun.dispatchReceiverParameter!!.type)
                        .adapt(irGet(result.valueParameters[0]), builder)

                for (i in info.adaptedParameterTypes.indices) {
                    call.putValueArgument(i, info.parametersAdapters[i].adaptIfNeeded(irGet(result.valueParameters[i + 1]), builder))
                }
            }
            +irReturn(info.resultAdapter.adaptIfNeeded(callInvoke, builder))
        }

        // TODO find out a better way to export the such declarations only when it's required. Also, fix building roots for DCE, then.
        result.annotations += builder.irCallConstructor(jsRelatedSymbols.jsExportConstructor, typeArguments = emptyList())
        additionalDeclarations += result
        return result
    }

    private fun createKotlinToJsClosureConvertor(info: FunctionTypeInfo): IrSimpleFunction {
        val result = context.irFactory.buildFun {
            name = Name.identifier("__convertKotlinClosureToJsClosure_${info.signatureString}")
            returnType = jsRelatedSymbols.jsAnyType
            isExternal = true
        }
        result.parent = currentParent
        result.addValueParameter {
            name = Name.identifier("f")
            type = context.wasmSymbols.wasmStructRefType
        }
        val builder = context.createIrBuilder(result.symbol)
        // TODO: Cache created JS closures
        val arity = info.parametersAdapters.size
        val jsCode = buildString {
            append("(f) => ")
            append("getCachedJsObject(f, ")
            append("(")
            appendParameterList(arity)
            append(") => wasmExports[")
            append("$CALL_FUNCTION${info.signatureString}".toJsStringLiteral())
            append("](f, ")
            appendParameterList(arity)
            append(")")
            append(")")
        }

        result.annotations += builder.irCallConstructor(jsRelatedSymbols.jsFunConstructor, typeArguments = emptyList()).also {
            it.putValueArgument(0, builder.irString(jsCode))
        }

        additionalDeclarations += result
        return result
    }

    private fun createJsToKotlinClosureConverter(
        info: FunctionTypeInfo,
        jsClosureCaller: IrSimpleFunction,
    ): IrSimpleFunction {
        val functionType = info.functionType
        val result = context.irFactory.buildFun {
            name = Name.identifier("__convertJsClosureToKotlinClosure_${info.signatureString}")
            returnType = functionType
        }
        result.parent = currentParent
        result.addValueParameter {
            name = Name.identifier("f")
            type = jsRelatedSymbols.jsAnyType
        }

        val closureClass = context.irFactory.buildClass {
            name = Name.identifier("__JsClosureToKotlinClosure_${info.signatureString}")
        }.apply {
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes = listOf(functionType)
            parent = currentParent
        }

        val closureClassField = closureClass.addField {
            name = Name.identifier("jsClosure")
            type = jsRelatedSymbols.jsAnyType
            visibility = DescriptorVisibilities.PRIVATE
            isFinal = true
        }

        val closureClassConstructor = closureClass.addConstructor {
            isPrimary = true
        }.apply {
            val parameter = addValueParameter {
                name = closureClassField.name
                type = closureClassField.type
            }
            body = context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                +irSetField(irGet(closureClass.thisReceiver!!), closureClassField, irGet(parameter))
                +IrInstanceInitializerCallImpl(startOffset, endOffset, closureClass.symbol, context.irBuiltIns.unitType)
            }
        }

        closureClass.addFunction {
            name = Name.identifier("invoke")
            returnType = info.originalResultType
        }.apply {
            addDispatchReceiver { type = closureClass.defaultType }
            info.originalParameterTypes.forEachIndexed { index, irType ->
                addValueParameter {
                    name = Name.identifier("p$index")
                    type = irType
                }
            }
            val lambdaBuilder = context.createIrBuilder(symbol)
            body = lambdaBuilder.irBlockBody {
                val jsClosureCallerCall = irCall(jsClosureCaller)
                jsClosureCallerCall.putValueArgument(0, irGetField(irGet(dispatchReceiverParameter!!), closureClassField))
                for ((adapterIndex, paramAdapter) in info.parametersAdapters.withIndex()) {
                    jsClosureCallerCall.putValueArgument(
                        adapterIndex + 1,
                        paramAdapter.adaptIfNeeded(
                            irGet(valueParameters[adapterIndex]),
                            lambdaBuilder
                        )
                    )
                }
                +irReturn(info.resultAdapter.adaptIfNeeded(jsClosureCallerCall, lambdaBuilder))
            }

            overriddenSymbols =
                overriddenSymbols + functionType.classOrNull!!.functions.single { it.owner.name == Name.identifier("invoke") }
        }

        val builder = context.createIrBuilder(result.symbol)
        result.body = builder.irBlockBody {
            +irReturn(irCall(closureClassConstructor).also { it.putValueArgument(0, irGet(result.valueParameters[0])) })
        }

        additionalDeclarations += closureClass
        additionalDeclarations += result
        return result
    }

    private fun createJsClosureCaller(info: FunctionTypeInfo): IrSimpleFunction {
        val result = context.irFactory.buildFun {
            name = Name.identifier("__callJsClosure_${info.signatureString}")
            returnType = info.adaptedResultType
            isExternal = true
        }
        result.parent = currentParent
        result.addValueParameter {
            name = Name.identifier("f")
            type = jsRelatedSymbols.jsAnyType
        }
        val arity = info.adaptedParameterTypes.size
        repeat(arity) { paramIndex ->
            result.addValueParameter {
                name = Name.identifier("p$paramIndex")
                type = info.adaptedParameterTypes[paramIndex]
            }
        }
        val builder = context.createIrBuilder(result.symbol)
        val jsFun = buildString {
            append("(f, ")
            appendParameterList(arity)
            append(") => f(")
            appendParameterList(arity)
            append(")")
        }

        result.annotations += builder.irCallConstructor(jsRelatedSymbols.jsFunConstructor, typeArguments = emptyList()).also {
            it.putValueArgument(0, builder.irString(jsFun))
        }

        additionalDeclarations += result
        return result
    }

    inner class FunctionTypeInfo(val functionType: IrSimpleType, toJs: Boolean) {
        init {
            require(functionType.arguments.all { it is IrTypeProjection }) {
                "Star projection is not supported in function type interop ${functionType.render()}"
            }
        }

        val originalParameterTypes: List<IrType> =
            functionType.arguments.dropLast(1).map { (it as IrTypeProjection).type }

        val originalResultType: IrType =
            (functionType.arguments.last() as IrTypeProjection).type

        val parametersAdapters: List<InteropTypeAdapter?> =
            originalParameterTypes.map { parameterType ->
                if (toJs)
                    parameterType.jsToKotlinAdapterIfNeeded(isReturn = false)
                else
                    parameterType.kotlinToJsAdapterIfNeeded(isReturn = false)
            }

        val resultAdapter: InteropTypeAdapter? =
            if (toJs)
                originalResultType.kotlinToJsAdapterIfNeeded(isReturn = true)
            else
                originalResultType.jsToKotlinAdapterIfNeeded(isReturn = true)

        val adaptedParameterTypes: List<IrType> =
            originalParameterTypes.zip(parametersAdapters).map { (parameterType, adapter) ->
                (if (toJs) adapter?.fromType else adapter?.toType) ?: parameterType
            }

        val adaptedResultType: IrType =
            (if (toJs) resultAdapter?.toType else resultAdapter?.fromType) ?: originalResultType

        val signatureString: String = jsInteropNotNullTypeSignature(this)
    }

    private fun jsInteropNotNullTypeSignature(type: JsInteropFunctionsLowering.FunctionTypeInfo): String {
        val parameterTypes = type.originalParameterTypes.joinToString(separator = ",") { jsInteropTypeSignature(it) }
        val resultType = jsInteropTypeSignature(type.originalResultType)
        return "(($parameterTypes)->$resultType)"
    }

    private fun jsInteropNotNullTypeSignature(type: IrType): String {
        if (isExternalType(type)) {
            return "Js"
        }
        require(type is IrSimpleType)
        if (type.isFunction()) {
            return jsInteropNotNullTypeSignature(FunctionTypeInfo(type, true))
        }
        val klass = type.classOrNull ?: error("Unsupported JS interop type: ${type.render()}")
        if (klass.owner.packageFqName == FqName("kotlin")) {
            return klass.owner.name.identifier
        }
        error("Unsupported JS interop type: ${type.render()}")
    }

    private fun jsInteropTypeSignature(type: IrType): String {
        return if (type.isNullable()) {
            jsInteropNotNullTypeSignature(type.makeNotNull()) + "?"
        } else {
            jsInteropNotNullTypeSignature(type)
        }
    }

    interface InteropTypeAdapter {
        val fromType: IrType
        val toType: IrType
        fun adapt(expression: IrExpression, builder: IrBuilderWithScope): IrExpression
    }

    fun InteropTypeAdapter?.adaptIfNeeded(expression: IrExpression, builder: IrBuilderWithScope): IrExpression =
        this?.adapt(expression, builder) ?: expression

    /**
     * Adapter implemented as a single function call
     */
    class FunctionBasedAdapter(
        private val function: IrSimpleFunction,
    ) : InteropTypeAdapter {
        override val fromType = function.valueParameters[0].type
        override val toType = function.returnType
        override fun adapt(expression: IrExpression, builder: IrBuilderWithScope): IrExpression {
            val call = builder.irCall(function)
            call.putValueArgument(0, expression)
            return call
        }
    }

    class CombineAdapter(
        private val outerAdapter: InteropTypeAdapter,
        private val innerAdapter: InteropTypeAdapter,
    ) : InteropTypeAdapter {
        override val fromType = innerAdapter.fromType
        override val toType = outerAdapter.toType
        override fun adapt(expression: IrExpression, builder: IrBuilderWithScope): IrExpression {
            return outerAdapter.adapt(innerAdapter.adapt(expression, builder), builder)
        }
    }

    /**
     * Current V8 Wasm GC mandates structref type instead of structs and arrays
     */
    inner class SendKotlinObjectToJsAdapter(
        override val fromType: IrType
    ) : InteropTypeAdapter {
        override val toType: IrType = context.wasmSymbols.wasmStructRefType
        override fun adapt(expression: IrExpression, builder: IrBuilderWithScope): IrExpression {
            return builder.irReinterpretCast(expression, toType)
        }
    }

    /**
     * Current V8 Wasm GC mandates structref type instead of structs and arrays
     */
    inner class ReceivingKotlinObjectFromJsAdapter(
        override val toType: IrType
    ) : InteropTypeAdapter {
        override val fromType: IrType = context.wasmSymbols.wasmStructRefType
        override fun adapt(expression: IrExpression, builder: IrBuilderWithScope): IrExpression {
            val call = builder.irCall(context.wasmSymbols.refCastNull)
            call.putValueArgument(0, expression)
            call.putTypeArgument(0, toType)
            return call
        }
    }

    /**
     * Current V8 Wasm GC mandates structref type instead of structs and arrays
     */

    /**
     * Effectively `value!!`
     */
    inner class CheckNotNullNoAdapter(type: IrType) : InteropTypeAdapter {
        override val fromType: IrType = type.makeNullable()
        override val toType: IrType = type.makeNotNull()
        override fun adapt(expression: IrExpression, builder: IrBuilderWithScope): IrExpression {
            return builder.irComposite {
                val tmp = irTemporary(expression)
                +irIfNull(
                    type = toType,
                    subject = irGet(tmp),
                    thenPart = builder.irCall(symbols.throwNullPointerException),
                    elsePart = irGet(tmp)
                )
            }
        }
    }

    /**
     * Effectively `value?.let { adapter(it) }`
     */
    inner class NullOrAdapter(
        private val adapter: InteropTypeAdapter
    ) : InteropTypeAdapter {
        override val fromType: IrType = adapter.fromType.makeNullable()
        override val toType: IrType = adapter.toType.makeNullable()
        override fun adapt(expression: IrExpression, builder: IrBuilderWithScope): IrExpression {
            return builder.irComposite {
                val tmp = irTemporary(expression)
                +irIfNull(
                    type = toType,
                    subject = irGet(tmp),
                    thenPart = irNull(toType),
                    elsePart = irImplicitCast(adapter.adapt(irGet(tmp), builder), toType)
                )
            }
        }
    }

    /**
     * Effectively `adapter(value!!)`
     */
    inner class CheckNotNullAndAdapter(
        private val adapter: InteropTypeAdapter
    ) : InteropTypeAdapter {
        override val fromType: IrType = adapter.fromType.makeNullable()
        override val toType: IrType = adapter.toType
        override fun adapt(expression: IrExpression, builder: IrBuilderWithScope): IrExpression {
            return builder.irComposite {
                val temp = irTemporary(expression)
                +irIfNull(
                    type = toType,
                    subject = irGet(temp),
                    thenPart = irCall(this@JsInteropFunctionsLowering.context.wasmSymbols.throwNullPointerException),
                    elsePart = adapter.adapt(irImplicitCast(irGet(temp), adapter.fromType.makeNotNull()), builder),
                )
            }
        }
    }

    /**
     * Vararg parameter adapter
     */
    inner class CopyToJsArrayAdapter(
        override val fromType: IrType,
        private val fromElementType: IrType,
    ) : InteropTypeAdapter {
        override val toType: IrType =
            jsRelatedSymbols.jsAnyType

        private val elementAdapter =
            primitivesToExternRefAdapters[fromElementType]
                ?: fromElementType.kotlinToJsAdapterIfNeeded(false)

        private val arrayClass = fromType.classOrNull!!
        private val getMethod = arrayClass.getSimpleFunction("get")!!.owner
        private val sizeMethod = arrayClass.getPropertyGetter("size")!!.owner

        override fun adapt(expression: IrExpression, builder: IrBuilderWithScope): IrExpression {
            return builder.irComposite {
                val originalArrayVar = irTemporary(expression)

                //  val newJsArray = []
                //  var index = 0
                //  while(index != size) {
                //      newJsArray.push(adapt(originalArray[index]));
                //      index++
                //  }
                val newJsArrayVar = irTemporary(irCall(jsRelatedSymbols.newJsArray))
                val indexVar = irTemporary(irInt(0), isMutable = true)
                val arraySizeVar = irTemporary(irCall(sizeMethod).apply { dispatchReceiver = irGet(originalArrayVar) })

                +irWhile().apply {
                    condition = irNotEquals(irGet(indexVar), irGet(arraySizeVar))
                    body = irBlock {
                        val adaptedValue = elementAdapter.adaptIfNeeded(
                            irImplicitCast(
                                irCall(getMethod).apply {
                                    dispatchReceiver = irGet(originalArrayVar)
                                    putValueArgument(0, irGet(indexVar))
                                },
                                fromElementType
                            ),
                            this@irBlock
                        )
                        +irCall(jsRelatedSymbols.jsArrayPush).apply {
                            putValueArgument(0, irGet(newJsArrayVar))
                            putValueArgument(1, adaptedValue)
                        }
                        val inc = indexVar.type.getClass()!!.functions.single { it.name == OperatorNameConventions.INC }
                        +irSet(
                            indexVar,
                            irCallOp(inc.symbol, indexVar.type, irGet(indexVar)),
                            origin = IrStatementOrigin.PREFIX_INCR
                        )
                    }
                }
                +irGet(newJsArrayVar)
            }
        }
    }

    companion object {
        const val CALL_FUNCTION = "__callFunction_"
    }
}

internal fun StringBuilder.appendParameterList(size: Int, name: String = "p", isEnd: Boolean = true) =
    repeat(size) {
        append(name)
        append(it)
        if (!isEnd || it + 1 < size)
            append(", ")
    }

/**
 * Redirect calls to external and @JsExport functions to created wrappers
 */
class JsInteropFunctionCallsLowering(val context: WasmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (!context.isWasmJsTarget) return
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid()
                val newFun: IrSimpleFunction? = context.mapping.wasmJsInteropFunctionToWrapper[expression.symbol.owner]
                return if (newFun != null && container != newFun) {
                    irCall(expression, newFun)
                } else {
                    expression
                }
            }
        })
    }
}

/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.copyAsValueParameter
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.symbols.JsSymbolBuilder
import org.jetbrains.kotlin.ir.backend.js.symbols.initialize
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

// TODO: generate $metadata$ property and fill it with corresponding KFunction/KProperty interface
class CallableReferenceLowering(val context: JsIrBackendContext) {

    private data class CallableReferenceKey(
        val declaration: IrFunction,
        val hasDispatchReference: Boolean,
        val hasExtensionReceiver: Boolean
    )

    private val callableToGetterFunction = mutableMapOf<CallableReferenceKey, IrFunction>()
    private val collectedReferenceMap = mutableMapOf<CallableReferenceKey, IrCallableReference>()

    private val callableNameConst = JsIrBuilder.buildString(context.irBuiltIns.stringType, Namer.KCALLABLE_NAME)
    private val getterConst = JsIrBuilder.buildString(context.irBuiltIns.stringType, Namer.KPROPERTY_GET)
    private val setterConst = JsIrBuilder.buildString(context.irBuiltIns.stringType, Namer.KPROPERTY_SET)

    private val newDeclarations = mutableListOf<IrDeclaration>()

    fun getReferenceCollector() = object : FileLoweringPass {
        private val collector = CallableReferenceCollector()
        override fun lower(irFile: IrFile) = irFile.acceptVoid(collector)
    }::lower

    fun getClosureBuilder() = object : FileLoweringPass {
        override fun lower(irFile: IrFile) {
            newDeclarations.clear()
            buildClosures(irFile)
            irFile.declarations += newDeclarations
        }

    }::lower

    fun getReferenceReplacer() = object : FileLoweringPass {
        private val replacer = CallableReferenceTransformer()
        override fun lower(irFile: IrFile) {
            irFile.transformChildrenVoid(replacer)
        }
    }::lower

    private fun makeCallableKey(declaration: IrFunction, reference: IrCallableReference) =
        CallableReferenceKey(declaration, reference.dispatchReceiver != null, reference.extensionReceiver != null)

    inner class CallableReferenceCollector : IrElementVisitorVoid {
        override fun visitFunctionReference(expression: IrFunctionReference) {
            collectedReferenceMap[makeCallableKey(expression.symbol.owner, expression)] = expression
        }

        override fun visitPropertyReference(expression: IrPropertyReference) {
            //Note: The getter is taken because the `invoke()` function of the resulted reference has to be corresponding getter call
            collectedReferenceMap[makeCallableKey(expression.getter!!.owner, expression)] = expression
        }

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }
    }

    private fun buildClosures(irFile: IrFile) {

        val declarationsSet = mutableSetOf<IrFunctionSymbol>()
        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

            override fun visitFunction(declaration: IrFunction) {
                super.visitFunction(declaration)
                declarationsSet += declaration.symbol
            }
        })


        for (v in collectedReferenceMap.values) {
            newDeclarations += v.accept(object : IrElementVisitor<List<IrDeclaration>, Nothing?> {
                override fun visitElement(element: IrElement, data: Nothing?) = error("Unreachable execution")
                override fun visitFunctionReference(expression: IrFunctionReference, data: Nothing?) =
                    if (expression.symbol in declarationsSet) lowerKFunctionReference(expression.symbol.owner, expression) else emptyList()

                override fun visitPropertyReference(expression: IrPropertyReference, data: Nothing?) =
                    if (expression.getter in declarationsSet) lowerKPropertyReference(
                        expression.getter!!.owner,
                        expression
                    ) else emptyList()
            }, null)
        }
    }

    inner class CallableReferenceTransformer : IrElementTransformerVoid() {
        override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
            return callableToGetterFunction[makeCallableKey(expression.symbol.owner, expression)]?.let {
                redirectToFunction(expression, it)
            } ?: expression
        }

        override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
            return callableToGetterFunction[makeCallableKey(expression.getter!!.owner, expression)]?.let {
                redirectToFunction(expression, it)
            } ?: expression
        }

        private fun redirectToFunction(callable: IrCallableReference, newTarget: IrFunction) =
            IrCallImpl(
                callable.startOffset, callable.endOffset,
                newTarget.symbol.owner.returnType,
                newTarget.symbol,
                newTarget.symbol.descriptor,
                callable.origin
            ).apply {
                copyTypeArgumentsFrom(callable)
                var index = 0
                callable.dispatchReceiver?.let { putValueArgument(index++, it) }
                callable.extensionReceiver?.let { putValueArgument(index++, it) }
                for (i in 0 until callable.valueArgumentsCount) {
                    val arg = callable.getValueArgument(i)
                    if (arg != null) {
                        putValueArgument(index++, arg)
                    }
                }
            }
    }

    private fun createFunctionClosureGetterName(descriptor: CallableDescriptor) = createHelperFunctionName(descriptor, "KReferenceGet")
    private fun createPropertyClosureGetterName(descriptor: CallableDescriptor) = createHelperFunctionName(descriptor, "KPropertyGet")
    private fun createClosureInstanceName(descriptor: CallableDescriptor) = createHelperFunctionName(descriptor, "KReferenceClosure")

    private fun createHelperFunctionName(descriptor: CallableDescriptor, suffix: String): String {
        val nameBuilder = StringBuilder()
        if (descriptor is ClassConstructorDescriptor) {
            nameBuilder.append(descriptor.constructedClass.fqNameSafe)
            nameBuilder.append('_')
        }
        nameBuilder.append(descriptor.name)
        nameBuilder.append('_')
        nameBuilder.append(suffix)
        return nameBuilder.toString()
    }


    private fun getReferenceName(descriptor: CallableDescriptor): String {
        if (descriptor is ClassConstructorDescriptor) {
            return descriptor.constructedClass.name.identifier
        }
        return descriptor.name.identifier
    }

    private fun lowerKFunctionReference(declaration: IrFunction, functionReference: IrFunctionReference): List<IrDeclaration> {
        // transform
        // x = Foo::bar ->
        // x = Foo_bar_KreferenceGet(c1: closure$C1, c2: closure$C2) : KFunctionN<Foo, T2, ..., TN, TReturn> {
        //   [ if ($cache$ == null) { ] // in case reference has no closure param cache it
        //     val x = fun Foo_bar_KreferenceClosure(p0: Foo, p1: T2, p2: T3): TReturn {
        //        return p0.bar(c1, c2, p1, p2)
        //     }
        //     x.callableName = "bar"
        //   [ $cache$ = x } ]
        //   return {$cache$|x}
        // }

        // KFunctionN<Foo, T2, ..., TN, TReturn>, arguments.size = N + 1

        val refGetFunction = buildGetFunction(declaration, functionReference, createFunctionClosureGetterName(declaration.descriptor))
        val refClosureFunction = buildClosureFunction(declaration, refGetFunction, functionReference)

        val additionalDeclarations = generateGetterBodyWithGuard(refGetFunction) {
            val irClosureReference = JsIrBuilder.buildFunctionReference(functionReference.type, refClosureFunction.symbol)
            val irVarSymbol = JsSymbolBuilder.buildTempVar(refGetFunction.symbol, irClosureReference.type)
            val irVar = JsIrBuilder.buildVar(irVarSymbol, irClosureReference, type = irClosureReference.type)

            // TODO: fill other fields of callable reference (returnType, parameters, isFinal, etc.)
            val irSetName = JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVarSymbol))
                putValueArgument(1, callableNameConst)
                putValueArgument(2, JsIrBuilder.buildString(context.irBuiltIns.stringType, getReferenceName(declaration.descriptor)))
            }
            Pair(listOf(irVar, irSetName), irVarSymbol)
        }


        callableToGetterFunction[makeCallableKey(declaration, functionReference)] = refGetFunction

        return additionalDeclarations + listOf(refGetFunction)
    }

    private fun lowerKPropertyReference(getterDeclaration: IrFunction, propertyReference: IrPropertyReference): List<IrDeclaration> {
        // transform
        // x = Foo::bar ->
        // x = Foo_bar_KreferenceGet() : KPropertyN<Foo, PType> {
        //   if ($cache$ == null) { // very likely property reference is going to be cached
        //     val x = fun Foo_bar_KreferenceClosure_get(r: Foo): PType {
        //        return r.<get>()
        //     }
        //     x.get = x
        //     x.callableName = "bar"
        //     if (mutable) {
        //       x.set = fun Foo_bar_KreferenceClosure_set(r: Foo, v: PType>) {
        //         r.<set>(v)
        //       }
        //     }
        //     $cache$ = x
        //   }
        //   return $cache$
        // }

        val getterName = createPropertyClosureGetterName(propertyReference.descriptor)
        val refGetFunction = buildGetFunction(propertyReference.getter!!.owner, propertyReference, getterName)

        val getterFunction = propertyReference.getter?.let { buildClosureFunction(it.owner, refGetFunction, propertyReference) }!!
        val setterFunction = propertyReference.setter?.let { buildClosureFunction(it.owner, refGetFunction, propertyReference) }

        val additionalDeclarations = generateGetterBodyWithGuard(refGetFunction) {
            val statements = mutableListOf<IrStatement>()

            val getterFunctionType = context.builtIns.getFunction(getterFunction.valueParameters.size + 1)
            val type = getterFunctionType.toIrType()
            val irGetReference = JsIrBuilder.buildFunctionReference(type, getterFunction.symbol)
            val irVarSymbol = JsSymbolBuilder.buildTempVar(refGetFunction.symbol, type)

            statements += JsIrBuilder.buildVar(irVarSymbol, irGetReference, type = type)

            statements += JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVarSymbol))
                putValueArgument(1, getterConst)
                putValueArgument(2, JsIrBuilder.buildGetValue(irVarSymbol))
            }

            if (setterFunction != null) {
                val setterFunctionType = context.builtIns.getFunction(setterFunction.valueParameters.size + 1)
                val irSetReference = JsIrBuilder.buildFunctionReference(setterFunctionType.toIrType(), setterFunction.symbol)
                statements += JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                    putValueArgument(0, JsIrBuilder.buildGetValue(irVarSymbol))
                    putValueArgument(1, setterConst)
                    putValueArgument(2, irSetReference)
                }
            }

            // TODO: fill other fields of callable reference (returnType, parameters, isFinal, etc.)
            statements += JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVarSymbol))
                putValueArgument(1, callableNameConst)
                putValueArgument(2, JsIrBuilder.buildString(context.irBuiltIns.stringType, getReferenceName(propertyReference.descriptor)))
            }

            Pair(statements, irVarSymbol)
        }

        callableToGetterFunction[makeCallableKey(getterDeclaration, propertyReference)] = refGetFunction

        return additionalDeclarations + listOf(refGetFunction)
    }

    private fun generateGetterBodyWithGuard(
        getterFunction: IrSimpleFunction,
        builder: () -> Pair<List<IrStatement>, IrValueSymbol>
    ): List<IrDeclaration> {

        val (bodyStatements, varSymbol) = builder()
        val statements = mutableListOf<IrStatement>()
        val returnValue: IrExpression
        val returnStatements: List<IrDeclaration>
        if (getterFunction.valueParameters.isEmpty()) {
            // compose cache for 'direct' closure
            // if ($cache$ === null) {
            //   $cache$ = <body>
            // }
            //
            val cacheName = "${getterFunction.name}_${Namer.KCALLABLE_CACHE_SUFFIX}"
            val type = getterFunction.returnType
            val cacheVarSymbol =
                JsSymbolBuilder.buildVar(getterFunction.descriptor.containingDeclaration, type, cacheName, true)
            val irNull = JsIrBuilder.buildNull(context.irBuiltIns.nothingNType)
            val irCacheDeclaration = JsIrBuilder.buildVar(cacheVarSymbol, irNull, type = type)
            val irCacheValue = JsIrBuilder.buildGetValue(cacheVarSymbol)
            val irIfCondition = JsIrBuilder.buildCall(context.irBuiltIns.eqeqSymbol).apply {
                putValueArgument(0, irCacheValue)
                putValueArgument(1, irNull)
            }
            val irSetCache = JsIrBuilder.buildSetVariable(cacheVarSymbol, JsIrBuilder.buildGetValue(varSymbol), context.irBuiltIns.unitType)
            val thenStatements = mutableListOf<IrStatement>().apply {
                addAll(bodyStatements)
                add(irSetCache)
            }
            val irThenBranch = JsIrBuilder.buildBlock(context.irBuiltIns.unitType, thenStatements)
            val irIfNode = JsIrBuilder.buildIfElse(context.irBuiltIns.unitType, irIfCondition, irThenBranch)
            statements += irIfNode
            returnValue = irCacheValue
            returnStatements = listOf(irCacheDeclaration)
        } else {
            statements += bodyStatements
            returnValue = JsIrBuilder.buildGetValue(varSymbol)
            returnStatements = emptyList()
        }

        statements += JsIrBuilder.buildReturn(getterFunction.symbol, returnValue, context.irBuiltIns.nothingType)

        getterFunction.body = JsIrBuilder.buildBlockBody(statements)
        return returnStatements
    }

    private fun generateSignatureForClosure(
        callable: IrFunction,
        getter: IrFunction,
        closure: IrSimpleFunctionSymbol,
        reference: IrCallableReference
    ): List<IrValueParameterSymbol> {
        val result = mutableListOf<IrValueParameterSymbol>()

        if (callable.dispatchReceiverParameter != null && reference.dispatchReceiver == null) {
            result.add(JsSymbolBuilder.buildValueParameter(closure, result.size, callable.dispatchReceiverParameter!!.type))
        }

        if (callable.extensionReceiverParameter != null && reference.extensionReceiver == null) {
            result.add(JsSymbolBuilder.buildValueParameter(closure, result.size, callable.extensionReceiverParameter!!.type))
        }

        for (i in getter.valueParameters.size until callable.valueParameters.size) {
            val param = callable.valueParameters[i]
            val paramName = param.name.run { if (!isSpecial) identifier else null }
            result += JsSymbolBuilder.buildValueParameter(closure, result.size, param.type, paramName)
        }

        return result
    }

    private fun buildGetFunction(declaration: IrFunction, reference: IrCallableReference, getterName: String): IrSimpleFunction {

        val callableType = reference.type
        var kFunctionValueParamsCount = (callableType as? IrSimpleType)?.arguments?.size?.minus(1) ?: 0

        if (declaration.dispatchReceiverParameter != null && reference.dispatchReceiver == null) {
            kFunctionValueParamsCount--
        }

        if (declaration.extensionReceiverParameter != null && reference.extensionReceiver == null) {
            kFunctionValueParamsCount--
        }

        assert(kFunctionValueParamsCount >= 0)

        // The `getter` function takes only closure parameters
        val receivers = mutableListOf<IrValueParameter>()
        if (reference.dispatchReceiver != null) {
            receivers += declaration.dispatchReceiverParameter!!
        }
        if (reference.extensionReceiver != null) {
            receivers += declaration.extensionReceiverParameter!!
        }

        val getterValueParameters = receivers + declaration.valueParameters.dropLast(kFunctionValueParamsCount)


        val refGetSymbol = JsSymbolBuilder.buildSimpleFunction(declaration.descriptor.containingDeclaration, getterName).apply {
            initialize(
                valueParameters = getterValueParameters.mapIndexed { i, p -> p.descriptor.copyAsValueParameter(descriptor, i) },
                returnType = callableType
            )
        }

        return JsIrBuilder.buildFunction(refGetSymbol, callableType).apply {
            for (i in 0 until getterValueParameters.size) {
                val p = getterValueParameters[i]
                valueParameters +=
                        IrValueParameterImpl(
                            p.startOffset,
                            p.endOffset,
                            p.origin,
                            refGetSymbol.descriptor.valueParameters[i],
                            p.type,
                            p.varargElementType
                        )
            }
        }
    }

    private fun buildClosureFunction(
        declaration: IrFunction,
        refGetFunction: IrSimpleFunction,
        reference: IrCallableReference
    ): IrFunction {
        val closureName = createClosureInstanceName(declaration.descriptor)
        val refClosureSymbol = JsSymbolBuilder.buildSimpleFunction(refGetFunction.descriptor, closureName)

        // the params which are passed to closure
        val closureParamSymbols = generateSignatureForClosure(declaration, refGetFunction, refClosureSymbol, reference)
        val closureParamDescriptors = closureParamSymbols.map { it.descriptor as ValueParameterDescriptor }

        val returnType = declaration.returnType
        refClosureSymbol.initialize(valueParameters = closureParamDescriptors, returnType = returnType)

        val closureFunction = JsIrBuilder.buildFunction(refClosureSymbol, returnType)

        for (param in closureParamSymbols) {
            // TODO always take type from param
            val type = if (param.isBound) param.owner.type else context.irBuiltIns.anyType
            closureFunction.valueParameters += JsIrBuilder.buildValueParameter(param, type)
        }

        val irCall = JsIrBuilder.buildCall(declaration.symbol, type = returnType)

        var cp = 0
        var gp = 0

        if (declaration.dispatchReceiverParameter != null) {
            val dispatchReceiverDeclaration =
                if (reference.dispatchReceiver != null) refGetFunction.valueParameters[gp++].symbol else closureParamSymbols[cp++]
            irCall.dispatchReceiver = JsIrBuilder.buildGetValue(dispatchReceiverDeclaration)
        }

        if (declaration.extensionReceiverParameter != null) {
            val extensionReceiverDeclaration =
                if (reference.extensionReceiver != null) refGetFunction.valueParameters[gp++].symbol else closureParamSymbols[cp++]
            irCall.extensionReceiver = JsIrBuilder.buildGetValue(extensionReceiverDeclaration)
        }

        var j = 0

        for (i in gp until refGetFunction.valueParameters.size) {
            irCall.putValueArgument(j++, JsIrBuilder.buildGetValue(refGetFunction.valueParameters[i].symbol))
        }

        for (i in cp until closureParamSymbols.size) {
            irCall.putValueArgument(j++, JsIrBuilder.buildGetValue(closureParamSymbols[i]))
        }

        val irClosureReturn = JsIrBuilder.buildReturn(closureFunction.symbol, irCall, context.irBuiltIns.nothingType)

        closureFunction.body = JsIrBuilder.buildBlockBody(listOf(irClosureReturn))

        return closureFunction
    }
}
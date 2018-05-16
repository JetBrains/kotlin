/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.copyAsValueParameter
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
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
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType

class CallableReferenceLowering(val context: JsIrBackendContext) : FileLoweringPass, DeclarationContainerLoweringPass {

    private val callableToGetterFunction = mutableMapOf<CallableDescriptor, IrFunction>()

    // TODO: replace descriptor usage with symbol instead
    private val collectedReferenceMap = mutableMapOf<IrDeclaration, IrCallableReference>()

    private val callableNameConst = JsIrBuilder.buildString(context.irBuiltIns.string, Namer.KCALLABLE_NAME)
    private val getterConst = JsIrBuilder.buildString(context.irBuiltIns.string, Namer.KPROPERTY_GET)
    private val setterConst = JsIrBuilder.buildString(context.irBuiltIns.string, Namer.KPROPERTY_SET)

    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(CallableReferenceCollector())
        runOnFilePostfix(irFile)
        irFile.transformChildrenVoid(CallableReferenceTransformer())
    }

    inner class CallableReferenceCollector : IrElementVisitorVoid {
        override fun visitFunctionReference(expression: IrFunctionReference) {
            collectedReferenceMap[expression.symbol.owner] = expression
        }

        override fun visitPropertyReference(expression: IrPropertyReference) {
            //Note: The getter is taken because the `invoke()` function of the resulted reference has to be corresponding getter call
            collectedReferenceMap[expression.getter!!.owner] = expression
        }

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }
    }

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat { d ->
            collectedReferenceMap[d]?.accept(object : IrElementVisitor<List<IrDeclaration>, IrFunction> {
                override fun visitElement(element: IrElement, data: IrFunction) = error("Unreachable execution")
                override fun visitFunctionReference(expression: IrFunctionReference, data: IrFunction) =
                    lowerKFunctionReference(data, expression)

                override fun visitPropertyReference(expression: IrPropertyReference, data: IrFunction) =
                    lowerKPropertyReference(data, expression)
            }, d as IrFunction)
        }
    }

    inner class CallableReferenceTransformer : IrElementTransformerVoid() {
        override fun visitCallableReference(expression: IrCallableReference) = callableToGetterFunction[expression.descriptor]?.let {
            redirectToFunction(expression, it)
        } ?: expression

        private fun redirectToFunction(callable: IrCallableReference, newTarget: IrFunction) =
            IrCallImpl(callable.startOffset, callable.endOffset, newTarget.symbol, callable.origin).apply {
                copyTypeArgumentsFrom(callable)
                var index = 0
                for (i in 0 until callable.valueArgumentsCount) {
                    val arg = callable.getValueArgument(i)
                    if (arg != null) {
                        putValueArgument(index++, arg)
                    }
                }
            }
    }

    private fun createClosureGetterName(descriptor: CallableDescriptor) = createHelperFunctionName(descriptor, "KReferenceGet")
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

        val refGetFunction = buildGetFunction(declaration, functionReference.type, createClosureGetterName(declaration.descriptor))
        val refClosureFunction = buildClosureFunction(declaration, refGetFunction)

        val additionalDeclarations = generateGetterBodyWithGuard(refGetFunction) {
            val irClosureReference = JsIrBuilder.buildFunctionReference(functionReference.type, refClosureFunction.symbol)
            val irVarSymbol = JsSymbolBuilder.buildTempVar(refGetFunction.symbol, irClosureReference.type)
            val irVar = JsIrBuilder.buildVar(irVarSymbol, irClosureReference)

            // TODO: fill other fields of callable reference (returnType, parameters, isFinal, etc.)
            val irSetName = JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVarSymbol))
                putValueArgument(1, callableNameConst)
                putValueArgument(2, JsIrBuilder.buildString(context.irBuiltIns.string, getReferenceName(declaration.descriptor)))
            }
            Pair(listOf(irVar, irSetName), irVarSymbol)
        }

        callableToGetterFunction[functionReference.descriptor] = refGetFunction

        return additionalDeclarations + listOf(declaration, refGetFunction)
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
        val refGetFunction = buildGetFunction(propertyReference.getter!!.owner, propertyReference.type, getterName)

        val getterFunction = propertyReference.getter?.let { buildClosureFunction(it.owner, refGetFunction) }!!
        val setterFunction = propertyReference.setter?.let { buildClosureFunction(it.owner, refGetFunction) }

        val additionalDeclarations = generateGetterBodyWithGuard(refGetFunction) {
            val statements = mutableListOf<IrStatement>()

            val getterFunctionType = context.builtIns.getFunction(getterFunction.valueParameters.size + 1)
            val irGetReference = JsIrBuilder.buildFunctionReference(getterFunctionType.defaultType, getterFunction.symbol)
            val irVarSymbol = JsSymbolBuilder.buildTempVar(refGetFunction.symbol, getterFunctionType.defaultType)

            statements += JsIrBuilder.buildVar(irVarSymbol, irGetReference)

            JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).run {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVarSymbol))
                putValueArgument(1, getterConst)
                putValueArgument(2, JsIrBuilder.buildGetValue(irVarSymbol))
                statements += this
            }

            if (setterFunction != null) {
                val setterFunctionType = context.builtIns.getFunction(setterFunction.valueParameters.size + 1)
                val irSetReference = JsIrBuilder.buildFunctionReference(setterFunctionType.defaultType, setterFunction.symbol)
                JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).run {
                    putValueArgument(0, JsIrBuilder.buildGetValue(irVarSymbol))
                    putValueArgument(1, setterConst)
                    putValueArgument(2, irSetReference)
                    statements += this
                }
            }

            // TODO: fill other fields of callable reference (returnType, parameters, isFinal, etc.)
            JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).run {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVarSymbol))
                putValueArgument(1, callableNameConst)
                putValueArgument(2, JsIrBuilder.buildString(context.irBuiltIns.string, getReferenceName(propertyReference.descriptor)))
                statements += this
            }

            Pair(statements, irVarSymbol)
        }

        callableToGetterFunction[propertyReference.descriptor] = refGetFunction

        return additionalDeclarations + listOf(getterDeclaration, refGetFunction)
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
            val cacheVarSymbol =
                JsSymbolBuilder.buildVar(getterFunction.descriptor.containingDeclaration, getterFunction.returnType, cacheName, true)
            val irCacheNull = JsIrBuilder.buildNull(cacheVarSymbol.descriptor.type)
            val irCacheDeclaration = JsIrBuilder.buildVar(cacheVarSymbol, irCacheNull)
            val irCacheValue = JsIrBuilder.buildGetValue(cacheVarSymbol)
            val irIfCondition = JsIrBuilder.buildCall(context.irBuiltIns.eqeqeqSymbol).apply {
                putValueArgument(0, irCacheValue)
                putValueArgument(1, irCacheNull)
            }
            val irSetCache = JsIrBuilder.buildSetVariable(cacheVarSymbol, JsIrBuilder.buildGetValue(varSymbol))
            val thenStatements = mutableListOf<IrStatement>().apply {
                addAll(bodyStatements)
                add(irSetCache)
            }
            val irThenBranch = JsIrBuilder.buildBlock(context.irBuiltIns.unit, thenStatements)
            val irIfNode = JsIrBuilder.buildIfElse(context.irBuiltIns.unit, irIfCondition, irThenBranch)
            statements += irIfNode
            returnValue = irCacheValue
            returnStatements = listOf(irCacheDeclaration)
        } else {
            statements += bodyStatements
            returnValue = JsIrBuilder.buildGetValue(varSymbol)
            returnStatements = emptyList()
        }
        statements += JsIrBuilder.buildReturn(getterFunction.symbol, returnValue)

        getterFunction.body = JsIrBuilder.buildBlockBody(statements)
        return returnStatements
    }

    private fun generateSignatureForClosure(
        callable: IrFunctionSymbol,
        getter: IrSimpleFunctionSymbol,
        closure: IrSimpleFunctionSymbol
    ): List<IrValueParameterSymbol> {
        val result = mutableListOf<IrValueParameterSymbol>()

        callable.owner.dispatchReceiverParameter?.run { result.add(JsSymbolBuilder.buildValueParameter(closure, result.size, type)) }
        callable.owner.extensionReceiverParameter?.run { result.add(JsSymbolBuilder.buildValueParameter(closure, result.size, type)) }

        for (i in getter.owner.valueParameters.size until callable.owner.valueParameters.size) {
            val param = callable.owner.valueParameters[i]
            val paramName = param.name.run { if (!isSpecial) identifier else null }
            result += JsSymbolBuilder.buildValueParameter(closure, result.size, param.type, paramName)
        }

        return result
    }

    private fun buildGetFunction(declaration: IrFunction, callableType: KotlinType, getterName: String): IrSimpleFunction {

        val closureParams = callableType.arguments.dropLast(1) // drop return type
        var kFunctionValueParamsCount = closureParams.size
        if (declaration.dispatchReceiverParameter != null) kFunctionValueParamsCount--
        if (declaration.extensionReceiverParameter != null) kFunctionValueParamsCount--

        assert(kFunctionValueParamsCount >= 0)

        // The `getter` function takes only closure parameters
        val getterValueParameters = declaration.valueParameters.dropLast(kFunctionValueParamsCount)

        val refGetSymbol = JsSymbolBuilder.buildSimpleFunction(declaration.descriptor.containingDeclaration, getterName).apply {
            initialize(
                valueParameters = getterValueParameters.mapIndexed { i, p -> p.descriptor.copyAsValueParameter(descriptor, i) },
                type = callableType
            )
        }

        return JsIrBuilder.buildFunction(refGetSymbol).apply {
            for (i in 0 until getterValueParameters.size) {
                val p = getterValueParameters[i]
                valueParameters += IrValueParameterImpl(p.startOffset, p.endOffset, p.origin, refGetSymbol.descriptor.valueParameters[i])
            }
        }
    }

    private fun buildClosureFunction(declaration: IrFunction, refGetFunction: IrSimpleFunction): IrFunction {
        val closureName = createClosureInstanceName(declaration.descriptor)
        val refClosureSymbol = JsSymbolBuilder.buildSimpleFunction(refGetFunction.descriptor, closureName)

        // the params which are passed to closure
        val closureParamSymbols = generateSignatureForClosure(declaration.symbol, refGetFunction.symbol, refClosureSymbol)
        val closureParamDescriptors = closureParamSymbols.map { it.descriptor as ValueParameterDescriptor }

        refClosureSymbol.initialize(valueParameters = closureParamDescriptors, type = declaration.returnType)

        return JsIrBuilder.buildFunction(refClosureSymbol).apply {
            for (it in closureParamSymbols) {
                valueParameters += JsIrBuilder.buildValueParameter(it)
            }

            val irCall = JsIrBuilder.buildCall(declaration.symbol)

            var p = 0
            declaration.dispatchReceiverParameter?.run { irCall.dispatchReceiver = JsIrBuilder.buildGetValue(closureParamSymbols[p++]) }
            declaration.extensionReceiverParameter?.run { irCall.extensionReceiver = JsIrBuilder.buildGetValue(closureParamSymbols[p++]) }

            var j = 0
            for (v in refGetFunction.valueParameters) {
                irCall.putValueArgument(j++, JsIrBuilder.buildGetValue(v.symbol))
            }

            for (i in p until closureParamSymbols.size) {
                irCall.putValueArgument(j++, JsIrBuilder.buildGetValue(closureParamSymbols[i]))
            }

            val irClosureReturn = JsIrBuilder.buildReturn(symbol, irCall)

            body = JsIrBuilder.buildBlockBody(listOf(irClosureReturn))
        }
    }
}
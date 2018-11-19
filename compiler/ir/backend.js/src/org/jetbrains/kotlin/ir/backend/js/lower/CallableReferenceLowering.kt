/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedTypeParameterDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.backend.common.lower.BOUND_VALUE_PARAMETER
import org.jetbrains.kotlin.backend.common.utils.isSubtypeOf
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.util.isInlined
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

data class CallableReferenceKey(
    val declaration: IrFunction,
    val hasDispatchReceiver: Boolean,
    val hasExtensionReceiver: Boolean,
    val signature: String
)

// TODO: generate $metadata$ property and fill it with corresponding KFunction/KProperty interface
class CallableReferenceLowering(val context: JsIrBackendContext) : FileLoweringPass {
    private val callableNameConst get() = JsIrBuilder.buildString(context.irBuiltIns.stringType, Namer.KCALLABLE_NAME)
    private val getterConst get() = JsIrBuilder.buildString(context.irBuiltIns.stringType, Namer.KPROPERTY_GET)
    private val setterConst get() = JsIrBuilder.buildString(context.irBuiltIns.stringType, Namer.KPROPERTY_SET)
    private val callableToFactoryFunction = context.callableReferencesCache

    private val newDeclarations = mutableListOf<IrDeclaration>()
    private val implicitDeclarationFile = context.implicitDeclarationFile

    override fun lower(irFile: IrFile) {
        newDeclarations.clear()
        irFile.transformChildrenVoid(CallableReferenceLowerTransformer())
        implicitDeclarationFile.declarations += newDeclarations
    }

    private fun makeCallableKey(declaration: IrFunction, reference: IrCallableReference) =
        CallableReferenceKey(
            declaration,
            reference.dispatchReceiver != null,
            reference.extensionReceiver != null,
            reference.type.asString()
        )

    inner class CallableReferenceLowerTransformer : IrElementTransformerVoid() {
        override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
            expression.transformChildrenVoid(this)
            val declaration = expression.symbol.owner
            if (declaration.origin == JsIrBackendContext.callableClosureOrigin) return expression
            val key = makeCallableKey(declaration, expression)
            val factory = callableToFactoryFunction.getOrPut(key) { lowerKFunctionReference(declaration, expression) }
            return redirectToFunction(expression, factory)
        }

        override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
            expression.transformChildrenVoid(this)
            val declaration = expression.getter!!.owner
            val key = makeCallableKey(declaration, expression)
            val factory = callableToFactoryFunction.getOrPut(key) { lowerKPropertyReference(declaration, expression) }
            return redirectToFunction(expression, factory)
        }

        override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference): IrExpression {
            expression.transformChildrenVoid(this)
            val key = makeCallableKey(expression.getter.owner, expression)
            val factory = callableToFactoryFunction.getOrPut(key) { lowerLocalKPropertyReference(expression) }
            return redirectToFunction(expression, factory)
        }
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

    private fun createFunctionFactoryName(declaration: IrDeclaration) = createHelperFunctionName(declaration, "KFunctionFactory")
    private fun createPropertyFactoryName(declaration: IrDeclaration) = createHelperFunctionName(declaration, "KPropertyFactory")
    private fun createClosureInstanceName(declaration: IrDeclaration) = createHelperFunctionName(declaration, "KReferenceClosure")

    private fun createHelperFunctionName(declaration: IrDeclaration, suffix: String): String {
        val nameBuilder = StringBuilder()
        if (declaration is IrConstructor) {
            val klass = declaration.parent as IrClass
            nameBuilder.append(klass.name.asString())
            nameBuilder.append('_')
        }

        when (declaration) {
            is IrFunction -> nameBuilder.append(declaration.name)
            is IrProperty -> nameBuilder.append(declaration.name)
            is IrVariable -> nameBuilder.append(declaration.name)
            else -> TODO("Unexpected declaration type")
        }

        nameBuilder.append('_')
        nameBuilder.append(suffix)
        return nameBuilder.toString()
    }


    private fun getReferenceName(declaration: IrDeclaration) = when (declaration) {
        is IrConstructor -> (declaration.parent as IrClass).name.identifier
        is IrProperty -> declaration.name.identifier
        is IrSimpleFunction -> declaration.name.asString()
        is IrVariable -> declaration.name.asString().replace("\$delegate", "")
        else -> TODO("Unexpected declaration type")
    }

    private fun lowerKFunctionReference(declaration: IrFunction, functionReference: IrFunctionReference): IrSimpleFunction {
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

        val factoryFunction = buildFactoryFunction(declaration, functionReference, createFunctionFactoryName(declaration))
        val closureFunction = buildClosureFunction(declaration, factoryFunction, functionReference, functionReference.type.arity)

        val additionalDeclarations = generateFactoryBodyWithGuard(factoryFunction) {
            val irClosureReference = JsIrBuilder.buildFunctionReference(functionReference.type, closureFunction.symbol)

            val irVar = JsIrBuilder.buildVar(irClosureReference.type, factoryFunction, initializer = irClosureReference)

            // TODO: fill other fields of callable reference (returnType, parameters, isFinal, etc.)
            val irSetName = JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVar.symbol))
                putValueArgument(1, callableNameConst)
                putValueArgument(2, JsIrBuilder.buildString(context.irBuiltIns.stringType, getReferenceName(declaration)))
            }
            Pair(listOf(closureFunction, irVar, irSetName), irVar.symbol)
        }

        newDeclarations += additionalDeclarations + factoryFunction

        return factoryFunction
    }

    private fun lowerKPropertyReference(getterDeclaration: IrSimpleFunction, propertyReference: IrPropertyReference): IrSimpleFunction {
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

        val arity = propertyReference.type.arity
        val factoryName = createPropertyFactoryName(getterDeclaration.correspondingProperty!!)
        val factoryFunction = buildFactoryFunction(propertyReference.getter!!.owner, propertyReference, factoryName)

        val getterFunction = propertyReference.getter?.let { buildClosureFunction(it.owner, factoryFunction, propertyReference, arity) }!!
        val setterFunction = propertyReference.setter?.let { buildClosureFunction(it.owner, factoryFunction, propertyReference, arity + 1) }

        val additionalDeclarations = generateFactoryBodyWithGuard(factoryFunction) {
            val statements = mutableListOf<IrStatement>(getterFunction)

            val getterFunctionType = context.builtIns.getFunction(getterFunction.valueParameters.size + 1)
            val type = getterFunctionType.toIrType(symbolTable = context.symbolTable)
            val irGetReference = JsIrBuilder.buildFunctionReference(type, getterFunction.symbol)
            val irVar = JsIrBuilder.buildVar(type, factoryFunction, initializer = irGetReference)

            statements += irVar

            statements += JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVar.symbol))
                putValueArgument(1, getterConst)
                putValueArgument(2, JsIrBuilder.buildGetValue(irVar.symbol))
            }

            if (setterFunction != null) {
                statements += setterFunction
                val setterFunctionType = context.builtIns.getFunction(setterFunction.valueParameters.size + 1)
                val irSetReference = JsIrBuilder.buildFunctionReference(
                    setterFunctionType.toIrType(symbolTable = context.symbolTable),
                    setterFunction.symbol
                )
                statements += JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                    putValueArgument(0, JsIrBuilder.buildGetValue(irVar.symbol))
                    putValueArgument(1, setterConst)
                    putValueArgument(2, irSetReference)
                }
            }

            // TODO: fill other fields of callable reference (returnType, parameters, isFinal, etc.)
            statements += JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVar.symbol))
                putValueArgument(1, callableNameConst)
                putValueArgument(
                    2, JsIrBuilder.buildString(
                        context.irBuiltIns.stringType,
                        getReferenceName(getterDeclaration.correspondingProperty!!)
                    )
                )
            }

            Pair(statements, irVar.symbol)
        }

        newDeclarations += additionalDeclarations + factoryFunction

        return factoryFunction
    }

    private fun lowerLocalKPropertyReference(propertyReference: IrLocalDelegatedPropertyReference): IrSimpleFunction {
        // transform
        // ::bar ->
        // Foo_bar_KreferenceGet() : KPropertyN<Foo, PType> {
        //   if ($cache$ == null) {
        //     val x = fun Foo_bar_KreferenceClosure_get(): PType {
        //        throw IllegalStateException()
        //     }
        //     x.get = x
        //     x.callableName = "bar"
        //     $cache$ = x
        //   }
        //   return $cache$
        // }

        val arity = propertyReference.type.arity
        val declaration = propertyReference.delegate.owner
        val factoryName = createPropertyFactoryName(declaration)
        val factoryFunction = buildFactoryFunction(propertyReference.getter.owner, propertyReference, factoryName)
        val closureFunction = buildClosureFunction(context.irBuiltIns.throwIseFun, factoryFunction, propertyReference, arity)

        val additionalDeclarations = generateFactoryBodyWithGuard(factoryFunction) {
            val statements = mutableListOf<IrStatement>(closureFunction)

            val getterFunctionType = context.builtIns.getFunction(closureFunction.valueParameters.size + 1)
            val type = getterFunctionType.toIrType(symbolTable = context.symbolTable)
            val irGetReference = JsIrBuilder.buildFunctionReference(type, closureFunction.symbol)
            val irVar = JsIrBuilder.buildVar(type, factoryFunction, initializer = irGetReference)
            val irVarSymbol = irVar.symbol

            statements += irVar

            statements += JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVarSymbol))
                putValueArgument(1, getterConst)
                putValueArgument(2, JsIrBuilder.buildGetValue(irVarSymbol))
            }

            statements += JsIrBuilder.buildCall(context.intrinsics.jsSetJSField.symbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(irVarSymbol))
                putValueArgument(1, callableNameConst)
                putValueArgument(2, JsIrBuilder.buildString(context.irBuiltIns.stringType, getReferenceName(declaration)))
            }

            Pair(statements, irVarSymbol)
        }

        newDeclarations += additionalDeclarations + factoryFunction

        return factoryFunction
    }

    private fun generateFactoryBodyWithGuard(
        factoryFunction: IrSimpleFunction,
        builder: () -> Pair<List<IrStatement>, IrValueSymbol>
    ): List<IrDeclaration> {

        val (bodyStatements, varSymbol) = builder()
        val statements = mutableListOf<IrStatement>()
        val returnValue: IrExpression
        val returnStatements: List<IrDeclaration>
        if (factoryFunction.valueParameters.isEmpty()) {
            // compose cache for 'direct' closure
            // if ($cache$ === null) {
            //   $cache$ = <body>
            // }
            //
            val cacheName = "${factoryFunction.name}_${Namer.KCALLABLE_CACHE_SUFFIX}"
            val type = factoryFunction.returnType
            val irNull = { JsIrBuilder.buildNull(context.irBuiltIns.nothingNType) }
            val cacheVar = JsIrBuilder.buildVar(type, factoryFunction.parent, cacheName, true, initializer = irNull())

            val irCacheValue = { JsIrBuilder.buildGetValue(cacheVar.symbol) }
            val irIfCondition = JsIrBuilder.buildCall(context.irBuiltIns.eqeqSymbol).apply {
                putValueArgument(0, irCacheValue())
                putValueArgument(1, irNull())
            }
            val irSetCache =
                JsIrBuilder.buildSetVariable(cacheVar.symbol, JsIrBuilder.buildGetValue(varSymbol), context.irBuiltIns.unitType)
            val thenStatements = mutableListOf<IrStatement>().apply {
                addAll(bodyStatements)
                add(irSetCache)
            }
            val irThenBranch = JsIrBuilder.buildBlock(context.irBuiltIns.unitType, thenStatements)
            val irIfNode = JsIrBuilder.buildIfElse(context.irBuiltIns.unitType, irIfCondition, irThenBranch)
            statements += irIfNode
            returnValue = irCacheValue()
            returnStatements = listOf(cacheVar)
        } else {
            statements += bodyStatements
            returnValue = JsIrBuilder.buildGetValue(varSymbol)
            returnStatements = emptyList()
        }

        statements += JsIrBuilder.buildReturn(factoryFunction.symbol, returnValue, context.irBuiltIns.nothingType)

        factoryFunction.body = JsIrBuilder.buildBlockBody(statements)
        return returnStatements
    }

    private fun IrType.boxIfInlined() = if (isInlined()) {
        context.irBuiltIns.anyNType
    } else {
        this
    }

    private fun generateSignatureForClosure(
        callable: IrFunction,
        factory: IrFunction,
        closure: IrSimpleFunction,
        reference: IrCallableReference,
        arity: Int
    ): List<IrValueParameter> {
        val result = mutableListOf<IrValueParameter>()

        /*
         * class D {
         *   fun foo(a, b, c) {} <= callable signature
         * }
         *
         * val d = D()
         * val reference = d::foo <= binds dispatch receiver
         *
         * is translated into
         *
         * function foo_get(d) { <= factory signature(d)
         *   return function (a, b, c) { <= result signature (a, b, c)
         *     return d.foo(a, b, c)
         *   }
         * }
         */
        var capturedParams = factory.valueParameters.size
        val functionSignature = reference.type.arguments.dropLast(1).map { (it as IrTypeProjection).type }.toList()

        callable.dispatchReceiverParameter?.let { dispatch ->
            if (reference.dispatchReceiver == null) {
                result.add(JsIrBuilder.buildValueParameter(dispatch.name, result.size, dispatch.type.boxIfInlined()).also {
                    it.parent = closure
                })
            } else {
                // do not add dispatch receiver in result signature if it is bound
                capturedParams--
            }
        }

        callable.extensionReceiverParameter?.let { ext ->
            if (reference.extensionReceiver == null) {
                result.add(JsIrBuilder.buildValueParameter(ext.name, result.size, ext.type.boxIfInlined()).also { it.parent = closure })
            } else {
                // the same as for dispatch
                capturedParams--
            }
        }

        for ((index, param) in (result.size until arity).zip(callable.valueParameters.drop(capturedParams))) {
            val type = if (index < functionSignature.size) functionSignature[index] else param.type
            val paramName = param.name.run { if (!isSpecial) identifier else "p$index" }
            result += JsIrBuilder.buildValueParameter(paramName, result.size, type.boxIfInlined()).also { it.parent = closure }
        }

        return result
    }

    private fun buildFactoryFunction(declaration: IrFunction, reference: IrCallableReference, getterName: String): IrSimpleFunction {
        // The `getter` function takes only closure parameters
        val receivers = mutableListOf<IrValueParameter>()

        reference.dispatchReceiver?.let {
            // in case outer `this` for inner constructors
            receivers += declaration.dispatchReceiverParameter ?: declaration.valueParameters[0]
        }

        reference.extensionReceiver?.let {
            receivers += declaration.extensionReceiverParameter!!
        }

        val boundValueParameters = receivers + declaration.valueParameters.filter { it.origin == BOUND_VALUE_PARAMETER }

        val factoryDeclaration = JsIrBuilder.buildFunction(getterName, reference.type, implicitDeclarationFile, declaration.visibility)

        for ((i, p) in boundValueParameters.withIndex()) {
            val descriptor = WrappedValueParameterDescriptor()
            factoryDeclaration.valueParameters += IrValueParameterImpl(
                p.startOffset,
                p.endOffset,
                p.origin,
                IrValueParameterSymbolImpl(descriptor),
                p.name,
                i,
                p.type,
                p.varargElementType,
                p.isCrossinline,
                p.isNoinline
            ).also {
                descriptor.bind(it)
                it.parent = factoryDeclaration
            }
        }

        val typeParameters =
            if (declaration is IrConstructor) (declaration.parent as IrClass).typeParameters else declaration.typeParameters

        for (t in typeParameters) {
            val descriptor = WrappedTypeParameterDescriptor()
            factoryDeclaration.typeParameters += IrTypeParameterImpl(
                t.startOffset,
                t.endOffset,
                t.origin,
                IrTypeParameterSymbolImpl(descriptor),
                t.name,
                t.index,
                t.isReified,
                t.variance
            ).also {
                descriptor.bind(it)
                it.parent = factoryDeclaration
            }
        }

        return factoryDeclaration
    }

    private val IrType.arguments get() = (this as? IrSimpleType)?.arguments ?: emptyList()
    private val IrType.arity get() = arguments.size - 1

    private fun buildClosureFunction(
        declaration: IrFunction,
        factoryFunction: IrSimpleFunction,
        reference: IrCallableReference,
        arity: Int
    ): IrFunction {
        val closureName = createClosureInstanceName(declaration)
        val returnType = declaration.returnType.boxIfInlined()
        val closureFunction =
            JsIrBuilder.buildFunction(
                closureName,
                returnType,
                factoryFunction,
                Visibilities.LOCAL,
                origin = JsIrBackendContext.callableClosureOrigin
            )

        // the params which are passed to closure
        val boundParamSymbols = factoryFunction.valueParameters.map { it.symbol }
        val unboundParamDeclarations = generateSignatureForClosure(declaration, factoryFunction, closureFunction, reference, arity)
        val unboundParamSymbols = unboundParamDeclarations.map { it.symbol }

        closureFunction.valueParameters += unboundParamDeclarations

        val callTarget = context.ir.defaultParameterDeclarationsCache[declaration] ?: declaration

        val irCall = JsIrBuilder.buildCall(callTarget.symbol, type = returnType)

        var cp = 0
        var gp = 0

        if (callTarget.dispatchReceiverParameter != null) {
            val dispatchReceiverDeclaration =
                if (reference.dispatchReceiver != null) boundParamSymbols[gp++] else unboundParamSymbols[cp++]
            irCall.dispatchReceiver = JsIrBuilder.buildGetValue(dispatchReceiverDeclaration)
        }

        if (callTarget.extensionReceiverParameter != null) {
            val extensionReceiverDeclaration =
                if (reference.extensionReceiver != null) boundParamSymbols[gp++] else unboundParamSymbols[cp++]
            irCall.extensionReceiver = JsIrBuilder.buildGetValue(extensionReceiverDeclaration)
        }

        var j = 0

        for (i in gp until boundParamSymbols.size) {
            irCall.putValueArgument(j++, JsIrBuilder.buildGetValue(boundParamSymbols[i]))
        }

        for (i in cp until unboundParamSymbols.size) {
            val closureParam = unboundParamSymbols[i].owner
            val value = JsIrBuilder.buildGetValue(unboundParamSymbols[i])
            val parameter = callTarget.valueParameters[j]
            val argument = if (parameter.varargElementType?.let { closureParam.type.isSubtypeOf(it) } == true) {
                // fun foo(x: X, y: vararg Y): Z
                // val r: (X, Y) -> Z = ::foo
                IrVarargImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, parameter.type, parameter.varargElementType!!, listOf(value))
            } else value
            irCall.putValueArgument(j++, argument)
        }

        val irClosureReturn = JsIrBuilder.buildReturn(closureFunction.symbol, irCall, context.irBuiltIns.nothingType)

        closureFunction.body = JsIrBuilder.buildBlockBody(listOf(irClosureReturn))

        return closureFunction
    }
}

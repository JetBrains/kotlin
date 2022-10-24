/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.ir.ValueRemapper
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.calls.getFunction
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irEqeqeqWithoutBox
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrRawFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

object ES6_THIS_VARIABLE_ORIGIN : IrDeclarationOriginImpl("ES6_THIS_VARIABLE_ORIGIN")
object ES6_UTILITY_PARAMETER_ORIGIN : IrDeclarationOriginImpl("ES6_SUPER_PARAMETERS_ORIGIN")

class ES6ConstructorLowering(val context: JsIrBackendContext) : DeclarationTransformer {
    private val patchedConstructorSymbols = mutableSetOf<IrConstructorSymbol>()

    private val secondaryParamsType get() = context.irBuiltIns.arrayClass.typeWith(context.irBuiltIns.anyNType)
    private val secondaryHandlerDecl get() = context.irBuiltIns.functionN(2)
    private val IrFunction.secondaryHandlerType
        get() = secondaryHandlerDecl.typeWith(
            parentAsClass.defaultType,
            secondaryParamsType,
            context.irBuiltIns.unitType
        )

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!context.es6mode || declaration !is IrConstructor) return null

        hackEnums(declaration)
        hackExceptions(context, declaration)

        val superCall = declaration.getSuperCall()
        val parentClass = declaration.parentAsClass
        val containsSecondaryConstructor = !declaration.isPrimary || parentClass.containsSecondaryConstructors()

        if (!declaration.shouldModify(superCall, containsSecondaryConstructor)) return null

        return if (declaration.isPrimary) {
            declaration.transformPrimaryConstructor(containsSecondaryConstructor, superCall)
        } else {
            declaration.transformSecondaryConstructor(superCall)
        }
    }

    private fun IrConstructor.shouldModify(superCall: IrDelegatingConstructorCall?, containsSecondaryConstructor: Boolean): Boolean {
        return !isExpect &&
                !isExternal &&
                body != null &&
                !context.inlineClassesUtils.isClassInlineLike(parentAsClass) &&
                (containsSecondaryConstructor || superCall?.symbol?.owner?.isPrimary == false)
    }

    private fun IrClass.containsSecondaryConstructors(): Boolean {
        return declarations.any { it is IrConstructor && !it.isPrimary }
    }

    private fun IrConstructor.getSuperCall(): IrDelegatingConstructorCall? {
        var result: IrDelegatingConstructorCall? = null
        (body as IrBlockBody).acceptChildren(object : IrElementVisitor<Unit, Any?> {
            override fun visitElement(element: IrElement, data: Any?) {}

            override fun visitBlock(expression: IrBlock, data: Any?) {
                expression.statements.forEach { it.accept(this, data) }
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Any?) {
                result = result ?: expression
            }
        }, null)
        return result
    }

    /*
    * Foo::constructor(...) {
    *   body
    * }
    * =>
    * Foo::constructor(..., $secondary, $secondaryParams, $superParams) {
    *   super($superParams[0], $superParams[1], ..., $superParams[SUPER_CONSTRUCTOR_ARGS_COUNT])
    *   body
    *   if ($secondary !== undefined) $secondary(this, $secondaryParams)
    * }
    *
    * WHERE:
    *     $secondary - reference to the function which contains a body of a secondary constructor (need only if the parent class contains any secondary constructor)
    *     $secondaryParams - array of params which should be provided for a specific `init` function (need only if the parent class contains any secondary constructor)
    *     $superParams - array of parameters for super class constructor (need if the parent class is a subclass, and there is only secondaries constructors or primary constructor call a secondary constructor of the super class)
    * */
    private fun IrConstructor.transformPrimaryConstructor(
        containsSecondaryConstructor: Boolean,
        superCall: IrDelegatingConstructorCall?,
    ): List<IrDeclaration>? {
        val body = this.body as IrBlockBody
        val isSyntheticPrimary = isSynthetic

        var superParams: IrValueDeclaration? = null
        var secondaryInit: IrValueParameter? = null
        var secondaryParams: IrValueParameter? = null

        valueParameters = buildList {
            addAll(valueParameters)

            if (containsSecondaryConstructor) {
                secondaryInit = generateSecondaryHandler(size).also(::add)
                secondaryParams = generateSecondaryParams(size).also(::add)
            }

            if (isSyntheticPrimary && superCall != null) {
                superParams = generateSuperParams(size).also(::add)
            }
        }

        if (!isSyntheticPrimary && superCall?.symbol?.owner?.isPrimary == false) {
            superParams = generateSuperParamsVariable(superCall)
        }

        if (superParams != null) {
            patchDelegatedSuperCalls(superParams!!.symbol)
            (superParams as? IrVariable)?.let { body.statements.add(0, it) }
        }

        if (secondaryInit != null) {
            body.statements.add(generateSecondaryHandlerCall(secondaryInit!!, secondaryParams!!, isSyntheticPrimary))
        }

        return null
    }

    /*
    * Foo::constructor(...) {
    *   body
    * }
    * =>
    * Foo_init_$Init$($this, $secondaryParams) {
    *   a = $secondaryParams[0]
    *   b = $secondaryParams[1]
    *   ...
    *   body[ this = $this ]
    * }
    * Foo_init_$Params$(...) {
    *   return arrayOf(...$PRIMARY_CONSTRUCTOR_PARAMS, Foo_init_$Init$, arrayOf(...), Super_init_$Params$(...))
    * }
    * Foo_init_$Create$(...) {
    *   val params = Foo_init_$Params$(...)
    *   return new Foo(params[0], params[1], ..., params[N])
    * }
    *
    * WHERE:
    *     $secondaryParams - array of params which should be provided for a specific `init` function (need only if the parent class contains any secondary constructor)
    */
    private fun IrConstructor.transformSecondaryConstructor(superCall: IrDelegatingConstructorCall?): List<IrDeclaration> {
        val irClass = parentAsClass
        val initFunction = generateInitFunction(irClass)
        val paramsFunction = generateParamsFunction(irClass, superCall, initFunction)
        return listOf(initFunction, paramsFunction, generateCreateFunction(irClass, paramsFunction))
    }

    private fun IrConstructor.generateInitFunction(irClass: IrClass): IrSimpleFunction {
        val constructorName = "${irClass.name}_init"
        val functionName = "${constructorName}_\$Init\$"

        return factory.buildFun {
            updateFrom(this@generateInitFunction)
            name = Name.identifier(functionName)
            returnType = context.irBuiltIns.unitType
            origin = JsIrBuilder.SYNTHESIZED_DECLARATION
        }.also {
            it.parent = parent
            it.copyTypeParametersFrom(irClass)

            val thisParam = it.generateThisParam(irClass)
            it.valueParameters += thisParam

            val secondaryParams = it.generateSecondaryParams()
            it.valueParameters += secondaryParams

            it.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                val mappings = valueParameters
                    .mapIndexed { i, p ->
                        val variable = it.generateParamVariable(i, p, secondaryParams)
                        statements += variable
                        p.symbol to variable.symbol
                    }
                    .plus(irClass.thisReceiver!!.symbol to thisParam.symbol)
                    .toMap<IrValueSymbol, IrValueSymbol>()
                statements += body!!.deepCopyWithSymbols(it).statements
                transformChildrenVoid(InitFunctionBodyRemapper(mappings, context))
            }
        }
    }

    private fun IrConstructor.generateParamsFunction(
        irClass: IrClass,
        superCall: IrDelegatingConstructorCall?,
        initFunction: IrSimpleFunction
    ): IrSimpleFunction {
        val constructorName = "${irClass.name}_init"
        val functionName = "${constructorName}_\$Params\$"
        val superCtor = superCall?.symbol?.owner
        val isDelegatedToPrimary = superCtor?.let { it.parentAsClass.symbol == irClass.symbol && it.isPrimary } ?: false

        return factory.buildFun {
            updateFrom(this@generateParamsFunction)
            name = Name.identifier(functionName)
            returnType = secondaryParamsType
            origin = JsIrBuilder.SYNTHESIZED_DECLARATION
        }.also {
            it.parent = parent
            it.valueParameters = valueParameters.map { p -> p.deepCopyWithSymbols(it) }
            it.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                val parameters = buildList {
                    if (superCtor != null && isDelegatedToPrimary) {
                        superCtor.valueParameters
                            .filter { it.origin != ES6_UTILITY_PARAMETER_ORIGIN }
                            .forEachIndexed { i, _ ->
                                add(superCall.takeIf { i < superCall.valueArgumentsCount }?.getValueArgument(i) ?: context.getVoid())
                            }
                    }

                    add(IrRawFunctionReferenceImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.dynamicType, initFunction.symbol))
                    add(irArrayOf(it.valueParameters.map { p -> JsIrBuilder.buildGetValue(p.symbol) }))

                    if (superCtor != null && !isDelegatedToPrimary) {
                        if (superCtor.isPrimary) {
                            val superPrimaryParams = List(superCall.valueArgumentsCount) { i ->
                                superCall.getValueArgument(i) ?: context.getVoid()
                            }
                            add(irArrayOf(superPrimaryParams))
                        } else {
                            add(superCall.deepCopyWithSymbols(it))
                        }
                    }
                }
                statements += JsIrBuilder.buildReturn(it.symbol, irArrayOf(parameters), secondaryParamsType)
            }

            context.mapping.secondaryConstructorToDelegate[this] = it
        }
    }

    private fun IrConstructor.generateCreateFunction(irClass: IrClass, paramsFunction: IrSimpleFunction): IrSimpleFunction {
        val constructorName = "${irClass.name}_init"
        val functionName = "${constructorName}_\$Create\$"
        val primaryConstructor = irClass.primaryConstructor!!

        return factory.buildFun {
            updateFrom(this@generateCreateFunction)
            name = Name.identifier(functionName)
            returnType = irClass.defaultType
            origin = JsIrBuilder.SYNTHESIZED_DECLARATION
        }.also {
            it.parent = parent
            it.valueParameters = valueParameters.map { p -> p.deepCopyWithSymbols(it) }
            it.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                val params = generateAllParamsVariable(paramsFunction)
                statements += params
                val primaryConstructorCall = JsIrBuilder.buildConstructorCall(primaryConstructor.symbol).apply {
                    primaryConstructor.valueParameters.forEachIndexed { i, _ ->
                        putValueArgument(i, irArrayAccess(JsIrBuilder.buildGetValue(params.symbol), i))
                    }
                }
                statements += JsIrBuilder.buildReturn(it.symbol, primaryConstructorCall, irClass.defaultType)
            }

            context.mapping.secondaryConstructorToFactory[this] = it
        }
    }

    private fun IrConstructor.patchDelegatedSuperCalls(superParamsSymbol: IrValueSymbol) {
        transformChildrenVoid(
            object : IrElementTransformerVoid() {
                override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                    val symbol = expression.symbol
                    val owner = symbol.owner
                    val newSymbol = symbol.takeIf { owner.isPrimary } ?: owner.parentAsClass.primaryConstructor!!.symbol
                    val argumentsCount = newSymbol.owner.countOfPatchedArguments

                    return IrDelegatingConstructorCallImpl(
                        expression.startOffset,
                        expression.endOffset,
                        expression.type,
                        newSymbol,
                        expression.typeArgumentsCount,
                        argumentsCount
                    ).apply {
                        (0 until argumentsCount).forEach {
                            putValueArgument(it, irArrayAccess(JsIrBuilder.buildGetValue(superParamsSymbol), it))
                        }
                    }
                }
            }
        )
    }

    private val IrConstructor.countOfPatchedArguments: Int
        get() {
            var initialParamSize = valueParameters.size

            if (patchedConstructorSymbols.contains(symbol)) {
                return initialParamSize
            }

            val superCall = getSuperCall()
            val containsSecondaryConstructor = parentAsClass.containsSecondaryConstructors()

            if (!shouldModify(superCall, containsSecondaryConstructor)) {
                return initialParamSize
            }

            if (containsSecondaryConstructor) {
                initialParamSize += 2
            }

            if (isSynthetic && superCall != null) {
                initialParamSize += 1
            }

            return initialParamSize
        }

    private fun IrFunction.generateAllParamsVariable(paramsFunction: IrSimpleFunction): IrVariable {
        return with(context.createIrBuilder(symbol)) {
            buildVariable(
                this@generateAllParamsVariable,
                startOffset,
                endOffset,
                origin,
                Name.identifier("\$params"),
                secondaryHandlerType,
                isConst = true
            ).apply {
                initializer = irCall(paramsFunction).apply {
                    valueParameters.forEachIndexed { i, p ->
                        putValueArgument(i, irGet(p))
                    }
                }
            }
        }
    }

    private fun IrFunction.generateSuperParamsVariable(superCall: IrDelegatingConstructorCall): IrVariable {
        return with(context.createIrBuilder(symbol)) {
            buildVariable(
                this@generateSuperParamsVariable,
                startOffset,
                endOffset,
                origin,
                Name.identifier(Namer.SUPER_PARAMS),
                secondaryHandlerType,
                isConst = true
            ).apply {
                initializer = superCall.deepCopyWithSymbols(this@generateSuperParamsVariable)
            }
        }
    }

    private fun IrFunction.generateParamVariable(index: Int, param: IrValueParameter, secondaryParams: IrValueParameter): IrVariable {
        return buildVariable(
            this@generateParamVariable,
            param.startOffset,
            param.endOffset,
            param.origin,
            param.name,
            param.type,
            isConst = true
        ).apply {
            initializer = irArrayAccess(JsIrBuilder.buildGetValue(secondaryParams.symbol), index)
        }
    }

    private fun irArrayAccess(target: IrExpression, index: Int): IrCall {
        val intType = context.irBuiltIns.intType
        val arrayGetFunction = this@ES6ConstructorLowering.context.intrinsics.array.getFunction

        return JsIrBuilder.buildCall(arrayGetFunction).apply {
            dispatchReceiver = target
            putValueArgument(0, index.toIrConst(intType))
        }
    }

    private fun IrFunction.generateSecondaryHandler(i: Int = valueParameters.size): IrValueParameter {
        return buildValueParameter(this) {
            name = Name.identifier(Namer.SECONDARY_INIT)
            isHidden = true
            type = secondaryHandlerType
            origin = ES6_UTILITY_PARAMETER_ORIGIN
            index = i
        }
    }

    private fun IrFunction.generateSecondaryParams(i: Int = valueParameters.size): IrValueParameter {
        return buildValueParameter(this) {
            name = Name.identifier(Namer.SECONDARY_PARAMS)
            isHidden = true
            type = secondaryParamsType
            origin = ES6_UTILITY_PARAMETER_ORIGIN
            index = i
        }
    }

    private fun IrFunction.generateSuperParams(i: Int = valueParameters.size): IrValueParameter {
        return buildValueParameter(this) {
            name = Name.identifier(Namer.SUPER_PARAMS)
            isHidden = true
            type = secondaryParamsType
            origin = ES6_UTILITY_PARAMETER_ORIGIN
            index = i
        }
    }

    private fun IrFunction.generateThisParam(irClass: IrClass): IrValueParameter {
        return buildValueParameter(this) {
            name = Name.identifier(Namer.SYNTHETIC_RECEIVER_NAME)
            type = irClass.defaultType
            origin = ES6_THIS_VARIABLE_ORIGIN
            index = valueParameters.size
        }
    }


    private fun IrConstructor.generateSecondaryHandlerCall(
        initFn: IrValueParameter,
        initParams: IrValueParameter,
        isSynthetic: Boolean
    ): IrStatement {
        return with(context.createIrBuilder(symbol, startOffset, endOffset)) {
            JsIrBuilder.buildCall(secondaryHandlerDecl.invokeFun!!.symbol)
                .apply {
                    dispatchReceiver = irGet(initFn)
                    putValueArgument(0, irGet(parentAsClass.thisReceiver!!))
                    putValueArgument(1, irGet(initParams))
                }
                .run {
                    if (!isSynthetic) {
                        this
                    } else {
                        irIfThen(irNot(irEqeqeqWithoutBox(irGet(initFn), this@ES6ConstructorLowering.context.getVoid())), this)
                    }
                }

        }
    }

    private fun irArrayOf(elements: List<IrExpression>): IrExpression {
        return IrVarargImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            secondaryParamsType,
            context.irBuiltIns.anyNType,
            elements
        )
    }

    private val IrConstructor.isSynthetic get() = origin === PrimaryConstructorLowering.SYNTHETIC_PRIMARY_CONSTRUCTOR
}

private class InitFunctionBodyRemapper(
    symbolMapping: Map<IrValueSymbol, IrValueSymbol>,
    val context: JsIrBackendContext
) : ValueRemapper(symbolMapping) {
    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
        return JsIrBuilder.buildGetObjectValue(context.irBuiltIns.unitType, context.irBuiltIns.unitClass);
    }
}

private fun hackEnums(constructor: IrConstructor) {
    constructor.transformChildren(object : IrElementTransformerVoid() {
        override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
            return (expression.argument as? IrDelegatingConstructorCall) ?: expression
        }
    }, null)
}

/**
 * Swap call synthetic primary ctor and call extendThrowable
 */
private fun hackExceptions(context: JsIrBackendContext, constructor: IrConstructor) {
    val setPropertiesSymbol = context.setPropertiesToThrowableInstanceSymbol

    val statements = (constructor.body as IrBlockBody).statements

    var callIndex = -1
    var superCallIndex = -1
    for (i in statements.indices) {
        val s = statements[i]

        if (s is IrCall && s.symbol === setPropertiesSymbol) {
            callIndex = i
        }
        if (s is IrDelegatingConstructorCall && s.symbol.owner.origin === PrimaryConstructorLowering.SYNTHETIC_PRIMARY_CONSTRUCTOR) {
            superCallIndex = i
        }
    }

    if (callIndex != -1 && superCallIndex != -1) {
        val tmp = statements[callIndex]
        statements[callIndex] = statements[superCallIndex]
        statements[superCallIndex] = tmp
    }
}

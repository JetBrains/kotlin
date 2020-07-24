/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.ES6AddInternalParametersToConstructorPhase.ES6_INIT_BOX_PARAMETER
import org.jetbrains.kotlin.ir.backend.js.lower.ES6AddInternalParametersToConstructorPhase.ES6_RESULT_TYPE_PARAMETER
import org.jetbrains.kotlin.ir.backend.js.lower.PrimaryConstructorLowering.SYNTHETIC_PRIMARY_CONSTRUCTOR
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

object ES6_THIS_VARIABLE_ORIGIN : IrDeclarationOriginImpl("ES6_THIS_VARIABLE_ORIGIN")

class ES6ConstructorLowering(val context: JsIrBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (!context.es6mode) return

        if (container !is IrConstructor) return

        if (container.hasStrictSignature()) return

        hackEnums(container)
        hackExceptions(context, container)

        val superCall = getSuperCall(container) ?: return
        val superCtor = superCall.symbol.owner
        val helper = LowerCtorHelper(context, container, superCtor)

        if (container.isPrimary) {
            if (superCtor.isPrimary) primaryToPrimary(helper)
            else primaryToSecondary(helper)
        } else {
            if (superCtor.isPrimary) secondaryToPrimary(helper)
            else secondaryToSecondary(helper)

            replaceCallToDefaultPrimary(context, container)
            changeIrConstructorToIrFunction(context, container)
        }
    }

    /**
     * constructor(args, box) {
     *   var currBox = box || {}
     *
     *   //1. Superclass is Any
     *   Object.assign(this, currBox)
     *   //body
     *
     *   //2. Base class isInline or isExternal or array/string
     *   super(args)
     *   Object.assign(this, currBox)
     *   //body
     *
     *   //3. Base class !isInline and !isExternal and !(array/string)
     *   //fill initialization box
     *   super(args, currBox)
     *   //body
     * }
     */
    private fun primaryToPrimary(helper: LowerCtorHelper) = with(helper) {
        statements.add(0, boxOrEmptyObject)

        if (superCtor.parentAsClass.defaultType.isAny()) {
            //after superCall
            statements.add(1, openBoxStatement(thisSymbol, boxSymbol))
            return
        }

        if (superCtor.hasStrictSignature()) {
            //after superCall
            statements.add(2, openBoxStatement(thisSymbol, boxSymbol))
        } else {
            fillInitializerBox(boxSymbol, constructor)
            putBoxToSuperCall(boxSymbol, constructor)
        }
    }

    /**
     * constructor(args, box) {
     *   var currBox = box || {}
     *
     *   //1. Base class isExternal or array/string
     *   var $this$ = Base_constructor(.., currBox)
     *   Object.assign(this, currBox)
     *   return $this$
     *
     *   //2. Base class !isInline and !isExternal and !(array/string)
     *   //fill initialization box
     *   var $this$ = Base_constructor(.., currBox, new.target)
     *   //body
     *   return $this$
     * }
     */
    private fun primaryToSecondary(helper: LowerCtorHelper) = with(helper) {
        statements.add(0, boxOrEmptyObject)

        if (superCtor.hasStrictSignature()) {
            fun createNewSuperCall(): IrCall {
                val callType = JsIrBuilder.buildCall(context.intrinsics.jsClass, context.dynamicType, listOf(superCtor.returnType))
                val newTarget = JsIrBuilder.buildCall(context.intrinsics.jsNewTarget)
                val args = IrVarargImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    context.dynamicType,
                    context.dynamicType,
                    getElements(constructor)
                )

                return JsIrBuilder.buildCall(context.intrinsics.jsConstruct).apply {
                    putValueArgument(0, callType)
                    putValueArgument(1, newTarget)
                    putValueArgument(2, args)

                    putTypeArgument(0, superCtor.returnType)
                }
            }

            val newThis = createThisVariable(createNewSuperCall())
            changeReturnUnitToReturnInstance(newThis)

            statements.add(2, openBoxStatement(newThis.symbol, boxSymbol))
            statements += returnThis(constructor, newThis)

            redirectOldThisToNewOne(constructor, newThis)
        } else {
            putBoxToSuperCall(boxSymbol, constructor)
            fillInitializerBox(boxSymbol, constructor)

            val newTarget = JsIrBuilder.buildCall(context.intrinsics.jsNewTarget)
            val newThis = createThisVariable(createNewSuperCallPrimaryToSecondary(superCall, newTarget))
            changeReturnUnitToReturnInstance(newThis)

            statements += returnThis(constructor, newThis)

            redirectOldThisToNewOne(constructor, newThis)
        }
    }

    /**
     * Derived_constructor(.., box, resultType) {
     *   var currBox = box || {}
     *   var currResultType = resultType || Derived
     *
     *   //1. Base class isInline or isExternal or array/string
     *   var $this$ = construct(Base, currResultType, [..])
     *   Object.assign(this, currBox)
     *   //body
     *   return $this$
     *
     *   //2. Base class !isInline and !isExternal and !(array/string)
     *   //fill initialization box
     *   var $this$ = construct(Base, currResultType, [.., currBox])
     *   //body
     *   return $this$
     * }
     */
    private fun secondaryToPrimary(helper: LowerCtorHelper) = with(helper) {
        statements.add(0, boxOrEmptyObject)
        statements.add(1, resultTypeOrDefaultType())
        val resultTypeSymbol = (statements[1] as IrVariable).symbol

        if (superCtor.hasStrictSignature()) {
            val newThis = createThisVariable(createNewSuperCallSecondaryToPrimary(constructor, superCtor, resultTypeSymbol))
            changeReturnUnitToReturnInstance(newThis)

            statements.add(3, openBoxStatement(newThis.symbol, boxSymbol))
            statements.add(returnThis(constructor, newThis))

            redirectOldThisToNewOne(constructor, newThis)
        } else {
            fillInitializerBox(boxSymbol, constructor)

            val newThis = createThisVariable(createNewSuperCallSecondaryToPrimary(constructor, superCtor, resultTypeSymbol, boxSymbol))
            changeReturnUnitToReturnInstance(newThis)

            statements.add(returnThis(constructor, newThis))

            redirectOldThisToNewOne(constructor, newThis)
        }
    }

    /**
     * Derived_constructor(.., box, resultType) {
     *   var currBox = box || {}
     *   var currResultType = resultType || Derived
     *   //fill initialization box
     *
     *   //1. Base class isInline or isExternal or array/string
     *   var $this$ = Base_constructor(..)
     *   Object.assign(this, currBox)
     *   //body
     *   return $this$
     *
     *   //2. Base class !isInline and !isExternal and !(array/string)
     *   var $this$ = Base_constructor(.., currBox, currResultType)
     *   //body
     *   return $this$
     * }
     */
    private fun secondaryToSecondary(helper: LowerCtorHelper) = with(helper) {
        statements.add(0, boxOrEmptyObject)
        statements.add(1, resultTypeOrDefaultType())
        val resultTypeSymbol = (statements[1] as IrVariable).symbol

        putBoxToSuperCall(boxSymbol, constructor)
        fillInitializerBox(boxSymbol, constructor)

        if (superCtor.hasStrictSignature()) {
            val newThis = createThisVariable(superCall)
            changeReturnUnitToReturnInstance(newThis)

            statements.add(3, openBoxStatement(newThis.symbol, boxSymbol))
            statements += returnThis(constructor, newThis)

            redirectOldThisToNewOne(constructor, newThis)
        } else {
            val newThis = createThisVariable(createNewSuperCallSecondaryToSecondary(superCall, boxSymbol, resultTypeSymbol))
            changeReturnUnitToReturnInstance(newThis)

            statements += returnThis(constructor, newThis)

            redirectOldThisToNewOne(constructor, newThis)
        }
    }

    //superCallBuilder
    private fun createNewSuperCallSecondaryToSecondary(
        superCall: IrDelegatingConstructorCall,
        boxSymbol: IrValueSymbol,
        resultTypeSymbol: IrVariableSymbol
    ) = superCall.apply {
        putValueArgument(ES6_INIT_BOX_PARAMETER, JsIrBuilder.buildGetValue(boxSymbol))
        putValueArgument(ES6_RESULT_TYPE_PARAMETER, JsIrBuilder.buildGetValue(resultTypeSymbol))
    }

    //superCallBuilder
    private fun createNewSuperCallPrimaryToSecondary(
        superCall: IrDelegatingConstructorCall,
        newTarget: IrCall? = null
    ) = superCall.apply { putValueArgument(ES6_RESULT_TYPE_PARAMETER, newTarget) }

    private fun IrDelegatingConstructorCall.putValueArgument(origin: IrDeclarationOrigin, value: IrExpression?) {
        val valueParameters = symbol.owner.valueParameters
        for (i in valueParameters.indices) {
            if (valueParameters[i].origin === origin) {
                putValueArgument(i, value)
            }
        }
    }

    //superCallBuilder
    private fun createNewSuperCallSecondaryToPrimary(
        constructor: IrConstructor,
        superCtor: IrConstructor,
        resultTypeSymbol: IrVariableSymbol,
        boxSymbol: IrVariableSymbol? = null
    ): IrCall {
        val callType = JsIrBuilder.buildCall(context.intrinsics.jsClass, context.dynamicType, listOf(superCtor.returnType))
        val resultType = JsIrBuilder.buildGetValue(resultTypeSymbol)
        val arguments = IrVarargImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            context.irBuiltIns.anyNType,
            context.irBuiltIns.anyNType,
            getElements(constructor, boxSymbol)
        )

        if (superCtor.parentAsClass.defaultType.isAny()) {
            //superType is Any, and we have default primary --> create call to default primary
            return JsIrBuilder.buildCall(context.intrinsics.jsConstruct, context.irBuiltIns.nothingType).apply {
                arguments.elements.clear()
                arguments.addElement(JsIrBuilder.buildGetValue(boxSymbol!!))

                putValueArgument(0, JsIrBuilder.buildCall(context.intrinsics.jsClass, context.dynamicType, listOf(constructor.returnType)))
                putValueArgument(1, resultType)
                putValueArgument(2, arguments)

                putTypeArgument(0, constructor.returnType)
            }
        }

        return JsIrBuilder.buildCall(context.intrinsics.jsConstruct, context.irBuiltIns.nothingType).apply {
            putValueArgument(0, callType)
            putValueArgument(1, resultType)
            putValueArgument(2, arguments)

            putTypeArgument(0, superCtor.returnType)
        }
    }

    /**
     * Copy arguments from superCall
     */
    private fun getElements(constructor: IrConstructor, boxSymbol: IrValueSymbol? = null): List<IrVarargElement> {
        val result = mutableListOf<IrVarargElement>()

        val superCall = constructor.body!!.statements.filterIsInstance<IrDelegatingConstructorCall>().first()
        repeat(superCall.valueArgumentsCount) { i ->
            val arg = superCall.getValueArgument(i) ?: return@repeat
            if (superCall.symbol.owner.valueParameters[i].origin === ES6_INIT_BOX_PARAMETER) {
                result += JsIrBuilder.buildGetValue(boxSymbol!!)
            } else {
                if (arg.type.getInlinedClass() != null) {
                    val any = context.irBuiltIns.anyNType
                    result += JsIrBuilder.buildTypeOperator(any, IrTypeOperator.REINTERPRET_CAST, arg, any)
                } else {
                    result += arg
                }
            }
        }

        return result
    }

    //builder
    private fun returnThis(constructor: IrConstructor, newThis: IrVariable): IrReturn {
        return JsIrBuilder.buildReturn(
            constructor.symbol,
            JsIrBuilder.buildGetValue(newThis.symbol),
            context.irBuiltIns.nothingType
        )
    }

    /**
     * Transform statements like `this.x = y` to `box.x = y`
     */
    private fun fillInitializerBox(boxSymbol: IrValueSymbol, constructor: IrConstructor) {
        val statements = (constructor.body as IrBlockBody).statements
        for (i in statements.indices) {
            val current = statements[i]
            if (current is IrSetField) {
                if ((current.receiver as? IrGetValue)?.symbol?.owner === constructor.parentAsClass.thisReceiver) {
                    current.receiver = JsIrBuilder.buildGetValue(boxSymbol)
                }
            }

            if (statements[i] is IrDelegatingConstructorCall) {
                break
            }
        }
    }

    //transformer
    private fun putBoxToSuperCall(boxSymbol: IrValueSymbol, constructor: IrConstructor) {
        constructor.transformChildren(object : IrElementTransformerVoid() {
            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                return expression.also { it.putValueArgument(ES6_INIT_BOX_PARAMETER, JsIrBuilder.buildGetValue(boxSymbol)) }
            }
        }, null)
    }

    //builder
    private fun openBoxStatement(thisSymbol: IrValueSymbol, boxSymbol: IrValueSymbol): IrCall {
        return JsIrBuilder.buildCall(context.intrinsics.jsOpenInitializerBox).also {
            it.putValueArgument(0, JsIrBuilder.buildGetValue(thisSymbol))
            it.putValueArgument(1, JsIrBuilder.buildGetValue(boxSymbol))
        }
    }

    //util
    private fun IrConstructor.hasStrictSignature(): Boolean {
        val primitives = with(context.irBuiltIns) { primitiveArrays + stringClass }
        return with(parentAsClass) { isExternal || isInline || symbol in primitives }
    }
}

//transformer
private fun replaceCallToDefaultPrimary(context: JsIrBackendContext, constructor: IrConstructor) {
    val thisSymbol = (((constructor.body as IrBlockBody).statements
        .find { it is IrVariable && it.origin === ES6_THIS_VARIABLE_ORIGIN }) as IrVariable?)?.symbol ?: return

    (constructor.body as IrBlockBody).statements.transform {
        if (it is IrDelegatingConstructorCall) {
            val superCtor = it.symbol.owner
            val initFunc = context.mapping.constructorToInitFunction[superCtor]!!

            JsIrBuilder.buildCall(initFunc.symbol).apply {
                putValueArgument(0, JsIrBuilder.buildGetValue(thisSymbol))
            }
        } else it
    }
}

//transformer
private fun redirectOldThisToNewOne(constructor: IrConstructor, newThis: IrVariable) {
    constructor.transformChildren(object : IrElementTransformerVoid() {
        override fun visitGetValue(expression: IrGetValue): IrExpression {
            return if (expression.symbol.owner === constructor.parentAsClass.thisReceiver!!) {
                with(expression) { IrGetValueImpl(startOffset, endOffset, type, newThis.symbol) }
            } else {
                expression
            }
        }
    }, null)
}

/**
 * Change `return Unit` to `return $this$`
 */
private fun LowerCtorHelper.changeReturnUnitToReturnInstance(newThis: IrVariable) {
    constructor.transformChildren(object : IrElementTransformerVoid() {
        override fun visitReturn(expression: IrReturn): IrExpression {
            return JsIrBuilder.buildReturn(
                constructor.symbol,
                JsIrBuilder.buildGetValue(newThis.symbol),
                expression.type
            )
        }
    }, null)
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

        if (s is IrCall && s.symbol === setPropertiesSymbol) callIndex = i
        if (s is IrDelegatingConstructorCall && s.symbol.owner.origin === SYNTHETIC_PRIMARY_CONSTRUCTOR) superCallIndex = i
    }

    if (callIndex != -1 && superCallIndex != -1) {
        val tmp = statements[callIndex]
        statements[callIndex] = statements[superCallIndex]
        statements[superCallIndex] = tmp
    }
}

private class LowerCtorHelper(
    private val context: JsIrBackendContext,
    val constructor: IrConstructor,
    val superCtor: IrConstructor
) {
    private val boxParameterSymbol = constructor.valueParameters.find { it.origin === ES6_INIT_BOX_PARAMETER }!!.symbol
    val resultTypeParameterSymbol by lazy {
        constructor.valueParameters.find { it.origin === ES6_RESULT_TYPE_PARAMETER }!!.symbol
    }

    val statements = (constructor.body as IrBlockBody).statements

    /**
     * var currBox = box || {}
     */
    val boxOrEmptyObject = boxOrEmptyObject(boxParameterSymbol, constructor)
    private fun boxOrEmptyObject(boxSymbol: IrValueSymbol, parent: IrConstructor): IrVariable {
        val emptyObject = JsIrBuilder.buildCall(context.intrinsics.jsEmptyObject)
        val or = JsIrBuilder.buildCall(context.intrinsics.jsOr, context.dynamicType).apply {
            putValueArgument(0, JsIrBuilder.buildGetValue(boxSymbol))
            putValueArgument(1, emptyObject)
        }
        return JsIrBuilder.buildVar(context.dynamicType, parent, "currBox", initializer = or)
    }

    val boxSymbol = boxOrEmptyObject.symbol
    val thisSymbol = constructor.parentAsClass.thisReceiver!!.symbol

    val superCall: IrDelegatingConstructorCall by lazy { getSuperCall(constructor)!! }

    /**
     * var $this$ = `newSuperCall`
     */
    fun createThisVariable(newSuperCall: IrExpression): IrVariable {
        val newThis = JsIrBuilder.buildVar(
            context.dynamicType,
            constructor,
            "\$this\$",
            initializer = newSuperCall
        ).apply {
            origin = ES6_THIS_VARIABLE_ORIGIN
        }

        (constructor.body as IrBlockBody).statements.transform {
            if (it === superCall) newThis
            else it
        }

        return newThis
    }

    /**
     * var currResultType = resultType || D
     */
    fun resultTypeOrDefaultType(): IrStatement {
        val defaultType = JsIrBuilder.buildCall(context.intrinsics.es6DefaultType).apply {
            putTypeArgument(0, constructor.parentAsClass.defaultType)
        }
        val or = JsIrBuilder.buildCall(context.intrinsics.jsOr, context.dynamicType).apply {
            putValueArgument(0, JsIrBuilder.buildGetValue(resultTypeParameterSymbol))
            putValueArgument(1, defaultType)
        }
        return JsIrBuilder.buildVar(context.dynamicType, constructor, "currResultType", initializer = or)
    }
}

private fun getSuperCall(constructor: IrConstructor): IrDelegatingConstructorCall? {
    var result: IrDelegatingConstructorCall? = null
    (constructor.body as IrBlockBody).acceptChildren(object : IrElementVisitor<Unit, Any?> {
        override fun visitElement(element: IrElement, data: Any?) { }

        override fun visitBlock(expression: IrBlock, data: Any?) {
            expression.statements.forEach { it.accept(this, data) }
        }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Any?) {
            result = result ?: expression
        }
    }, null)
    return result
}

private fun changeIrConstructorToIrFunction(context: JsIrBackendContext, container: IrConstructor) {
    val newConstructor = context.jsIrDeclarationBuilder.buildFunction(
        "${container.parentAsClass.name}_constructor",
        container.returnType,
        container.parent
    ).apply {
        container.valueParameters.forEach { param ->
            addValueParameter(param.name.asString(), param.type, param.origin)
        }

        val parametersMap = container.valueParameters.zip(valueParameters).toMap()
        body = container.body
        transformChildren(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                val newParam = parametersMap[expression.symbol.owner]
                return if (newParam != null) JsIrBuilder.buildGetValue(newParam.symbol) else expression
            }
        }, null)
    }

    container.parentAsClass.declarations.transform {
        if (it === container) newConstructor else it
    }

    context.mapping.secondaryConstructorToDelegate[container] = newConstructor
}

/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyToWithoutSuperTypes
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class InteropCallableReferenceLowering(val context: JsIrBackendContext) : BodyLoweringPass {
    private val newDeclarations = mutableListOf<IrDeclaration>()
    private lateinit var implicitDeclarationFile: IrFile //= context.implicitDeclarationFile

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        newDeclarations.clear()
        implicitDeclarationFile = container.file // TODO
        irBody.transformChildrenVoid(CallableReferenceLowerTransformer())
        implicitDeclarationFile.declarations += newDeclarations
    }

    inner class CallableReferenceLowerTransformer : IrElementTransformerVoid() {
        override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
            expression.transformChildrenVoid(this)
            if (expression.origin === CallableReferenceLowering.Companion.CALLABLE_REFERNCE_CREATE) {
                return transformToJavaScriptFunction(expression)
            }
            return expression
        }
    }

    private fun transformToJavaScriptFunction(expression: IrConstructorCall): IrExpression {
        // TODO: perform inlining
        val factory = buildFactoryFunction2(expression)
        newDeclarations += factory
        val newCall = expression.run {
            IrCallImpl(startOffset, endOffset, type, factory.symbol, typeArgumentsCount, valueArgumentsCount, origin)
        }

        newCall.dispatchReceiver = expression.dispatchReceiver
        newCall.extensionReceiver = expression.extensionReceiver

        for (i in 0 until expression.typeArgumentsCount) {
            newCall.putTypeArgument(i, expression.getTypeArgument(i))
        }

        for (i in 0 until expression.valueArgumentsCount) {
            newCall.putValueArgument(i, expression.getValueArgument(i))
        }

        return newCall
    }

    private fun buildLambdaBody(instance: IrVariable, lambdaDeclaration: IrSimpleFunction, invokeFun: IrSimpleFunction): IrBlockBody {
        val invokeExpression = IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            invokeFun.returnType,
            invokeFun.symbol,
            0,
            invokeFun.valueParameters.size,
            EXPLICIT_INVOKE,
            null
        )

        fun getValue(d: IrValueDeclaration): IrExpression = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, d.symbol)

        invokeExpression.dispatchReceiver = getValue(instance)
        for ((i, vp) in lambdaDeclaration.valueParameters.withIndex()) {
            invokeExpression.putValueArgument(i, getValue(vp))
        }

        return IrBlockBodyImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            listOf(
                IrReturnImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    context.irBuiltIns.nothingType,
                    lambdaDeclaration.symbol,
                    invokeExpression
                )
            )
        )
    }

    private fun buildFactoryBody(factoryFunction: IrSimpleFunction, expression: IrConstructorCall): IrBlockBody {
        val constructor = expression.symbol.owner
        val lambdaClass = constructor.parentAsClass
        val invokeFun = lambdaClass.declarations.filterIsInstance<IrSimpleFunction>().single { it.name.asString() == "invoke" }
        val superInvokeFun = invokeFun.overriddenSymbols.single { it.owner.isSuspend == invokeFun.isSuspend }.owner
        val lambdaName = Name.identifier("${lambdaClass.name.asString()}\$lambda")

        val lambdaDeclaration = buildFun {
            startOffset = invokeFun.startOffset
            endOffset = invokeFun.endOffset
            // Since box/unbox is done on declaration side in case of suspend function use the specified type
            returnType = if (invokeFun.isSuspend) invokeFun.returnType else superInvokeFun.returnType
            name = lambdaName
            isSuspend = invokeFun.isSuspend
        }

        lambdaDeclaration.parent = factoryFunction

        lambdaDeclaration.valueParameters = superInvokeFun.valueParameters.map { it.copyTo(lambdaDeclaration) }

        val statements = ArrayList<IrStatement>(4)

        val instanceVal = JsIrBuilder.buildVar(expression.type, factoryFunction, "i").apply {
            initializer = expression.run {
                val newCtorCall = IrConstructorCallImpl(startOffset, endOffset, type, symbol, typeArgumentsCount, constructorTypeArgumentsCount, valueArgumentsCount, origin)
                // TODO: forward type arguments
                require(expression.dispatchReceiver == null)
                require(expression.extensionReceiver == null)

                for ((i, vp) in factoryFunction.valueParameters.withIndex()) {
                    newCtorCall.putValueArgument(i, IrGetValueImpl(startOffset, endOffset, vp.type, vp.symbol))
                }

                newCtorCall
            }
        }

        statements.add(instanceVal)

        lambdaDeclaration.body = buildLambdaBody(instanceVal, lambdaDeclaration, invokeFun)

        val functionExpression =
            expression.run { IrFunctionExpressionImpl(startOffset, endOffset, type, lambdaDeclaration, expression.origin!!) }

        val nameGetter = context.mapping.reflectedNameAccessor[lambdaClass]

        if (nameGetter != null || lambdaDeclaration.isSuspend) {
            val tmpVar = JsIrBuilder.buildVar(functionExpression.type, factoryFunction, "l", initializer = functionExpression)
            statements.add(tmpVar)

            if (nameGetter != null) {
                statements.add(setDynamicProperty(tmpVar.symbol, Namer.KCALLABLE_NAME) {
                    IrCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        nameGetter.returnType,
                        nameGetter.symbol,
                        0,
                        0,
                        CallableReferenceLowering.Companion.CALLABLE_REFERNCE_INVOKE
                    ).apply {
                        dispatchReceiver = JsIrBuilder.buildGetValue(instanceVal.symbol)
                    }
                })
            }

            if (lambdaDeclaration.isSuspend) {
                statements.add(setDynamicProperty(tmpVar.symbol, Namer.KCALLABLE_ARITY) {
                    IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.intType, lambdaDeclaration.valueParameters.size)
                })
            }

            statements.add(JsIrBuilder.buildReturn(factoryFunction.symbol, JsIrBuilder.buildGetValue(tmpVar.symbol), context.irBuiltIns.nothingType))
        } else {
            statements.add(JsIrBuilder.buildReturn(factoryFunction.symbol, functionExpression, context.irBuiltIns.nothingType))
        }

        return expression.run {
            IrBlockBodyImpl(startOffset, endOffset, statements)
        }
    }

    private fun buildFactoryFunction2(expression: IrConstructorCall): IrSimpleFunction {

        val constructor = expression.symbol.owner
        val lambdaClass = constructor.parentAsClass

        val factoryName = Name.identifier("${lambdaClass.name.asString()}\$factory")

        val factoryDeclaration = buildFun {
            startOffset = expression.startOffset
            endOffset = expression.endOffset
            returnType = expression.type
            name = factoryName
        }

        factoryDeclaration.parent = implicitDeclarationFile

        factoryDeclaration.valueParameters = constructor.valueParameters.map { it.copyTo(factoryDeclaration) }
        factoryDeclaration.typeParameters = constructor.typeParameters.map {
            it.copyToWithoutSuperTypes(factoryDeclaration).also { tp ->
                // TODO: make sure it is done well
                tp.superTypes += it.superTypes
            }
        }

        factoryDeclaration.body = buildFactoryBody(factoryDeclaration, expression)

        return factoryDeclaration
    }


    private inline fun setDynamicProperty(r: IrValueSymbol, property: String, value: () -> IrExpression): IrStatement {
        return IrDynamicOperatorExpressionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType, IrDynamicOperator.EQ).apply {
            receiver = IrDynamicMemberExpressionImpl(startOffset, endOffset, context.dynamicType, property, JsIrBuilder.buildGetValue(r))
            arguments += value()
        }
    }

    companion object {
        object EXPLICIT_INVOKE : IrStatementOriginImpl("EXPLICIT_INVOKE")
    }
}

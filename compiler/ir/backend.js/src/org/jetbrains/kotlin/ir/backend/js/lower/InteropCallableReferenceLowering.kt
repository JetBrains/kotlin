/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyToWithoutSuperTypes
import org.jetbrains.kotlin.backend.common.lower.LoweredStatementOrigins
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.cast

class InteropCallableReferenceLowering(val context: JsIrBackendContext) : BodyLoweringPass {
    private val newDeclarations = mutableListOf<IrDeclaration>()
    private lateinit var implicitDeclarationFile: IrFile
    private val transformedLambdas = mutableMapOf<IrConstructorSymbol, IrSimpleFunctionSymbol>()

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        newDeclarations.clear()

        implicitDeclarationFile = container.file // TODO
        irBody.transformChildrenVoid(CallableReferenceLowerTransformer())
        implicitDeclarationFile.declarations.addAll(newDeclarations)
    }

    inner class CallableReferenceLowerTransformer : IrElementTransformerVoid() {
        override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
            expression.transformChildrenVoid(this)
            if (expression.origin === JsStatementOrigins.CALLABLE_REFERENCE_CREATE) {
                return transformToJavaScriptFunction(expression)
            }
            return expression
        }
    }

    private fun transformToJavaScriptFunction(expression: IrConstructorCall): IrExpression {
        val irConstructor = expression.symbol

        // There could be more than one lambda instantiation so don't create redundant copies
        // For testcase take a look into `boxInline/suspend/twiceRegeneratedAnonymousObject.kt`
        val factory = transformedLambdas.getOrPut(irConstructor) {
            buildFactoryFunction(expression).also { f ->
                newDeclarations += f
            }.symbol
        }

        val newCall = expression.run {
            IrCallImpl(startOffset, endOffset, type, factory, typeArgumentsCount, valueArgumentsCount, origin)
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

    private fun inlineLambdaBody(
        lambdaDeclaration: IrSimpleFunction,
        invokeFun: IrSimpleFunction,
        invokeMapping: Map<IrValueParameterSymbol, IrValueParameterSymbol>,
        factoryMapping: Map<IrFieldSymbol, IrValueParameterSymbol>
    ): IrBlockBody {
        val body = invokeFun.body ?: error("invoke() method has to have a body")

        fun IrExpression.getValue(d: IrValueSymbol): IrExpression = IrGetValueImpl(startOffset, endOffset, d)

        // TODO: remap type parameters???
        body.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetField(expression: IrGetField): IrExpression {
                expression.transformChildrenVoid()
                val parameter = factoryMapping[expression.symbol] ?: return expression
                return expression.getValue(parameter)
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                expression.transformChildrenVoid()
                val parameter = invokeMapping[expression.symbol] ?: return expression
                return expression.getValue(parameter)
            }

            override fun visitReturn(expression: IrReturn): IrExpression {
                expression.transformChildrenVoid()
                if (expression.returnTargetSymbol != invokeFun.symbol) return expression
                return expression.run {
                    IrReturnImpl(startOffset, endOffset, type, lambdaDeclaration.symbol, value)
                }
            }
        })

        // Fix parents of declarations inside body
        body.patchDeclarationParents(lambdaDeclaration)

        return body as IrBlockBody
    }

    private fun buildLambdaBody(instance: IrVariable, lambdaDeclaration: IrSimpleFunction, invokeFun: IrSimpleFunction): IrBlockBody {
        val invokeExpression = IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            invokeFun.returnType,
            invokeFun.symbol,
            0,
            invokeFun.valueParameters.size,
            JsStatementOrigins.EXPLICIT_INVOKE,
            null
        )

        fun getValue(d: IrValueDeclaration): IrExpression = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, d.symbol)

        invokeExpression.dispatchReceiver = getValue(instance)
        for ((i, vp) in lambdaDeclaration.valueParameters.withIndex()) {
            invokeExpression.putValueArgument(i, getValue(vp))
        }

        return context.irFactory.createBlockBody(
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

    private fun capturedFieldsToParametersMap(constructor: IrConstructor, factoryFunction: IrSimpleFunction): Map<IrFieldSymbol, IrValueParameterSymbol> {
        val statements = constructor.body?.let { it.cast<IrBlockBody>().statements } ?: error("Expecting Body for function ref constructor")

        val fieldSetters = statements.filterIsInstance<IrSetField>()
            .filter { it.origin == LoweredStatementOrigins.STATEMENT_ORIGIN_INITIALIZER_OF_FIELD_FOR_CAPTURED_VALUE }

        fun remapVP(vp: IrValueParameterSymbol): IrValueParameterSymbol {
            return factoryFunction.valueParameters[vp.owner.index].symbol
        }

        return fieldSetters.associate { it.symbol to remapVP(it.value.cast<IrGetValue>().symbol.cast()) }
    }

    private fun extractReferenceReflectionName(getter: IrSimpleFunction): IrExpression {
        val body = getter.body?.cast<IrBlockBody>() ?: error("Expected body for ${getter.render()}")
        val statements = body.statements

        val returnStmt = statements[0] as IrReturn
        return returnStmt.value
    }

    private fun buildFactoryBody(factoryFunction: IrSimpleFunction, expression: IrConstructorCall): IrBlockBody {
        val constructor = expression.symbol.owner
        val lambdaClass = constructor.parentAsClass
        val invokeFun = lambdaClass.declarations.filterIsInstance<IrSimpleFunction>().single { it.name.asString() == "invoke" }
        val superInvokeFun = invokeFun.overriddenSymbols.single { it.owner.isSuspend == invokeFun.isSuspend }.owner
        val lambdaName = Name.identifier("${lambdaClass.name.asString()}\$lambda")

        val lambdaDeclaration = context.irFactory.buildFun {
            startOffset = invokeFun.startOffset
            endOffset = invokeFun.endOffset
            // Since box/unbox is done on declaration side in case of suspend function use the specified type
            returnType = if (invokeFun.isSuspend) invokeFun.returnType else superInvokeFun.returnType
            visibility = DescriptorVisibilities.LOCAL
            name = lambdaName
            isSuspend = invokeFun.isSuspend
        }

        lambdaDeclaration.parent = factoryFunction

        lambdaDeclaration.valueParameters = superInvokeFun.valueParameters.map { it.copyTo(lambdaDeclaration) }

        val statements = ArrayList<IrStatement>(4)
        val isSuspendLambda = invokeFun.overriddenSymbols.any { it.owner.isSuspend }

        if (isSuspendLambda) {
            // Due to suspend lambda is a class itself it's not easy to inline it correctly and moreover I see no reason to do so
            val instanceVal = JsIrBuilder.buildVar(expression.type, factoryFunction, "i").apply {
                initializer = expression.run {
                    val newCtorCall = IrConstructorCallImpl(
                        startOffset,
                        endOffset,
                        type,
                        symbol,
                        typeArgumentsCount,
                        constructorTypeArgumentsCount,
                        valueArgumentsCount,
                        origin
                    )
                    // TODO: forward type arguments
                    assert(expression.dispatchReceiver == null)
                    assert(expression.extensionReceiver == null)

                    for ((i, vp) in factoryFunction.valueParameters.withIndex()) {
                        newCtorCall.putValueArgument(i, IrGetValueImpl(startOffset, endOffset, vp.type, vp.symbol))
                    }

                    newCtorCall
                }
            }

            statements.add(instanceVal)

            lambdaDeclaration.body = buildLambdaBody(instanceVal, lambdaDeclaration, invokeFun)

        } else {
            val fieldToParameterMapping = capturedFieldsToParametersMap(constructor, factoryFunction)
            val oldToNewInvokeParametersMapping = mutableMapOf<IrValueParameterSymbol, IrValueParameterSymbol>()
            invokeFun.valueParameters.forEach {
                oldToNewInvokeParametersMapping[it.symbol] = lambdaDeclaration.valueParameters[it.index].symbol
            }
            lambdaDeclaration.body =
                inlineLambdaBody(lambdaDeclaration, invokeFun, oldToNewInvokeParametersMapping, fieldToParameterMapping)

            val classContainer = lambdaClass.parent as IrDeclarationContainer

            // lambdas could contain another lambdas and local classes in so let do not lose them
            val lambdaInnerClasses =
                lambdaClass.declarations.filter { it is IrClass || (it is IrSimpleFunction && it.dispatchReceiverParameter == null) }
            classContainer.declarations.remove(lambdaClass)
            classContainer.declarations.addAll(lambdaInnerClasses)
            lambdaInnerClasses.forEach { it.parent = classContainer }
        }

        val functionExpression =
            expression.run { IrFunctionExpressionImpl(startOffset, endOffset, type, lambdaDeclaration, expression.origin!!) }

        val nameGetter = context.mapping.reflectedNameAccessor[lambdaClass]

        if (nameGetter != null || lambdaDeclaration.isSuspend) {
            val tmpVar = JsIrBuilder.buildVar(functionExpression.type, factoryFunction, "l", initializer = functionExpression)
            statements.add(tmpVar)

            if (nameGetter != null) {
                statements.add(setDynamicProperty(tmpVar.symbol, Namer.KCALLABLE_NAME, extractReferenceReflectionName(nameGetter)))
            }

            if (lambdaDeclaration.isSuspend) {
                statements.add(
                    setDynamicProperty(
                        tmpVar.symbol, Namer.KCALLABLE_ARITY,
                        IrConstImpl.int(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            context.irBuiltIns.intType,
                            lambdaDeclaration.valueParameters.size
                        )
                    )
                )
            }

            statements.add(JsIrBuilder.buildReturn(factoryFunction.symbol, JsIrBuilder.buildGetValue(tmpVar.symbol), context.irBuiltIns.nothingType))
        } else {
            statements.add(JsIrBuilder.buildReturn(factoryFunction.symbol, functionExpression, context.irBuiltIns.nothingType))
        }

        return context.irFactory.createBlockBody(expression.startOffset, expression.endOffset, statements)
    }

    private fun buildFactoryFunction(expression: IrConstructorCall): IrSimpleFunction {

        val constructor = expression.symbol.owner
        val lambdaClass = constructor.parentAsClass

        val factoryName = Name.identifier("${lambdaClass.name.asString()}\$factory")

        val factoryDeclaration = context.irFactory.buildFun {
            startOffset = expression.startOffset
            endOffset = expression.endOffset
            visibility = lambdaClass.visibility
            returnType = expression.type
            name = factoryName
            origin = JsStatementOrigins.FACTORY_ORIGIN
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


    private fun setDynamicProperty(r: IrValueSymbol, property: String, value: IrExpression): IrStatement {
        return IrDynamicOperatorExpressionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType, IrDynamicOperator.EQ).apply {
            receiver = IrDynamicMemberExpressionImpl(startOffset, endOffset, context.dynamicType, property, JsIrBuilder.buildGetValue(r))
            arguments += value
        }
    }
}

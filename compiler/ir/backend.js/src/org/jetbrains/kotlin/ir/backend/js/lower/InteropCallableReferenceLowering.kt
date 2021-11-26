/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.compilationException
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
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.cast

class InteropCallableReferenceLowering(val context: JsIrBackendContext) : BodyLoweringPass {

    override fun lower(irFile: IrFile) {
        val ctorToFactoryMap = mutableMapOf<IrConstructorSymbol, IrSimpleFunctionSymbol>()
        irFile.transform(CallableReferenceClassTransformer(ctorToFactoryMap), null)
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
                expression.transformChildrenVoid()
                if (expression.origin != JsStatementOrigins.CALLABLE_REFERENCE_CREATE) return expression
                return ctorToFactoryMap[expression.symbol]?.let { factory ->
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

                    newCall
                } ?: expression
            }
        })
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        compilationException("Unreachable", irBody)
    }

    private inner class CallableReferenceClassTransformer(private val ctorToFactoryMap: MutableMap<IrConstructorSymbol, IrSimpleFunctionSymbol>) : IrElementTransformerVoid() {
        override fun visitFile(declaration: IrFile): IrFile {
            declaration.transformChildrenVoid()
            declaration.transformDeclarationsFlat { it.transformCallableReference() }
            return declaration
        }

        override fun visitClass(declaration: IrClass): IrStatement {
            declaration.transformChildrenVoid()
            declaration.transformDeclarationsFlat { it.transformCallableReference() }
            return declaration
        }

        override fun visitScript(declaration: IrScript): IrStatement {
            declaration.transformChildrenVoid()
            declaration.statements.transformFlat { s ->
                if (s is IrDeclaration) s.transformCallableReference()
                else null
            }
            return declaration
        }

        private fun IrDeclaration.asCallableReference(): IrClass? {
            if (origin == CallableReferenceLowering.Companion.FUNCTION_REFERENCE_IMPL || origin == CallableReferenceLowering.Companion.LAMBDA_IMPL)
                return this as? IrClass
            return null
        }

        private fun IrDeclaration.transformCallableReference(): List<IrDeclaration>? {
            return asCallableReference()?.let {
                replaceWithFactory(it)
            }
        }

        private fun replaceWithFactory(lambdaClass: IrClass): List<IrDeclaration> {
            return buildFactoryFunction(lambdaClass, ctorToFactoryMap).onEach { it.parent = lambdaClass.parent }
        }
    }

    private fun inlineLambdaBody(
        lambdaDeclaration: IrSimpleFunction,
        invokeFun: IrSimpleFunction,
        invokeMapping: Map<IrValueParameterSymbol, IrValueParameterSymbol>,
        factoryMapping: Map<IrFieldSymbol, IrValueParameterSymbol>
    ): IrBlockBody {
        val body = invokeFun.body
            ?: compilationException(
                "invoke() method has to have a body",
                invokeFun
            )

        fun IrExpression.getValue(d: IrValueSymbol): IrExpression = IrGetValueImpl(startOffset, endOffset, d)
        fun IrExpression.getCastedValue(d: IrValueSymbol, toType: IrType): IrExpression =
            IrTypeOperatorCallImpl(startOffset, endOffset, toType, IrTypeOperator.IMPLICIT_CAST, toType, getValue(d))

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
                val parameterType = invokeFun.valueParameters[parameter.owner.index].type
                return expression.getCastedValue(parameter, parameterType)
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
        val statements = constructor.body?.let { it.cast<IrBlockBody>().statements }
            ?: compilationException(
                "Expecting Body for function ref constructor",
                constructor
            )

        val fieldSetters = statements.filterIsInstance<IrSetField>()
            .filter { it.origin == LoweredStatementOrigins.STATEMENT_ORIGIN_INITIALIZER_OF_FIELD_FOR_CAPTURED_VALUE }

        fun remapVP(vp: IrValueParameterSymbol): IrValueParameterSymbol {
            return factoryFunction.valueParameters[vp.owner.index].symbol
        }

        return fieldSetters.associate { it.symbol to remapVP(it.value.cast<IrGetValue>().symbol.cast()) }
    }

    private fun extractReferenceReflectionName(getter: IrSimpleFunction): IrExpression {
        val body = getter.body?.cast<IrBlockBody>()
            ?: compilationException(
                "Expected body",
                getter
            )
        val statements = body.statements

        val returnStmt = statements[0] as IrReturn
        return returnStmt.value
    }

    private fun buildFactoryBody(
        factoryFunction: IrSimpleFunction,
        lambdaClass: IrClass,
        newDeclarations: MutableList<IrDeclaration>
    ): IrBlockBody {
        val invokeFun = lambdaClass.declarations.filterIsInstance<IrSimpleFunction>().single { it.name.asString() == "invoke" }
        val superInvokeFun = invokeFun.overriddenSymbols.first { it.owner.isSuspend == invokeFun.isSuspend }.owner
        val lambdaName = Name.identifier("${lambdaClass.name.asString()}\$lambda")

        val superClass = superInvokeFun.parentAsClass
        val anyNType = context.irBuiltIns.anyNType
        val lambdaDeclaration = context.irFactory.buildFun {
            startOffset = invokeFun.startOffset
            endOffset = invokeFun.endOffset
            // Since box/unbox is done on declaration side in case of suspend function use the specified type
            returnType = if (invokeFun.isSuspend) invokeFun.returnType else anyNType
            visibility = DescriptorVisibilities.LOCAL
            name = lambdaName
            isSuspend = invokeFun.isSuspend
        }

        lambdaDeclaration.parent = factoryFunction

        lambdaDeclaration.valueParameters = superInvokeFun.valueParameters.mapIndexed { id, vp ->
            vp.copyTo(lambdaDeclaration, type = anyNType, name = invokeFun.valueParameters[id].name)
        }

        val statements = ArrayList<IrStatement>(4)
        val isSuspendLambda = invokeFun.overriddenSymbols.any { it.owner.isSuspend }
        val constructor = lambdaClass.declarations.firstNotNullOf { it as? IrConstructor }

        if (isSuspendLambda) {
            // Due to suspend lambda is a class itself it's not easy to inline it correctly and moreover I see no reason to do so
            val lambdaType = lambdaClass.defaultType
            val instanceVal = JsIrBuilder.buildVar(lambdaType, factoryFunction, "i").apply {
                val newCtorCall = IrConstructorCallImpl(
                    lambdaClass.startOffset,
                    lambdaClass.endOffset,
                    lambdaType,
                    constructor.symbol,
                    lambdaClass.typeParameters.size,
                    constructor.typeParameters.size,
                    constructor.valueParameters.size
                )

                for ((i, vp) in factoryFunction.valueParameters.withIndex()) {
                    newCtorCall.putValueArgument(i, IrGetValueImpl(startOffset, endOffset, vp.type, vp.symbol))
                }

                initializer = newCtorCall
            }

            statements.add(instanceVal)

            lambdaDeclaration.body = buildLambdaBody(instanceVal, lambdaDeclaration, invokeFun)

            newDeclarations.add(lambdaClass)
        } else {
            val fieldToParameterMapping = capturedFieldsToParametersMap(constructor, factoryFunction)
            val oldToNewInvokeParametersMapping = mutableMapOf<IrValueParameterSymbol, IrValueParameterSymbol>()
            invokeFun.valueParameters.forEach {
                oldToNewInvokeParametersMapping[it.symbol] = lambdaDeclaration.valueParameters[it.index].symbol
            }
            lambdaDeclaration.body =
                inlineLambdaBody(lambdaDeclaration, invokeFun, oldToNewInvokeParametersMapping, fieldToParameterMapping)

            // lambdas could contain another lambdas and local classes in so let do not lose them
            val lambdaInnerClasses =
                lambdaClass.declarations.filter { it is IrClass || (it is IrSimpleFunction && it.dispatchReceiverParameter == null) }

            newDeclarations.addAll(lambdaInnerClasses)
        }

        val lambdaType = lambdaClass.superTypes.single { it.classifierOrNull === superClass.symbol }
        val functionExpression = lambdaClass.run {
            IrFunctionExpressionImpl(startOffset, endOffset, lambdaType, lambdaDeclaration, JsStatementOrigins.CALLABLE_REFERENCE_CREATE)
        }

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

            statements.add(
                JsIrBuilder.buildReturn(
                    factoryFunction.symbol,
                    JsIrBuilder.buildGetValue(tmpVar.symbol),
                    context.irBuiltIns.nothingType
                )
            )
        } else {
            statements.add(JsIrBuilder.buildReturn(factoryFunction.symbol, functionExpression, context.irBuiltIns.nothingType))
        }

        return context.irFactory.createBlockBody(lambdaClass.startOffset, lambdaClass.endOffset, statements)
    }

    private fun buildFactoryFunction(
        lambdaClass: IrClass,
        ctorToFactoryMap: MutableMap<IrConstructorSymbol, IrSimpleFunctionSymbol>
    ): List<IrDeclaration> {
        val newDeclarations = mutableListOf<IrDeclaration>()
        val constructor = lambdaClass.declarations.single { it is IrConstructor } as IrConstructor

        val factoryDeclaration = context.irFactory.buildFun {
            startOffset = lambdaClass.startOffset
            endOffset = lambdaClass.endOffset
            visibility = lambdaClass.visibility
            returnType = lambdaClass.defaultType
            name = lambdaClass.name
            origin = JsStatementOrigins.FACTORY_ORIGIN
        }

        factoryDeclaration.valueParameters = constructor.valueParameters.map { it.copyTo(factoryDeclaration) }
        factoryDeclaration.typeParameters = constructor.typeParameters.map {
            it.copyToWithoutSuperTypes(factoryDeclaration).also { tp ->
                // TODO: make sure it is done well
                tp.superTypes += it.superTypes
            }
        }

        factoryDeclaration.body = buildFactoryBody(factoryDeclaration, lambdaClass, newDeclarations)

        newDeclarations.add(factoryDeclaration)
        ctorToFactoryMap[constructor.symbol] = factoryDeclaration.symbol

        return newDeclarations
    }


    private fun setDynamicProperty(r: IrValueSymbol, property: String, value: IrExpression): IrStatement {
        return IrDynamicOperatorExpressionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType, IrDynamicOperator.EQ).apply {
            receiver = IrDynamicMemberExpressionImpl(startOffset, endOffset, context.dynamicType, property, JsIrBuilder.buildGetValue(r))
            arguments += value
        }
    }
}

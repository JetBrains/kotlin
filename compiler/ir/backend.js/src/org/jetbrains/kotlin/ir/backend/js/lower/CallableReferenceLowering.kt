/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class CallableReferenceLowering(private val context: CommonBackendContext) : BodyLoweringPass {

    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, withLocalDeclarations = true)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val realContainer = container as? IrDeclarationParent ?: container.parent
        irBody.transformChildrenVoid(ReferenceTransformer(realContainer))
    }

    private val nothingType = context.irBuiltIns.nothingType
    private val stringType = context.irBuiltIns.stringType

    private inner class ReferenceTransformer(private val container: IrDeclarationParent) : IrElementTransformerVoid() {

        override fun visitBody(body: IrBody): IrBody {
            return body
        }

        override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
            expression.transformChildrenVoid(this)

            val function = expression.function
            val (clazz, ctor) = buildLambdaReference(function, expression)

            clazz.parent = container

            return expression.run {
                val vpCount = if (function.isSuspend) 1 else 0
                val ctorCall =
                    IrConstructorCallImpl(startOffset, endOffset, type, ctor.symbol, 0 /*TODO: properly set type arguments*/, 0, vpCount, CALLABLE_REFERENCE_CREATE).apply {
                        if (function.isSuspend) {
                            putValueArgument(0, IrConstImpl.constNull(startOffset, endOffset, context.irBuiltIns.nothingNType))
                        }
                    }
                IrCompositeImpl(startOffset, endOffset, type, origin, listOf(clazz, ctorCall))
            }
        }

        override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
            expression.transformChildrenVoid(this)

            val (clazz, ctor) = buildFunctionReference(expression)

            clazz.parent = container

            return expression.run {
                val boundReceiver = expression.run { dispatchReceiver ?: extensionReceiver }
                val vpCount = if (boundReceiver != null) 1 else 0
                val ctorCall = IrConstructorCallImpl(
                    startOffset, endOffset, type, ctor.symbol,
                    0 /*TODO: properly set type arguments*/, 0,
                    vpCount, CALLABLE_REFERENCE_CREATE).apply {
                    boundReceiver?.let {
                        putValueArgument(0, it)
                    }
                }
                IrCompositeImpl(startOffset, endOffset, type, origin, listOf(clazz, ctorCall))
            }
        }

        private fun buildFunctionReference(expression: IrFunctionReference): Pair<IrClass, IrConstructor> {
            return CallableReferenceBuilder(expression.symbol.owner, expression, false).build()
        }

        private fun buildLambdaReference(function: IrSimpleFunction, expression: IrFunctionExpression): Pair<IrClass, IrConstructor> {
            return CallableReferenceBuilder(function, expression, true).build()
        }
    }

    private inner class CallableReferenceBuilder(
        private val function: IrFunction,
        private val reference: IrExpression,
        private val isLambda: Boolean
    ) {

        private val isSuspendLambda = isLambda && function.isSuspend

        private val superClass = if (isSuspendLambda) context.ir.symbols.coroutineImpl.owner.defaultType else context.irBuiltIns.anyType
        private var boundReceiverField: IrField? = null

        private val superFunctionInterface = reference.type.classOrNull?.owner ?: error("Expected functional type")
        private val isKReference = superFunctionInterface.name.identifier[0] == 'K'

        private fun buildReferenceClass(): IrClass {
            return context.irFactory.buildClass {
                setSourceRange(reference)
                visibility = DescriptorVisibilities.LOCAL
                // A callable reference results in a synthetic class, while a lambda is not synthetic.
                // We don't produce GENERATED_SAM_IMPLEMENTATION, which is always synthetic.
                origin = if (isKReference || !isLambda) FUNCTION_REFERENCE_IMPL else LAMBDA_IMPL
                name = SpecialNames.NO_NAME_PROVIDED
            }.apply {
                superTypes = listOf(superClass, reference.type)
//                if (samSuperType == null)
//                    superTypes += functionSuperClass.typeWith(parameterTypes)
//                if (irFunctionReference.isSuspend) superTypes += context.ir.symbols.suspendFunctionInterface.defaultType
                createImplicitParameterDeclarationWithWrappedDescriptor()
                createReceiverField()
            }
        }

        private fun IrClass.createReceiverField() {
            if (isLambda) return

            val funRef = reference as IrFunctionReference
            val boundReceiver = funRef.run { dispatchReceiver ?: extensionReceiver }

            if (boundReceiver != null) {
                boundReceiverField = addField(BOUND_RECEIVER_NAME, boundReceiver.type)
            }
        }

        private fun createConstructor(clazz: IrClass): IrConstructor {
            return clazz.addConstructor {
                origin = GENERATED_MEMBER_IN_CALLABLE_REFERENCE
                returnType = clazz.defaultType
                isPrimary = true
            }.apply {

                val superConstructor = superClass.classOrNull!!.owner.declarations.single { it is IrConstructor && it.isPrimary } as IrConstructor

                val boundReceiverParameter = boundReceiverField?.let {
                    addValueParameter {
                        name = BOUND_RECEIVER_NAME
                        type = it.type
                        index = 0
                    }
                }

                var continuation: IrValueParameter? = null

                if (isSuspendLambda) {
                    val superContinuation = superConstructor.valueParameters.single()
                    continuation = addValueParameter {
                        name = superContinuation.name
                        type = superContinuation.type
                        index = if (boundReceiverParameter == null) 0 else 1
                    }
                }

                body = context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                    +irDelegatingConstructorCall(superConstructor).apply {
                        continuation?.let {
                            putValueArgument(0, getValue(it))
                        }
                    }
                    boundReceiverParameter?.let {
                        +irSetField(irGet(clazz.thisReceiver!!), boundReceiverField!!, irGet(it))
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, clazz.symbol, context.irBuiltIns.unitType)
                }
            }
        }

        private fun createInvokeMethod(clazz: IrClass): IrSimpleFunction {
            val superMethods = superFunctionInterface.declarations.filterIsInstance<IrSimpleFunction>()
            val superMethod = superMethods.single { it.name.asString() == "invoke" }
            return clazz.addFunction {
                setSourceRange(if (isLambda) function else reference)
                name = superMethod.name
                returnType = function.returnType
                isSuspend = superMethod.isSuspend
                isOperator = superMethod.isOperator
            }.apply {
                overriddenSymbols = listOf(superMethod.symbol)
                dispatchReceiverParameter = buildReceiverParameter(this, clazz.origin, clazz.defaultType, startOffset, endOffset)

                if (isLambda) createLambdaInvokeMethod() else createFunctionReferenceInvokeMethod()
            }
        }

        private fun IrSimpleFunction.createLambdaInvokeMethod() {
            annotations = function.annotations
            val valueParameterMap = function.explicitParameters.withIndex().associate { (index, param) ->
                param to param.copyTo(this, index = index)
            }
            valueParameters = valueParameterMap.values.toList()
            body = function.moveBodyTo(this, valueParameterMap)
        }

        fun getValue(d: IrValueDeclaration): IrGetValue =
            IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, d.type, d.symbol, CALLABLE_REFERENCE_INVOKE)

        /**
        inner class IN<IT> {
            private fun <T> foo() {
                class CC<TT>(t: T, tt: TT, ttt: IT)
            }
        }
        */

        private fun IrConstructor.countContextTypeParameters(): Int {
            fun countImpl(container: IrDeclarationParent): Int {
                return when (container) {
                    is IrClass -> container.typeParameters.size + container.run { if (isInner) countImpl(container.parent) else 0 }
                    is IrFunction -> container.typeParameters.size + countImpl(container.parent)
                    is IrProperty -> (container.run { getter ?: setter }?.typeParameters?.size ?: 0) + countImpl(container.parent)
                    is IrDeclaration -> countImpl(container.parent)
                    else -> 0
                }
            }

            return countImpl(parent)
        }

        private fun IrSimpleFunction.buildInvoke(): IrFunctionAccessExpression {
            val callee = function
            val irCall = reference.run {
                when (callee) {
                    is IrConstructor ->
                        IrConstructorCallImpl(
                            startOffset,
                            endOffset,
                            callee.parentAsClass.defaultType,
                            callee.symbol,
                            callee.countContextTypeParameters(),
                            callee.typeParameters.size,
                            callee.valueParameters.size,
                            CALLABLE_REFERENCE_INVOKE
                        )
                    is IrSimpleFunction ->
                        IrCallImpl(
                            startOffset,
                            endOffset,
                            callee.returnType,
                            callee.symbol,
                            callee.typeParameters.size,
                            callee.valueParameters.size,
                            CALLABLE_REFERENCE_INVOKE
                        )
                    else ->
                        error("unknown function kind: ${callee.render()}")
                }
            }

            val funRef = reference as IrFunctionReference

            val boundReceiver = funRef.run { dispatchReceiver ?: extensionReceiver } != null
            val hasReceiver = callee.run { dispatchReceiverParameter ?: extensionReceiverParameter } != null

            irCall.dispatchReceiver = funRef.dispatchReceiver
            irCall.extensionReceiver = funRef.extensionReceiver

            var i = 0
            val valueParameters = valueParameters

            for (ti in 0 until funRef.typeArgumentsCount) {
                irCall.putTypeArgument(ti, funRef.getTypeArgument(ti))
            }

            if (hasReceiver) {
                if (!boundReceiver) {
                    if (callee.dispatchReceiverParameter != null) irCall.dispatchReceiver = getValue(valueParameters[i++])
                    if (callee.extensionReceiverParameter != null) irCall.extensionReceiver = getValue(valueParameters[i++])
                } else {
                    val boundReceiverField = boundReceiverField
                    if (boundReceiverField != null) {
                        val thisValue = getValue(dispatchReceiverParameter!!)
                        val value =
                            IrGetFieldImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                boundReceiverField.symbol,
                                boundReceiverField.type,
                                thisValue,
                                CALLABLE_REFERENCE_INVOKE
                            )

                        if (funRef.dispatchReceiver != null) irCall.dispatchReceiver = value
                        if (funRef.extensionReceiver != null) irCall.extensionReceiver = value
                    }
                    if (callee.dispatchReceiverParameter != null && funRef.dispatchReceiver == null) {
                        irCall.dispatchReceiver = getValue(valueParameters[i++])
                    }
                    if (callee.extensionReceiverParameter != null && funRef.extensionReceiver == null) {
                        irCall.extensionReceiver = getValue(valueParameters[i++])
                    }
                }
            }

            var j = 0

            while (i < valueParameters.size) {
                irCall.putValueArgument(j++, getValue(valueParameters[i++]))
            }

            return irCall
        }

        private fun IrSimpleFunction.createFunctionReferenceInvokeMethod() {
            val parameterTypes = (reference.type as IrSimpleType).arguments.map { (it as IrTypeProjection).type }
            val argumentTypes = parameterTypes.dropLast(1)

            valueParameters = argumentTypes.mapIndexed { i, t ->
                buildValueParameter(this) {
                    name = Name.identifier("p$i")
                    type = t
                    index = i
                }
            }

            body = factory.createBlockBody(reference.startOffset, reference.endOffset, listOf(reference.run {
                IrReturnImpl(
                    startOffset,
                    endOffset, nothingType,
                    symbol,
                    buildInvoke()
                )
            }))
        }

        private fun createNameProperty(clazz: IrClass) {
            if (!isKReference) return

            val superProperty = superFunctionInterface.declarations.filterIsInstance<IrProperty>().single()
            val supperGetter = superProperty.getter ?: error("Expected getter for KFunction.name property")

            val nameProperty = clazz.addProperty() {
                visibility = superProperty.visibility
                name = superProperty.name
                origin = GENERATED_MEMBER_IN_CALLABLE_REFERENCE
            }

            val getter = nameProperty.addGetter() {
                returnType = stringType
            }
            getter.overriddenSymbols += supperGetter.symbol
            getter.dispatchReceiverParameter = buildValueParameter(getter) {
                name = THIS_NAME
                type = clazz.defaultType
            }

            getter.body = context.irFactory.createBlockBody(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(
                    IrReturnImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET, nothingType, getter.symbol, IrConstImpl.string(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, stringType, function.name.asString()
                        )
                    )
                )
            )

            context.mapping.reflectedNameAccessor[clazz] = getter
        }

        fun build(): Pair<IrClass, IrConstructor> {
            val clazz = buildReferenceClass()
            val ctor = createConstructor(clazz)
            createInvokeMethod(clazz)
            createNameProperty(clazz)
            // TODO: create name property for KFunction*

            return Pair(clazz, ctor)
        }
    }

    companion object {
        object LAMBDA_IMPL : IrDeclarationOriginImpl("LAMBDA_IMPL")
        object FUNCTION_REFERENCE_IMPL : IrDeclarationOriginImpl("FUNCTION_REFERENCE_IMPL")
        object GENERATED_MEMBER_IN_CALLABLE_REFERENCE : IrDeclarationOriginImpl("GENERATED_MEMBER_IN_CALLABLE_REFERENCE")

        object CALLABLE_REFERENCE_CREATE : IrStatementOriginImpl("CALLABLE_REFERENCE_CREATE")
        object CALLABLE_REFERENCE_INVOKE : IrStatementOriginImpl("CALLABLE_REFERENCE_INVOKE")

        val THIS_NAME = Name.special("<this>")
        val BOUND_RECEIVER_NAME = Name.identifier("\$boundThis")
    }

}

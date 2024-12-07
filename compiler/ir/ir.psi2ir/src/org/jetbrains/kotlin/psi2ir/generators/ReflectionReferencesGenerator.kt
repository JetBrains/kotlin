/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.SyntheticPropertyDescriptor
import org.jetbrains.kotlin.descriptors.synthetic.FunctionInterfaceConstructorDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.DescriptorMetadataSource
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.referenceClassifier
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.intermediate.CallBuilder
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS

internal class ReflectionReferencesGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {

    fun generateClassLiteral(ktClassLiteral: KtClassLiteralExpression): IrExpression {
        val ktArgument = ktClassLiteral.receiverExpression!!
        val lhs = getOrFail(BindingContext.DOUBLE_COLON_LHS, ktArgument)
        val resultType = getTypeInferredByFrontendOrFail(ktClassLiteral).toIrType()

        return if (lhs is DoubleColonLHS.Expression && !lhs.isObjectQualifier) {
            IrGetClassImpl(
                ktClassLiteral.startOffsetSkippingComments, ktClassLiteral.endOffset, resultType,
                ktArgument.genExpr()
            )
        } else {
            val typeConstructorDeclaration = lhs.type.constructor.declarationDescriptor
            val typeClass = typeConstructorDeclaration
                ?: throw AssertionError("Unexpected type constructor for ${lhs.type}: $typeConstructorDeclaration")
            IrClassReferenceImpl(
                ktClassLiteral.startOffsetSkippingComments, ktClassLiteral.endOffset, resultType,
                context.symbolTable.referenceClassifier(typeClass), lhs.type.toIrType()
            )
        }
    }

    fun generateCallableReference(ktCallableReference: KtCallableReferenceExpression): IrExpression {
        val resolvedCall = getResolvedCall(ktCallableReference.callableReference)!!
        val resolvedDescriptor = resolvedCall.resultingDescriptor
        val callableReferenceType =
            context.typeTranslator.approximateFunctionReferenceType(
                getTypeInferredByFrontendOrFail(ktCallableReference)
            )
        val callBuilder = unwrapCallableDescriptorAndTypeArguments(resolvedCall)

        return when {
            resolvedDescriptor is FunctionInterfaceConstructorDescriptor ||
                    resolvedDescriptor.original is FunctionInterfaceConstructorDescriptor ->
                generateFunctionInterfaceConstructorReference(
                    ktCallableReference, callableReferenceType, callBuilder.descriptor
                )

            isAdaptedCallableReference(resolvedCall, resolvedDescriptor, callableReferenceType) ->
                generateAdaptedCallableReference(ktCallableReference, callBuilder, callableReferenceType)

            else -> {
                // The K1 frontend generates synthetic properties for Java getX/setX-like methods as if they have _extension_ receiver.
                //
                // However, in IR we have to assume the following invariant:
                // the shape of an IrPropertyReference must match the shape of IrPropertyReference#getter.
                //
                // In the case of synthetic Java properties, IrPropertyReference#getter is the Java getX method,
                // which has a _dispatch_ receiver, not extension receiver.
                //
                // For this reason, we have to do this hack.
                val dispatchReceiverValue = if (resolvedDescriptor is SyntheticPropertyDescriptor) {
                    resolvedCall.extensionReceiver
                } else {
                    resolvedCall.dispatchReceiver
                }
                val extensionReceiverValue = if (resolvedDescriptor is SyntheticPropertyDescriptor) {
                    null
                } else {
                    resolvedCall.extensionReceiver
                }
                statementGenerator.generateCallReceiver(
                    ktCallableReference,
                    resolvedDescriptor,
                    dispatchReceiverValue, extensionReceiverValue, resolvedCall.contextReceivers,
                    isSafe = false
                ).call { dispatchReceiverValue, extensionReceiverValue, _ ->
                    generateCallableReference(
                        ktCallableReference,
                        callableReferenceType,
                        callBuilder.descriptor,
                        callBuilder.typeArguments
                    ).also { irCallableReference ->
                        irCallableReference.dispatchReceiver = dispatchReceiverValue?.loadIfExists()
                        irCallableReference.extensionReceiver = extensionReceiverValue?.loadIfExists()
                    }
                }
            }
        }
    }

    private fun isAdaptedCallableReference(
        resolvedCall: ResolvedCall<out CallableDescriptor>,
        resolvedDescriptor: CallableDescriptor,
        callableReferenceType: KotlinType
    ) = resolvedCall.valueArguments.isNotEmpty() ||
            requiresCoercionToUnit(resolvedDescriptor, callableReferenceType) ||
            requiresSuspendConversion(resolvedDescriptor, callableReferenceType)

    private fun generateFunctionInterfaceConstructorReference(
        ktCallableReference: KtCallableReferenceExpression,
        callableReferenceType: KotlinType,
        descriptor: CallableDescriptor
    ): IrExpression {
        //  {
        //      fun <ADAPTER_FUN>(function: <FUN_TYPE>): <FUN_INTERFACE_TYPE> =
        //          <FUN_INTERFACE_TYPE>(function!!)
        //      ::<ADAPTER_FUN>
        //  }
        val startOffset = ktCallableReference.startOffsetSkippingComments
        val endOffset = ktCallableReference.endOffset

        val irReferenceType = callableReferenceType.toIrType()

        val irAdapterFun = createFunInterfaceConstructorAdapter(startOffset, endOffset, descriptor)

        val irAdapterRef = IrFunctionReferenceImpl(
            startOffset, endOffset,
            type = irReferenceType,
            symbol = irAdapterFun.symbol,
            typeArgumentsCount = irAdapterFun.typeParameters.size,
            reflectionTarget = irAdapterFun.symbol,
            origin = IrStatementOrigin.FUN_INTERFACE_CONSTRUCTOR_REFERENCE
        )

        return IrBlockImpl(
            startOffset, endOffset,
            irReferenceType,
            IrStatementOrigin.FUN_INTERFACE_CONSTRUCTOR_REFERENCE,
            listOf(
                irAdapterFun,
                irAdapterRef
            )
        )
    }

    private fun createFunInterfaceConstructorAdapter(startOffset: Int, endOffset: Int, descriptor: CallableDescriptor): IrSimpleFunction {
        val samType = descriptor.returnType
            ?: throw AssertionError("Unresolved return type: $descriptor")
        val samClassDescriptor = samType.constructor.declarationDescriptor as? ClassDescriptor
            ?: throw AssertionError("Class type expected: $samType")
        val irSamType = samType.toIrType()

        val functionParameter = descriptor.valueParameters.singleOrNull()
            ?: throw AssertionError("Single value parameter expected: $descriptor")

        return context.irFactory.createSimpleFunction(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = IrDeclarationOrigin.ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR,
            name = samClassDescriptor.name,
            visibility = DescriptorVisibilities.LOCAL,
            isInline = false,
            isExpect = false,
            returnType = irSamType,
            modality = Modality.FINAL,
            symbol = IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
            isExternal = false,
        ).also { irAdapterFun ->
            context.symbolTable.withScope(irAdapterFun) {
                irAdapterFun.metadata = null
                irAdapterFun.dispatchReceiverParameter = null
                irAdapterFun.extensionReceiverParameter = null

                val fnType = functionParameter.type

                val irFnParameter = createAdapterParameter(startOffset, endOffset, functionParameter.name, fnType)
                val irFnType = irFnParameter.type

                val checkNotNull = context.irBuiltIns.checkNotNullSymbol.descriptor
                val checkNotNullSubstituted =
                    checkNotNull.substitute(
                        TypeSubstitutor.create(
                            mapOf(checkNotNull.typeParameters[0].typeConstructor to TypeProjectionImpl(fnType))
                        )
                    ) ?: throw AssertionError("Substitution failed for $checkNotNull: T=$fnType")

                irAdapterFun.valueParameters = listOf(irFnParameter)
                irAdapterFun.body =
                    IrBlockBodyBuilder(
                        context,
                        Scope(irAdapterFun.symbol),
                        startOffset,
                        endOffset
                    ).blockBody {
                        +irReturn(
                            irSamConversion(
                                irCall(context.irBuiltIns.checkNotNullSymbol).also { irCall ->
                                    this@ReflectionReferencesGenerator.context.callToSubstitutedDescriptorMap[irCall] =
                                        checkNotNullSubstituted
                                    irCall.type = irFnType
                                    irCall.putTypeArgument(0, irFnType)
                                    irCall.putValueArgument(0, irGet(irFnParameter))
                                },
                                irSamType
                            )
                        )
                    }
            }
        }
    }

    private fun requiresCoercionToUnit(descriptor: CallableDescriptor, callableReferenceType: KotlinType): Boolean {
        val ktExpectedReturnType = callableReferenceType.arguments.last().type
        return KotlinBuiltIns.isUnit(ktExpectedReturnType) && !KotlinBuiltIns.isUnit(descriptor.returnType!!)
    }

    private fun requiresSuspendConversion(descriptor: CallableDescriptor, callableReferenceType: KotlinType): Boolean =
        descriptor is FunctionDescriptor &&
                !descriptor.isSuspend &&
                callableReferenceType.isKSuspendFunctionType

    private fun generateAdaptedCallableReference(
        ktCallableReference: KtCallableReferenceExpression,
        callBuilder: CallBuilder,
        callableReferenceType: KotlinType
    ): IrExpression {
        val adapteeDescriptor = callBuilder.descriptor
        if (adapteeDescriptor !is FunctionDescriptor) {
            throw AssertionError("Function descriptor expected in adapted callable reference: $adapteeDescriptor")
        }

        val startOffset = ktCallableReference.startOffsetSkippingComments
        val endOffset = ktCallableReference.endOffset

        val adapteeSymbol = context.symbolTable.referenceFunction(adapteeDescriptor.original)

        val ktFunctionalType = getTypeInferredByFrontendOrFail(ktCallableReference)
        val irFunctionalType = ktFunctionalType.maybeKFunctionTypeToFunctionType().toIrType()

        val ktFunctionalTypeArguments = ktFunctionalType.arguments
        val ktExpectedReturnType = ktFunctionalTypeArguments.last().type
        val ktExpectedParameterTypes = ktFunctionalTypeArguments.take(ktFunctionalTypeArguments.size - 1).map { it.type }

        val irAdapterFun =
            createAdapterFun(
                startOffset,
                endOffset,
                adapteeDescriptor,
                ktExpectedParameterTypes,
                ktExpectedReturnType,
                callBuilder,
                callableReferenceType
            )
        val irCall = createAdapteeCall(startOffset, endOffset, adapteeSymbol, callBuilder, irAdapterFun)

        irAdapterFun.body = context.irFactory.createBlockBody(startOffset, endOffset).apply {
            if (KotlinBuiltIns.isUnit(ktExpectedReturnType))
                statements.add(irCall)
            else
                statements.add(IrReturnImpl(startOffset, endOffset, context.irBuiltIns.nothingType, irAdapterFun.symbol, irCall))
        }

        val resolvedCall = callBuilder.original
        return statementGenerator.generateCallReceiver(
            ktCallableReference,
            resolvedCall.resultingDescriptor, resolvedCall.dispatchReceiver,
            resolvedCall.extensionReceiver,
            resolvedCall.contextReceivers,
            isSafe = false
        ).call { dispatchReceiverValue, extensionReceiverValue, _ ->
            val irDispatchReceiver = dispatchReceiverValue?.loadIfExists()
            val irExtensionReceiver = extensionReceiverValue?.loadIfExists()
            check(irDispatchReceiver == null || irExtensionReceiver == null) {
                "Bound callable reference cannot have both receivers: $adapteeDescriptor"
            }
            val receiver = irDispatchReceiver ?: irExtensionReceiver
            val irAdapterRef = IrFunctionReferenceImpl(
                startOffset, endOffset, irFunctionalType, irAdapterFun.symbol, irAdapterFun.typeParameters.size,
                adapteeSymbol, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE
            )
            IrBlockImpl(startOffset, endOffset, irFunctionalType, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE).apply {
                statements.add(irAdapterFun)
                statements.add(irAdapterRef.apply { extensionReceiver = receiver })
            }
        }
    }

    private fun createAdapteeCall(
        startOffset: Int,
        endOffset: Int,
        adapteeSymbol: IrFunctionSymbol,
        callBuilder: CallBuilder,
        irAdapterFun: IrSimpleFunction
    ): IrExpression {
        val resolvedCall = callBuilder.original
        val resolvedDescriptor = resolvedCall.resultingDescriptor

        val irType = resolvedDescriptor.returnType!!.toIrType()

        val irCall = when (adapteeSymbol) {
            is IrConstructorSymbol ->
                IrConstructorCallImpl.fromSymbolDescriptor(startOffset, endOffset, irType, adapteeSymbol)
            is IrSimpleFunctionSymbol ->
                IrCallImpl.fromSymbolDescriptor(startOffset, endOffset, irType, adapteeSymbol)
        }

        val hasBoundDispatchReceiver = resolvedCall.dispatchReceiver != null && resolvedCall.dispatchReceiver !is TransientReceiver
        val hasBoundExtensionReceiver = resolvedCall.extensionReceiver != null && resolvedCall.extensionReceiver !is TransientReceiver
        val isImportedFromObject = callBuilder.original.resultingDescriptor is ImportedFromObjectCallableDescriptor<*>
        if (hasBoundDispatchReceiver || hasBoundExtensionReceiver || isImportedFromObject) {
            // In case of a bound reference, the receiver (which can only be one) is passed in the extension receiver parameter.
            val receiverValue = IrGetValueImpl(
                startOffset, endOffset, irAdapterFun.extensionReceiverParameter!!.symbol, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE
            )
            when {
                hasBoundDispatchReceiver || isImportedFromObject ->
                    irCall.dispatchReceiver = receiverValue
                hasBoundExtensionReceiver ->
                    irCall.extensionReceiver = receiverValue
            }
        }

        context.callToSubstitutedDescriptorMap[irCall] = resolvedDescriptor

        irCall.putTypeArguments(callBuilder.typeArguments) { it.toIrType() }

        putAdaptedValueArguments(startOffset, endOffset, irCall, irAdapterFun, resolvedCall)

        return irCall
    }

    private fun putAdaptedValueArguments(
        startOffset: Int,
        endOffset: Int,
        irAdapteeCall: IrFunctionAccessExpression,
        irAdapterFun: IrSimpleFunction,
        resolvedCall: ResolvedCall<*>
    ) {
        val adaptedArguments = resolvedCall.valueArguments
        var shift = 0
        if (resolvedCall.dispatchReceiver is TransientReceiver) {
            // Unbound callable reference 'A::foo', receiver is passed as a first parameter
            val irAdaptedReceiverParameter = irAdapterFun.valueParameters[0]
            irAdapteeCall.dispatchReceiver =
                IrGetValueImpl(startOffset, endOffset, irAdaptedReceiverParameter.type, irAdaptedReceiverParameter.symbol)
        } else if (resolvedCall.extensionReceiver is TransientReceiver) {
            val irAdaptedReceiverParameter = irAdapterFun.valueParameters[0]
            irAdapteeCall.extensionReceiver =
                IrGetValueImpl(startOffset, endOffset, irAdaptedReceiverParameter.type, irAdaptedReceiverParameter.symbol)
            shift = 1
        }

        for ((valueParameter, valueArgument) in adaptedArguments) {
            val substitutedValueParameter = resolvedCall.resultingDescriptor.valueParameters[valueParameter.index]
            irAdapteeCall.putValueArgument(
                valueParameter.index,
                adaptResolvedValueArgument(startOffset, endOffset, valueArgument, irAdapterFun, substitutedValueParameter, shift)
            )
        }
    }

    private fun adaptResolvedValueArgument(
        startOffset: Int,
        endOffset: Int,
        resolvedValueArgument: ResolvedValueArgument,
        irAdapterFun: IrSimpleFunction,
        valueParameter: ValueParameterDescriptor,
        shift: Int
    ): IrExpression? {
        return when (resolvedValueArgument) {
            is DefaultValueArgument ->
                null
            is VarargValueArgument ->
                if (resolvedValueArgument.arguments.isEmpty())
                    null
                else
                    IrVarargImpl(
                        startOffset, endOffset,
                        valueParameter.type.toIrType(), valueParameter.varargElementType!!.toIrType(),
                        resolvedValueArgument.arguments.map {
                            adaptValueArgument(startOffset, endOffset, it, irAdapterFun, shift)
                        }
                    )
            is ExpressionValueArgument -> {
                val valueArgument = resolvedValueArgument.valueArgument!!

                adaptValueArgument(startOffset, endOffset, valueArgument, irAdapterFun, shift) as IrExpression
            }
            else ->
                throw AssertionError("Unexpected ResolvedValueArgument: $resolvedValueArgument")
        }
    }

    private fun adaptValueArgument(
        startOffset: Int,
        endOffset: Int,
        valueArgument: ValueArgument,
        irAdapterFun: IrSimpleFunction,
        shift: Int
    ): IrVarargElement =
        when (valueArgument) {
            is FakeImplicitSpreadValueArgumentForCallableReference ->
                IrSpreadElementImpl(
                    startOffset, endOffset,
                    adaptValueArgument(startOffset, endOffset, valueArgument.expression, irAdapterFun, shift) as IrExpression
                )

            is FakePositionalValueArgumentForCallableReference -> {
                val irAdapterParameter = irAdapterFun.valueParameters[valueArgument.index + shift]
                IrGetValueImpl(startOffset, endOffset, irAdapterParameter.type, irAdapterParameter.symbol)
            }

            else ->
                throw AssertionError("Unexpected ValueArgument: $valueArgument")
        }

    private fun createAdapterFun(
        startOffset: Int,
        endOffset: Int,
        adapteeDescriptor: FunctionDescriptor,
        ktExpectedParameterTypes: List<KotlinType>,
        ktExpectedReturnType: KotlinType,
        callBuilder: CallBuilder,
        callableReferenceType: KotlinType
    ): IrSimpleFunction {
        val hasSuspendConversion = !adapteeDescriptor.isSuspend &&
                callableReferenceType.isKSuspendFunctionType

        return context.irFactory.createSimpleFunction(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE,
            name = adapteeDescriptor.name,
            visibility = DescriptorVisibilities.LOCAL,
            isInline = adapteeDescriptor.isInline, // TODO ?
            isExpect = false,
            returnType = ktExpectedReturnType.toIrType(),
            modality = Modality.FINAL,
            symbol = IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = adapteeDescriptor.isSuspend || hasSuspendConversion,
            isOperator = adapteeDescriptor.isOperator, // TODO ?
            isInfix = adapteeDescriptor.isInfix,
            isExternal = false,
        ).also { irAdapterFun ->
            context.symbolTable.withScope(irAdapterFun) {
                irAdapterFun.metadata = DescriptorMetadataSource.Function(adapteeDescriptor)

                irAdapterFun.dispatchReceiverParameter = null

                val boundReceiverType = callBuilder.original.getBoundReceiverType()
                if (boundReceiverType != null) {
                    irAdapterFun.extensionReceiverParameter =
                        createAdapterParameter(startOffset, endOffset, Name.identifier("receiver"), boundReceiverType)
                } else {
                    irAdapterFun.extensionReceiverParameter = null
                }

                irAdapterFun.valueParameters += ktExpectedParameterTypes.mapIndexed { index, ktExpectedParameterType ->
                    createAdapterParameter(startOffset, endOffset, Name.identifier("p$index"), ktExpectedParameterType)
                }
            }
        }
    }

    private fun ResolvedCall<*>.getBoundReceiverType(): KotlinType? {
        val descriptor = resultingDescriptor
        if (descriptor is ImportedFromObjectCallableDescriptor<*>) {
            return descriptor.containingObject.defaultType
        }

        val dispatchReceiver = dispatchReceiver.takeUnless { it is TransientReceiver }
        val extensionReceiver = extensionReceiver.takeUnless { it is TransientReceiver }
        return when {
            dispatchReceiver == null -> extensionReceiver?.type
            extensionReceiver == null -> dispatchReceiver.type
            else -> error("Bound callable references can't have both receivers: $resultingDescriptor")
        }
    }

    private fun createAdapterParameter(startOffset: Int, endOffset: Int, name: Name, type: KotlinType): IrValueParameter =
        context.irFactory.createValueParameter(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE,
            name = name,
            type = type.toIrType(),
            isAssignable = false,
            symbol = IrValueParameterSymbolImpl(),
            varargElementType = null,
            isCrossinline = false,
            isNoinline = false,
            isHidden = false,
        )

    fun generateCallableReference(
        ktElement: KtElement,
        type: KotlinType,
        callableDescriptor: CallableDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
        origin: IrStatementOrigin? = null
    ): IrCallableReference<*> {
        val startOffset = ktElement.startOffsetSkippingComments
        val endOffset = ktElement.endOffset
        return when (callableDescriptor) {
            is FunctionDescriptor -> {
                val symbol = context.symbolTable.referenceFunction(callableDescriptor.original)
                generateFunctionReference(startOffset, endOffset, type, symbol, callableDescriptor, typeArguments, origin)
            }
            is PropertyDescriptor -> {
                val mutable = ReflectionTypes.isNumberedKMutablePropertyType(type)
                generatePropertyReference(startOffset, endOffset, type, callableDescriptor, typeArguments, origin, mutable)
            }
            else ->
                throw AssertionError("Unexpected callable reference: $callableDescriptor")
        }
    }

    fun generateLocalDelegatedPropertyReference(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        variableDescriptor: VariableDescriptorWithAccessors,
        irDelegateSymbol: IrVariableSymbol,
        origin: IrStatementOrigin?
    ): IrLocalDelegatedPropertyReference {
        val getterDescriptor =
            variableDescriptor.getter ?: throw AssertionError("Local delegated property should have a getter: $variableDescriptor")
        val setterDescriptor = variableDescriptor.setter

        val getterSymbol = context.symbolTable.descriptorExtension.referenceSimpleFunction(getterDescriptor)
        val setterSymbol = setterDescriptor?.let { context.symbolTable.descriptorExtension.referenceSimpleFunction(it) }

        return IrLocalDelegatedPropertyReferenceImpl(
            startOffset, endOffset, type.toIrType(),
            context.symbolTable.descriptorExtension.referenceLocalDelegatedProperty(variableDescriptor),
            irDelegateSymbol, getterSymbol, setterSymbol,
            origin
        ).apply {
            context.callToSubstitutedDescriptorMap[this] = variableDescriptor
        }
    }

    private class DelegatedPropertySymbols(
        val propertySymbol: IrPropertySymbol,
        val getterSymbol: IrSimpleFunctionSymbol?,
        val setterSymbol: IrSimpleFunctionSymbol?
    )

    private fun resolvePropertySymbol(descriptor: PropertyDescriptor, mutable: Boolean): DelegatedPropertySymbols {
        val symbol = context.symbolTable.descriptorExtension.referenceProperty(descriptor)
        val syntheticJavaProperty = context.extensions.unwrapSyntheticJavaProperty(descriptor)
        if (syntheticJavaProperty != null) {
            val (getMethod, setMethod) = syntheticJavaProperty
            // This is the special case of synthetic java properties when requested property doesn't even exist but IR design
            // requires its symbol to be bound so let do that
            // see `irText/declarations/provideDelegate/javaDelegate.kt` and KT-45297
            val getterSymbol = context.symbolTable.descriptorExtension.referenceSimpleFunction(getMethod)
            val setterSymbol = if (mutable) setMethod?.let {
                context.symbolTable.descriptorExtension.referenceSimpleFunction(it)
            } else null
            if (!symbol.isBound) {
                val offset = UNDEFINED_OFFSET
                context.symbolTable.descriptorExtension.declareProperty(descriptor) {
                    context.irFactory.createProperty(
                        startOffset = offset,
                        endOffset = offset,
                        origin = IrDeclarationOrigin.SYNTHETIC_JAVA_PROPERTY_DELEGATE,
                        name = descriptor.name,
                        visibility = descriptor.visibility,
                        modality = descriptor.modality,
                        symbol = symbol,
                        isVar = descriptor.isVar,
                        isConst = descriptor.isConst,
                        isLateinit = descriptor.isLateInit,
                        isDelegated = descriptor.isDelegated,
                        isExternal = descriptor.isExternal,
                        isExpect = descriptor.isExpect,
                    ).also {
                        it.parent = scope.getLocalDeclarationParent()
                    }
                }
            }
            return DelegatedPropertySymbols(symbol, getterSymbol, setterSymbol)
        } else {
            val getterSymbol = descriptor.getter?.let { context.symbolTable.descriptorExtension.referenceSimpleFunction(it) }
            val setterSymbol = if (mutable) descriptor.setter?.let { context.symbolTable.descriptorExtension.referenceSimpleFunction(it) } else null
            return DelegatedPropertySymbols(symbol, getterSymbol, setterSymbol)
        }
    }

    private fun generatePropertyReference(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        propertyDescriptor: PropertyDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
        origin: IrStatementOrigin?,
        mutable: Boolean
    ): IrPropertyReference {
        val originalProperty = propertyDescriptor.original
        val symbols = resolvePropertySymbol(originalProperty, mutable)

        // The K1 frontend generates synthetic properties for Java getX/setX-like methods as if they have _extension_ receiver.
        //
        // However, in IR we have to assume the following invariant:
        // the shape of an IrPropertyReference must match the shape of IrPropertyReference#getter.
        //
        // In the case of synthetic Java properties, IrPropertyReference#getter is the Java getX method,
        // which has a _dispatch_ receiver, not extension receiver.
        //
        // For this reason, we have to do this hack.
        val dispatchReceiver = if (propertyDescriptor is SyntheticPropertyDescriptor)
            propertyDescriptor.getMethod.dispatchReceiverParameter
        else
            propertyDescriptor.dispatchReceiverParameter

        val extensionReceiver = if (propertyDescriptor is SyntheticPropertyDescriptor)
            propertyDescriptor.getMethod.extensionReceiverParameter
        else
            propertyDescriptor.extensionReceiverParameter

        return IrPropertyReferenceImplWithShape(
            startOffset, endOffset, type.toIrType(),
            symbols.propertySymbol,
            dispatchReceiver != null,
            extensionReceiver != null,
            if (typeArguments != null) propertyDescriptor.typeParametersCount else 0,
            getFieldForPropertyReference(originalProperty),
            symbols.getterSymbol,
            symbols.setterSymbol,
            origin
        ).apply {
            context.callToSubstitutedDescriptorMap[this] = propertyDescriptor
            putTypeArguments(typeArguments) { it.toIrType() }
        }
    }

    private fun getFieldForPropertyReference(originalProperty: PropertyDescriptor) =
        // NB this is a hack, we really don't know if an arbitrary property has a backing field or not
        when {
            originalProperty.isDelegated -> null
            originalProperty.getter != null -> null
            else -> context.symbolTable.descriptorExtension.referenceField(originalProperty)
        }

    private fun generateFunctionReference(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        symbol: IrFunctionSymbol,
        descriptor: FunctionDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
        origin: IrStatementOrigin?
    ): IrFunctionReference =
        IrFunctionReferenceImpl.fromSymbolDescriptor(
            startOffset, endOffset, type.toIrType(),
            symbol,
            reflectionTarget = symbol,
            origin = origin
        ).apply {
            context.callToSubstitutedDescriptorMap[this] = descriptor
            putTypeArguments(typeArguments) { it.toIrType() }
        }

    // This patches up a frontend bug -- adapted references are mistakenly given a KFunction type.
    private fun KotlinType.maybeKFunctionTypeToFunctionType() = when {
        isKFunctionType -> kFunctionTypeToFunctionType(false)
        isKSuspendFunctionType -> kFunctionTypeToFunctionType(true)
        else -> this
    }

    private fun KotlinType.kFunctionTypeToFunctionType(suspendFunction: Boolean) = createFunctionType(
        (statementGenerator.context.irBuiltIns as IrBuiltInsOverDescriptors).builtIns,
        annotations,
        null,
        emptyList(),
        arguments.dropLast(1).map { it.type },
        null,
        arguments.last().type,
        suspendFunction
    )
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.utils.ConversionTypeOrigin
import org.jetbrains.kotlin.fir.backend.utils.convertWithOffsets
import org.jetbrains.kotlin.fir.backend.utils.createWhenForSafeFall
import org.jetbrains.kotlin.fir.backend.utils.varargElementType
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.isRestrictSuspensionReceiver
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.FirSamResolver
import org.jetbrains.kotlin.fir.resolve.calls.ResolvedCallArgument
import org.jetbrains.kotlin.fir.resolve.calls.stages.FirFakeArgumentForCallableReference
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * A generator that converts callable references or arguments that needs an adapter in between. This covers:
 *   1) Suspend conversion where a reference to or qualified access of non-suspend functional type is passed as an argument whose expected
 *     type is a suspend functional type;
 *   2) coercion-to-unit where a reference to a function whose return type isn't Unit is passed as an argument whose expected return type is
 *     Unit;
 *   3) vararg spread where a reference to a function with vararg parameter is passed as an argument whose use of that vararg parameter
 *     requires spreading.
 */
class AdapterGenerator(
    private val c: Fir2IrComponents,
    private val conversionScope: Fir2IrConversionScope
) : Fir2IrComponents by c {

    private val samResolver = FirSamResolver(session, scopeSession)

    internal fun needToGenerateAdaptedCallableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        type: IrSimpleType,
        function: FirFunction
    ): Boolean {
        return needSuspendConversion(type, function) ||
                needCoercionToUnit(type, function) ||
                hasVarargOrDefaultArguments(callableReferenceAccess)
    }

    /**
     * For example,
     * fun referenceConsumer(f: suspend () -> Unit) = ...
     * fun nonSuspendFunction(...) = ...
     * fun useSite(...) = { ... referenceConsumer(::nonSuspendFunction) ... }
     *
     * At the use site, instead of referenced, we can put the suspend lambda as an adapter.
     */
    private fun needSuspendConversion(type: IrSimpleType, function: FirFunction): Boolean {
        return type.isSuspendFunction() && !function.isSuspend
    }

    /**
     * For example,
     * fun referenceConsumer(f: () -> Unit) = f()
     * fun referenced(...): Any { ... }
     * fun useSite(...) = { ... referenceConsumer(::referenced) ... }
     *
     * At the use site, instead of referenced, we can put the adapter: { ... -> referenced(...) }
     */
    private fun needCoercionToUnit(type: IrSimpleType, function: FirFunction): Boolean {
        val expectedReturnType = type.arguments.last().typeOrNull
        val actualReturnType = function.returnTypeRef.coneType
        return expectedReturnType?.isUnit() == true &&
                // In case of an external function whose return type is a type parameter, e.g., operator fun <T, R> invoke(T): R
                !actualReturnType.isUnit && actualReturnType.toSymbol() !is FirTypeParameterSymbol
    }

    /**
     * For example,
     * fun referenceConsumer(f: (Char, Char) -> String): String = ... // e.g., f(char1, char2)
     * fun referenced(vararg xs: Char) = ...
     * fun useSite(...) = { ... referenceConsumer(::referenced) ... }
     *
     * At the use site, instead of referenced, we can put the adapter: { a, b -> referenced(a, b) }
     */
    private fun hasVarargOrDefaultArguments(callableReferenceAccess: FirCallableReferenceAccess): Boolean {
        // Unbound callable reference 'A::foo'
        val calleeReference = callableReferenceAccess.calleeReference as? FirResolvedCallableReference ?: return false
        return calleeReference.mappedArguments.any { [_, value] ->
            value is ResolvedCallArgument.VarargArgument || value is ResolvedCallArgument.DefaultArgument
        }
    }

    private inner class AdaptedCallableReferenceContext(
        val callableReferenceAccess: FirCallableReferenceAccess,
        val adaptedType: IrSimpleType,
        explicitReceiverExpression: IrExpression?,
    ) {
        val firAdaptee: FirCallableDeclaration =
            callableReferenceAccess.toResolvedCallableReference()?.resolvedSymbol?.fir as FirCallableDeclaration
        val substitutor: ConeSubstitutor =
            callableReferenceAccess.createConeSubstitutorFromTypeArguments(session) ?: ConeSubstitutor.Empty
        val boundDispatchReceiver: IrExpression? =
            callableReferenceAccess.findBoundReceiver(explicitReceiverExpression, isDispatch = true)
        val boundExtensionReceiver: IrExpression? =
            callableReferenceAccess.findBoundReceiver(explicitReceiverExpression, isDispatch = false)

        val boundReceiver: IrExpression? get() = boundDispatchReceiver ?: boundExtensionReceiver
        val hasBoundReceiver: Boolean get() = boundDispatchReceiver != null || boundExtensionReceiver != null
    }

    internal fun generateRichFunctionReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        type: IrSimpleType,
        explicitReceiverExpression: IrExpression?,
        irFunctionSymbol: IrFunctionSymbol,
        contextArguments: List<IrExpression>
    ): IrRichFunctionReference {
        val context =
            AdaptedCallableReferenceContext(callableReferenceAccess, type, explicitReceiverExpression)
        val function = context.firAdaptee as FirFunction
        return callableReferenceAccess.convertWithOffsets { startOffset, endOffset ->
            IrRichFunctionReferenceImpl(
                startOffset, endOffset, type,
                reflectionTargetSymbol = irFunctionSymbol,
                overriddenFunctionSymbol = findInvokeSymbol(callableReferenceAccess.resolvedType as ConeClassLikeType)!!,
                invokeFunction = context.buildCallableReferenceAdapterFunction(
                    startOffset,
                    endOffset,
                    adapteeSymbol = irFunctionSymbol,
                ),
                hasUnitConversion = needCoercionToUnit(type, function),
                hasSuspendConversion = needSuspendConversion(type, function),
                hasVarargConversion = (callableReferenceAccess.calleeReference as? FirResolvedCallableReference)?.mappedArguments
                    ?.any { [_, value] -> value is ResolvedCallArgument.VarargArgument } == true,
                isRestrictedSuspension =
                    function.receiverParameter?.typeRef?.coneType?.isRestrictSuspensionReceiver() == true ||
                            function.dispatchReceiverType?.isRestrictSuspensionReceiver() == true ||
                            function.contextParameters.any { it.returnTypeRef.coneType.isRestrictSuspensionReceiver() },
            ).apply {
                for (contextArgument in contextArguments) {
                    boundValues.add(contextArgument)
                }

                context.boundReceiver?.let(boundValues::add)
            }
        }
    }

    internal fun generateRichPropertyReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        type: IrType,
        explicitReceiverExpression: IrExpression?,
        irPropertySymbol: IrPropertySymbol,
        referencedPropertyGetterSymbol: IrSimpleFunctionSymbol,
        referencedPropertySetterSymbol: IrSimpleFunctionSymbol?,
        contextArguments: List<IrExpression>,
        isForDelegate: Boolean,
    ): IrRichPropertyReferenceImpl {
        val context = AdaptedCallableReferenceContext(
            callableReferenceAccess,
            type as IrSimpleType,
            explicitReceiverExpression
        )

        return callableReferenceAccess.convertWithOffsets { startOffset, endOffset ->
            IrRichPropertyReferenceImpl(
                startOffset, endOffset, type,
                reflectionTargetSymbol = irPropertySymbol,
                getterFunction = context.buildCallableReferenceAdapterFunction(
                    startOffset,
                    endOffset,
                    referencedPropertyGetterSymbol,
                    isForDelegate = isForDelegate,
                ),
                setterFunction = referencedPropertySetterSymbol?.let {
                    context.buildCallableReferenceAdapterFunction(
                        startOffset,
                        endOffset,
                        it,
                        isSetter = true,
                        isForDelegate = isForDelegate,
                    )
                },
                origin = runIf(isForDelegate) { IrStatementOrigin.PROPERTY_REFERENCE_FOR_DELEGATE },
            ).apply {
                for (contextArgument in contextArguments) {
                    boundValues.add(contextArgument)
                }

                context.boundReceiver?.let(boundValues::add)
            }
        }
    }

    internal fun generateAdaptedCallableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        explicitReceiverExpression: IrExpression?,
        adapteeSymbol: IrFunctionSymbol,
        type: IrSimpleType
    ): IrExpression {
        return callableReferenceAccess.convertWithOffsets { startOffset, endOffset ->
            val context = AdaptedCallableReferenceContext(callableReferenceAccess, type, explicitReceiverExpression)
            val irAdapterFunction = context.buildCallableReferenceAdapterFunction(startOffset, endOffset, adapteeSymbol, isForRichReference = false)

            require(irAdapterFunction.typeParameters.isEmpty()) {
                "Internal error: function adapter ${irAdapterFunction.symbol} " +
                        "has unexpected type parameters: ${irAdapterFunction.typeParameters.map { it.symbol }}\n" +
                        "They should already be used to determine exact return type and value parameters types"
            }
            val irAdapterRef = IrFunctionReferenceImpl(
                startOffset, endOffset, type, irAdapterFunction.symbol, typeArgumentsCount = 0,
                null, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE
            ).apply {
                context.boundReceiver?.let {
                    arguments[0] = it
                }

                reflectionTarget = adapteeSymbol
            }
            IrBlockImpl(startOffset, endOffset, type, IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE).apply {
                statements.add(irAdapterFunction)
                statements.add(irAdapterRef)
            }
        }
    }

    private fun AdaptedCallableReferenceContext.buildCallableReferenceAdapterFunction(
        startOffset: Int,
        endOffset: Int,
        adapteeSymbol: IrFunctionSymbol,
        isForRichReference: Boolean = true,
        isSetter: Boolean = false,
        isForDelegate: Boolean = false,
    ): IrSimpleFunction {
        val irAdapterFunction = createAdapterFunctionForCallableReference(startOffset, endOffset, isForRichReference, isSetter)
        val irCall = createAdapteeCallForCallableReference(adapteeSymbol, irAdapterFunction, isSetter)

        if (!isForDelegate) {
            irAdapterFunction.body = IrFactoryImpl.createBlockBody(startOffset, endOffset) {
                if (isSetter || adaptedType.arguments.last().typeOrNull?.isUnit() == true) {
                    statements.add(Fir2IrImplicitCastInserter.coerceToUnitIfNeeded(irCall))
                } else {
                    statements.add(IrReturnImpl(startOffset, endOffset, builtins.nothingType, irAdapterFunction.symbol, irCall))
                }
            }
        }
        return irAdapterFunction
    }

    private fun FirCallableReferenceAccess.findBoundReceiver(
        explicitReceiverExpression: IrExpression?,
        isDispatch: Boolean
    ): IrExpression? {
        val firReceiver = if (isDispatch) dispatchReceiver else extensionReceiver
        if (firReceiver == null) {
            return null
        }
        with(callGenerator) {
            return findIrReceiver(explicitReceiverExpression, isDispatch)
        }
    }

    private fun AdaptedCallableReferenceContext.createAdapterFunctionForCallableReference(
        startOffset: Int,
        endOffset: Int,
        isForRichReference: Boolean,
        isSetter: Boolean,
    ): IrSimpleFunction {
        val parameterTypes = adaptedType.arguments.dropLast(1).map { it.typeOrNull ?: builtins.anyNType }
        val firMemberAdaptee = firAdaptee as FirMemberDeclaration
        val name = when (firAdaptee) {
            is FirConstructor -> SpecialNames.INIT
            else -> firAdaptee.symbol.name
        }
        return IrFactoryImpl.createSimpleFunction(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = if (firAdaptee is FirFunction && needToGenerateAdaptedCallableReference(callableReferenceAccess, adaptedType, firAdaptee)) {
                IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE
            } else {
                IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            },
            name = name,
            visibility = DescriptorVisibilities.LOCAL,
            isInline = firMemberAdaptee.isInline,
            isExpect = false,
            returnType = if (isSetter) builtins.unitType else adaptedType.arguments.last().typeOrNull ?: builtins.anyNType,
            modality = Modality.FINAL,
            symbol = IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = firMemberAdaptee.isSuspend || adaptedType.isSuspendFunction(),
            isOperator = firMemberAdaptee.isOperator,
            isInfix = firMemberAdaptee.isInfix,
            isExternal = false,
        ).also { irAdapterFunction ->
            irAdapterFunction.parameters = buildList {
                for ([index, contextParameter] in firAdaptee.contextParameters.withIndex()) {
                    this += createAdapterParameter(
                        irAdapterFunction,
                        Name.identifier("c$index"),
                        substitutor.substituteOrSelf(contextParameter.returnTypeRef.coneType).toIrType(),
                        IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE,
                        IrParameterKind.Regular,
                    )
                }

                boundReceiver?.let {
                    if (boundDispatchReceiver != null && boundExtensionReceiver != null) {
                        error("Bound callable references can't have both receivers: ${callableReferenceAccess.render()}")
                    } else {
                        this += createAdapterParameter(
                            irAdapterFunction,
                            Name.identifier("receiver"),
                            it.type,
                            IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE,
                            if (isForRichReference) IrParameterKind.Regular else IrParameterKind.ExtensionReceiver,
                        )
                    }
                }

                parameterTypes.mapIndexedTo(this) { index, parameterType ->
                    createAdapterParameter(
                        irAdapterFunction,
                        Name.identifier("p$index"),
                        parameterType,
                        IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE,
                        IrParameterKind.Regular,
                    )
                }

                if (isSetter) {
                    this += createAdapterParameter(
                        irAdapterFunction,
                        Name.identifier("value"),
                        adaptedType.arguments.last().typeOrNull ?: builtins.anyNType,
                        IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE,
                        IrParameterKind.Regular,
                    )
                }
            }

            irAdapterFunction.parent = conversionScope.parent()!!
        }
    }

    private fun createAdapterParameter(
        adapterFunction: IrFunction,
        name: Name,
        type: IrType,
        origin: IrDeclarationOrigin,
        kind: IrParameterKind,
    ): IrValueParameter =
        IrFactoryImpl.createValueParameter(
            startOffset = adapterFunction.startOffset,
            endOffset = adapterFunction.endOffset,
            origin = origin,
            name = name,
            type = type,
            isAssignable = false,
            symbol = IrValueParameterSymbolImpl(),
            varargElementType = null,
            isCrossinline = false,
            isNoinline = false,
            isHidden = false,
            kind = kind,
        ).also { irAdapterValueParameter ->
            irAdapterValueParameter.parent = adapterFunction
        }

    private fun IrValueDeclaration.toIrGetValue(startOffset: Int, endOffset: Int): IrGetValue =
        IrGetValueImpl(startOffset, endOffset, this.type, this.symbol)

    private fun AdaptedCallableReferenceContext.createAdapteeCallForCallableReference(
        adapteeSymbol: IrFunctionSymbol,
        adapterFunction: IrFunction,
        isSetter: Boolean,
    ): IrExpression = callableReferenceAccess.convertWithOffsets { startOffset, endOffset ->
        val type = if (isSetter) {
            builtins.unitType
        } else {
            substitutor.substituteOrSelf(firAdaptee.returnTypeRef.coneType).toIrType()
        }
        val irCall = when (adapteeSymbol) {
            is IrConstructorSymbol ->
                IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, type, adapteeSymbol)
            is IrSimpleFunctionSymbol ->
                IrCallImpl(
                    startOffset,
                    endOffset,
                    type,
                    adapteeSymbol,
                    typeArgumentsCount = callableReferenceAccess.typeArguments.size,
                    origin = null,
                    superQualifierSymbol = null
                )
        }

        var adapterParameterIndex = 0
        var parameterShift = 0

        fun passThroughParameter(argumentIndex: Int, origin: IrStatementOrigin?) {
            val receiverParameter = adapterFunction.parameters[parameterShift]
            val receiverValue = IrGetValueImpl(
                startOffset, endOffset, receiverParameter.type, receiverParameter.symbol,
                origin
            )
            irCall.arguments[argumentIndex] = receiverValue
            parameterShift++
        }

        fun Boolean.toInt(): Int = if (this) 1 else 0

        // The parameters of the adapter function are always
        // [context0, ..., contextN, (dispatchReceiver,) (extensionReceiver,) parameter0, ..., parameterN]
        // Receivers can be bound or unbound.
        // The order is in this way because bound values must always come first and context parameter are always bound,
        // and this way we don't need to distinguish between bound and unbound receivers.
        // The case with both receivers currently only exists with delegated member extensions.

        val hasDispatchReceiver = firAdaptee.dispatchReceiverType != null
        val hasExtensionReceiver = firAdaptee.isInstanceExtension

        repeat(firAdaptee.contextParameters.size) {
            passThroughParameter(
                argumentIndex = it + hasDispatchReceiver.toInt(),
                origin = IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE,
            )
        }

        if (hasDispatchReceiver) {
            passThroughParameter(
                argumentIndex = 0,
                // Not sure if it's important that the origin is null for bound receivers.
                // Let's preserve it to minimize changes.
                origin = IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE.takeIf { hasBoundReceiver },
            )
        }

        if (hasExtensionReceiver) {
            passThroughParameter(
                argumentIndex = firAdaptee.contextParameters.size + hasDispatchReceiver.toInt(),
                // Not sure if it's important that the origin is null for bound receivers.
                // Let's preserve it to minimize changes.
                origin = IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE.takeIf { hasBoundReceiver },
            )
        }

        if (isSetter) {
            passThroughParameter(
                argumentIndex = firAdaptee.contextParameters.size + hasDispatchReceiver.toInt() + hasExtensionReceiver.toInt(),
                origin = IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE,
            )
        }

        val mappedArguments = (callableReferenceAccess.calleeReference as? FirResolvedCallableReference)?.mappedArguments

        fun buildIrGetValueArgument(argument: FirExpression?): IrGetValue {
            val parameterIndex = (argument as? FirFakeArgumentForCallableReference)?.index ?: adapterParameterIndex
            adapterParameterIndex++
            return adapterFunction.parameters[parameterIndex + parameterShift].toIrGetValue(startOffset, endOffset)
        }

        if (firAdaptee is FirFunction) {
            firAdaptee.valueParameters.forEachIndexed { index, valueParameter ->
                val varargElementType = valueParameter.varargElementType?.let(substitutor::substituteOrSelf)?.toIrType()
                val parameterType = substitutor.substituteOrSelf(valueParameter.returnTypeRef.coneType).toIrType()
                when (val mappedArgument = mappedArguments?.get(valueParameter)) {
                    is ResolvedCallArgument.VarargArgument -> {
                        val valueArgument = if (mappedArgument.arguments.isEmpty()) {
                            null
                        } else {
                            val adaptedValueArgument = IrVarargImpl(
                                startOffset,
                                endOffset,
                                parameterType,
                                varargElementType!!
                            )
                            for (argument in mappedArgument.arguments) {
                                val irValueArgument = buildIrGetValueArgument(argument)
                                adaptedValueArgument.addElement(irValueArgument)
                            }
                            adaptedValueArgument
                        }
                        irCall.arguments[index + parameterShift] = valueArgument
                    }
                    ResolvedCallArgument.DefaultArgument -> {
                        irCall.arguments[index + parameterShift] = null
                    }
                    is ResolvedCallArgument.SimpleArgument, null -> {
                        val irValueArgument = buildIrGetValueArgument(mappedArgument?.callArgument)
                        if (valueParameter.isVararg) {
                            irCall.arguments[index + parameterShift] =
                                IrVarargImpl(
                                    startOffset, endOffset,
                                    parameterType, varargElementType!!,
                                    listOf(IrSpreadElementImpl(startOffset, endOffset, irValueArgument))
                                )
                        } else {
                            irCall.arguments[index + parameterShift] = irValueArgument
                        }
                    }
                }
            }
        }

        with(callGenerator) {
            irCall.applyTypeArguments(callableReferenceAccess)
        }
    }

    internal fun IrExpression.applyFunctionTypeConversion(
        argument: FirFunctionTypeConversionExpression,
    ): IrExpression {
        return when (val kind = argument.kind) {
            is FirFunctionConversionKind.BetweenFunctionTypes ->
                applyConversionBetweenFunctionTypes(
                    argument,
                    kind,
                )
            is FirFunctionConversionKind.Sam ->
                applySamConversion(argument)
        }
    }

    private fun IrExpression.applySamConversion(
        argument: FirFunctionTypeConversionExpression,
    ): IrExpression {
        val samFirType = argument.resolvedType.let { it.removeExternalProjections(session.typeContext) ?: it }
        val samType = samFirType.toIrType(ConversionTypeOrigin.DEFAULT)

        // Make sure the converted IrType owner indeed has a single abstract method, since FunctionReferenceLowering relies on it.
        fun IrExpression.generateSamConversion() =
            IrTypeOperatorCallImpl(
                this.startOffset, this.endOffset, samType, IrTypeOperator.SAM_CONVERSION, samType,
                castArgumentToFunctionalInterfaceForSamType(
                    argument = this,
                    argumentConeType = argument.expression.resolvedType,
                    samType = samFirType
                )
            )

        return if (this is IrBlock && (origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE || origin == IrStatementOrigin.FUNCTION_TYPE_EXPRESSION_CONVERSION)) {
            // The IR for adapted callable references should be
            // BLOCK ADAPTED_FUNCTION_REFERENCE(FUN ADAPTER_FOR_CALLABLE_REFERENCE, TYPE_OP SAM_CONVERSION(FUNCTION_REFERENCE))
            // Therefore, we need to insert the cast as the last statement of the block, not around the block itself.
            val lastIndex = statements.lastIndex
            val samConversion = (statements[lastIndex] as IrExpression).generateSamConversion()
            statements[lastIndex] = samConversion
            this.type = samConversion.type
            this
        } else {
            generateSamConversion()
        }
    }

    // See org.jetbrains.kotlin.psi2ir.generators.ArgumentsGenerationUtilsKt.castArgumentToFunctionalInterfaceForSamType (K1 counterpart)
    private fun castArgumentToFunctionalInterfaceForSamType(
        argument: IrExpression,
        argumentConeType: ConeKotlinType,
        samType: ConeKotlinType,
    ): IrExpression {
        // The rule for SAM conversions is: the argument must be a subtype of the required function type.
        // We handle intersection types, captured types, etc. by approximating both expected and actual types.
        val approximatedConeKotlinFunctionType = getFunctionTypeForPossibleSamType(samType)?.approximateForIrOrSelf() ?: return argument

        // This line is not present in the K1 counterpart because there is InsertImplicitCasts::cast that effectively removes
        // such unnecessary casts. At the same time, many IR lowerings assume that there are no such redundant casts and many
        // tests from FirBlackBoxCodegenTestGenerated relevant to INDY start failing once this line is removed.
        val approximateArgumentConeType = argumentConeType.approximateForIrOrSelf()

        if (approximateArgumentConeType.isSubtypeOf(approximatedConeKotlinFunctionType, session)) {
            return argument
        }

        val irFunctionType = approximatedConeKotlinFunctionType.toIrType()
        return argument.implicitCastTo(irFunctionType)
    }

    // This function is mostly a mirror of org.jetbrains.kotlin.backend.common.SamTypeApproximator.removeExternalProjections
    // First attempts, to share the code between K1 and K2 via type contexts stumbled upon the absence of star-projection-type in K2
    // and the possibility of incorrectly mapped details that might break some code when using K1.
    private fun ConeKotlinType.removeExternalProjections(typeContext: ConeInferenceContext): ConeKotlinType? =
        when (this) {
            is ConeRigidType -> removeExternalProjections()
            is ConeFlexibleType -> mapTypesOrNull(typeContext) { it.removeExternalProjections() }
        }

    private fun ConeRigidType.removeExternalProjections(): ConeRigidType? {
        if (this@removeExternalProjections !is ConeClassLikeType) return this

        with(session.typeContext) {
            val arguments = typeArguments.ifEmpty { return this@removeExternalProjections }
            val parameters = lookupTag.getParameters()
            val parameterSet = parameters.toSet()

            @Suppress("UNCHECKED_CAST")
            val newArguments = Array(arguments.size) { i ->
                val argument = arguments[i]
                val parameter = parameters.getOrNull(i) ?: return null
                when {
                    argument.kind == ProjectionKind.IN && c.configuration.carefulApproximationOfContravariantProjectionForSam -> {
                        // Just erasing `in` from the type projection would lead to an incorrect type for the SAM adapter,
                        // and error at runtime on JVM if invokedynamic + LambdaMetafactory is used, see KT-51868.
                        // So we do it "carefully". If we have a class `A<T>` and a method that takes e.g. `A<in String>`, we check
                        // if `T` has a non-trivial upper bound. If it has one, we don't attempt to perform a SAM conversion at all.
                        // Otherwise we erase the type to `Any?`, so `A<in String>` becomes `A<Any?>`, which is the computed SAM type.
                        val upperBound = parameter.getUpperBounds().singleOrNull()?.upperBoundIfFlexible()?.asCone() ?: return null
                        if (!upperBound.isNullableAny) return null

                        upperBound
                    }
                    argument is ConeKotlinTypeProjection -> argument.type
                    else -> parameter.typeParameterSymbol.starProjectionTypeRepresentation(parameterSet)
                }
            }

            return withArguments(newArguments)
        }
    }

    // See the definition from K1 at org.jetbrains.kotlin.types.StarProjectionImpl.get_type
    // In K1, it's used more frequently because of not-nullable TypeProjection::getType, but in K2 we almost got rid of it
    // But here, we still need it to more-or-less fully reproduce the semantics of K1 when generating SAM conversions
    private fun FirTypeParameterSymbol.starProjectionTypeRepresentation(containingParameterSet: Set<ConeTypeParameterLookupTag>): ConeKotlinType {
        val substitutor = object : AbstractConeSubstitutor(session.typeContext) {
            // We don't substitute types
            override fun substituteType(type: ConeKotlinType): ConeKotlinType? = null

            override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
                // But we substitute type parameters from the class-owner of this@FirTypeParameterSymbol as it's done in K1
                if (projection is ConeTypeParameterType && projection.lookupTag in containingParameterSet) return ConeStarProjection
                return super.substituteArgument(projection, index)
            }
        }

        return substitutor.substituteOrSelf(resolvedBounds.first().coneType)
    }

    internal fun getFunctionTypeForPossibleSamType(parameterType: ConeKotlinType): ConeKotlinType? {
        return samResolver.getSamInfoForPossibleSamType(parameterType)?.functionalType
    }

    /**
     * For example,
     * fun consumer(f: suspend () -> Unit) = ...
     * fun nonSuspendFunction = { ... }
     * fun useSite(...) = { ... consumer(nonSuspendFunction) ... }
     *
     * At the use site, instead of the argument, we can put the suspend lambda as an adapter.
     *
     * Instead of functions, a subtype of functional type can be used too:
     * class Foo {
     *   override fun invoke() = ...
     * }
     * fun useSite(...) = { ... consumer(Foo()) ... }
     */
    private fun IrExpression.applyConversionBetweenFunctionTypes(
        argument: FirFunctionTypeConversionExpression,
        kind: FirFunctionConversionKind.BetweenFunctionTypes,
    ): IrExpression {
        val expectedType = argument.resolvedType
        check(expectedType is ConeClassLikeType && expectedType.functionTypeKind(session) != null)

        val originalArgumentType = argument.expression.resolvedType.fullyExpandedType()
        // No conversion should happen if an argument already satisfies the expected type requirements
        check(!originalArgumentType.isSubtypeOf(expectedType, session))

        check(kind.isFromSimpleToCustom || kind.isForUnitCoercion)
        val isSuspendFunctionTypeExpected = expectedType.isSuspendOrKSuspendFunctionType(session)
        val needSuspendConversion = kind.isFromSimpleToCustom && isSuspendFunctionTypeExpected

        // This case is only applied when custom function types from a plugin are involved.
        // For that case plugins themselves should handle the conversion.
        // On the other hand, currently, there are no known plugins that do it (Compose dropped the conversion).
        if (!needSuspendConversion && !kind.isForUnitCoercion) return this

        val functionTypeBeforeConversion = kind.originalArgumentAsFunctionType
        check(functionTypeBeforeConversion.functionTypeKind(session) != null)

        val invokeSymbol = findInvokeSymbol(functionTypeBeforeConversion, originalArgumentType) ?: return this
        val expectedIrType = expectedType.toIrType() as IrSimpleType
        return argument.convertWithOffsets { startOffset, endOffset ->
            val irAdapterFunction = createAdapterFunctionForArgument(
                startOffset,
                endOffset,
                expectedIrType,
                adapteeParameterType = functionTypeBeforeConversion.toIrType(),
                originalArgumentType.isMarkedNullable,
                invokeSymbol,
                isSuspendFunctionTypeExpected,
            )
            val irAdapterRef = IrFunctionReferenceImpl(
                startOffset, endOffset, expectedIrType, irAdapterFunction.symbol, irAdapterFunction.typeParameters.size,
                null, IrStatementOrigin.FUNCTION_TYPE_EXPRESSION_CONVERSION
            )
            IrBlockImpl(startOffset, endOffset, expectedIrType, IrStatementOrigin.FUNCTION_TYPE_EXPRESSION_CONVERSION).apply {
                statements.add(irAdapterFunction)
                statements.add(irAdapterRef.apply { arguments[0] = this@applyConversionBetweenFunctionTypes })
            }
        }
    }

    /**
     * Returns the proper `invoke` symbol of a FunctionN type and the expected FunctionN argument type
     */
    private fun findInvokeSymbol(
        expectedFunctionalType: ConeClassLikeType,
        argumentType: ConeKotlinType
    ): IrSimpleFunctionSymbol? {
        if (argumentType.findSubtypeOfBasicFunctionType(session, expectedFunctionalType) == null) {
            return null
        }
        return findInvokeSymbol(expectedFunctionalType)
    }

    internal fun findInvokeSymbol(expectedFunctionalType: ConeClassLikeType): IrSimpleFunctionSymbol? {
        return if (expectedFunctionalType.isSomeFunctionType(session)) {
            expectedFunctionalType.findBaseInvokeSymbol()
        } else {
            expectedFunctionalType.findContributedInvokeSymbol(
                shouldCalculateReturnTypesOfFakeOverrides = true
            )
        }?.let {
            declarationStorage.getIrFunctionSymbol(it) as? IrSimpleFunctionSymbol
        }
    }

    private fun createAdapterFunctionForArgument(
        startOffset: Int,
        endOffset: Int,
        type: IrSimpleType,
        adapteeParameterType: IrType,
        argumentIsNullable: Boolean,
        invokeSymbol: IrSimpleFunctionSymbol,
        isSuspend: Boolean,
    ): IrSimpleFunction {
        val returnType = type.arguments.last().typeOrNull ?: builtins.anyNType
        val parameterTypes = type.arguments.dropLast(1).map { it.typeOrNull ?: builtins.anyNType }
        return IrFactoryImpl.createSimpleFunction(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = IrDeclarationOrigin.ADAPTER_FOR_SUSPEND_CONVERSION,
            name = Name.identifier(conversionScope.scope().inventNameForTemporary("suspendConversion")),
            visibility = DescriptorVisibilities.LOCAL,
            isInline = false,
            isExpect = false,
            returnType = returnType,
            modality = Modality.FINAL,
            symbol = IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = isSuspend,
            isOperator = false,
            isInfix = false,
            isExternal = false,
        ).also { irAdapterFunction ->
            irAdapterFunction.parameters = buildList {
                this += createAdapterParameter(
                    irAdapterFunction,
                    Name.identifier($$"$callee"),
                    adapteeParameterType,
                    IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_SUSPEND_CONVERSION,
                    IrParameterKind.ExtensionReceiver,
                )

                parameterTypes.mapIndexedTo(this) { index, parameterType ->
                    createAdapterParameter(
                        irAdapterFunction,
                        Name.identifier("p$index"),
                        parameterType,
                        IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_SUSPEND_CONVERSION,
                        IrParameterKind.Regular,
                    )
                }
            }
            irAdapterFunction.body = IrFactoryImpl.createBlockBody(startOffset, endOffset) {
                var irCall = createAdapteeCallForArgument(startOffset, endOffset, irAdapterFunction, invokeSymbol)

                if (argumentIsNullable) {
                    irCall = createWhenForSafeFall(irCall.type, irAdapterFunction.parameters[0].symbol, irCall)
                }

                if (returnType.isUnit()) {
                    statements.add(irCall)
                } else {
                    statements.add(IrReturnImpl(startOffset, endOffset, builtins.nothingType, irAdapterFunction.symbol, irCall))
                }
            }
            irAdapterFunction.parent = conversionScope.parent()!!
        }
    }

    private fun createAdapteeCallForArgument(
        startOffset: Int,
        endOffset: Int,
        adapterFunction: IrFunction,
        invokeSymbol: IrSimpleFunctionSymbol
    ): IrExpression {
        val irCall = IrCallImpl(
            startOffset, endOffset,
            adapterFunction.returnType,
            invokeSymbol,
            typeArgumentsCount = 0
        )
        for ([i, parameter] in adapterFunction.parameters.withIndex()) {
            irCall.arguments[i] = parameter.toIrGetValue(startOffset, endOffset)
        }
        return irCall
    }

    fun generateFunInterfaceConstructorReference(
        callableReference: FirCallableReferenceAccess,
        callableSymbol: FirFunctionSymbol<*>,
        irReferenceType: IrType
    ): IrExpression =
        callableReference.convertWithOffsets { startOffset: Int, endOffset: Int ->
            //  {
            //      fun <ADAPTER_FUN>(function: <FUN_TYPE>): <FUN_INTERFACE_TYPE> =
            //          <FUN_INTERFACE_TYPE>(function!!)
            //      ::<ADAPTER_FUN>
            //  }

            val irAdapterFun = generateFunInterfaceConstructorAdapter(startOffset, endOffset, callableSymbol, irReferenceType)

            val irAdapterRef = IrFunctionReferenceImpl(
                startOffset, endOffset,
                type = irReferenceType,
                symbol = irAdapterFun.symbol,
                typeArgumentsCount = irAdapterFun.typeParameters.size,
                reflectionTarget = irAdapterFun.symbol,
                origin = IrStatementOrigin.FUN_INTERFACE_CONSTRUCTOR_REFERENCE
            )

            IrBlockImpl(
                startOffset, endOffset,
                irReferenceType,
                IrStatementOrigin.FUN_INTERFACE_CONSTRUCTOR_REFERENCE,
                listOf(
                    irAdapterFun,
                    irAdapterRef
                )
            )
        }

    private fun IrSimpleType.getArgumentTypeAt(index: Int): IrType {
        val irTypeArgument = this.arguments[index] as? IrTypeProjection
            ?: throw AssertionError("Type projection expected at argument $index: ${this.render()}")
        return irTypeArgument.type
    }

    private fun generateFunInterfaceConstructorAdapter(
        startOffset: Int,
        endOffset: Int,
        callableSymbol: FirFunctionSymbol<*>,
        irReferenceType: IrType
    ): IrSimpleFunction {
        // Here irReferenceType is always kotlin.reflect.KFunction1<FUN_TYPE, FUN_INTERFACE_TYPE>
        val irSimpleReferenceType = irReferenceType as? IrSimpleType
            ?: throw AssertionError("Class type expected: ${irReferenceType.render()}")
        val irSamType = irSimpleReferenceType.getArgumentTypeAt(1)
        val irFunctionType = irSimpleReferenceType.getArgumentTypeAt(0)

        val functionParameter = callableSymbol.valueParameterSymbols.singleOrNull()
            ?: throw AssertionError("Single value parameter expected: ${callableSymbol.valueParameterSymbols}")

        return IrFactoryImpl.createSimpleFunction(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = IrDeclarationOrigin.ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR,
            name = callableSymbol.name,
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
        ).also { irAdapterFunction ->
            val irFunctionParameter = createAdapterParameter(
                irAdapterFunction,
                functionParameter.name,
                irFunctionType,
                IrDeclarationOrigin.ADAPTER_PARAMETER_FOR_CALLABLE_REFERENCE,
                IrParameterKind.Regular,
            )
            irAdapterFunction.parameters = listOf(irFunctionParameter)
            irAdapterFunction.body = IrFactoryImpl.createBlockBody(
                startOffset, endOffset,
                listOf(
                    IrReturnImpl(
                        startOffset, endOffset, c.builtins.nothingType, irAdapterFunction.symbol,
                        IrTypeOperatorCallImpl(
                            startOffset, endOffset, irSamType, IrTypeOperator.SAM_CONVERSION, irSamType,
                            IrCallImplWithShape(
                                startOffset, endOffset, irFunctionType, builtins.checkNotNullSymbol,
                                typeArgumentsCount = 1,
                                valueArgumentsCount = 1,
                                contextParameterCount = 0,
                                hasDispatchReceiver = false,
                                hasExtensionReceiver = false,
                                origin = IrStatementOrigin.EXCLEXCL
                            ).apply {
                                typeArguments[0] = irFunctionType
                                arguments[0] = IrGetValueImpl(startOffset, endOffset, irFunctionParameter.symbol)
                            }
                        )
                    )
                )
            )
            irAdapterFunction.parent = conversionScope.parent()!!
        }
    }
}

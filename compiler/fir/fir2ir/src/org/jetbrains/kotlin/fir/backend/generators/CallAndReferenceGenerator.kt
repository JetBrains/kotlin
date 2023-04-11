/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.isMarkedWithImplicitIntegerCoercion
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.approximateDeclarationType
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isMethodOfAny
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator.commonSuperType
import org.jetbrains.kotlin.util.OperatorNameConventions

class CallAndReferenceGenerator(
    private val components: Fir2IrComponents,
    private val visitor: Fir2IrVisitor,
    private val conversionScope: Fir2IrConversionScope
) : Fir2IrComponents by components {

    private val adapterGenerator = AdapterGenerator(components, conversionScope)

    private fun FirTypeRef.toIrType(): IrType =
        with(typeConverter) { toIrType(conversionScope.defaultConversionTypeContext()) }

    private fun ConeKotlinType.toIrType(): IrType =
        with(typeConverter) { toIrType(conversionScope.defaultConversionTypeContext()) }

    fun convertToIrCallableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        explicitReceiverExpression: IrExpression?,
        isDelegate: Boolean
    ): IrExpression {
        val type = approximateFunctionReferenceType(callableReferenceAccess.typeRef.coneType).toIrType()

        val callableSymbol = callableReferenceAccess.calleeReference.toResolvedCallableSymbol()
        if (callableSymbol?.origin == FirDeclarationOrigin.SamConstructor) {
            assert(explicitReceiverExpression == null) {
                "Fun interface constructor reference should be unbound: ${explicitReceiverExpression?.dump()}"
            }
            return adapterGenerator.generateFunInterfaceConstructorReference(
                callableReferenceAccess,
                callableSymbol as FirSyntheticFunctionSymbol,
                type
            )
        }

        val symbol = callableReferenceAccess.calleeReference.toSymbolForCall(
            callableReferenceAccess.dispatchReceiver,
            conversionScope,
            explicitReceiver = callableReferenceAccess.explicitReceiver,
            isDelegate = isDelegate,
            isReference = true
        )
        // val x by y ->
        //   val `x$delegate` = y
        //   val x get() = `x$delegate`.getValue(this, ::x)
        // The reference here (like the rest of the accessor) has DefaultAccessor source kind.
        val isForDelegate = callableReferenceAccess.source?.kind == KtFakeSourceElementKind.DelegatedPropertyAccessor
        val origin = if (isForDelegate) IrStatementOrigin.PROPERTY_REFERENCE_FOR_DELEGATE else null
        return callableReferenceAccess.convertWithOffsets { startOffset, endOffset ->
            when (symbol) {
                is IrPropertySymbol -> {
                    val referencedProperty = symbol.owner
                    val referencedPropertyGetter = referencedProperty.getter
                    val referencedPropertySetterSymbol =
                        if (callableReferenceAccess.typeRef.coneType.isKMutableProperty(session)) referencedProperty.setter?.symbol
                        else null
                    val backingFieldSymbol = when {
                        referencedPropertyGetter != null -> null
                        else -> referencedProperty.backingField?.symbol
                    }
                    IrPropertyReferenceImpl(
                        startOffset, endOffset, type, symbol,
                        typeArgumentsCount = referencedPropertyGetter?.typeParameters?.size ?: 0,
                        field = backingFieldSymbol,
                        getter = referencedPropertyGetter?.symbol,
                        setter = referencedPropertySetterSymbol,
                        origin = origin
                    ).applyTypeArguments(callableReferenceAccess).applyReceivers(callableReferenceAccess, explicitReceiverExpression)
                }

                is IrLocalDelegatedPropertySymbol -> {
                    IrLocalDelegatedPropertyReferenceImpl(
                        startOffset, endOffset, type, symbol,
                        delegate = symbol.owner.delegate.symbol,
                        getter = symbol.owner.getter.symbol,
                        setter = symbol.owner.setter?.symbol,
                        origin = origin
                    )
                }

                is IrFieldSymbol -> {
                    val referencedField = symbol.owner
                    val propertySymbol = referencedField.correspondingPropertySymbol
                        ?: run {
                            // In case of [IrField] without the corresponding property, we've created it directly from [FirField].
                            // Since it's used as a field reference, we need a bogus property as a placeholder.
                            val firSymbol =
                                (callableReferenceAccess.calleeReference as FirResolvedNamedReference).resolvedSymbol as FirFieldSymbol
                            declarationStorage.getOrCreateIrPropertyByPureField(firSymbol.fir, referencedField.parent).symbol
                        }
                    IrPropertyReferenceImpl(
                        startOffset, endOffset, type,
                        propertySymbol,
                        typeArgumentsCount = (type as? IrSimpleType)?.arguments?.size ?: 0,
                        field = symbol,
                        getter = if (referencedField.isStatic) null else propertySymbol.owner.getter?.symbol,
                        setter = if (referencedField.isStatic) null else propertySymbol.owner.setter?.symbol,
                        origin
                    ).applyReceivers(callableReferenceAccess, explicitReceiverExpression)
                }

                is IrFunctionSymbol -> {
                    assert(type.isFunctionTypeOrSubtype()) {
                        "Callable reference whose symbol refers to a function should be of functional type."
                    }
                    type as IrSimpleType
                    val function = symbol.owner
                    if (adapterGenerator.needToGenerateAdaptedCallableReference(callableReferenceAccess, type, function)) {
                        // Receivers are being applied inside
                        with(adapterGenerator) {
                            // TODO: Figure out why `adaptedType` is different from the `type`?
                            val adaptedType = callableReferenceAccess.typeRef.coneType.toIrType() as IrSimpleType
                            generateAdaptedCallableReference(callableReferenceAccess, explicitReceiverExpression, symbol, adaptedType)
                        }
                    } else {
                        val klass = function.parent as? IrClass
                        val typeArgumentCount = function.typeParameters.size +
                                if (function is IrConstructor) klass?.typeParameters?.size ?: 0 else 0
                        IrFunctionReferenceImpl(
                            startOffset, endOffset, type, symbol,
                            typeArgumentsCount = typeArgumentCount,
                            valueArgumentsCount = function.valueParameters.size,
                            reflectionTarget = symbol
                        ).applyTypeArguments(callableReferenceAccess)
                            .applyReceivers(callableReferenceAccess, explicitReceiverExpression)
                    }
                }

                else -> {
                    IrErrorCallExpressionImpl(
                        startOffset, endOffset, type, "Unsupported callable reference: ${callableReferenceAccess.render()}"
                    )
                }
            }
        }
    }

    private fun approximateFunctionReferenceType(kotlinType: ConeKotlinType): ConeKotlinType {
        // This is a hack to support intersection types in function references on JVM.
        // Function reference type KFunctionN<T1, ..., TN, R> might contain intersection types in its top-level arguments.
        // Intersection types in expressions and local variable declarations usually don't bother us.
        // However, in case of function references type mapping affects behavior:
        // resulting function reference class will have a bridge method, which will downcast its arguments to the expected types.
        // This would cause ClassCastException in case of usual type approximation,
        // because '{ X1 & ... & Xm }' would be approximated to 'Nothing'.
        // JVM_OLD just relies on type mapping for generic argument types in such case.
        if (!kotlinType.isReflectFunctionType(session))
            return kotlinType
        if (kotlinType !is ConeSimpleKotlinType)
            return kotlinType
        if (kotlinType.typeArguments.none { it.type is ConeIntersectionType })
            return kotlinType
        val functionParameterTypes = kotlinType.typeArguments.take(kotlinType.typeArguments.size - 1)
        val functionReturnType = kotlinType.typeArguments.last()
        return ConeClassLikeTypeImpl(
            (kotlinType as ConeClassLikeType).lookupTag,
            (functionParameterTypes.map { approximateFunctionReferenceParameterType(it) } + functionReturnType).toTypedArray(),
            kotlinType.isNullable,
            kotlinType.attributes
        )
    }

    private fun approximateFunctionReferenceParameterType(typeProjection: ConeTypeProjection): ConeTypeProjection {
        if (typeProjection.isStarProjection) return typeProjection
        val intersectionType = typeProjection as? ConeIntersectionType ?: return typeProjection
        val newType = intersectionType.alternativeType
            ?: session.typeContext.commonSuperType(intersectionType.intersectedTypes.toList()) as? ConeKotlinType
            ?: return typeProjection
        return newType.toTypeProjection(typeProjection.kind)
    }

    private fun FirQualifiedAccessExpression.tryConvertToSamConstructorCall(type: IrType): IrTypeOperatorCall? {
        val calleeReference = calleeReference as? FirResolvedNamedReference ?: return null
        val fir = calleeReference.resolvedSymbol.fir
        if (this is FirFunctionCall && fir is FirSimpleFunction && fir.origin == FirDeclarationOrigin.SamConstructor) {
            return convertWithOffsets { startOffset, endOffset ->
                IrTypeOperatorCallImpl(
                    startOffset, endOffset, type, IrTypeOperator.SAM_CONVERSION, type, visitor.convertToIrExpression(argument)
                )
            }
        }
        return null
    }

    private fun FirExpression.superQualifierSymbol(): IrClassSymbol? {
        if (this !is FirQualifiedAccessExpression) {
            return null
        }
        val dispatchReceiverReference = calleeReference
        if (dispatchReceiverReference !is FirSuperReference) {
            return null
        }
        val superTypeRef = dispatchReceiverReference.superTypeRef
        val coneSuperType = superTypeRef.coneTypeSafe<ConeClassLikeType>() ?: return null
        val firClassSymbol = coneSuperType.fullyExpandedType(session).lookupTag.toSymbol(session) as? FirClassSymbol<*>
        if (firClassSymbol != null) {
            return classifierStorage.getIrClassSymbol(firClassSymbol)
        }
        return null
    }

    private val Name.dynamicOperator
        get() = when (this) {
            OperatorNameConventions.UNARY_PLUS -> IrDynamicOperator.UNARY_PLUS
            OperatorNameConventions.UNARY_MINUS -> IrDynamicOperator.UNARY_MINUS
            OperatorNameConventions.NOT -> IrDynamicOperator.EXCL
            OperatorNameConventions.PLUS -> IrDynamicOperator.BINARY_PLUS
            OperatorNameConventions.MINUS -> IrDynamicOperator.BINARY_MINUS
            OperatorNameConventions.TIMES -> IrDynamicOperator.MUL
            OperatorNameConventions.DIV -> IrDynamicOperator.DIV
            OperatorNameConventions.REM -> IrDynamicOperator.MOD
            OperatorNameConventions.AND -> IrDynamicOperator.ANDAND
            OperatorNameConventions.OR -> IrDynamicOperator.OROR
            OperatorNameConventions.EQUALS -> IrDynamicOperator.EQEQ
            OperatorNameConventions.PLUS_ASSIGN -> IrDynamicOperator.PLUSEQ
            OperatorNameConventions.MINUS_ASSIGN -> IrDynamicOperator.MINUSEQ
            OperatorNameConventions.TIMES_ASSIGN -> IrDynamicOperator.MULEQ
            OperatorNameConventions.DIV_ASSIGN -> IrDynamicOperator.DIVEQ
            OperatorNameConventions.REM_ASSIGN -> IrDynamicOperator.MODEQ
            else -> null
        }

    private val FirQualifiedAccessExpression.dynamicOperator
        get() = when (calleeReference.source?.kind) {
            is KtFakeSourceElementKind.ArrayAccessNameReference -> when (calleeReference.resolved?.name) {
                OperatorNameConventions.SET -> IrDynamicOperator.EQ
                OperatorNameConventions.GET -> IrDynamicOperator.ARRAY_ACCESS
                else -> error("Unexpected name")
            }

            is KtFakeSourceElementKind.DesugaredPrefixNameReference -> when (calleeReference.resolved?.name) {
                OperatorNameConventions.INC -> IrDynamicOperator.PREFIX_INCREMENT
                OperatorNameConventions.DEC -> IrDynamicOperator.PREFIX_DECREMENT
                else -> error("Unexpected name")
            }

            is KtFakeSourceElementKind.DesugaredPostfixNameReference -> when (calleeReference.resolved?.name) {
                OperatorNameConventions.INC -> IrDynamicOperator.POSTFIX_INCREMENT
                OperatorNameConventions.DEC -> IrDynamicOperator.POSTFIX_DECREMENT
                else -> error("Unexpected name")
            }

            else -> null
        }

    private fun convertToIrCallForDynamic(
        qualifiedAccess: FirQualifiedAccessExpression,
        explicitReceiverExpression: IrExpression,
        type: IrType,
        calleeReference: FirReference,
        symbol: FirBasedSymbol<*>,
        annotationMode: Boolean = false,
        dynamicOperator: IrDynamicOperator? = null,
        noArguments: Boolean = false,
    ): IrExpression {
        var convertedExplicitReceiver = explicitReceiverExpression

        return qualifiedAccess.convertWithOffsets { startOffset, endOffset ->
            when (symbol) {
                is FirFunctionSymbol<*> -> {
                    val name = calleeReference.resolved?.name
                        ?: error("Must have a name")
                    val operator = dynamicOperator
                        ?: name.dynamicOperator
                        ?: qualifiedAccess.dynamicOperator
                        ?: IrDynamicOperator.INVOKE
                    val theType = if (name == OperatorNameConventions.COMPARE_TO) {
                        typeConverter.irBuiltIns.booleanType
                    } else {
                        type
                    }
                    if (operator == IrDynamicOperator.INVOKE && qualifiedAccess !is FirImplicitInvokeCall) {
                        convertedExplicitReceiver = IrDynamicMemberExpressionImpl(
                            startOffset, endOffset, type, name.identifier, explicitReceiverExpression
                        )
                    }
                    IrDynamicOperatorExpressionImpl(startOffset, endOffset, theType, operator)
                }

                is FirPropertySymbol -> {
                    val name = calleeReference.resolved?.name ?: error("There must be a name")
                    IrDynamicMemberExpressionImpl(startOffset, endOffset, type, name.identifier, explicitReceiverExpression)
                }

                else -> generateErrorCallExpression(startOffset, endOffset, calleeReference, type)
            }
        }.applyTypeArguments(qualifiedAccess).applyReceivers(qualifiedAccess, convertedExplicitReceiver)
            .applyCallArguments((qualifiedAccess as? FirCall)?.takeIf { !noArguments }, annotationMode)
    }

    fun convertToIrCall(
        qualifiedAccess: FirQualifiedAccessExpression,
        typeRef: FirTypeRef,
        explicitReceiverExpression: IrExpression?,
        annotationMode: Boolean = false,
        dynamicOperator: IrDynamicOperator? = null,
        variableAsFunctionMode: Boolean = false,
        noArguments: Boolean = false
    ): IrExpression {
        try {
            val type = typeRef.toIrType()
            val samConstructorCall = qualifiedAccess.tryConvertToSamConstructorCall(type)
            if (samConstructorCall != null) return samConstructorCall

            val dispatchReceiver = qualifiedAccess.dispatchReceiver
            val calleeReference = qualifiedAccess.calleeReference

            val firSymbol = calleeReference.toResolvedBaseSymbol()
            val isDynamicAccess = firSymbol?.origin == FirDeclarationOrigin.DynamicScope

            if (isDynamicAccess) {
                return convertToIrCallForDynamic(
                    qualifiedAccess,
                    explicitReceiverExpression ?: error("Must've had a receiver"),
                    type,
                    calleeReference,
                    firSymbol ?: error("Must have had a symbol"),
                    annotationMode,
                    dynamicOperator,
                    noArguments,
                )
            }

            val symbol = calleeReference.toSymbolForCall(
                dispatchReceiver,
                conversionScope,
                explicitReceiver = qualifiedAccess.explicitReceiver
            )

            // We might have had a dynamic receiver, but resolved
            // into a non-fake member. For example, we can
            // resolve into members of `Any`.
            val convertedExplicitReceiver = if (explicitReceiverExpression?.type is IrDynamicType) {
                qualifiedAccess.convertWithOffsets { startOffset, endOffset ->
                    val callableDeclaration = firSymbol?.fir as? FirCallableDeclaration
                    val targetType = callableDeclaration?.dispatchReceiverType?.toIrType()
                        ?: callableDeclaration?.receiverParameter?.typeRef?.toIrType()
                        ?: error("Couldn't get the proper receiver")
                    IrTypeOperatorCallImpl(
                        startOffset, endOffset, targetType,
                        IrTypeOperator.IMPLICIT_DYNAMIC_CAST,
                        targetType, explicitReceiverExpression,
                    )
                }
            } else {
                explicitReceiverExpression
            }

            return qualifiedAccess.convertWithOffsets { startOffset, endOffset ->
                if (calleeReference is FirSuperReference) {
                    if (dispatchReceiver !is FirNoReceiverExpression) {
                        return@convertWithOffsets visitor.convertToIrExpression(dispatchReceiver)
                    }
                }
                when (symbol) {
                    is IrConstructorSymbol -> IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, type, symbol)
                    is IrSimpleFunctionSymbol -> {
                        IrCallImpl(
                            startOffset, endOffset, type, symbol,
                            typeArgumentsCount = symbol.owner.typeParameters.size,
                            valueArgumentsCount = symbol.owner.valueParameters.size,
                            origin = calleeReference.statementOrigin(),
                            superQualifierSymbol = dispatchReceiver.superQualifierSymbol()
                        )
                    }

                    is IrLocalDelegatedPropertySymbol -> {
                        IrCallImpl(
                            startOffset, endOffset, type, symbol.owner.getter.symbol,
                            typeArgumentsCount = symbol.owner.getter.typeParameters.size,
                            valueArgumentsCount = 0,
                            origin = IrStatementOrigin.GET_LOCAL_PROPERTY,
                            superQualifierSymbol = dispatchReceiver.superQualifierSymbol()
                        )
                    }

                    is IrPropertySymbol -> {
                        val getter = symbol.owner.getter
                        val backingField = symbol.owner.backingField
                        when {
                            getter != null -> IrCallImpl(
                                startOffset, endOffset, type, getter.symbol,
                                typeArgumentsCount = getter.typeParameters.size,
                                valueArgumentsCount = getter.valueParameters.size,
                                origin = IrStatementOrigin.GET_PROPERTY,
                                superQualifierSymbol = dispatchReceiver.superQualifierSymbol()
                            )

                            backingField != null -> IrGetFieldImpl(
                                startOffset, endOffset, backingField.symbol, type,
                                superQualifierSymbol = dispatchReceiver.superQualifierSymbol()
                            )

                            else -> IrErrorCallExpressionImpl(
                                startOffset, endOffset, type,
                                description = "No getter or backing field found for ${calleeReference.render()}"
                            )
                        }
                    }

                    is IrFieldSymbol -> if (annotationMode) {
                        val resolvedSymbol = calleeReference.toResolvedCallableSymbol() ?: error("should have resolvedSymbol")
                        val returnType = resolvedSymbol.resolvedReturnTypeRef.toIrType()
                        val firConstExpression = (resolvedSymbol.fir as FirVariable).initializer as? FirConstExpression<*>
                            ?: error("should be FirConstExpression")
                        firConstExpression.toIrConst(returnType)
                    } else {
                        IrGetFieldImpl(
                            startOffset, endOffset, symbol, type,
                            origin = IrStatementOrigin.GET_PROPERTY.takeIf { calleeReference !is FirDelegateFieldReference },
                            superQualifierSymbol = dispatchReceiver.superQualifierSymbol()
                        )
                    }

                    is IrValueSymbol -> {
                        IrGetValueImpl(
                            // Note: sometimes we change an IR type of local variable
                            // (see component call case: Fir2IrDeclarationStorage.createIrVariable -> val type = ...)
                            // That's why we should use here v the IR variable type and not FIR converted type (to prevent IR inconsistency)
                            startOffset, endOffset, symbol.owner.type, symbol,
                            origin = if (variableAsFunctionMode) IrStatementOrigin.VARIABLE_AS_FUNCTION
                            else calleeReference.statementOrigin()
                        )
                    }

                    is IrEnumEntrySymbol -> IrGetEnumValueImpl(startOffset, endOffset, type, symbol)
                    else -> generateErrorCallExpression(startOffset, endOffset, calleeReference, type)
                }
            }.applyTypeArguments(qualifiedAccess).applyReceivers(qualifiedAccess, convertedExplicitReceiver)
                .applyCallArguments(qualifiedAccess, annotationMode)
        } catch (e: Throwable) {
            throw IllegalStateException(
                "Error while translating ${qualifiedAccess.render()} " +
                        "from file ${conversionScope.containingFileIfAny()?.name ?: "???"} to BE IR", e
            )
        }
    }

    private fun convertToIrSetCallForDynamic(
        variableAssignment: FirVariableAssignment,
        receiverExpression: IrExpression,
        type: IrType,
        calleeReference: FirReference,
        symbol: FirBasedSymbol<*>,
        assignedValue: IrExpression,
    ): IrExpression {
        var convertedExplicitReceiver = receiverExpression

        return variableAssignment.convertWithOffsets { startOffset, endOffset ->
            when (symbol) {
                is FirPropertySymbol -> {
                    val name = calleeReference.resolved?.name ?: error("There must be a name")
                    convertedExplicitReceiver = IrDynamicMemberExpressionImpl(
                        startOffset, endOffset, type, name.identifier, receiverExpression
                    )
                    IrDynamicOperatorExpressionImpl(startOffset, endOffset, type, IrDynamicOperator.EQ).apply {
                        arguments.add(assignedValue)
                    }
                }

                else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
            }
        }.apply {
            variableAssignment.unwrapLValue()?.let { applyReceivers(it, convertedExplicitReceiver) }
        }
    }

    fun convertToIrSetCall(variableAssignment: FirVariableAssignment, explicitReceiverExpression: IrExpression?): IrExpression {
        try {
            val type = irBuiltIns.unitType
            val calleeReference = variableAssignment.calleeReference ?: error("Reference not resolvable")
            val assignedValue = visitor.convertToIrExpression(variableAssignment.rValue)

            val firSymbol = calleeReference.toResolvedBaseSymbol()
            val isDynamicAccess = firSymbol?.origin == FirDeclarationOrigin.DynamicScope

            if (isDynamicAccess) {
                val receiverExpression = (explicitReceiverExpression
                    ?: (variableAssignment.dispatchReceiver as? FirThisReceiverExpression)?.let(visitor::convertToIrExpression)
                    ?: error("Must've had a receiver"))

                return convertToIrSetCallForDynamic(
                    variableAssignment,
                    receiverExpression,
                    type,
                    calleeReference,
                    firSymbol ?: error("Must've had a symbol"),
                    assignedValue,
                )
            }

            val symbol = calleeReference.toSymbolForCall(
                variableAssignment.dispatchReceiver,
                conversionScope,
                explicitReceiver = variableAssignment.explicitReceiver,
                preferGetter = false,
            )
            val origin = variableAssignment.getIrAssignmentOrigin()

            val lValue = variableAssignment.unwrapLValue() ?: error("Assignment lValue unwrapped to null")
            return variableAssignment.convertWithOffsets(calleeReference) { startOffset, endOffset ->
                when (symbol) {
                    is IrFieldSymbol -> IrSetFieldImpl(startOffset, endOffset, symbol, type, origin).apply {
                        value = assignedValue
                    }

                    is IrLocalDelegatedPropertySymbol -> {
                        val setter = symbol.owner.setter
                        when {
                            setter != null -> IrCallImpl(
                                startOffset, endOffset, type, setter.symbol,
                                typeArgumentsCount = setter.typeParameters.size,
                                valueArgumentsCount = setter.valueParameters.size,
                                origin = origin,
                                superQualifierSymbol = variableAssignment.dispatchReceiver.superQualifierSymbol()
                            ).apply {
                                putContextReceiverArguments(lValue)
                                putValueArgument(0, assignedValue)
                            }

                            else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
                        }
                    }

                    is IrPropertySymbol -> {
                        val irProperty = symbol.owner
                        val setter = irProperty.setter
                        var backingField = irProperty.backingField

                        // If we found neither a setter nor a backing field, check if we have an override (possibly fake) of a val with
                        // backing field. This can happen in a class initializer where `this` was smart-casted. See KT-57105.
                        if (setter == null && backingField == null) {
                            backingField = irProperty.overriddenBackingFieldOrNull()
                        }

                        when {
                            setter != null -> IrCallImpl(
                                startOffset, endOffset, type, setter.symbol,
                                typeArgumentsCount = setter.typeParameters.size,
                                valueArgumentsCount = setter.valueParameters.size,
                                origin = origin,
                                superQualifierSymbol = variableAssignment.dispatchReceiver.superQualifierSymbol()
                            ).apply {
                                putValueArgument(putContextReceiverArguments(lValue), assignedValue)
                            }

                            backingField != null -> IrSetFieldImpl(
                                startOffset, endOffset, backingField.symbol, type,
                                origin = null, // NB: to be consistent with PSI2IR, origin should be null here
                                superQualifierSymbol = variableAssignment.dispatchReceiver.superQualifierSymbol()
                            ).apply {
                                value = assignedValue
                            }

                            else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
                        }
                    }

                    is IrSimpleFunctionSymbol -> {
                        IrCallImpl(
                            startOffset, endOffset, type, symbol,
                            typeArgumentsCount = symbol.owner.typeParameters.size,
                            valueArgumentsCount = 1,
                            origin = origin
                        ).apply {
                            putValueArgument(0, assignedValue)
                        }
                    }

                    is IrVariableSymbol -> {
                        IrSetValueImpl(startOffset, endOffset, type, symbol, assignedValue, origin)
                    }

                    else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
                }
            }.applyTypeArguments(lValue).applyReceivers(lValue, explicitReceiverExpression)
        } catch (e: Throwable) {
            throw IllegalStateException(
                "Error while translating ${variableAssignment.render()} " +
                        "from file ${conversionScope.containingFileIfAny()?.name ?: "???"} to BE IR", e
            )
        }
    }

    private fun IrProperty.overriddenBackingFieldOrNull(): IrField? {
        return overriddenSymbols.firstNotNullOfOrNull {
            val owner = it.owner
            owner.backingField ?: owner.overriddenBackingFieldOrNull()
        }
    }

    fun convertToIrConstructorCall(annotation: FirAnnotation): IrExpression {
        val coneType = annotation.annotationTypeRef.coneTypeSafe<ConeLookupTagBasedType>()
            ?.fullyExpandedType(session) as? ConeLookupTagBasedType
        val type = coneType?.toIrType()
        val symbol = type?.classifierOrNull
        return annotation.convertWithOffsets { startOffset, endOffset ->
            when (symbol) {
                is IrClassSymbol -> {
                    val irClass = symbol.owner
                    val irConstructor = (annotation.toResolvedCallableSymbol() as? FirConstructorSymbol)?.let {
                        this.declarationStorage.getIrConstructorSymbol(it)
                    } ?: run {
                        // Fallback for FirReferencePlaceholderForResolvedAnnotations from jar
                        val fir = coneType.lookupTag.toSymbol(session)?.fir as? FirClass
                        var constructorSymbol: FirConstructorSymbol? = null
                        fir?.unsubstitutedScope(
                            session,
                            scopeSession,
                            withForcedTypeCalculator = true,
                            memberRequiredPhase = null,
                        )?.processDeclaredConstructors {
                            if (it.fir.isPrimary && constructorSymbol == null) {
                                constructorSymbol = it
                            }
                        }

                        constructorSymbol?.let {
                            this.declarationStorage.getIrConstructorSymbol(it)
                        }
                    }
                    if (irConstructor == null) {
                        IrErrorCallExpressionImpl(startOffset, endOffset, type, "No annotation constructor found: ${irClass.name}")
                    } else {
                        IrConstructorCallImpl(
                            startOffset, endOffset, type, irConstructor,
                            valueArgumentsCount = irConstructor.owner.valueParameters.size,
                            typeArgumentsCount = 0,
                            constructorTypeArgumentsCount = 0
                        )
                    }

                }

                else -> {
                    IrErrorCallExpressionImpl(
                        startOffset,
                        endOffset,
                        type ?: createErrorType(),
                        "Unresolved reference: ${annotation.render()}"
                    )
                }
            }
        }.applyCallArguments(annotation.toAnnotationCall(), annotationMode = true)
    }

    private fun FirAnnotation.toAnnotationCall(): FirAnnotationCall? {
        if (this is FirAnnotationCall) return this
        return buildAnnotationCall {
            useSiteTarget = this@toAnnotationCall.useSiteTarget
            annotationTypeRef = this@toAnnotationCall.annotationTypeRef
            val symbol = annotationTypeRef.coneType.fullyExpandedType(session).toSymbol(session) as? FirRegularClassSymbol ?: return null

            val constructorSymbol =
                symbol.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false, memberRequiredPhase = null)
                    .getDeclaredConstructors().firstOrNull() ?: return null

            val argumentToParameterToMapping = constructorSymbol.valueParameterSymbols.mapNotNull {
                val parameter = it.fir
                val argument = this@toAnnotationCall.argumentMapping.mapping[parameter.name] ?: return@mapNotNull null
                argument to parameter
            }.toMap(LinkedHashMap())
            argumentList = buildResolvedArgumentList(argumentToParameterToMapping)
            calleeReference = buildResolvedNamedReference {
                name = symbol.classId.shortClassName
                resolvedSymbol = constructorSymbol
            }
        }
    }

    internal fun convertToGetObject(qualifier: FirResolvedQualifier): IrExpression {
        return convertToGetObject(qualifier, null)!!
    }

    internal fun convertToGetObject(
        qualifier: FirResolvedQualifier,
        callableReferenceAccess: FirCallableReferenceAccess?
    ): IrExpression? {
        val classSymbol = (qualifier.typeRef.coneType as? ConeClassLikeType)?.lookupTag?.toSymbol(session)

        if (callableReferenceAccess?.isBound == false) {
            return null
        }

        val irType = qualifier.typeRef.toIrType()
        return qualifier.convertWithOffsets { startOffset, endOffset ->
            if (classSymbol != null) {
                IrGetObjectValueImpl(
                    startOffset, endOffset, irType,
                    classSymbol.toSymbol() as IrClassSymbol
                )
            } else {
                IrErrorCallExpressionImpl(
                    startOffset, endOffset, irType,
                    "Resolved qualifier ${qualifier.render()} does not have correctly resolved type"
                )
            }
        }
    }

    private fun FirFunctionCall.buildSubstitutorByCalledFunction(function: FirFunction?): ConeSubstitutor? {
        if (function == null) return null
        val map = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
        for ((index, typeParameter) in function.typeParameters.withIndex()) {
            val typeProjection = typeArguments.getOrNull(index) as? FirTypeProjectionWithVariance ?: continue
            map[typeParameter.symbol] = typeProjection.typeRef.coneType
        }
        return ConeSubstitutorByMap(map, session)
    }

    private fun extractArgumentsMapping(
        call: FirCall
    ): Triple<List<FirValueParameter>?, Map<FirExpression, FirValueParameter>?, ConeSubstitutor> {
        val calleeReference = when (call) {
            is FirFunctionCall -> call.calleeReference
            is FirDelegatedConstructorCall -> call.calleeReference
            is FirAnnotationCall -> call.calleeReference
            else -> null
        }
        val function = ((calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirFunctionSymbol<*>)?.fir
        val valueParameters = function?.valueParameters
        val argumentMapping = call.resolvedArgumentMapping
        val substitutor = (call as? FirFunctionCall)?.buildSubstitutorByCalledFunction(function) ?: ConeSubstitutor.Empty
        return Triple(valueParameters, argumentMapping, substitutor)
    }

    internal fun IrExpression.applyCallArguments(
        statement: FirStatement?,
        annotationMode: Boolean
    ): IrExpression {
        val call = statement as? FirCall
        return when (this) {
            is IrMemberAccessExpression<*> -> {
                val contextReceiverCount = putContextReceiverArguments(statement)
                if (call == null) return this
                val argumentsCount = call.arguments.size
                if (argumentsCount <= valueArgumentsCount) {
                    apply {
                        val (valueParameters, argumentMapping, substitutor) = extractArgumentsMapping(call)
                        if (argumentMapping != null && (annotationMode || argumentMapping.isNotEmpty())) {
                            if (valueParameters != null) {
                                return applyArgumentsWithReorderingIfNeeded(
                                    argumentMapping, valueParameters, substitutor, annotationMode,
                                    contextReceiverCount,
                                )
                            }
                        }
                        // Case without argument mapping (deserialized annotation)
                        // TODO: support argument mapping in deserialized annotations and remove me
                        for ((index, argument) in call.arguments.withIndex()) {
                            val valueParameter = when (argument) {
                                is FirNamedArgumentExpression -> valueParameters?.find { it.name == argument.name }
                                else -> null
                            } ?: valueParameters?.get(index)
                            val argumentExpression = convertArgument(argument, valueParameter, substitutor)
                            putValueArgument(
                                (valueParameters?.indexOf(valueParameter)?.takeIf { it >= 0 } ?: index) + contextReceiverCount,
                                argumentExpression
                            )
                        }
                    }
                } else {
                    val name = if (this is IrCallImpl) symbol.owner.name else "???"
                    IrErrorCallExpressionImpl(
                        startOffset, endOffset, type,
                        "Cannot bind $argumentsCount arguments to $name call with $valueArgumentsCount parameters"
                    ).apply {
                        for (argument in call.arguments) {
                            addArgument(visitor.convertToIrExpression(argument))
                        }
                    }
                }
            }

            is IrDynamicOperatorExpression -> apply {
                if (call == null) return@apply
                val (valueParameters, argumentMapping, substitutor) = extractArgumentsMapping(call)
                if (argumentMapping != null && (annotationMode || argumentMapping.isNotEmpty())) {
                    if (valueParameters != null) {
                        val dynamicCallVarargArgument = argumentMapping.keys.firstOrNull() as? FirVarargArgumentsExpression
                            ?: error("Dynamic call must have a single vararg argument")
                        for (argument in dynamicCallVarargArgument.arguments) {
                            val irArgument = convertArgument(argument, null, substitutor, annotationMode)
                            arguments.add(irArgument)
                        }
                    }
                }
            }

            is IrErrorCallExpressionImpl -> apply {
                for (argument in call?.arguments.orEmpty()) {
                    addArgument(visitor.convertToIrExpression(argument))
                }
            }

            else -> this
        }
    }

    private fun IrMemberAccessExpression<*>.putContextReceiverArguments(statement: FirStatement?): Int {
        if (statement !is FirContextReceiverArgumentListOwner) return 0

        val contextReceiverCount = statement.contextReceiverArguments.size
        if (contextReceiverCount > 0) {
            for (index in 0 until contextReceiverCount) {
                putValueArgument(
                    index,
                    visitor.convertToIrExpression(statement.contextReceiverArguments[index]),
                )
            }
        }

        return contextReceiverCount
    }

    private fun IrMemberAccessExpression<*>.applyArgumentsWithReorderingIfNeeded(
        argumentMapping: Map<FirExpression, FirValueParameter>,
        valueParameters: List<FirValueParameter>,
        substitutor: ConeSubstitutor,
        annotationMode: Boolean,
        contextReceiverCount: Int,
    ): IrExpression {
        val converted = argumentMapping.entries.map { (argument, parameter) ->
            parameter to convertArgument(argument, parameter, substitutor, annotationMode)
        }
        // If none of the parameters have side effects, the evaluation order doesn't matter anyway.
        // For annotations, this is always true, since arguments have to be compile-time constants.
        if (!annotationMode && !converted.all { (_, irArgument) -> irArgument.hasNoSideEffects() } &&
            needArgumentReordering(argumentMapping.values, valueParameters)
        ) {
            return IrBlockImpl(startOffset, endOffset, type, IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL).apply {
                fun IrExpression.freeze(nameHint: String): IrExpression {
                    if (isUnchanging()) return this
                    val (variable, symbol) = createTemporaryVariable(this, conversionScope, nameHint)
                    statements.add(variable)
                    return IrGetValueImpl(startOffset, endOffset, symbol, null)
                }

                dispatchReceiver = dispatchReceiver?.freeze("\$this")
                extensionReceiver = extensionReceiver?.freeze("\$receiver")
                for ((parameter, irArgument) in converted) {
                    putValueArgument(
                        valueParameters.indexOf(parameter) + contextReceiverCount,
                        irArgument.freeze(parameter.name.asString())
                    )
                }
                statements.add(this@applyArgumentsWithReorderingIfNeeded)
            }
        } else {
            for ((parameter, irArgument) in converted) {
                putValueArgument(valueParameters.indexOf(parameter) + contextReceiverCount, irArgument)
            }
            if (annotationMode) {
                for ((index, parameter) in valueParameters.withIndex()) {
                    if (parameter.isVararg && !argumentMapping.containsValue(parameter)) {
                        val defaultValue = parameter.defaultValue
                        val value = if (defaultValue != null) {
                            convertArgument(defaultValue, parameter, ConeSubstitutor.Empty, annotationMode = true)
                        } else {
                            val elementType = parameter.returnTypeRef.toIrType()
                            IrVarargImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                elementType,
                                elementType.toArrayOrPrimitiveArrayType(irBuiltIns)
                            )
                        }
                        putValueArgument(index, value)
                    }
                }
            }
            return this
        }
    }

    private fun needArgumentReordering(
        parametersInActualOrder: Collection<FirValueParameter>,
        valueParameters: List<FirValueParameter>
    ): Boolean {
        var lastValueParameterIndex = UNDEFINED_PARAMETER_INDEX
        for (parameter in parametersInActualOrder) {
            val index = valueParameters.indexOf(parameter)
            if (index < lastValueParameterIndex) {
                return true
            }
            lastValueParameterIndex = index
        }
        return false
    }

    private fun convertArgument(
        argument: FirExpression,
        parameter: FirValueParameter?,
        substitutor: ConeSubstitutor,
        annotationMode: Boolean = false
    ): IrExpression {
        var irArgument = visitor.convertToIrExpression(argument, annotationMode)
        if (parameter != null) {
            with(visitor.implicitCastInserter) {
                irArgument = irArgument.cast(argument, argument.typeRef, parameter.returnTypeRef)
            }
        }
        with(adapterGenerator) {
            if (parameter?.returnTypeRef is FirResolvedTypeRef) {
                // Java type case (from annotations)
                val parameterType = parameter.returnTypeRef.coneType
                val unwrappedParameterType = if (parameter.isVararg) parameterType.arrayElementType()!! else parameterType
                val samFunctionType = getFunctionTypeForPossibleSamType(unwrappedParameterType)
                irArgument = irArgument.applySuspendConversionIfNeeded(argument, samFunctionType ?: unwrappedParameterType)
                irArgument = irArgument.applySamConversionIfNeeded(argument, parameter, substitutor)
            }
        }
        return irArgument
            .applyAssigningArrayElementsToVarargInNamedForm(argument, parameter)
            .applyImplicitIntegerCoercionIfNeeded(argument, parameter)
    }

    private fun IrExpression.applyAssigningArrayElementsToVarargInNamedForm(
        argument: FirExpression,
        parameter: FirValueParameter?
    ): IrExpression {
        // TODO: Need to refer to language feature: AllowAssigningArrayElementsToVarargsInNamedFormForFunctions
        if (this !is IrVarargImpl ||
            parameter?.isVararg != true ||
            argument !is FirVarargArgumentsExpression ||
            argument.arguments.none { it is FirNamedArgumentExpression }
        ) {
            return this
        }
        elements.forEachIndexed { i, irVarargElement ->
            if (irVarargElement !is IrSpreadElement &&
                argument.arguments[i] is FirNamedArgumentExpression &&
                irVarargElement is IrExpression &&
                irVarargElement.type.isArray()
            ) {
                elements[i] = IrSpreadElementImpl(irVarargElement.startOffset, irVarargElement.endOffset, irVarargElement)
            }
        }
        return this
    }

    private fun IrExpression.applyImplicitIntegerCoercionIfNeeded(
        argument: FirExpression,
        parameter: FirValueParameter?
    ): IrExpression {
        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.ImplicitSignedToUnsignedIntegerConversion)) return this

        if (parameter == null || !parameter.isMarkedWithImplicitIntegerCoercion) return this

        fun IrExpression.applyToElement(argument: FirExpression, conversionFunction: IrSimpleFunctionSymbol): IrExpression =
            if (argument is FirConstExpression<*> ||
                argument.calleeReference?.toResolvedCallableSymbol()?.let {
                    it.resolvedStatus.isConst && it.isMarkedWithImplicitIntegerCoercion
                } == true
            ) {
                IrCallImpl(
                    startOffset, endOffset,
                    conversionFunction.owner.returnType,
                    conversionFunction,
                    typeArgumentsCount = 0,
                    valueArgumentsCount = 0
                ).apply {
                    extensionReceiver = this@applyToElement
                }
            } else this@applyToElement

        if (parameter.isMarkedWithImplicitIntegerCoercion) {
            if (this is IrVarargImpl && argument is FirVarargArgumentsExpression) {

                val targetTypeFqName = varargElementType.classFqName ?: return this
                val conversionFunctions = irBuiltIns.getNonBuiltInFunctionsByExtensionReceiver(
                    Name.identifier("to" + targetTypeFqName.shortName().asString()),
                    StandardNames.BUILT_INS_PACKAGE_NAME.asString()
                )
                if (conversionFunctions.isNotEmpty()) {
                    elements.forEachIndexed { i, irVarargElement ->
                        val targetFun = argument.arguments[i].typeRef.toIrType().classifierOrNull?.let { conversionFunctions[it] }
                        if (targetFun != null && irVarargElement is IrExpression) {
                            elements[i] =
                                irVarargElement.applyToElement(argument.arguments[i], targetFun)
                        }
                    }
                }
                return this
            } else {
                val targetIrType = parameter.returnTypeRef.toIrType()
                val targetTypeFqName = targetIrType.classFqName ?: return this
                val conversionFunctions = irBuiltIns.getNonBuiltInFunctionsByExtensionReceiver(
                    Name.identifier("to" + targetTypeFqName.shortName().asString()),
                    StandardNames.BUILT_INS_PACKAGE_NAME.asString()
                )
                val sourceTypeClassifier = argument.typeRef.toIrType().classifierOrNull ?: return this

                val conversionFunction = conversionFunctions[sourceTypeClassifier] ?: return this

                return this.applyToElement(argument, conversionFunction)
            }
        }
        return this
    }

    internal fun IrExpression.applyTypeArguments(access: FirQualifiedAccessExpression): IrExpression {
        if (this !is IrMemberAccessExpression<*>) return this
        val argumentsCount = access.typeArguments.size
        if (argumentsCount <= typeArgumentsCount) {
            for ((index, argument) in access.typeArguments.withIndex()) {
                val typeParameter = access.findTypeParameter(index)
                val argumentFirType = (argument as FirTypeProjectionWithVariance).typeRef
                val argumentIrType = if (typeParameter?.isReified == true) {
                    argumentFirType.approximateDeclarationType(
                        session,
                        containingCallableVisibility = null,
                        isLocal = false,
                        stripEnhancedNullability = false
                    ).toIrType()
                } else {
                    argumentFirType.toIrType()
                }
                putTypeArgument(index, argumentIrType)
            }
            return this
        } else {
            val name = if (this is IrCallImpl) symbol.owner.name else "???"
            return IrErrorExpressionImpl(
                startOffset, endOffset, type,
                "Cannot bind $argumentsCount type arguments to $name call with $typeArgumentsCount type parameters"
            )
        }
    }

    private fun FirQualifiedAccessExpression.findTypeParameter(index: Int): FirTypeParameter? =
        ((calleeReference as? FirResolvedNamedReference)?.resolvedSymbol?.fir as? FirTypeParametersOwner)?.typeParameters?.get(index)

    private fun FirQualifiedAccessExpression.findIrDispatchReceiver(explicitReceiverExpression: IrExpression?): IrExpression? =
        findIrReceiver(explicitReceiverExpression, isDispatch = true)

    private fun FirQualifiedAccessExpression.findIrExtensionReceiver(explicitReceiverExpression: IrExpression?): IrExpression? =
        findIrReceiver(explicitReceiverExpression, isDispatch = false)

    internal fun FirQualifiedAccessExpression.findIrReceiver(
        explicitReceiverExpression: IrExpression?,
        isDispatch: Boolean,
    ): IrExpression? {
        val firReceiver = if (isDispatch) dispatchReceiver else extensionReceiver
        if (firReceiver == explicitReceiver) {
            return explicitReceiverExpression
        }

        return firReceiver.takeIf { it !is FirNoReceiverExpression }
            ?.let { visitor.convertToIrReceiverExpression(it, calleeReference, this as? FirCallableReferenceAccess) }
            ?: explicitReceiverExpression
            ?: run {
                if (this is FirCallableReferenceAccess) return null
                val name = if (isDispatch) "Dispatch" else "Extension"
                error("$name receiver expected: ${render()} to ${calleeReference.render()}")
            }
    }

    private fun IrExpression.applyReceivers(
        qualifiedAccess: FirQualifiedAccessExpression,
        explicitReceiverExpression: IrExpression?,
    ): IrExpression {
        when (this) {
            is IrMemberAccessExpression<*> -> {
                val ownerFunction =
                    symbol.owner as? IrFunction
                        ?: (symbol.owner as? IrProperty)?.getter
                if (ownerFunction?.dispatchReceiverParameter != null) {
                    val baseDispatchReceiver = qualifiedAccess.findIrDispatchReceiver(explicitReceiverExpression)
                    dispatchReceiver =
                        if (!ownerFunction.isMethodOfAny() || baseDispatchReceiver?.type?.classOrNull?.owner?.isInterface != true) {
                            baseDispatchReceiver
                        } else {
                            // NB: for FE 1.0, this type cast is added by InterfaceObjectCallsLowering
                            // However, it doesn't work for FIR due to different f/o structure
                            // (FIR calls Any method directly, but FE 1.0 calls its interface f/o instead)
                            IrTypeOperatorCallImpl(
                                baseDispatchReceiver.startOffset,
                                baseDispatchReceiver.endOffset,
                                irBuiltIns.anyType,
                                IrTypeOperator.IMPLICIT_CAST,
                                irBuiltIns.anyType,
                                baseDispatchReceiver
                            )
                        }
                }
                if (ownerFunction?.extensionReceiverParameter != null) {
                    extensionReceiver = qualifiedAccess.findIrExtensionReceiver(explicitReceiverExpression)?.let {
                        val symbol = qualifiedAccess.calleeReference.toResolvedCallableSymbol()
                            ?: error("Symbol for call ${qualifiedAccess.render()} not found")
                        symbol.fir.receiverParameter?.typeRef?.let { receiverType ->
                            with(visitor.implicitCastInserter) {
                                it.cast(
                                    qualifiedAccess.extensionReceiver,
                                    qualifiedAccess.extensionReceiver.typeRef,
                                    receiverType
                                )
                            }
                        } ?: it
                    }
                }
            }

            is IrFieldAccessExpression -> {
                val ownerField = symbol.owner
                if (!ownerField.isStatic) {
                    receiver = qualifiedAccess.findIrDispatchReceiver(explicitReceiverExpression)
                }
            }

            is IrDynamicOperatorExpression -> {
                receiver = explicitReceiverExpression ?: error("No receiver for dynamic")
            }
        }
        return this
    }

    private fun generateErrorCallExpression(
        startOffset: Int,
        endOffset: Int,
        calleeReference: FirReference,
        type: IrType? = null
    ): IrErrorCallExpression {
        return IrErrorCallExpressionImpl(
            startOffset, endOffset, type ?: createErrorType(),
            "Unresolved reference: ${calleeReference.render()}"
        )
    }
}

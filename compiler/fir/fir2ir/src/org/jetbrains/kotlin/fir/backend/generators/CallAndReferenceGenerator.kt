/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import com.intellij.openapi.progress.ProcessCanceledException
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.isMethodOfAny
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.FirSimpleSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.calls.getExpectedType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.approximateDeclarationType
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.impl.originalConstructorIfTypeAlias
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasForConstructor
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.isUnsignedArray
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator.commonSuperType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class CallAndReferenceGenerator(
    private val components: Fir2IrComponents,
    private val visitor: Fir2IrVisitor,
    private val conversionScope: Fir2IrConversionScope,
) : Fir2IrComponents by components {

    private val adapterGenerator = AdapterGenerator(components, conversionScope)

    private fun FirTypeRef.toIrType(): IrType =
        with(typeConverter) { toIrType(conversionScope.defaultConversionTypeOrigin()) }

    private fun ConeKotlinType.toIrType(): IrType =
        with(typeConverter) { toIrType(conversionScope.defaultConversionTypeOrigin()) }

    fun convertToIrCallableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        explicitReceiverExpression: IrExpression?,
        isDelegate: Boolean,
    ): IrExpression {
        val type = approximateFunctionReferenceType(callableReferenceAccess.resolvedType).toIrType()

        val firSymbol = callableReferenceAccess.calleeReference.extractSymbolForCall()
        if (firSymbol?.origin == FirDeclarationOrigin.SamConstructor) {
            assert(explicitReceiverExpression == null) {
                "Fun interface constructor reference should be unbound: ${explicitReceiverExpression?.dump()}"
            }
            return adapterGenerator.generateFunInterfaceConstructorReference(
                callableReferenceAccess,
                firSymbol as FirSyntheticFunctionSymbol,
                type
            )
        }

        // val x by y ->
        //   val `x$delegate` = y
        //   val x get() = `x$delegate`.getValue(this, ::x)
        // The reference here (like the rest of the accessor) has DefaultAccessor source kind.
        val isForDelegate = callableReferenceAccess.source?.kind == KtFakeSourceElementKind.DelegatedPropertyAccessor
        val origin = if (isForDelegate) IrStatementOrigin.PROPERTY_REFERENCE_FOR_DELEGATE else null
        return callableReferenceAccess.convertWithOffsets { startOffset, endOffset ->

            fun FirCallableSymbol<*>.toSymbolForCall(): IrSymbol? {
                return toSymbolForCall(
                    callableReferenceAccess.dispatchReceiver,
                    explicitReceiver = callableReferenceAccess.explicitReceiver,
                    isDelegate = isDelegate,
                    isReference = true
                )
            }

            fun convertReferenceToRegularProperty(propertySymbol: FirPropertySymbol): IrExpression? {
                val irPropertySymbol = propertySymbol.toSymbolForCall() as? IrPropertySymbol ?: return null
                val referencedPropertyGetterSymbol = declarationStorage.findGetterOfProperty(irPropertySymbol)
                val referencedPropertySetterSymbol = runIf(callableReferenceAccess.resolvedType.isKMutableProperty(session)) {
                    declarationStorage.findSetterOfProperty(irPropertySymbol)
                }
                val backingFieldSymbol = when {
                    referencedPropertyGetterSymbol != null -> null
                    else -> declarationStorage.findBackingFieldOfProperty(irPropertySymbol)
                }
                return IrPropertyReferenceImpl(
                    startOffset, endOffset, type, irPropertySymbol,
                    typeArgumentsCount = callableReferenceAccess.toResolvedCallableSymbol()?.fir?.typeParameters?.size ?: 0,
                    field = backingFieldSymbol,
                    getter = referencedPropertyGetterSymbol,
                    setter = referencedPropertySetterSymbol,
                    origin = origin
                ).applyTypeArguments(callableReferenceAccess).applyReceivers(callableReferenceAccess, explicitReceiverExpression)
            }

            fun convertReferenceToSyntheticProperty(propertySymbol: FirSimpleSyntheticPropertySymbol): IrExpression? {
                val irPropertySymbol = callablesGenerator.generateIrPropertyForSyntheticPropertyReference(
                    propertySymbol,
                    conversionScope.parentFromStack()
                ).symbol
                val property = propertySymbol.syntheticProperty
                val referencedPropertyGetterSymbol = declarationStorage.getIrFunctionSymbol(property.getter.delegate.unwrapUseSiteSubstitutionOverrides().symbol) as? IrSimpleFunctionSymbol ?: return null
                val referencedPropertySetterSymbol = runIf(callableReferenceAccess.resolvedType.isKMutableProperty(session)) {
                    property.setter?.delegate?.unwrapUseSiteSubstitutionOverrides()?.symbol?.let {
                        declarationStorage.getIrFunctionSymbol(it) as? IrSimpleFunctionSymbol? ?: return null
                    }
                }
                return IrPropertyReferenceImpl(
                    startOffset, endOffset, type, irPropertySymbol,
                    typeArgumentsCount = callableReferenceAccess.toResolvedCallableSymbol()?.fir?.typeParameters?.size ?: 0,
                    field = null,
                    getter = referencedPropertyGetterSymbol,
                    setter = referencedPropertySetterSymbol,
                    origin = origin
                ).applyTypeArguments(callableReferenceAccess).applyReceivers(callableReferenceAccess, explicitReceiverExpression)
            }

            fun convertReferenceToLocalDelegatedProperty(propertySymbol: FirPropertySymbol): IrExpression? {
                val irPropertySymbol = propertySymbol.toSymbolForCall() as? IrLocalDelegatedPropertySymbol ?: return null

                return IrLocalDelegatedPropertyReferenceImpl(
                    startOffset, endOffset, type, irPropertySymbol,
                    delegate = declarationStorage.findDelegateVariableOfProperty(irPropertySymbol),
                    getter = declarationStorage.findGetterOfProperty(irPropertySymbol),
                    setter = declarationStorage.findSetterOfProperty(irPropertySymbol),
                    origin = origin
                )
            }

            fun convertReferenceToField(fieldSymbol: FirFieldSymbol): IrExpression? {
                val irFieldSymbol = fieldSymbol.toSymbolForCall() as? IrFieldSymbol ?: return null

                val field = fieldSymbol.fir
                val propertySymbol = declarationStorage.findPropertyForBackingField(irFieldSymbol)
                    ?: run {
                        // In case of [IrField] without the corresponding property, we've created it directly from [FirField].
                        // Since it's used as a field reference, we need a bogus property as a placeholder.
                        @OptIn(UnsafeDuringIrConstructionAPI::class)
                        declarationStorage.getOrCreateIrPropertyByPureField(fieldSymbol.fir, irFieldSymbol.owner.parent).symbol
                    }
                return IrPropertyReferenceImpl(
                    startOffset, endOffset, type,
                    propertySymbol,
                    typeArgumentsCount = 0,
                    field = irFieldSymbol,
                    getter = runIf(!field.isStatic) { declarationStorage.findGetterOfProperty(propertySymbol) },
                    setter = runIf(!field.isStatic) { declarationStorage.findSetterOfProperty(propertySymbol) },
                    origin
                ).applyReceivers(callableReferenceAccess, explicitReceiverExpression)
            }

            fun convertReferenceToFunction(functionSymbol: FirFunctionSymbol<*>): IrExpression? {
                val irFunctionSymbol = functionSymbol.toSymbolForCall() as? IrFunctionSymbol ?: return null

                require(type is IrSimpleType)
                var function = callableReferenceAccess.calleeReference.toResolvedFunctionSymbol()!!.fir
                if (function is FirConstructor) {
                    // The number of type parameters of typealias constructor may mismatch with that number in the original constructor.
                    // And for IR, we need to use the original constructor as a source of truth
                    function = function.originalConstructorIfTypeAlias ?: function
                }
                return if (adapterGenerator.needToGenerateAdaptedCallableReference(callableReferenceAccess, type, function)) {
                    // Receivers are being applied inside
                    with(adapterGenerator) {
                        // TODO: Figure out why `adaptedType` is different from the `type`?
                        val adaptedType = callableReferenceAccess.resolvedType.toIrType() as IrSimpleType
                        generateAdaptedCallableReference(callableReferenceAccess, explicitReceiverExpression, irFunctionSymbol, adaptedType)
                    }
                } else {
                    IrFunctionReferenceImpl(
                        startOffset, endOffset, type, irFunctionSymbol,
                        typeArgumentsCount = function.typeParameters.size,
                        valueArgumentsCount = function.valueParameters.size + function.contextReceivers.size,
                        reflectionTarget = irFunctionSymbol
                    ).applyTypeArguments(callableReferenceAccess)
                        .applyReceivers(callableReferenceAccess, explicitReceiverExpression)
                }
            }

            when (firSymbol) {
                is FirSimpleSyntheticPropertySymbol -> convertReferenceToSyntheticProperty(firSymbol)
                is FirPropertySymbol -> when {
                    firSymbol.isLocal -> when {
                        firSymbol.hasDelegate -> convertReferenceToLocalDelegatedProperty(firSymbol)
                        else -> null
                    }
                    else -> convertReferenceToRegularProperty(firSymbol)
                }
                is FirFunctionSymbol<*> -> convertReferenceToFunction(firSymbol)
                is FirFieldSymbol -> convertReferenceToField(firSymbol)
                else -> null
            } ?: IrErrorCallExpressionImpl(
                startOffset, endOffset, type, "Unsupported callable reference: ${callableReferenceAccess.render()}"
            )
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
            return classifierStorage.getOrCreateIrClass(firClassSymbol).symbol
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

            is KtFakeSourceElementKind.DesugaredPrefixInc -> IrDynamicOperator.PREFIX_INCREMENT
            is KtFakeSourceElementKind.DesugaredPrefixDec -> IrDynamicOperator.PREFIX_DECREMENT
            is KtFakeSourceElementKind.DesugaredPostfixInc -> IrDynamicOperator.POSTFIX_INCREMENT
            is KtFakeSourceElementKind.DesugaredPostfixDec -> IrDynamicOperator.POSTFIX_DECREMENT

            is KtFakeSourceElementKind.DesugaredArrayAugmentedAssign -> when (calleeReference.resolved?.name) {
                OperatorNameConventions.SET -> IrDynamicOperator.EQ
                OperatorNameConventions.GET -> IrDynamicOperator.ARRAY_ACCESS
                else -> error("Unexpected name")
            }

            else -> null
        }

    private fun convertToIrCallForDynamic(
        qualifiedAccess: FirQualifiedAccessExpression,
        explicitReceiverExpression: IrExpression?,
        type: IrType,
        calleeReference: FirReference,
        symbol: FirBasedSymbol<*>,
        dynamicOperator: IrDynamicOperator? = null,
        noArguments: Boolean = false,
    ): IrExpression {
        val selectedReceiver = qualifiedAccess.findIrDynamicReceiver(explicitReceiverExpression)

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
                    IrDynamicOperatorExpressionImpl(startOffset, endOffset, theType, operator).apply {
                        receiver = if (operator == IrDynamicOperator.INVOKE && qualifiedAccess !is FirImplicitInvokeCall) {
                            IrDynamicMemberExpressionImpl(startOffset, endOffset, type, name.identifier, selectedReceiver)
                        } else {
                            selectedReceiver
                        }
                    }
                }

                is FirPropertySymbol -> {
                    val name = calleeReference.resolved?.name ?: error("There must be a name")
                    IrDynamicMemberExpressionImpl(startOffset, endOffset, type, name.identifier, selectedReceiver)
                }

                else -> generateErrorCallExpression(startOffset, endOffset, calleeReference, type)
            }
        }
            .applyTypeArguments(qualifiedAccess)
            .applyCallArguments((qualifiedAccess as? FirCall)?.takeIf { !noArguments })
    }

    internal fun injectGetValueCall(element: FirElement, calleeReference: FirReference): IrExpression? {
        val injectedValue = findInjectedValue(calleeReference)
        if (injectedValue != null) {
            return element.convertWithOffsets { startOffset, endOffset ->
                useInjectedValue(injectedValue, calleeReference, startOffset, endOffset)
            }
        }

        return null
    }

    internal fun useInjectedValue(
        injectedValue: InjectedValue,
        calleeReference: FirReference,
        startOffset: Int,
        endOffset: Int,
    ): IrGetValueImpl {
        val type = injectedValue.typeRef.toIrType()
        val origin = calleeReference.statementOrigin()
        return IrGetValueImpl(startOffset, endOffset, type, injectedValue.irParameterSymbol, origin)
    }

    fun convertToIrCall(
        qualifiedAccess: FirQualifiedAccessExpression,
        type: ConeKotlinType,
        explicitReceiverExpression: IrExpression?,
        dynamicOperator: IrDynamicOperator? = null,
        variableAsFunctionMode: Boolean = false,
        noArguments: Boolean = false,
    ): IrExpression {
        try {
            injectGetValueCall(qualifiedAccess, qualifiedAccess.calleeReference)?.let { return it }

            val irType = type.toIrType()
            val samConstructorCall = qualifiedAccess.tryConvertToSamConstructorCall(irType)
            if (samConstructorCall != null) return samConstructorCall

            val dispatchReceiver = qualifiedAccess.dispatchReceiver
            val calleeReference = qualifiedAccess.calleeReference

            val firSymbol = calleeReference.toResolvedBaseSymbol()
            val isDynamicAccess = firSymbol?.origin == FirDeclarationOrigin.DynamicScope

            if (isDynamicAccess) {
                return convertToIrCallForDynamic(
                    qualifiedAccess,
                    explicitReceiverExpression,
                    irType,
                    calleeReference,
                    firSymbol ?: error("Must have had a symbol"),
                    dynamicOperator,
                    noArguments,
                )
            }

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
                    if (dispatchReceiver != null) {
                        return@convertWithOffsets visitor.convertToIrExpression(dispatchReceiver)
                    }
                }
                val symbol = calleeReference.toSymbolForCall(
                    dispatchReceiver,
                    explicitReceiver = qualifiedAccess.explicitReceiver
                )
                when (symbol) {
                    is IrConstructorSymbol -> {
                        require(firSymbol is FirConstructorSymbol)
                        val constructor = firSymbol.unwrapCallRepresentative().fir as FirConstructor
                        val totalTypeParametersCount = constructor.typeParameters.size
                        val constructorTypeParametersCount = constructor.typeParameters.count { it is FirTypeParameter }
                        IrConstructorCallImpl(
                            startOffset,
                            endOffset,
                            irType,
                            symbol,
                            typeArgumentsCount = totalTypeParametersCount,
                            valueArgumentsCount = firSymbol.valueParametersSize(),
                            constructorTypeArgumentsCount = constructorTypeParametersCount,
                        )
                    }
                    is IrSimpleFunctionSymbol -> {
                        require(firSymbol is FirCallableSymbol<*>) { "Illegal symbol: ${firSymbol!!::class}" }
                        IrCallImpl(
                            startOffset, endOffset, irType, symbol,
                            typeArgumentsCount = firSymbol.typeParameterSymbols.size,
                            valueArgumentsCount = firSymbol.valueParametersSize(),
                            origin = calleeReference.statementOrigin(),
                            superQualifierSymbol = dispatchReceiver?.superQualifierSymbol()
                        )
                    }

                    is IrLocalDelegatedPropertySymbol -> {
                        IrCallImpl(
                            startOffset, endOffset, irType,
                            declarationStorage.findGetterOfProperty(symbol),
                            typeArgumentsCount = calleeReference.toResolvedCallableSymbol()!!.fir.typeParameters.size,
                            valueArgumentsCount = 0,
                            origin = IrStatementOrigin.GET_LOCAL_PROPERTY,
                            superQualifierSymbol = dispatchReceiver?.superQualifierSymbol()
                        )
                    }

                    is IrPropertySymbol -> {
                        val property = calleeReference.toResolvedPropertySymbol()!!.fir
                        val getterSymbol = declarationStorage.findGetterOfProperty(symbol)
                        val backingFieldSymbol = declarationStorage.findBackingFieldOfProperty(symbol)
                        when {
                            getterSymbol != null -> {
                                IrCallImpl(
                                    startOffset, endOffset, irType,
                                    getterSymbol,
                                    typeArgumentsCount = property.typeParameters.size,
                                    valueArgumentsCount = property.contextReceivers.size,
                                    origin = incOrDeclSourceKindToIrStatementOrigin[qualifiedAccess.source?.kind]
                                        ?: IrStatementOrigin.GET_PROPERTY,
                                    superQualifierSymbol = dispatchReceiver?.superQualifierSymbol()
                                )
                            }

                            backingFieldSymbol != null -> IrGetFieldImpl(
                                startOffset, endOffset, backingFieldSymbol, irType,
                                superQualifierSymbol = dispatchReceiver?.superQualifierSymbol()
                            )

                            else -> IrErrorCallExpressionImpl(
                                startOffset, endOffset, irType,
                                description = "No getter or backing field found for ${calleeReference.render()}"
                            )
                        }
                    }

                    is IrFieldSymbol -> IrGetFieldImpl(
                        startOffset, endOffset, symbol, irType,
                        superQualifierSymbol = dispatchReceiver?.superQualifierSymbol()
                    )

                    is IrValueSymbol -> {
                        val variable = calleeReference.toResolvedVariableSymbol()!!.fir
                        IrGetValueImpl(
                            startOffset, endOffset,
                            // Note: there is a case with componentN function when IR type of variable differs from FIR type
                            variable.irTypeForPotentiallyComponentCall(predefinedType = irType),
                            symbol,
                            origin = if (variableAsFunctionMode) IrStatementOrigin.VARIABLE_AS_FUNCTION
                            else incOrDeclSourceKindToIrStatementOrigin[qualifiedAccess.source?.kind] ?: calleeReference.statementOrigin()
                        )
                    }

                    is IrEnumEntrySymbol -> IrGetEnumValueImpl(startOffset, endOffset, irType, symbol)
                    else -> generateErrorCallExpression(startOffset, endOffset, calleeReference, irType)
                }
            }.applyTypeArguments(qualifiedAccess).applyReceivers(qualifiedAccess, convertedExplicitReceiver)
                .applyCallArguments(qualifiedAccess)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            throw IllegalStateException(
                "Error while translating ${qualifiedAccess.render()} " +
                        "from file ${conversionScope.containingFileIfAny()?.name ?: "???"} to BE IR", e
            )
        }
    }

    private fun FirCallableSymbol<*>.valueParametersSize(): Int {
        return when (this) {
            is FirSyntheticPropertySymbol -> 0
            is FirNamedFunctionSymbol -> fir.valueParameters.size + fir.contextReceivers.size
            is FirConstructorSymbol -> fir.valueParameters.size + fir.contextReceivers.size
            is FirFunctionSymbol<*> -> fir.valueParameters.size
            else -> error("Illegal symbol: ${this::class}")
        }
    }

    private fun convertToIrSetCallForDynamic(
        variableAssignment: FirVariableAssignment,
        explicitReceiverExpression: IrExpression?,
        type: IrType,
        calleeReference: FirReference,
        symbol: FirBasedSymbol<*>,
        assignedValue: IrExpression,
    ): IrExpression {
        val selectedReceiver =
            (variableAssignment.unwrapLValue() ?: error("Assignment has no lValue")).findIrDynamicReceiver(explicitReceiverExpression)

        return variableAssignment.convertWithOffsets { startOffset, endOffset ->
            when (symbol) {
                is FirPropertySymbol -> {
                    val name = calleeReference.resolved?.name ?: error("There must be a name")
                    IrDynamicOperatorExpressionImpl(startOffset, endOffset, type, IrDynamicOperator.EQ).apply {
                        receiver = IrDynamicMemberExpressionImpl(
                            startOffset, endOffset, type, name.identifier, selectedReceiver
                        )
                        arguments.add(assignedValue)
                    }
                }

                else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
            }
        }
    }

    /**
     * A dynamic call has either an explicit receiver or an implicit this dispatch receiver.
     */
    private fun FirQualifiedAccessExpression.findIrDynamicReceiver(
        explicitReceiverExpression: IrExpression?,
    ): IrExpression {
        return explicitReceiverExpression
            ?: (dispatchReceiver as? FirThisReceiverExpression)?.let(visitor::convertToIrExpression)
            ?: error("No receiver for dynamic call")
    }

    private fun injectSetValueCall(element: FirElement, calleeReference: FirReference, assignedValue: IrExpression): IrExpression? {
        val injectedValue = findInjectedValue(calleeReference)
        if (injectedValue != null) {
            return element.convertWithOffsets { startOffset, endOffset ->
                val type = irBuiltIns.unitType
                val origin = calleeReference.statementOrigin()
                IrSetValueImpl(startOffset, endOffset, type, injectedValue.irParameterSymbol, assignedValue, origin)
            }
        }

        return null
    }

    internal fun findInjectedValue(calleeReference: FirReference) = extensions.findInjectedValue(calleeReference, conversionScope)

    fun convertToIrSetCall(variableAssignment: FirVariableAssignment, explicitReceiverExpression: IrExpression?): IrExpression {
        try {
            val type = irBuiltIns.unitType
            val calleeReference = variableAssignment.calleeReference ?: error("Reference not resolvable")
            val assignedValue = visitor.convertToIrExpression(variableAssignment.rValue)

            injectSetValueCall(variableAssignment, calleeReference, assignedValue)?.let { return it }

            val firSymbol = calleeReference.toResolvedBaseSymbol()
            val isDynamicAccess = firSymbol?.origin == FirDeclarationOrigin.DynamicScope

            if (isDynamicAccess) {
                return convertToIrSetCallForDynamic(
                    variableAssignment,
                    explicitReceiverExpression,
                    type,
                    calleeReference,
                    firSymbol ?: error("Must've had a symbol"),
                    assignedValue,
                )
            }

            val symbol = calleeReference.toSymbolForCall(
                extractDispatchReceiverOfAssignment(variableAssignment),
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
                        val firProperty = calleeReference.toResolvedPropertySymbol()!!.fir
                        val setterSymbol = declarationStorage.findSetterOfProperty(symbol)
                        when {
                            setterSymbol != null -> IrCallImpl(
                                startOffset, endOffset, type, setterSymbol,
                                typeArgumentsCount = firProperty.typeParameters.size,
                                valueArgumentsCount = 1 + firProperty.contextReceivers.size,
                                origin = origin,
                                superQualifierSymbol = variableAssignment.dispatchReceiver?.superQualifierSymbol()
                            ).apply {
                                putContextReceiverArguments(lValue)
                                putValueArgument(0, assignedValue)
                            }

                            else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
                        }
                    }

                    is IrPropertySymbol -> {
                        val setterSymbol = declarationStorage.findSetterOfProperty(symbol)
                        val backingFieldSymbol = declarationStorage.findBackingFieldOfProperty(symbol)
                        val firProperty = calleeReference.toResolvedPropertySymbol()!!.fir

                        when {
                            setterSymbol != null -> IrCallImpl(
                                startOffset, endOffset, type, setterSymbol,
                                typeArgumentsCount = firProperty.typeParameters.size,
                                valueArgumentsCount = 1 + firProperty.contextReceivers.size,
                                origin = origin,
                                superQualifierSymbol = variableAssignment.dispatchReceiver?.superQualifierSymbol()
                            ).apply {
                                putValueArgument(putContextReceiverArguments(lValue), assignedValue)
                            }

                            backingFieldSymbol != null -> IrSetFieldImpl(
                                startOffset, endOffset, backingFieldSymbol, type,
                                origin = null, // NB: to be consistent with PSI2IR, origin should be null here
                                superQualifierSymbol = variableAssignment.dispatchReceiver?.superQualifierSymbol()
                            ).apply {
                                value = assignedValue
                            }

                            else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
                        }
                    }

                    is IrSimpleFunctionSymbol -> {
                        val firFunction = calleeReference.toResolvedFunctionSymbol()?.fir
                        IrCallImpl(
                            startOffset, endOffset, type, symbol,
                            typeArgumentsCount = firFunction?.typeParameters?.size ?: 0,
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
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            throw IllegalStateException(
                "Error while translating ${variableAssignment.render()} " +
                        "from file ${conversionScope.containingFileIfAny()?.name ?: "???"} to BE IR", e
            )
        }
    }


    /**
     * If we have assignment like `this.x = ...` and this `this` is a dispatch this of some class, then we should unwrap
     *   smartcast if possible to generate SetField instead of setter call
     *
     * See KT-57105
     */
    private fun extractDispatchReceiverOfAssignment(variableAssignment: FirVariableAssignment): FirExpression? {
        val receiver = variableAssignment.dispatchReceiver ?: return null
        if (receiver !is FirSmartCastExpression) return receiver
        val thisReceiver = receiver.originalExpression as? FirThisReceiverExpression ?: return receiver
        val thisClass = thisReceiver.calleeReference.boundSymbol as? FirClassSymbol<*> ?: return receiver
        val propertySymbol = variableAssignment.calleeReference?.toResolvedPropertySymbol() ?: return receiver
        val propertyDispatchReceiverType = propertySymbol.dispatchReceiverType ?: return receiver
        return when (thisClass.defaultType().isSubtypeOf(propertyDispatchReceiverType, session)) {
            true -> thisReceiver
            false -> receiver
        }
    }

    fun convertToIrConstructorCall(annotation: FirAnnotation): IrExpression {
        val coneType = annotation.annotationTypeRef.coneTypeSafe<ConeLookupTagBasedType>()
            ?.fullyExpandedType(session) as? ConeLookupTagBasedType
        val type = coneType?.toIrType()
        val symbol = type?.classifierOrNull
        val irConstructorCall = annotation.convertWithOffsets { startOffset, endOffset ->
            if (symbol !is IrClassSymbol) {
                return@convertWithOffsets IrErrorCallExpressionImpl(
                    startOffset, endOffset, type ?: createErrorType(), "Unresolved reference: ${annotation.render()}"
                )
            }

            val firConstructorSymbol = annotation.toResolvedCallableSymbol(session) as? FirConstructorSymbol
                ?: run {
                    // Fallback for FirReferencePlaceholderForResolvedAnnotations from jar
                    val fir = coneType.lookupTag.toSymbol(session)?.fir as? FirClass
                    var constructorSymbol: FirConstructorSymbol? = null
                    fir?.unsubstitutedScope()?.processDeclaredConstructors {
                        if (it.fir.isPrimary && constructorSymbol == null) {
                            constructorSymbol = it
                        }
                    }
                    constructorSymbol
                } ?: return@convertWithOffsets IrErrorCallExpressionImpl(
                    startOffset, endOffset, type, "No annotation constructor found: $symbol"
                )

            // Annotation constructor may come unresolved on partial module compilation (inside the IDE).
            // Here it is resolved together with default parameter values, as transformers convert them to constants.
            // Also see 'IrConstAnnotationTransformer'.
            firConstructorSymbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

            val irConstructor = declarationStorage.getIrConstructorSymbol(firConstructorSymbol)

            IrConstructorCallImpl(
                startOffset, endOffset, type, irConstructor,
                // Get the number of value arguments from FIR because of a possible cycle where an annotation constructor
                // parameter is annotated with the same annotation.
                // In this case, the IR value parameters won't be initialized yet, and we will get 0 from
                // `irConstructor.owner.valueParameters.size`.
                // See KT-58294
                valueArgumentsCount = firConstructorSymbol.valueParameterSymbols.size,
                typeArgumentsCount = annotation.typeArguments.size,
                constructorTypeArgumentsCount = 0
            )
        }
        return visitor.withAnnotationMode {
            val annotationCall = annotation.toAnnotationCall()
            irConstructorCall
                .applyCallArguments(annotationCall)
                .applyTypeArguments(annotationCall?.typeArguments, null)
        }
    }

    private fun FirAnnotation.toAnnotationCall(): FirAnnotationCall? {
        if (this is FirAnnotationCall) return this
        return buildAnnotationCall {
            useSiteTarget = this@toAnnotationCall.useSiteTarget
            annotationTypeRef = this@toAnnotationCall.annotationTypeRef
            val symbol = annotationTypeRef.coneType.fullyExpandedType(session).toSymbol(session) as? FirRegularClassSymbol ?: return null

            val constructorSymbol = symbol.unsubstitutedScope().getDeclaredConstructors().firstOrNull() ?: return null

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

            /**
             * This is not right, but it doesn't make sense as [FirAnnotationCall.containingDeclarationSymbol] uses only in FIR
             */
            containingDeclarationSymbol = constructorSymbol
        }
    }

    internal fun convertToGetObject(qualifier: FirResolvedQualifier): IrExpression {
        return convertToGetObject(qualifier, null)!!
    }

    internal fun convertToGetObject(
        qualifier: FirResolvedQualifier,
        callableReferenceAccess: FirCallableReferenceAccess?,
    ): IrExpression? {
        val classSymbol = (qualifier.resolvedType as? ConeClassLikeType)?.lookupTag?.toSymbol(session)

        if (callableReferenceAccess?.isBound == false) {
            return null
        }

        val irType = qualifier.resolvedType.toIrType()
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

    private fun extractArgumentsMapping(
        call: FirCall,
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
        val substitutor = (call as? FirFunctionCall)?.buildSubstitutorByCalledCallable() ?: ConeSubstitutor.Empty
        return Triple(valueParameters, argumentMapping, substitutor)
    }

    internal fun IrExpression.applyCallArguments(
        statement: FirStatement?,
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
                        if (argumentMapping != null && (visitor.annotationMode || argumentMapping.isNotEmpty())) {
                            if (valueParameters != null) {
                                return applyArgumentsWithReorderingIfNeeded(
                                    argumentMapping, valueParameters, substitutor, contextReceiverCount, call,
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
                    val calleeSymbol = (this as? IrCallImpl)?.symbol

                    @OptIn(UnsafeDuringIrConstructionAPI::class)
                    val description = calleeSymbol?.signature?.render()
                        ?: calleeSymbol?.takeIf { it.isBound }?.owner?.render()
                        ?: "???"
                    IrErrorCallExpressionImpl(
                        startOffset, endOffset, type,
                        "Cannot bind $argumentsCount arguments to '$description' call with $valueArgumentsCount parameters"
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
                if (argumentMapping != null && (visitor.annotationMode || argumentMapping.isNotEmpty())) {
                    if (valueParameters != null) {
                        val dynamicCallVarargArgument = argumentMapping.keys.firstOrNull() as? FirVarargArgumentsExpression
                            ?: error("Dynamic call must have a single vararg argument")
                        for (argument in dynamicCallVarargArgument.arguments) {
                            val irArgument = convertArgument(argument, null, substitutor)
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
        contextReceiverCount: Int,
        call: FirCall,
    ): IrExpression {
        val converted = argumentMapping.entries.map { (argument, parameter) ->
            parameter to convertArgument(argument, parameter, substitutor)
        }
        // If none of the parameters have side effects, the evaluation order doesn't matter anyway.
        // For annotations, this is always true, since arguments have to be compile-time constants.
        if (!visitor.annotationMode && !converted.all { (_, irArgument) -> irArgument.hasNoSideEffects() } &&
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
            if (visitor.annotationMode) {
                val function = call.toReference(session)?.toResolvedCallableSymbol()?.fir as? FirFunction
                for ((index, parameter) in valueParameters.withIndex()) {
                    if (parameter.isVararg && !argumentMapping.containsValue(parameter)) {
                        val value = if (function?.itOrExpectHasDefaultParameterValue(index) == true) {
                            null
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
        valueParameters: List<FirValueParameter>,
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
    ): IrExpression {
        val unsubstitutedParameterType = parameter?.returnTypeRef?.coneType?.fullyExpandedType(session)
        // Normally argument type should be correct itself.
        // However, for deserialized annotations it's possible to have imprecise Array<Any> type
        // for empty integer literal arguments.
        // In this case we have to use parameter type itself which is more precise, like Array<String> or IntArray.
        // See KT-62598 and its fix for details.
        val expectedType = unsubstitutedParameterType.takeIf { visitor.annotationMode && unsubstitutedParameterType?.isArrayType == true }
        val unwrappedArgument = argument.unwrapArgument()
        var irArgument = visitor.convertToIrExpression(unwrappedArgument, expectedType = expectedType)
        if (unsubstitutedParameterType != null) {
            with(visitor.implicitCastInserter) {
                val argumentType = unwrappedArgument.resolvedType.fullyExpandedType(session)
                if (unwrappedArgument is FirSmartCastExpression) {
                    val substitutedParameterType = substitutor.substituteOrSelf(unsubstitutedParameterType)
                    // here we should use a substituted parameter type to properly choose the component of an intersection type
                    //  to provide a proper cast to the smartcasted type
                    irArgument = irArgument.insertCastForSmartcastWithIntersection(argumentType, substitutedParameterType)
                }
                // here we should pass unsubstituted parameter type to properly infer if the original type accepts null or not
                // to properly insert nullability check
                irArgument = irArgument.insertSpecialCast(unwrappedArgument, argumentType, unsubstitutedParameterType)
            }
        }
        with(adapterGenerator) {
            if (parameter?.returnTypeRef is FirResolvedTypeRef) {
                // Java type case (from annotations)
                val parameterType = parameter.returnTypeRef.coneType
                val unwrappedParameterType = if (parameter.isVararg) parameterType.arrayElementType()!! else parameterType
                val samFunctionType = getFunctionTypeForPossibleSamType(unwrappedParameterType)
                irArgument = irArgument.applySuspendConversionIfNeeded(unwrappedArgument, samFunctionType ?: unwrappedParameterType)
                irArgument = irArgument.applySamConversionIfNeeded(unwrappedArgument, parameter)
            }
        }
        return irArgument
            .applyAssigningArrayElementsToVarargInNamedForm(unwrappedArgument, parameter)
            .applyImplicitIntegerCoercionIfNeeded(unwrappedArgument, parameter)
    }

    private fun IrExpression.applyAssigningArrayElementsToVarargInNamedForm(
        argument: FirExpression,
        parameter: FirValueParameter?,
    ): IrExpression {
        if (this is IrVararg && parameter?.isVararg == true && argument is FirVarargArgumentsExpression && elements.size == 1) {
            val irVarargElement = elements[0]
            if (argument.arguments[0] is FirNamedArgumentExpression &&
                // IrVarargElement can be either IrSpreadElement (then nothing to do) or IrExpression
                irVarargElement is IrExpression &&
                (irVarargElement.type.isArray() || irVarargElement.type.isPrimitiveArray() || irVarargElement.type.isUnsignedArray())
            ) {
                elements[0] = IrSpreadElementImpl(irVarargElement.startOffset, irVarargElement.endOffset, irVarargElement)
            }
        }
        return this
    }

    private fun IrExpression.applyImplicitIntegerCoercionIfNeeded(
        argument: FirExpression,
        parameter: FirValueParameter?,
    ): IrExpression {
        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.ImplicitSignedToUnsignedIntegerConversion)) return this

        if (parameter == null || !parameter.isMarkedWithImplicitIntegerCoercion) return this
        if (!argument.getExpectedType(parameter).fullyExpandedType(session).isUnsignedTypeOrNullableUnsignedType) return this

        fun IrExpression.applyToElement(
            argument: FirExpression,
            firConversionFunction: FirNamedFunctionSymbol,
            irConversionFunction: IrSimpleFunctionSymbol,
        ): IrExpression {
            return if (argument.isIntegerLiteralOrOperatorCall() ||
                argument.toReference(session)?.toResolvedCallableSymbol()?.let {
                    it.resolvedStatus.isConst && it.isMarkedWithImplicitIntegerCoercion
                } == true
            ) {
                IrCallImpl(
                    startOffset, endOffset,
                    firConversionFunction.fir.returnTypeRef.toIrType(),
                    irConversionFunction,
                    typeArgumentsCount = 0,
                    valueArgumentsCount = 0
                ).apply {
                    extensionReceiver = this@applyToElement
                }
            } else {
                this@applyToElement
            }
        }

        return when {
            parameter.isMarkedWithImplicitIntegerCoercion -> when {
                this is IrVarargImpl && argument is FirVarargArgumentsExpression -> {
                    val targetTypeFqName = varargElementType.classFqName ?: return this
                    val conversionFunctions = irBuiltIns.getNonBuiltInFunctionsWithFirCounterpartByExtensionReceiver(
                        Name.identifier("to" + targetTypeFqName.shortName().asString()),
                        StandardNames.BUILT_INS_PACKAGE_NAME.asString()
                    )
                    if (conversionFunctions.isNotEmpty()) {
                        elements.forEachIndexed { i, irVarargElement ->
                            if (irVarargElement !is IrExpression) return@forEachIndexed
                            val argumentClassifier = argument.arguments[i].resolvedType.toIrType().classifierOrNull ?: return@forEachIndexed
                            val (targetFirFun, targetIrFun) = conversionFunctions[argumentClassifier] ?: return@forEachIndexed
                            elements[i] = irVarargElement.applyToElement(argument.arguments[i], targetFirFun, targetIrFun)
                        }
                    }
                    this
                }
                else -> {
                    val targetIrType = parameter.returnTypeRef.toIrType()
                    val targetTypeFqName = targetIrType.classFqName ?: return this
                    val conversionFunctions = irBuiltIns.getNonBuiltInFunctionsWithFirCounterpartByExtensionReceiver(
                        Name.identifier("to" + targetTypeFqName.shortName().asString()),
                        StandardNames.BUILT_INS_PACKAGE_NAME.asString()
                    )
                    val sourceTypeClassifier = argument.resolvedType.toIrType().classifierOrNull ?: return this

                    val (firConversionFunction, irConversionFunction) = conversionFunctions[sourceTypeClassifier] ?: return this

                    this.applyToElement(argument, firConversionFunction, irConversionFunction)
                }
            }
            else -> this
        }
    }

    internal fun IrExpression.applyTypeArguments(access: FirQualifiedAccessExpression): IrExpression {
        val calleeReference = access.calleeReference
        val originalTypeArguments = access.typeArguments
        val callableFir = calleeReference.toResolvedCallableSymbol()?.fir

        // If we have a constructor call through a type alias, we can't apply the type arguments as is.
        // The type arguments in FIR correspond to the original type arguments as passed to the type alias.
        // However, the type alias can map the type arguments arbitrarily (change order, change count by mapping K,V -> Map<K,V> or by
        // having an unused argument).
        // We need to map the type arguments using the expansion of the type alias.

        val typeArguments = (callableFir as? FirConstructor)
            ?.typeAliasForConstructor
            ?.let { originalTypeArguments.toExpandedTypeArguments(it) }
            ?: originalTypeArguments


        return applyTypeArguments(
            typeArguments,
            (callableFir as? FirTypeParametersOwner)?.typeParameters
        )
    }

    /**
     * Applies the list of type arguments to the given type alias, expands it fully and returns the list of type arguments for the
     * resulting type.
     */
    private fun List<FirTypeProjection>.toExpandedTypeArguments(typeAliasSymbol: FirTypeAliasSymbol): List<FirTypeProjection> {
        return typeAliasSymbol
            .constructType(map { it.toConeTypeProjection() }.toTypedArray(), false)
            .fullyExpandedType(session)
            .typeArguments
            .map { typeProjection ->
                buildTypeProjectionWithVariance {
                    variance = when (typeProjection) {
                        is ConeKotlinTypeProjectionIn -> Variance.IN_VARIANCE
                        is ConeKotlinTypeProjectionOut -> Variance.OUT_VARIANCE
                        else -> Variance.INVARIANT
                    }
                    typeRef = (typeProjection as? ConeKotlinType)?.let {
                        buildResolvedTypeRef { type = it }
                    } ?: buildErrorTypeRef {
                        diagnostic = ConeSimpleDiagnostic("Expansion contains unexpected type ${typeProjection.javaClass}")
                    }
                }
            }
    }

    private fun IrExpression.applyTypeArguments(
        typeArguments: List<FirTypeProjection>?,
        typeParameters: List<FirTypeParameter>?,
    ): IrExpression {
        if (this !is IrMemberAccessExpression<*>) return this

        val argumentsCount = typeArguments?.size ?: return this
        if (argumentsCount <= typeArgumentsCount) {
            for ((index, argument) in typeArguments.withIndex()) {
                val typeParameter = typeParameters?.get(index)
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
            val name = if (this is IrCallImpl) symbol.signature.toString() else "???"
            return IrErrorExpressionImpl(
                startOffset, endOffset, type,
                "Cannot bind $argumentsCount type arguments to $name call with $typeArgumentsCount type parameters"
            )
        }
    }

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

        return firReceiver
            ?.let { visitor.convertToIrReceiverExpression(it, this) }
            ?: explicitReceiverExpression
    }

    private fun IrExpression.applyReceivers(
        qualifiedAccess: FirQualifiedAccessExpression,
        explicitReceiverExpression: IrExpression?,
    ): IrExpression {
        when (this) {
            is IrMemberAccessExpression<*> -> {
                val resolvedFirSymbol = qualifiedAccess.toResolvedCallableSymbol()
                if (resolvedFirSymbol?.dispatchReceiverType != null) {
                    val baseDispatchReceiver = qualifiedAccess.findIrDispatchReceiver(explicitReceiverExpression)
                    var firDispatchReceiver = qualifiedAccess.dispatchReceiver
                    if (firDispatchReceiver is FirPropertyAccessExpression && firDispatchReceiver.calleeReference is FirSuperReference) {
                        firDispatchReceiver = firDispatchReceiver.dispatchReceiver
                    }
                    val notFromAny = !resolvedFirSymbol.isFunctionFromAny()
                    val notAnInterface = firDispatchReceiver?.resolvedType?.toRegularClassSymbol(session)?.isInterface != true
                    dispatchReceiver =
                        if (notFromAny || notAnInterface) {
                            baseDispatchReceiver
                        } else {
                            requireNotNull(baseDispatchReceiver)
                            // NB: for FE 1.0, this type cast is added by InterfaceObjectCallsLowering
                            // However, it doesn't work for FIR due to different f/o structure
                            // (FIR calls Any method directly, but FE 1.0 calls its interface f/o instead)
                            implicitCast(
                                baseDispatchReceiver,
                                irBuiltIns.anyType,
                                IrTypeOperator.IMPLICIT_CAST
                            )
                        }
                }
                // constructors don't have extension receiver but may have receiver parameter in case of inner classes
                if (resolvedFirSymbol?.receiverParameter != null && resolvedFirSymbol !is FirConstructorSymbol) {
                    extensionReceiver = qualifiedAccess.findIrExtensionReceiver(explicitReceiverExpression)?.let {
                        val symbol = qualifiedAccess.calleeReference.toResolvedCallableSymbol()
                            ?: error("Symbol for call ${qualifiedAccess.render()} not found")
                        symbol.fir.receiverParameter?.typeRef?.let { receiverType ->
                            with(visitor.implicitCastInserter) {
                                val extensionReceiver = qualifiedAccess.extensionReceiver!!
                                val substitutor = qualifiedAccess.buildSubstitutorByCalledCallable()
                                it.insertSpecialCast(
                                    extensionReceiver,
                                    extensionReceiver.resolvedType,
                                    substitutor.substituteOrSelf(receiverType.coneType),
                                )
                            }
                        } ?: it
                    }
                }
            }

            is IrFieldAccessExpression -> {
                val firDeclaration = qualifiedAccess.toResolvedCallableSymbol()!!.fir.propertyIfBackingField
                // Top-level properties are considered as static in IR
                val fieldIsStatic = firDeclaration.isStatic || (firDeclaration is FirProperty && !firDeclaration.isLocal && firDeclaration.containingClassLookupTag() == null)
                if (!fieldIsStatic) {
                    receiver = qualifiedAccess.findIrDispatchReceiver(explicitReceiverExpression)
                }
            }
        }
        return this
    }

    private fun FirCallableSymbol<*>.isFunctionFromAny(): Boolean {
        if (this !is FirNamedFunctionSymbol) return false
        return isMethodOfAny
    }

    private fun generateErrorCallExpression(
        startOffset: Int,
        endOffset: Int,
        calleeReference: FirReference,
        type: IrType? = null,
    ): IrErrorCallExpression {
        return IrErrorCallExpressionImpl(
            startOffset, endOffset, type ?: createErrorType(),
            "Unresolved reference: ${calleeReference.render()}"
        )
    }
}

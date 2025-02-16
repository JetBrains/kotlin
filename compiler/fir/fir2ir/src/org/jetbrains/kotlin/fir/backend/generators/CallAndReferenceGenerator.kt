/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.utils.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationCall
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.FirSimpleSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.calls.getExpectedType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.approximateDeclarationType
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class CallAndReferenceGenerator(
    private val c: Fir2IrComponents,
    private val visitor: Fir2IrVisitor,
    private val conversionScope: Fir2IrConversionScope,
) : Fir2IrComponents by c {

    private val adapterGenerator = AdapterGenerator(c, conversionScope)

    private fun FirTypeRef.toIrType(): IrType = toIrType(c, conversionScope.defaultConversionTypeOrigin())

    private fun ConeKotlinType.toIrType(): IrType = toIrType(c, conversionScope.defaultConversionTypeOrigin())

    fun convertToIrCallableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        explicitReceiverExpression: IrExpression?,
        isDelegate: Boolean,
    ): IrExpression {
        val type = approximateFunctionReferenceType(callableReferenceAccess.resolvedType).toIrType()

        val firSymbol = callableReferenceAccess.calleeReference.extractDeclarationSiteSymbol(c)
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
                return toIrSymbolForCallableReference(
                    c,
                    callableReferenceAccess.dispatchReceiver,
                    lhs = callableReferenceAccess.explicitReceiver,
                    isDelegate = isDelegate
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
                )
                    .applyTypeArguments(callableReferenceAccess)
                    .applyReceiversAndArguments(callableReferenceAccess, firSymbol, explicitReceiverExpression)
            }

            fun convertReferenceToSyntheticProperty(propertySymbol: FirSimpleSyntheticPropertySymbol): IrExpression? {
                val irPropertySymbol = callablesGenerator.generateIrPropertyForSyntheticPropertyReference(
                    propertySymbol,
                    conversionScope.parentFromStack()
                ).symbol
                val property = propertySymbol.syntheticProperty
                val referencedPropertyGetterSymbol =
                    declarationStorage.getIrFunctionSymbol(property.getter.delegate.unwrapUseSiteSubstitutionOverrides().symbol) as? IrSimpleFunctionSymbol
                        ?: return null
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
                )
                    .applyTypeArguments(callableReferenceAccess)
                    .applyReceiversAndArguments(callableReferenceAccess, firSymbol, explicitReceiverExpression)
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

            fun convertReferenceToField(fieldSymbol: FirFieldSymbol): IrExpression {
                val field = fieldSymbol.fir
                val irPropertySymbol = fieldSymbol.toSymbolForCall() as IrPropertySymbol
                val irFieldSymbol = declarationStorage.findBackingFieldOfProperty(irPropertySymbol)!!
                return IrPropertyReferenceImpl(
                    startOffset, endOffset, type,
                    irPropertySymbol,
                    typeArgumentsCount = 0,
                    field = irFieldSymbol,
                    getter = runIf(!field.isStatic) { declarationStorage.findGetterOfProperty(irPropertySymbol) },
                    setter = runIf(!field.isStatic) { declarationStorage.findSetterOfProperty(irPropertySymbol) },
                    origin
                )
                    .applyReceiversAndArguments(callableReferenceAccess, firSymbol, explicitReceiverExpression)
            }

            fun convertReferenceToFunction(functionSymbol: FirFunctionSymbol<*>): IrExpression? {
                val irFunctionSymbol = functionSymbol.toSymbolForCall() as? IrFunctionSymbol ?: return null

                require(type is IrSimpleType)
                var function = callableReferenceAccess.calleeReference.toResolvedFunctionSymbol()!!.fir
                if (function is FirConstructor) {
                    // The number of type parameters of typealias constructor may mismatch with that number in the original constructor.
                    // And for IR, we need to use the original constructor as a source of truth
                    function = function.typeAliasConstructorInfo?.originalConstructor ?: function
                }
                return if (adapterGenerator.needToGenerateAdaptedCallableReference(callableReferenceAccess, type, function)) {
                    // Receivers are being applied inside
                    with(adapterGenerator) {
                        generateAdaptedCallableReference(callableReferenceAccess, explicitReceiverExpression, irFunctionSymbol, type)
                    }
                } else {
                    IrFunctionReferenceImplWithShape(
                        startOffset, endOffset, type, irFunctionSymbol,
                        typeArgumentsCount = function.typeParameters.size,
                        valueArgumentsCount = function.valueParameters.size + function.contextParameters.size,
                        contextParameterCount = function.contextParameters.size,
                        hasDispatchReceiver = function.dispatchReceiverType != null,
                        hasExtensionReceiver = function.isExtension,
                        reflectionTarget = irFunctionSymbol
                    )
                        .applyTypeArguments(callableReferenceAccess)
                        .applyReceiversAndArguments(callableReferenceAccess, firSymbol, explicitReceiverExpression)
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
        // Approximate a function type's input types to their supertypes.
        // Approximating the outer type will lead to the input types being approximated to their subtypes
        // because the input type parameters have in variance.
        if (kotlinType !is ConeClassLikeType) return kotlinType

        val typeArguments = kotlinType.typeArguments
        return kotlinType.withArguments(Array(typeArguments.size) { i ->
            val projection = typeArguments[i]
            if (i < typeArguments.lastIndex) {
                projection.type?.approximateForIrOrNull(this)?.toTypeProjection(projection.kind) ?: projection
            } else {
                projection
            }
        })
    }

    private fun FirQualifiedAccessExpression.tryConvertToSamConstructorCall(type: IrType): IrTypeOperatorCall? {
        val calleeReference = calleeReference as? FirResolvedNamedReference ?: return null
        val fir = calleeReference.resolvedSymbol.fir
        if (this is FirFunctionCall && fir is FirSimpleFunction && fir.origin == FirDeclarationOrigin.SamConstructor) {
            val (_, _, substitutor) = extractArgumentsMapping(this)
            val irArgument = convertArgument(argument, fir.valueParameters.first(), substitutor)
            return convertWithOffsets { startOffset, endOffset ->
                IrTypeOperatorCallImpl(
                    startOffset, endOffset, type, IrTypeOperator.SAM_CONVERSION, type, irArgument
                )
            }
        }
        return null
    }

    private fun FirExpression.superQualifierSymbolForFunctionAndPropertyAccess(): IrClassSymbol? {
        if (this !is FirQualifiedAccessExpression) {
            return null
        }
        val dispatchReceiverReference = calleeReference
        if (dispatchReceiverReference !is FirSuperReference) {
            return null
        }
        val superTypeRef = dispatchReceiverReference.superTypeRef
        val coneSuperType = superTypeRef.coneType
        val firClassSymbol = coneSuperType.fullyExpandedType(session).toClassSymbol(session)
        if (firClassSymbol != null) {
            return classifierStorage.getIrClassSymbol(firClassSymbol)
        }
        return null
    }

    /**
     * Should be called on dispatch receiver of corresponding field access
     *
     * This method attempts to replicate the behavior of K1 in generation of `superQualifiedSymbol` for IrFieldAccessExpression
     */
    private fun FirExpression.superQualifierSymbolForFieldAccess(firResolvedSymbol: FirBasedSymbol<*>?): IrClassSymbol? {
        if (firResolvedSymbol is FirBackingFieldSymbol || firResolvedSymbol is FirDelegateFieldSymbol) return null

        val classSymbol = when (this) {
            is FirResolvedQualifier -> {
                if (resolvedToCompanionObject) return null
                this.symbol as? FirClassSymbol<*>
            }
            else -> {
                val type = this.resolvedType.fullyExpandedType(session).lowerBoundIfFlexible()

                fun findIntersectionComponent(type: ConeKotlinType): ConeKotlinType? {
                    if (type !is ConeIntersectionType) return type
                    // in the case of intersection type in the receiver we need to find the component which contains referenced field
                    if (firResolvedSymbol !is FirCallableSymbol<*>) return null
                    val containingClassType = firResolvedSymbol.dispatchReceiverType as? ConeLookupTagBasedType ?: return null
                    val containingClassLookupTag = containingClassType.lookupTag
                    val typeContext = session.typeContext
                    return type.intersectedTypes.first {
                        val componentConstructor = with(typeContext) { it.lowerBoundIfFlexible().typeConstructor() }
                        AbstractTypeChecker.isSubtypeOfClass(
                            typeContext,
                            componentConstructor,
                            containingClassLookupTag
                        )
                    }
                }
                findIntersectionComponent(type)?.toClassSymbol(session)
            }
        } ?: return null

        /**
         * class Some {
         *     companion object {
         *         val x: Int = 1
         *     }
         * }
         *
         * Some.x // <--- no superQualifiedSymbol
         */
        if (classSymbol.isCompanion) return null

        val irClassSymbol = classifierStorage.getIrClassSymbol(classSymbol)

        /**
         * enum class Some {
         *     A, B;
         *
         *     val x: Int = 1
         *
         *     fun foo() {
         *         this.x // <--- no superQualifiedSymbol
         *     }
         * }
         */
        if (classSymbol.classKind == ClassKind.ENUM_CLASS && conversionScope.parentStack.any { (it as? IrClass)?.symbol == irClassSymbol }) {
            return null
        }


        /**
         * // FILE: Base.java
         * public class Base {
         *     int fromJava = 0;
         * }
         *
         * // FILE: Derived.kt
         * class Derived : Base() {
         *     val fromKotlin = 1
         *
         *     init {
         *         this.fromJava // <--- no superQualifiedSymbol
         *         this.fromKotlin // <--- superQualifiedSymbol is set
         *     }
         *  }
         */
        if (
            conversionScope.initBlocksStack.any { it.parentAsClass.symbol == irClassSymbol } &&
            !(this is FirThisReceiverExpression && firResolvedSymbol?.fir is FirJavaField)
        ) {
            return null
        }
        return irClassSymbol
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

    private val FirQualifiedAccessExpression.dynamicOperator: IrDynamicOperator?
        get() {
            val kind = calleeReference.source?.kind
            val isOperationOnArray = (explicitReceiver as? FirQualifiedAccessExpression)
                ?.calleeReference?.resolved?.name == SpecialNames.ARRAY

            return when {
                kind is KtFakeSourceElementKind.ArrayAccessNameReference || isOperationOnArray -> when (calleeReference.resolved?.name) {
                    OperatorNameConventions.SET -> IrDynamicOperator.EQ
                    OperatorNameConventions.GET -> IrDynamicOperator.ARRAY_ACCESS
                    else -> error("Unexpected name")
                }

                kind is KtFakeSourceElementKind.DesugaredPrefixInc -> IrDynamicOperator.PREFIX_INCREMENT
                kind is KtFakeSourceElementKind.DesugaredPrefixDec -> IrDynamicOperator.PREFIX_DECREMENT
                kind is KtFakeSourceElementKind.DesugaredPostfixInc -> IrDynamicOperator.POSTFIX_INCREMENT
                kind is KtFakeSourceElementKind.DesugaredPostfixDec -> IrDynamicOperator.POSTFIX_DECREMENT

                kind is KtFakeSourceElementKind.DesugaredAugmentedAssign -> when (calleeReference.resolved?.name) {
                    OperatorNameConventions.SET -> IrDynamicOperator.EQ
                    OperatorNameConventions.GET -> IrDynamicOperator.ARRAY_ACCESS
                    else -> error("Unexpected name")
                }

                else -> null
            }
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
                        typeConverter.builtins.booleanType
                    } else {
                        type
                    }
                    IrDynamicOperatorExpressionImpl(startOffset, endOffset, theType, operator).apply {
                        receiver = if (operator == IrDynamicOperator.INVOKE && qualifiedAccess !is FirImplicitInvokeCall) {
                            IrDynamicMemberExpressionImpl(startOffset, endOffset, type, name.identifier, selectedReceiver)
                        } else {
                            selectedReceiver
                        }

                        if (noArguments || qualifiedAccess !is FirCall) return@apply

                        val (valueParameters, argumentMapping, substitutor) = extractArgumentsMapping(qualifiedAccess)
                        if (valueParameters == null || argumentMapping == null || !visitor.annotationMode && argumentMapping.isEmpty()) return@apply

                        val dynamicCallVarargArgument = argumentMapping.keys.firstOrNull() as? FirVarargArgumentsExpression
                            ?: error("Dynamic call must have a single vararg argument")
                        for (argument in dynamicCallVarargArgument.arguments) {
                            val irArgument = convertArgument(argument, null, substitutor)
                            arguments.add(irArgument)
                        }
                    }
                }

                is FirPropertySymbol -> {
                    val name = calleeReference.resolved?.name ?: error("There must be a name")
                    IrDynamicMemberExpressionImpl(startOffset, endOffset, type, name.identifier, selectedReceiver)
                }

                else -> generateErrorCallExpression(startOffset, endOffset, calleeReference, type)
                    .applyIf(qualifiedAccess is FirCall && !noArguments) {
                        applyReceiversAndArguments(qualifiedAccess, declarationSiteSymbol = null, explicitReceiverExpression = null)
                    }
            }
        }
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
    ): IrExpression = convertCatching(qualifiedAccess, conversionScope) {
        injectGetValueCall(qualifiedAccess, qualifiedAccess.calleeReference)?.let { return it }

        val irType = type.toIrType()
        val samConstructorCall = qualifiedAccess.tryConvertToSamConstructorCall(irType)
        if (samConstructorCall != null) return samConstructorCall

        val dispatchReceiver = qualifiedAccess.dispatchReceiver
        val calleeReference = qualifiedAccess.calleeReference

        if (calleeReference is FirSuperReference && dispatchReceiver != null) {
            return visitor.convertToIrExpression(dispatchReceiver)
        }

        val firSymbol = calleeReference.extractDeclarationSiteSymbol(c)
        val isDynamicAccess = firSymbol?.origin == FirDeclarationOrigin.DynamicScope

        if (isDynamicAccess) {
            return convertToIrCallForDynamic(
                qualifiedAccess,
                explicitReceiverExpression,
                irType,
                calleeReference,
                firSymbol,
                dynamicOperator,
                noArguments,
            )
        }

        // We might have had a dynamic receiver, but resolved
        // into a non-fake member. For example, we can
        // resolve into members of `Any`.
        val convertedExplicitReceiver = if (explicitReceiverExpression?.type is IrDynamicType) {
            qualifiedAccess.convertWithOffsets { startOffset, endOffset ->
                // we should use a dispatch receiver type from the raw resolved symbol, without unwrapping fake-overrides
                // to get proper use-site dispatch receiver
                val callableDeclaration = calleeReference.toResolvedCallableSymbol()?.fir
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
            val irSymbol = firSymbol?.toIrSymbolForCall(
                c,
                dispatchReceiver,
                explicitReceiver = qualifiedAccess.explicitReceiver
            )
            when (irSymbol) {
                is IrConstructorSymbol -> {
                    require(firSymbol is FirConstructorSymbol)
                    val constructor = firSymbol.unwrapCallRepresentative(c).fir as FirConstructor
                    val totalTypeParametersCount = constructor.typeParameters.size
                    val constructorTypeParametersCount = constructor.typeParameters.count { it is FirTypeParameter }
                    IrConstructorCallImplWithShape(
                        startOffset,
                        endOffset,
                        irType,
                        irSymbol,
                        typeArgumentsCount = totalTypeParametersCount,
                        valueArgumentsCount = firSymbol.valueParametersSize(),
                        contextParameterCount = constructor.contextParameters.size,
                        constructorTypeArgumentsCount = constructorTypeParametersCount,
                        hasDispatchReceiver = firSymbol.dispatchReceiverType != null,
                        hasExtensionReceiver = firSymbol.isExtension,
                    )
                }
                is IrSimpleFunctionSymbol -> {
                    val callOrigin = calleeReference.statementOrigin()
                    /*
                     * For `x += y` -> `x = x.plus(y)` receiver of call `plus` should also have an augmented assignment origin.
                     * But it's hard to detect this origin during conversion the receiver itself, so we update it afterward.
                     */
                    if (
                        explicitReceiverExpression != null &&
                        calleeReference.source?.kind is KtFakeSourceElementKind.DesugaredAugmentedAssign &&
                        callOrigin != null &&
                        // This is to reproduce K1 behavior. K1 does not set origin for augmented assignment-originated `get` and `set`
                        firSymbol.name != OperatorNameConventions.GET && firSymbol.name != OperatorNameConventions.SET
                    ) {
                        explicitReceiverExpression.updateStatementOrigin(callOrigin)
                    }
                    IrCallImplWithShape(
                        startOffset, endOffset, irType, irSymbol,
                        typeArgumentsCount = firSymbol.typeParameterSymbols.size,
                        valueArgumentsCount = firSymbol.valueParametersSize(),
                        contextParameterCount = firSymbol.fir.contextParameters.size,
                        hasDispatchReceiver = firSymbol.dispatchReceiverType != null,
                        hasExtensionReceiver = firSymbol.isExtension,
                        origin = callOrigin,
                        superQualifierSymbol = dispatchReceiver?.superQualifierSymbolForFunctionAndPropertyAccess()
                    )
                }

                is IrLocalDelegatedPropertySymbol -> {
                    IrCallImpl(
                        startOffset, endOffset, irType,
                        declarationStorage.findGetterOfProperty(irSymbol),
                        typeArgumentsCount = calleeReference.toResolvedCallableSymbol()!!.fir.typeParameters.size,
                        origin = IrStatementOrigin.GET_LOCAL_PROPERTY,
                        superQualifierSymbol = dispatchReceiver?.superQualifierSymbolForFunctionAndPropertyAccess()
                    )
                }

                is IrPropertySymbol -> {
                    val property = calleeReference.toResolvedPropertySymbol()!!.fir
                    val getterSymbol = declarationStorage.findGetterOfProperty(irSymbol)
                    val backingFieldSymbol = declarationStorage.findBackingFieldOfProperty(irSymbol)
                    when {
                        getterSymbol != null -> {
                            // In case the receiver is an intersection type containing a value class and the property is an intersection
                            // override, the return type might be approximated to Any.
                            // Native and Wasm are sensitive regarding the expression type of value class property access,
                            // that's why we unwrap the intersection override and use the type of the value class property.
                            // See compiler/testData/codegen/box/inlineClasses/kt70461.kt
                            val finalIrType =
                                if (firSymbol.isInlineClassProperty &&
                                    property.isIntersectionOverride &&
                                    property.dispatchReceiverType is ConeIntersectionType
                                ) {
                                    property.baseForIntersectionOverride!!.returnTypeRef.toIrType()
                                } else {
                                    irType
                                }

                            IrCallImplWithShape(
                                startOffset, endOffset,
                                finalIrType,
                                getterSymbol,
                                typeArgumentsCount = property.typeParameters.size,
                                valueArgumentsCount = property.contextParameters.size,
                                contextParameterCount = property.contextParameters.size,
                                hasDispatchReceiver = property.dispatchReceiverType != null,
                                hasExtensionReceiver = property.isExtension,
                                origin = incOrDecSourceKindToIrStatementOrigin[qualifiedAccess.source?.kind]
                                    ?: augmentedAssignSourceKindToIrStatementOrigin[qualifiedAccess.source?.kind]
                                    ?: IrStatementOrigin.GET_PROPERTY,
                                superQualifierSymbol = dispatchReceiver?.superQualifierSymbolForFunctionAndPropertyAccess()
                            )
                        }

                        backingFieldSymbol != null -> IrGetFieldImpl(
                            startOffset, endOffset, backingFieldSymbol, irType,
                            superQualifierSymbol = dispatchReceiver?.superQualifierSymbolForFieldAccess(firSymbol)
                        )

                        else -> IrErrorCallExpressionImpl(
                            startOffset, endOffset, irType,
                            description = "No getter or backing field found for ${calleeReference.render()}"
                        )
                    }
                }

                is IrFieldSymbol -> IrGetFieldImpl(
                    startOffset, endOffset, irSymbol, irType,
                    superQualifierSymbol = dispatchReceiver?.superQualifierSymbolForFieldAccess(firSymbol)
                )

                is IrValueSymbol -> {
                    val variable = calleeReference.toResolvedVariableSymbol()!!.fir
                    IrGetValueImpl(
                        startOffset, endOffset,
                        // Note: there is a case with componentN function when IR type of variable differs from FIR type
                        variable.irTypeForPotentiallyComponentCall(c, predefinedType = irType),
                        irSymbol,
                        origin = if (variableAsFunctionMode) IrStatementOrigin.VARIABLE_AS_FUNCTION
                        else incOrDecSourceKindToIrStatementOrigin[qualifiedAccess.source?.kind] ?: calleeReference.statementOrigin()
                    )
                }

                is IrEnumEntrySymbol -> IrGetEnumValueImpl(startOffset, endOffset, irType, irSymbol)
                else -> generateErrorCallExpression(startOffset, endOffset, calleeReference, irType)
            }
        }.applyTypeArguments(qualifiedAccess)
            .applyReceiversAndArguments(qualifiedAccess, firSymbol, convertedExplicitReceiver)
    }

    private fun FirCallableSymbol<*>.valueParametersSize(): Int {
        return when (this) {
            is FirSyntheticPropertySymbol -> 0
            is FirNamedFunctionSymbol -> fir.valueParameters.size + fir.contextParameters.size
            is FirConstructorSymbol -> fir.valueParameters.size + fir.contextParameters.size
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
                val type = builtins.unitType
                val origin = calleeReference.statementOrigin()
                IrSetValueImpl(startOffset, endOffset, type, injectedValue.irParameterSymbol, assignedValue, origin)
            }
        }

        return null
    }

    internal fun findInjectedValue(calleeReference: FirReference) = extensions.findInjectedValue(calleeReference, conversionScope)

    fun convertToIrSetCall(
        variableAssignment: FirVariableAssignment,
        explicitReceiverExpression: IrExpression?,
    ): IrExpression = convertCatching(variableAssignment, conversionScope) {
        val type = builtins.unitType
        val calleeReference = variableAssignment.calleeReference ?: error("Reference not resolvable")
        // TODO(KT-63348): An expected type should be passed to the IrExpression conversion.
        val irRhs = visitor.convertToIrExpression(variableAssignment.rValue)

        // For compatibility with K1, special constructs on the RHS like if, when, etc. should have the type of the LHS, see KT-68401.
        if (variableAssignment.rValue.toResolvedCallableSymbol(session)?.origin == FirDeclarationOrigin.Synthetic.FakeFunction) {
            irRhs.type = variableAssignment.lValue.resolvedType.toIrType()
        }

        val irRhsWithCast = with(visitor.implicitCastInserter) {
            wrapWithImplicitCastForAssignment(variableAssignment, irRhs)
                .insertSpecialCast(
                    variableAssignment.rValue,
                    variableAssignment.rValue.resolvedType,
                    variableAssignment.lValue.resolvedType
                )
        }

        injectSetValueCall(variableAssignment, calleeReference, irRhsWithCast)?.let { return it }

        val firSymbol = calleeReference.extractDeclarationSiteSymbol(c)
        val isDynamicAccess = firSymbol?.origin == FirDeclarationOrigin.DynamicScope

        if (isDynamicAccess) {
            return convertToIrSetCallForDynamic(
                variableAssignment,
                explicitReceiverExpression,
                type,
                calleeReference,
                firSymbol,
                irRhsWithCast,
            )
        }

        val symbol = firSymbol?.toIrSymbolForSetCall(
            c,
            dispatchReceiver = extractDispatchReceiverOfAssignment(variableAssignment),
            explicitReceiver = variableAssignment.explicitReceiver,
        )
        val origin = variableAssignment.getIrAssignmentOrigin()

        val lValue = variableAssignment.unwrapLValue() ?: error("Assignment lValue unwrapped to null")
        return variableAssignment.convertWithOffsets(calleeReference) { startOffset, endOffset ->
            when (symbol) {
                is IrFieldSymbol -> IrSetFieldImpl(
                    startOffset, endOffset, symbol, type, origin,
                    superQualifierSymbol = lValue.dispatchReceiver?.superQualifierSymbolForFieldAccess(firSymbol)
                ).apply {
                    value = irRhsWithCast
                }

                is IrLocalDelegatedPropertySymbol -> {
                    val firProperty = calleeReference.toResolvedPropertySymbol()!!.fir
                    val setterSymbol = declarationStorage.findSetterOfProperty(symbol)
                    when {
                        setterSymbol != null -> IrCallImpl(
                            startOffset, endOffset, type, setterSymbol,
                            typeArgumentsCount = firProperty.typeParameters.size,
                            origin = origin,
                            superQualifierSymbol = variableAssignment.dispatchReceiver?.superQualifierSymbolForFunctionAndPropertyAccess()
                        )

                        else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
                    }
                }

                is IrPropertySymbol -> {
                    val setterSymbol = declarationStorage.findSetterOfProperty(symbol)
                    val backingFieldSymbol = declarationStorage.findBackingFieldOfProperty(symbol)
                    val firProperty = calleeReference.toResolvedPropertySymbol()!!.fir

                    when {
                        setterSymbol != null -> IrCallImplWithShape(
                            startOffset, endOffset, type, setterSymbol,
                            typeArgumentsCount = firProperty.typeParameters.size,
                            valueArgumentsCount = 1 + firProperty.contextParameters.size,
                            contextParameterCount = firProperty.contextParameters.size,
                            hasDispatchReceiver = firProperty.dispatchReceiverType != null,
                            hasExtensionReceiver = firProperty.isExtension,
                            origin = origin,
                            superQualifierSymbol = variableAssignment.dispatchReceiver?.superQualifierSymbolForFunctionAndPropertyAccess()
                        )

                        backingFieldSymbol != null -> IrSetFieldImpl(
                            startOffset, endOffset, backingFieldSymbol, type,
                            origin = null, // NB: to be consistent with PSI2IR, origin should be null here
                            superQualifierSymbol = variableAssignment.dispatchReceiver?.superQualifierSymbolForFieldAccess(firSymbol)
                        ).apply {
                            value = irRhsWithCast
                        }

                        else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
                    }
                }

                is IrSimpleFunctionSymbol -> {
                    // Assignment to synthetic var property
                    val firFunction = calleeReference.toResolvedFunctionSymbol()?.fir
                    IrCallImpl(
                        startOffset, endOffset, type, symbol,
                        typeArgumentsCount = firFunction?.typeParameters?.size ?: 0,
                        origin = origin
                    )
                }

                is IrVariableSymbol -> {
                    IrSetValueImpl(startOffset, endOffset, type, symbol, irRhsWithCast, origin)
                }

                else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
            }
        }
            .applyTypeArguments(lValue)
            .applyReceiversAndArguments(lValue, firSymbol, explicitReceiverExpression, irAssignmentRhs = irRhs)
    }

    /** Wrap an assignment - as needed - with an implicit cast to the left-hand side type. */
    private fun wrapWithImplicitCastForAssignment(assignment: FirVariableAssignment, value: IrExpression): IrExpression {
        if (value is IrTypeOperatorCall) return value // Value is already cast.

        val rValue = assignment.rValue
        if (rValue !is FirSmartCastExpression) return value // Value was not smartcast.

        // Convert the original type to not-null, as an implicit cast is not needed in this case.
        val originalType = rValue.originalExpression.resolvedType.withNullability(nullable = false, session.typeContext)
        val assignmentType = assignment.lValue.resolvedType
        if (originalType.isSubtypeOf(assignmentType, session)) return value // Cast is not needed.

        return implicitCast(value, assignmentType.toIrType(), IrTypeOperator.IMPLICIT_CAST)
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
        val coneType = annotation.annotationTypeRef.coneType.fullyExpandedType(session)
        val type = coneType.toIrType()
        if (configuration.skipBodies && type is IrErrorType) {
            // Preserve constructor calls to error classes in kapt mode because kapt stub generator will later restore them in the
            // "correct error types" mode.
            return annotation.convertWithOffsets { startOffset, endOffset ->
                @OptIn(UnsafeDuringIrConstructionAPI::class) // Error class constructor is already created, see IrErrorClassImpl.
                IrConstructorCallImpl(
                    startOffset, endOffset, type, type.symbol.owner.primaryConstructor!!.symbol,
                    typeArgumentsCount = 0, constructorTypeArgumentsCount = 0,
                    source = FirAnnotationSourceElement(annotation),
                )
            }
        }
        val annotationIsAccessible = coneType.toRegularClassSymbol(session) != null
        val symbol = type.classifierOrNull
        val firConstructorSymbol = (annotation.toResolvedCallableSymbol(session) as? FirConstructorSymbol)
            ?: run {
                // Fallback for FirReferencePlaceholderForResolvedAnnotations from jar
                val fir = coneType.toClassSymbol(session)?.fir
                var constructorSymbol: FirConstructorSymbol? = null
                fir?.unsubstitutedScope(c)?.processDeclaredConstructors {
                    if (it.fir.isPrimary && constructorSymbol == null) {
                        constructorSymbol = it
                    }
                }
                constructorSymbol
            }
        val irConstructorCall = annotation.convertWithOffsets { startOffset, endOffset ->
            when {
                // In compiler facility (debugger) scenario it's possible that annotation call is resolved in the session
                //  where this annotation was applied, but invisible in the current session.
                // In that case we shouldn't generate `IrConstructorCall`, as it will point to non-existing constructor
                //  of stub IR for not found class
                symbol !is IrClassSymbol || !annotationIsAccessible -> IrErrorCallExpressionImpl(
                    startOffset, endOffset, type, "Unresolved reference: ${annotation.render()}"
                )

                firConstructorSymbol == null -> IrErrorCallExpressionImpl(
                    startOffset, endOffset, type, "No annotation constructor found: $symbol"
                )

                else -> {
                    // Annotation constructor may come unresolved on partial module compilation (inside the IDE).
                    // Here it is resolved together with default parameter values, as transformers convert them to constants.
                    // Also see 'IrConstAnnotationTransformer'.
                    firConstructorSymbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

                    val fullyExpandedConstructorSymbol = firConstructorSymbol.let {
                        it.fir.typeAliasConstructorInfo?.originalConstructor?.unwrapUseSiteSubstitutionOverrides()?.symbol ?: it
                    }
                    val irConstructor = declarationStorage.getIrConstructorSymbol(fullyExpandedConstructorSymbol)

                    IrConstructorCallImplWithShape(
                        startOffset, endOffset, type, irConstructor,
                        // Get the number of value arguments from FIR because of a possible cycle where an annotation constructor
                        // parameter is annotated with the same annotation.
                        // In this case, the IR value parameters won't be initialized yet, and we will get 0 from
                        // `irConstructor.owner.valueParameters.size`.
                        // See KT-58294
                        valueArgumentsCount = firConstructorSymbol.valueParameterSymbols.size,
                        contextParameterCount = firConstructorSymbol.resolvedContextParameters.size,
                        hasDispatchReceiver = firConstructorSymbol.dispatchReceiverType != null,
                        hasExtensionReceiver = firConstructorSymbol.isExtension,
                        typeArgumentsCount = fullyExpandedConstructorSymbol.typeParameterSymbols.size,
                        constructorTypeArgumentsCount = 0,
                        source = FirAnnotationSourceElement(annotation),
                    )
                }
            }
        }
        return visitor.withAnnotationMode {
            val annotationCall = annotation.toAnnotationCall()
            irConstructorCall
                .applyReceiversAndArguments(annotationCall, declarationSiteSymbol = firConstructorSymbol, explicitReceiverExpression = null)
                .applyTypeArgumentsWithTypealiasConstructorRemapping(firConstructorSymbol?.fir, annotationCall?.typeArguments.orEmpty())
        }
    }

    private fun FirAnnotation.toAnnotationCall(): FirAnnotationCall? {
        if (this is FirAnnotationCall) return this
        return buildAnnotationCall {
            useSiteTarget = this@toAnnotationCall.useSiteTarget
            annotationTypeRef = this@toAnnotationCall.annotationTypeRef
            val symbol = annotationTypeRef.coneType.fullyExpandedType(session).toRegularClassSymbol(session) ?: return null

            val constructorSymbol = symbol.unsubstitutedScope(c).getDeclaredConstructors().firstOrNull() ?: return null

            val argumentToParameterToMapping = constructorSymbol.valueParameterSymbols.mapNotNull {
                val parameter = it.fir
                val argument = this@toAnnotationCall.argumentMapping.mapping[parameter.name] ?: return@mapNotNull null
                argument to parameter
            }.toMap(LinkedHashMap())
            argumentList = buildResolvedArgumentList(original = null, argumentToParameterToMapping)
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
        val classSymbol = qualifier.resolvedType.toClassLikeSymbol(session)

        if (callableReferenceAccess?.isBound == false) {
            return null
        }

        val irType = qualifier.resolvedType.toIrType()
        return qualifier.convertWithOffsets { startOffset, endOffset ->
            if (classSymbol != null) {
                IrGetObjectValueImpl(
                    startOffset, endOffset, irType,
                    classSymbol.toIrSymbol(c) as IrClassSymbol
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
        val substitutor = (call as? FirFunctionCall)?.buildSubstitutorByCalledCallable(c) ?: ConeSubstitutor.Empty
        return Triple(valueParameters, argumentMapping, substitutor)
    }

    private fun convertArgument(
        argument: FirExpression,
        parameter: FirValueParameter?,
        substitutor: ConeSubstitutor,
    ): IrExpression {
        val unsubstitutedParameterType = parameter?.returnTypeRef?.coneType?.fullyExpandedType(session)
        var irArgument = visitor.convertToIrExpression(
            argument,
            // Normally an argument type should be correct itself.
            // However, for deserialized annotations it's possible to have an imprecise Array<Any> type
            // for empty integer literal arguments.
            // In this case we have to use a parameter type itself which is more precise, like Array<String> or IntArray.
            // See KT-62598 and its fix for details.
            expectedTypeForAnnotationArgument =
                unsubstitutedParameterType.takeIf { visitor.annotationMode && unsubstitutedParameterType?.isArrayType == true }
        )

        if (unsubstitutedParameterType != null) {
            with(visitor.implicitCastInserter) {
                val argumentType = argument.resolvedType.fullyExpandedType(session)

                fun insertCastToArgument(argument: FirExpression): IrExpression = when (argument) {
                    is FirSmartCastExpression -> {
                        val substitutedParameterType = substitutor.substituteOrSelf(unsubstitutedParameterType)
                        // here we should use a substituted parameter type to properly choose the component of an intersection type
                        //  to provide a proper cast to the smartcasted type
                        irArgument.insertCastForSmartcastWithIntersection(argumentType, substitutedParameterType)
                    }
                    is FirWhenSubjectExpression -> {
                        insertCastToArgument(argument.whenRef.value.subject!!)
                    }
                    else -> irArgument
                }
                irArgument = insertCastToArgument(argument)
                // here we should pass unsubstituted parameter type to properly infer if the original type accepts null or not
                // to properly insert nullability check
                irArgument = irArgument.insertSpecialCast(argument, argumentType, unsubstitutedParameterType)
            }

            with(adapterGenerator) {
                val unwrappedParameterType =
                    if (parameter.isVararg) unsubstitutedParameterType.arrayElementType()!! else unsubstitutedParameterType
                val samFunctionType = getFunctionTypeForPossibleSamType(unwrappedParameterType)
                irArgument = irArgument.applySuspendConversionIfNeeded(
                    argument,
                    substitutor.substituteOrSelf(samFunctionType ?: unwrappedParameterType)
                )
                irArgument = irArgument.applySamConversionIfNeeded(argument)
            }
        }

        return irArgument
            .applyImplicitIntegerCoercionIfNeeded(argument, parameter)
    }

    private fun IrExpression.applyImplicitIntegerCoercionIfNeeded(
        argument: FirExpression,
        parameter: FirValueParameter?,
    ): IrExpression {
        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.ImplicitSignedToUnsignedIntegerConversion)) return this

        if (parameter == null || !parameter.isMarkedWithImplicitIntegerCoercion) return this
        if (!argument.getExpectedType(session, parameter).fullyExpandedType(session).isUnsignedTypeOrNullableUnsignedType) return this

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
                    typeArgumentsCount = 0
                ).apply {
                    arguments[0] = this@applyToElement
                }
            } else {
                this@applyToElement
            }
        }

        return when {
            parameter.isMarkedWithImplicitIntegerCoercion -> when {
                this is IrVarargImpl && argument is FirVarargArgumentsExpression -> {
                    val targetTypeFqName = varargElementType.classFqName ?: return this
                    val conversionFunctions = builtins.getNonBuiltInFunctionsWithFirCounterpartByExtensionReceiver(
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
                    val conversionFunctions = builtins.getNonBuiltInFunctionsWithFirCounterpartByExtensionReceiver(
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

    private fun IrExpression.updateStatementOrigin(newOrigin: IrStatementOrigin) {
        when (this) {
            is IrFieldAccessExpression -> origin = origin ?: newOrigin
            is IrMemberAccessExpression<*> -> origin = origin ?: newOrigin
            is IrValueAccessExpression -> origin = origin ?: newOrigin
        }
    }

    ////// TYPE ARGUMENT MAPPING

    internal fun IrExpression.applyTypeArguments(access: FirQualifiedAccessExpression): IrExpression =
        applyTypeArgumentsWithTypealiasConstructorRemapping(access.calleeReference.toResolvedCallableSymbol()?.fir, access.typeArguments)

    /**
     * Should be used whenever a [callableFir] representing a constructor produced by typealias
     * expansion gets manually unwrapped to the original constructor.
     * If you manually unwrap the constructor, you must manually remap the type arguments.
     */
    private fun IrExpression.applyTypeArgumentsWithTypealiasConstructorRemapping(
        callableFir: FirCallableDeclaration?,
        originalTypeArguments: List<FirTypeProjection>,
    ): IrExpression {
        val refinedTypeArguments = callableFir.refineTypeArgumentsOfTypeAliasConstructor(originalTypeArguments)

        return applyTypeArguments(
            refinedTypeArguments,
            (callableFir as? FirTypeParametersOwner)?.typeParameters
        )
    }

    /**
     * If the given `FirCallableDeclaration` is `FirConstructor` with `TypeAliasConstructor` origin,
     * it applies the list of passed type arguments (`originalTypeArguments`) to the corresponding type alias, expands it fully
     * and returns the list of type arguments for the resulting type.
     * Otherwise, it returns the passed type arguments.
     *
     * In the case of type alias constructor, the type arguments can be mapped arbitrarily because of the following reasons:
     *   * Changed order
     *   * Changed count by mapping K,V -> Map<K,V> or by having an unused argument
     *   * Extra type arguments that correspond to outer type parameters of an inner class (they are used while resolving, but not needed on backend)
     * We need to map them using the typealias parameters and typealias constructor substitutors.
     */
    private fun FirCallableDeclaration?.refineTypeArgumentsOfTypeAliasConstructor(originalTypeArguments: List<FirTypeProjection>): List<FirTypeRef> {
        val typeAliasConstructorInfo = (this as? FirConstructor)?.typeAliasConstructorInfo
            ?: return originalTypeArguments.map { (it as FirTypeProjectionWithVariance).typeRef }
        val typeAliasSymbol = typeAliasConstructorInfo.typeAliasSymbol

        val parametersSubstitutor = createParametersSubstitutor(
            session,
            typeAliasSymbol.typeParameterSymbols.zip(originalTypeArguments) { typeParameter, typeArgument ->
                typeParameter to typeArgument.toConeTypeProjection()
            }.toMap()
        )

        /**
         * Filter out type arguments that correspond outer type parameters of an inner class
         * To perform it, we should use two substitutors: `typeAliasConstructorInfo.substitutor` and `parametersSubstitutor`.
         * Consider the following example:
         *
         * ```kt
         * class Foo<T> {
         *     inner class Inner
         * }
         *
         * typealias InnerAlias<K> = Foo<K>.Inner
         *
         * fun test() {
         *     val foo = Foo<String>()
         *     foo.InnerAlias() // Filter out `String` type argument (String <- K <- T, where T is `FirOuterClassTypeParameterRef`)
         * }
         * ```
         *
         * In the example above, `parametersSubstitutor` holds `K -> String` substitution, `typeAliasConstructorInfo.substitutor` holds `T` -> K` substituion.
         */
        val containingInnerClass = typeAliasConstructorInfo.originalConstructor.takeIf { it.isInner }?.getContainingClass()
        val typeAliasConstructorSubstitutor = typeAliasConstructorInfo.substitutor
        val ignoredTypeArguments = if (typeAliasConstructorSubstitutor != null && containingInnerClass != null) {
            containingInnerClass.typeParameters.filterIsInstance<FirOuterClassTypeParameterRef>().mapNotNullTo(mutableSetOf()) {
                typeAliasConstructorSubstitutor.substituteOrNull(it.toConeType())
            }
        } else {
            emptySet()
        }

        return buildList {
            for ((index, typeArgument) in typeAliasSymbol.resolvedExpandedTypeRef.coneType.typeArguments.withIndex()) {
                if (ignoredTypeArguments.contains(typeArgument)) continue

                val typeProjection = parametersSubstitutor.substituteArgument(typeArgument, index) ?: typeArgument
                val typeRef = if (typeProjection is ConeKotlinType) {
                    buildResolvedTypeRef {
                        coneType = typeProjection
                    }
                } else {
                    buildErrorTypeRef {
                        diagnostic = ConeSimpleDiagnostic("Expansion contains unexpected type ${typeProjection.javaClass}")
                    }
                }
                add(typeRef)
            }
        }
    }

    private fun IrExpression.applyTypeArguments(
        typeArguments: List<FirTypeRef>?,
        typeParameters: List<FirTypeParameter>?,
    ): IrExpression {
        if (this !is IrMemberAccessExpression<*>) return this

        val argumentsCount = typeArguments?.size ?: return this
        if (argumentsCount <= this.typeArguments.size) {
            for ((index, argument) in typeArguments.withIndex()) {
                val typeParameter = typeParameters?.get(index)
                val argumentIrType = if (typeParameter?.isReified == true) {
                    argument.approximateDeclarationType(
                        session,
                        containingCallableVisibility = null,
                        isLocal = false,
                        stripEnhancedNullability = false
                    ).toIrType()
                } else {
                    argument.toIrType()
                }
                this.typeArguments[index] = argumentIrType
            }
            return this
        } else {
            val name = if (this is IrCallImpl) symbol.signature.toString() else "???"
            return IrErrorExpressionImpl(
                startOffset, endOffset, type,
                "Cannot bind $argumentsCount type arguments to $name call with ${typeArguments.size} type parameters"
            )
        }
    }

    ////// RECEIVER AND CONTEXT/VALUE ARGUMENT MAPPING

    private class ReceiverInfo(
        val hasDispatchReceiver: Boolean,
        val hasExtensionReceiver: Boolean,
    ) {
        fun contextArgumentOffset(): Int {
            return if (hasDispatchReceiver) 1 else 0
        }

        fun valueArgumentOffset(contextArgumentCount: Int): Int {
            return extensionReceiverOffset(contextArgumentCount) + (if (hasExtensionReceiver) 1 else 0)
        }

        fun extensionReceiverOffset(contextArgumentCount: Int): Int {
            return (if (hasDispatchReceiver) 1 else 0) + contextArgumentCount
        }
    }

    /**
     * @param irAssignmentRhs If passed, this expression will be the only applied argument.
     * Context arguments will still be set from [statement].
     */
    fun IrExpression.applyReceiversAndArguments(
        statement: FirStatement?,
        declarationSiteSymbol: FirCallableSymbol<*>?,
        explicitReceiverExpression: IrExpression?,
        irAssignmentRhs: IrExpression? = null,
    ): IrExpression {
        if (statement == null) return this

        val receiverInfo = putReceivers(statement, declarationSiteSymbol, explicitReceiverExpression)

        return if (irAssignmentRhs != null && this is IrMemberAccessExpression<*>) {
            val contextArgumentCount = this.putContextArguments(statement, receiverInfo)
            this.arguments[receiverInfo.valueArgumentOffset(contextArgumentCount)] = irAssignmentRhs
            this
        } else {
            applyCallArguments(statement, declarationSiteSymbol, receiverInfo)
        }
    }

    private fun IrExpression.putReceivers(
        statement: FirStatement,
        declarationSiteSymbol: FirCallableSymbol<*>?,
        explicitReceiverExpression: IrExpression?,
    ): ReceiverInfo {
        var hasDispatchReceiver = false
        var hasExtensionReceiver = false
        when (this) {
            is IrMemberAccessExpression<*> if statement is FirQualifiedAccessExpression -> {
                if (declarationSiteSymbol?.dispatchReceiverType != null) {
                    // Although type alias constructors with inner RHS work as extension functions
                    // (https://github.com/Kotlin/KEEP/blob/master/proposals/type-aliases.md#type-alias-constructors-for-inner-classes),
                    // They should work as real constructors with initialized `dispatchReceiver` instead of `extensionReceiver` on IR level.
                    val isConstructorOnTypealiasWithInnerRhs =
                        (statement.calleeReference.symbol as? FirConstructorSymbol)?.let {
                            it.origin == FirDeclarationOrigin.Synthetic.TypeAliasConstructor && it.receiverParameter != null
                        } == true
                    val baseDispatchReceiver = if (!isConstructorOnTypealiasWithInnerRhs) {
                        statement.findIrDispatchReceiver(explicitReceiverExpression)
                    } else {
                        statement.findIrExtensionReceiver(explicitReceiverExpression)
                    }
                    var firDispatchReceiver = statement.dispatchReceiver
                    if (firDispatchReceiver is FirPropertyAccessExpression && firDispatchReceiver.calleeReference is FirSuperReference) {
                        firDispatchReceiver = firDispatchReceiver.dispatchReceiver
                    }
                    val notFromAny = !declarationSiteSymbol.isFunctionFromAny()
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
                                builtins.anyType,
                                IrTypeOperator.IMPLICIT_CAST
                            )
                        }
                    hasDispatchReceiver = true
                }
                // constructors don't have extension receiver (except a case with type alias and inner RHS that is handled above),
                // but may have receiver parameter in case of inner classes
                if (declarationSiteSymbol?.receiverParameter != null && declarationSiteSymbol !is FirConstructorSymbol) {
                    val contextArgumentCount = (statement as? FirContextArgumentListOwner)?.contextArguments?.size ?: 0
                    val extensionReceiverIndex = (if (hasDispatchReceiver) 1 else 0) + contextArgumentCount
                    arguments[extensionReceiverIndex] =
                        statement.findIrExtensionReceiver(explicitReceiverExpression)?.let {
                            val symbol = statement.calleeReference.toResolvedCallableSymbol()
                                ?: error("Symbol for call ${statement.render()} not found")
                            symbol.fir.receiverParameter?.typeRef?.let { receiverType ->
                                with(visitor.implicitCastInserter) {
                                    val extensionReceiver = statement.extensionReceiver!!
                                    val substitutor = statement.buildSubstitutorByCalledCallable(c)
                                    it.insertSpecialCast(
                                        extensionReceiver,
                                        extensionReceiver.resolvedType,
                                        substitutor.substituteOrSelf(receiverType.coneType),
                                    )
                                }
                            } ?: it
                        }
                    hasExtensionReceiver = true
                }
            }

            is IrMemberAccessExpression<*> if statement is FirDelegatedConstructorCall -> {
                statement.dispatchReceiver?.let {
                    dispatchReceiver = visitor.convertToIrExpression(it)
                    hasDispatchReceiver = true
                }
            }

            is IrFieldAccessExpression -> {
                require(statement is FirQualifiedAccessExpression)
                val firDeclaration = declarationSiteSymbol!!.fir.propertyIfBackingField
                // Top-level properties are considered as static in IR
                val fieldIsStatic =
                    firDeclaration.isStatic || (firDeclaration is FirProperty && !firDeclaration.isLocal && firDeclaration.containingClassLookupTag() == null)
                if (!fieldIsStatic) {
                    receiver = statement.findIrDispatchReceiver(explicitReceiverExpression)
                    hasDispatchReceiver = true
                }
            }

            is IrConstructorCall if statement is FirAnnotationCall -> {
                // annotation calls don't have receivers
            }

            is IrGetValue, is IrSetValue, is IrGetEnumValue, is IrGetObjectValue, is IrErrorCallExpression -> {
                // no receivers
            }

            else -> error("Can't apply receivers of ${statement.javaClass.simpleName} to ${this.javaClass.simpleName}")
        }

        return ReceiverInfo(hasDispatchReceiver, hasExtensionReceiver)
    }

    private fun IrExpression.applyCallArguments(
        statement: FirStatement?,
        declarationSiteSymbol: FirCallableSymbol<*>?,
        receiverInfo: ReceiverInfo,
    ): IrExpression {
        val call = statement as? FirCall
        return when (this) {
            is IrMemberAccessExpression<*> -> {
                val contextArgumentCount = putContextArguments(statement, receiverInfo)
                if (call == null) return this
                val argumentsCount = call.arguments.size
                if (declarationSiteSymbol != null && argumentsCount <= declarationSiteSymbol.valueParametersSize()) {
                    apply {
                        val (valueParameters, argumentMapping, substitutor) = extractArgumentsMapping(call)
                        if (argumentMapping != null && (visitor.annotationMode || argumentMapping.isNotEmpty()) && valueParameters != null) {
                            return applyArgumentsWithReorderingIfNeeded(
                                argumentMapping, valueParameters, substitutor, receiverInfo, contextArgumentCount, call,
                            )
                        }
                        check(argumentsCount == 0) { "Non-empty unresolved argument list." }
                    }
                } else {
                    val calleeSymbol = (this as? IrCallImpl)?.symbol

                    @OptIn(UnsafeDuringIrConstructionAPI::class)
                    val description = calleeSymbol?.signature?.render()
                        ?: calleeSymbol?.takeIf { it.isBound }?.owner?.render()
                        ?: "???"
                    IrErrorCallExpressionImpl(
                        startOffset, endOffset, type,
                        "Cannot bind $argumentsCount arguments to '$description' call with ${declarationSiteSymbol?.valueParametersSize()} parameters"
                    ).apply {
                        for (argument in call.arguments) {
                            arguments.add(visitor.convertToIrExpression(argument))
                        }
                    }
                }
            }

            is IrErrorCallExpressionImpl -> apply {
                for (argument in call?.arguments.orEmpty()) {
                    arguments.add(visitor.convertToIrExpression(argument))
                }
            }

            else -> this
        }
    }

    private fun IrMemberAccessExpression<*>.applyArgumentsWithReorderingIfNeeded(
        argumentMapping: Map<FirExpression, FirValueParameter>,
        valueParameters: List<FirValueParameter>,
        substitutor: ConeSubstitutor,
        receiverInfo: ReceiverInfo,
        contextArgumentCount: Int,
        call: FirCall,
    ): IrExpression {
        val converted = convertArguments(argumentMapping, substitutor)
        // If none of the parameters have side effects, the evaluation order doesn't matter anyway.
        // For annotations, this is always true, since arguments have to be compile-time constants.
        if (!visitor.annotationMode && !converted.all { (_, irArgument) -> irArgument.hasNoSideEffects() } &&
            needArgumentReordering(argumentMapping.values, valueParameters)
        ) {
            return IrBlockImpl(startOffset, endOffset, type, IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL).apply {
                fun IrExpression.freeze(nameHint: String): IrExpression {
                    if (isUnchanging()) return this
                    val (variable, symbol) = conversionScope.createTemporaryVariable(this, nameHint)
                    statements.add(variable)
                    return IrGetValueImpl(startOffset, endOffset, symbol, null)
                }

                if (receiverInfo.hasDispatchReceiver) {
                    arguments[0] = arguments[0]?.freeze($$"$this")
                }

                if (receiverInfo.hasExtensionReceiver) {
                    val extensionReceiverIndex = receiverInfo.extensionReceiverOffset(contextArgumentCount)
                    arguments[extensionReceiverIndex] = arguments[extensionReceiverIndex]?.freeze($$"$receiver")
                }

                val valueArgumentOffset = receiverInfo.valueArgumentOffset(contextArgumentCount)
                for ((parameter, irArgument) in converted) {
                    arguments[valueArgumentOffset + valueParameters.indexOf(parameter)] = irArgument.freeze(parameter.name.asString())
                }
                statements.add(this@applyArgumentsWithReorderingIfNeeded)
            }
        } else {
            val valueArgumentOffset = receiverInfo.valueArgumentOffset(contextArgumentCount)
            for ((parameter, irArgument) in converted) {
                arguments[valueArgumentOffset + valueParameters.indexOf(parameter)] = irArgument
            }
            if (visitor.annotationMode) {
                val function = call.toReference(session)?.toResolvedCallableSymbol()?.fir as? FirFunction
                for ((index, parameter) in valueParameters.withIndex()) {
                    if (parameter.isVararg && !argumentMapping.containsValue(parameter)) {
                        val value = if (function?.itOrExpectHasDefaultParameterValue(index) == true) {
                            null
                        } else {
                            val varargType = parameter.returnTypeRef.toIrType()
                            IrVarargImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                varargType,
                                varargType.getArrayElementType(builtins)
                            )
                        }
                        arguments[valueArgumentOffset + index] = value
                    }
                }
            }
            return this
        }
    }

    private fun convertArguments(
        argumentMapping: Map<FirExpression, FirValueParameter>,
        substitutor: ConeSubstitutor,
    ): List<Pair<FirValueParameter, IrExpression>> =
        argumentMapping.entries.mapNotNull { (argument, parameter) ->
            if (visitor.isGetClassOfUnresolvedTypeInAnnotation(argument)) null
            else (parameter to convertArgument(argument, parameter, substitutor))
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

    /**
     * @return the number of context arguments.
     */
    private fun IrMemberAccessExpression<*>.putContextArguments(statement: FirStatement?, receiverInfo: ReceiverInfo): Int {
        if (statement !is FirContextArgumentListOwner) return 0

        val offset = receiverInfo.contextArgumentOffset()
        val contextArgumentCount = statement.contextArguments.size
        if (contextArgumentCount > 0) {
            for (index in 0 until contextArgumentCount) {
                // The order of arguments is dispatch receiver, context parameters, extension receiver, regular parameters
                arguments[offset + index] = visitor.convertToIrExpression(statement.contextArguments[index])
            }
        }

        return contextArgumentCount
    }
}

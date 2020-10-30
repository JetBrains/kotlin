/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.dispatchReceiverClassOrNull
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirDelegateFieldReference
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.calls.getExpectedTypeForSAMConversion
import org.jetbrains.kotlin.fir.resolve.calls.isFunctional
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.UNDEFINED_PARAMETER_INDEX
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi2ir.generators.hasNoSideEffects
import org.jetbrains.kotlin.types.AbstractTypeApproximator

class CallAndReferenceGenerator(
    private val components: Fir2IrComponents,
    private val visitor: Fir2IrVisitor,
    private val conversionScope: Fir2IrConversionScope
) : Fir2IrComponents by components {

    private val approximator = object : AbstractTypeApproximator(session.typeContext) {}
    private val adapterGenerator = AdapterGenerator(components, conversionScope)

    private fun FirTypeRef.toIrType(): IrType = with(typeConverter) { toIrType() }

    private fun ConeKotlinType.toIrType(): IrType = with(typeConverter) { toIrType() }

    fun convertToIrCallableReference(
        callableReferenceAccess: FirCallableReferenceAccess,
        explicitReceiverExpression: IrExpression?
    ): IrExpression {
        val symbol = callableReferenceAccess.calleeReference.toSymbolForCall(session, classifierStorage, declarationStorage, conversionScope)
        val type = callableReferenceAccess.typeRef.toIrType()
        fun propertyOrigin(): IrStatementOrigin? =
            when (callableReferenceAccess.source?.psi?.parent) {
                is KtPropertyDelegate -> IrStatementOrigin.PROPERTY_REFERENCE_FOR_DELEGATE
                else -> null
            }
        return callableReferenceAccess.convertWithOffsets { startOffset, endOffset ->
            when (symbol) {
                is IrPropertySymbol -> {
                    val referencedProperty = symbol.owner
                    val referencedPropertyGetter = referencedProperty.getter
                    val backingFieldSymbol = when {
                        referencedPropertyGetter != null -> null
                        else -> referencedProperty.backingField?.symbol
                    }
                    IrPropertyReferenceImpl(
                        startOffset, endOffset, type, symbol,
                        typeArgumentsCount = referencedPropertyGetter?.typeParameters?.size ?: 0,
                        backingFieldSymbol,
                        referencedPropertyGetter?.symbol,
                        referencedProperty.setter?.symbol,
                        propertyOrigin()
                    )
                }
                is IrLocalDelegatedPropertySymbol -> {
                    IrLocalDelegatedPropertyReferenceImpl(
                        startOffset, endOffset, type, symbol,
                        symbol.owner.delegate.symbol,
                        symbol.owner.getter.symbol as IrSimpleFunctionSymbol,
                        symbol.owner.setter?.symbol as IrSimpleFunctionSymbol?,
                        IrStatementOrigin.PROPERTY_REFERENCE_FOR_DELEGATE
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
                        symbol,
                        getter = if (referencedField.isStatic) null else propertySymbol.owner.getter?.symbol,
                        setter = if (referencedField.isStatic) null else propertySymbol.owner.setter?.symbol,
                        propertyOrigin()
                    )
                }
                is IrConstructorSymbol -> {
                    val constructor = symbol.owner
                    val klass = constructor.parent as? IrClass
                    IrFunctionReferenceImpl(
                        startOffset, endOffset, type, symbol,
                        typeArgumentsCount = constructor.typeParameters.size + (klass?.typeParameters?.size ?: 0),
                        valueArgumentsCount = constructor.valueParameters.size,
                        reflectionTarget = symbol
                    )
                }
                is IrFunctionSymbol -> {
                    assert(type.isFunctionTypeOrSubtype()) {
                        "Callable reference whose symbol refers to a function should be of functional type."
                    }
                    type as IrSimpleType
                    val function = symbol.owner
                    if (adapterGenerator.needToGenerateAdaptedCallableReference(callableReferenceAccess, type, function)) {
                        with(adapterGenerator) {
                            val adaptedType = callableReferenceAccess.typeRef.coneType.kFunctionTypeToFunctionType()
                            generateAdaptedCallableReference(callableReferenceAccess, explicitReceiverExpression, symbol, adaptedType)
                        }
                    } else {
                        IrFunctionReferenceImpl(
                            startOffset, endOffset, type, symbol,
                            typeArgumentsCount = function.typeParameters.size,
                            valueArgumentsCount = function.valueParameters.size,
                            reflectionTarget = symbol
                        )
                    }
                }
                else -> {
                    IrErrorCallExpressionImpl(
                        startOffset, endOffset, type, "Unsupported callable reference: ${callableReferenceAccess.render()}"
                    )
                }
            }
        }.applyTypeArguments(callableReferenceAccess).applyReceivers(callableReferenceAccess, explicitReceiverExpression)
    }

    private fun FirQualifiedAccess.tryConvertToSamConstructorCall(type: IrType): IrTypeOperatorCall? {
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

    private fun FirExpression.superQualifierSymbol(callSymbol: IrSymbol?): IrClassSymbol? {
        if (this !is FirQualifiedAccess) {
            return null
        }
        val dispatchReceiverReference = calleeReference
        if (dispatchReceiverReference !is FirSuperReference) {
            return null
        }
        val superTypeRef = dispatchReceiverReference.superTypeRef
        val coneSuperType = superTypeRef.coneTypeSafe<ConeClassLikeType>()
        if (coneSuperType != null) {
            val firClassSymbol = coneSuperType.fullyExpandedType(session).lookupTag.toSymbol(session) as? FirClassSymbol<*>
            if (firClassSymbol != null) {
                return classifierStorage.getIrClassSymbol(firClassSymbol)
            }
        } else if (superTypeRef is FirComposedSuperTypeRef) {
            val owner = callSymbol?.owner
            if (owner != null && owner is IrDeclaration) {
                return owner.parentClassOrNull?.symbol
            }
        }
        return null
    }

    fun convertToIrCall(
        qualifiedAccess: FirQualifiedAccess,
        typeRef: FirTypeRef,
        explicitReceiverExpression: IrExpression?,
        annotationMode: Boolean = false,
        variableAsFunctionMode: Boolean = false
    ): IrExpression {
        try {
            val type = typeRef.toIrType()
            val samConstructorCall = qualifiedAccess.tryConvertToSamConstructorCall(type)
            if (samConstructorCall != null) return samConstructorCall

            val symbol = qualifiedAccess.calleeReference.toSymbolForCall(
                session,
                classifierStorage,
                declarationStorage,
                conversionScope
            )
            return qualifiedAccess.convertWithOffsets { startOffset, endOffset ->
                val dispatchReceiver = qualifiedAccess.dispatchReceiver
                if (qualifiedAccess.calleeReference is FirSuperReference) {
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
                            origin = qualifiedAccess.calleeReference.statementOrigin(),
                            superQualifierSymbol = dispatchReceiver.superQualifierSymbol(symbol)
                        )
                    }
                    is IrLocalDelegatedPropertySymbol -> {
                        IrCallImpl(
                            startOffset, endOffset, type, symbol.owner.getter.symbol as IrSimpleFunctionSymbol,
                            typeArgumentsCount = symbol.owner.getter.typeParameters.size,
                            valueArgumentsCount = 0,
                            origin = IrStatementOrigin.GET_LOCAL_PROPERTY,
                            superQualifierSymbol = dispatchReceiver.superQualifierSymbol(symbol)
                        )
                    }
                    is IrPropertySymbol -> {
                        val getter = symbol.owner.getter
                        val backingField = symbol.owner.backingField
                        when {
                            getter != null -> IrCallImpl(
                                startOffset, endOffset, type, getter.symbol,
                                typeArgumentsCount = getter.typeParameters.size,
                                valueArgumentsCount = 0,
                                origin = IrStatementOrigin.GET_PROPERTY,
                                superQualifierSymbol = dispatchReceiver.superQualifierSymbol(symbol)
                            )
                            backingField != null -> IrGetFieldImpl(
                                startOffset, endOffset, backingField.symbol, type,
                                superQualifierSymbol = dispatchReceiver.superQualifierSymbol(symbol)
                            )
                            else -> IrErrorCallExpressionImpl(
                                startOffset, endOffset, type,
                                description = "No getter or backing field found for ${qualifiedAccess.calleeReference.render()}"
                            )
                        }
                    }
                    is IrFieldSymbol -> IrGetFieldImpl(
                        startOffset, endOffset, symbol, type,
                        origin = IrStatementOrigin.GET_PROPERTY.takeIf { qualifiedAccess.calleeReference !is FirDelegateFieldReference },
                        superQualifierSymbol = dispatchReceiver.superQualifierSymbol(symbol)
                    )
                    is IrValueSymbol -> IrGetValueImpl(
                        startOffset, endOffset, type, symbol,
                        origin = if (variableAsFunctionMode) IrStatementOrigin.VARIABLE_AS_FUNCTION
                        else qualifiedAccess.calleeReference.statementOrigin()
                    )
                    is IrEnumEntrySymbol -> IrGetEnumValueImpl(startOffset, endOffset, type, symbol)
                    else -> generateErrorCallExpression(startOffset, endOffset, qualifiedAccess.calleeReference, type)
                }
            }.applyCallArguments(qualifiedAccess as? FirCall, annotationMode)
                .applyTypeArguments(qualifiedAccess).applyReceivers(qualifiedAccess, explicitReceiverExpression)
        } catch (e: Throwable) {
            throw IllegalStateException(
                "Error while translating ${qualifiedAccess.render()} " +
                        "from file ${conversionScope.containingFileIfAny()?.name ?: "???"} to BE IR", e
            )
        }
    }

    fun convertToIrSetCall(variableAssignment: FirVariableAssignment, explicitReceiverExpression: IrExpression?): IrExpression {
        val type = irBuiltIns.unitType
        val calleeReference = variableAssignment.calleeReference
        val symbol = calleeReference.toSymbolForCall(session, classifierStorage, declarationStorage, conversionScope, preferGetter = false)
        val origin = IrStatementOrigin.EQ
        return variableAssignment.convertWithOffsets { startOffset, endOffset ->
            val assignedValue = visitor.convertToIrExpression(variableAssignment.rValue)
            when (symbol) {
                is IrFieldSymbol -> IrSetFieldImpl(startOffset, endOffset, symbol, type, origin).apply {
                    value = assignedValue
                }
                is IrLocalDelegatedPropertySymbol -> {
                    val setter = symbol.owner.setter
                    when {
                        setter != null -> IrCallImpl(
                            startOffset, endOffset, type, setter.symbol as IrSimpleFunctionSymbol,
                            typeArgumentsCount = setter.typeParameters.size,
                            valueArgumentsCount = 1,
                            origin = origin,
                            superQualifierSymbol = variableAssignment.dispatchReceiver.superQualifierSymbol(symbol)
                        ).apply {
                            putValueArgument(0, assignedValue)
                        }
                        else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
                    }
                }
                is IrPropertySymbol -> {
                    val irProperty = symbol.owner
                    val setter = irProperty.setter
                    val backingField = irProperty.backingField
                    when {
                        setter != null -> IrCallImpl(
                            startOffset, endOffset, type, setter.symbol,
                            typeArgumentsCount = setter.typeParameters.size,
                            valueArgumentsCount = 1,
                            origin = origin,
                            superQualifierSymbol = variableAssignment.dispatchReceiver.superQualifierSymbol(symbol)
                        ).apply {
                            putValueArgument(0, assignedValue)
                        }
                        backingField != null -> IrSetFieldImpl(
                            startOffset, endOffset, backingField.symbol, type,
                            origin = null, // NB: to be consistent with PSI2IR, origin should be null here
                            superQualifierSymbol = variableAssignment.dispatchReceiver.superQualifierSymbol(symbol)
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
                is IrVariableSymbol -> IrSetValueImpl(startOffset, endOffset, type, symbol, assignedValue, origin)
                else -> generateErrorCallExpression(startOffset, endOffset, calleeReference)
            }
        }.applyTypeArguments(variableAssignment).applyReceivers(variableAssignment, explicitReceiverExpression)
    }

    fun convertToIrConstructorCall(annotationCall: FirAnnotationCall): IrExpression {
        val coneType = annotationCall.annotationTypeRef.coneTypeSafe<ConeLookupTagBasedType>()
        val type = coneType?.toIrType()
        val symbol = type?.classifierOrNull
        return annotationCall.convertWithOffsets { startOffset, endOffset ->
            when (symbol) {
                is IrClassSymbol -> {
                    val irClass = symbol.owner
                    val irConstructor = (annotationCall.toResolvedCallableSymbol() as? FirConstructorSymbol)?.let {
                        this.declarationStorage.getIrConstructorSymbol(it)
                    } ?: run {
                        // Fallback for FirReferencePlaceholderForResolvedAnnotations from jar
                        val fir = coneType.lookupTag.toSymbol(session)?.fir as? FirClass<*>
                        var constructorSymbol: FirConstructorSymbol? = null
                        fir?.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = true)?.processDeclaredConstructors {
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
                        "Unresolved reference: ${annotationCall.render()}"
                    )
                }
            }
        }.applyCallArguments(annotationCall, annotationMode = true)
    }

    internal fun convertToGetObject(qualifier: FirResolvedQualifier): IrExpression {
        return convertToGetObject(qualifier, null)!!
    }

    internal fun convertToGetObject(
        qualifier: FirResolvedQualifier,
        callableReferenceAccess: FirCallableReferenceAccess?
    ): IrExpression? {
        val classSymbol = (qualifier.typeRef.coneType as? ConeClassLikeType)?.lookupTag?.toSymbol(session)
        if (callableReferenceAccess != null && classSymbol is FirRegularClassSymbol) {
            val classIdMatched = classSymbol.classId == qualifier.classId
            if (!classIdMatched) {
                // Check whether we need get companion object as dispatch receiver
                if (!classSymbol.fir.isCompanion || classSymbol.classId.outerClassId != qualifier.classId) {
                    return null
                }
                val resolvedReference = callableReferenceAccess.calleeReference as FirResolvedNamedReference
                val firCallableSymbol = resolvedReference.resolvedSymbol as FirCallableSymbol<*>
                // Make sure the reference indeed refers to a member of that companion
                if (firCallableSymbol.dispatchReceiverClassOrNull() != classSymbol.toLookupTag()) {
                    return null
                }
            }
        }
        val irType = qualifier.typeRef.toIrType()
        return qualifier.convertWithOffsets { startOffset, endOffset ->
            if (classSymbol != null) {
                IrGetObjectValueImpl(
                    startOffset, endOffset, irType,
                    classSymbol.toSymbol(this.session, this.classifierStorage) as IrClassSymbol
                )
            } else {
                IrErrorCallExpressionImpl(
                    startOffset, endOffset, irType,
                    "Resolved qualifier ${qualifier.render()} does not have correctly resolved type"
                )
            }
        }
    }

    internal fun IrExpression.applyCallArguments(call: FirCall?, annotationMode: Boolean): IrExpression {
        if (call == null) return this
        return when (this) {
            is IrMemberAccessExpression<*> -> {
                val argumentsCount = call.arguments.size
                if (argumentsCount <= valueArgumentsCount) {
                    apply {
                        val calleeReference = when (call) {
                            is FirFunctionCall -> call.calleeReference
                            is FirDelegatedConstructorCall -> call.calleeReference
                            is FirAnnotationCall -> call.calleeReference
                            else -> null
                        } as? FirResolvedNamedReference
                        val function = (calleeReference?.resolvedSymbol as? FirFunctionSymbol<*>)?.fir
                        val valueParameters = function?.valueParameters
                        val argumentMapping = call.argumentMapping
                        if (argumentMapping != null && (annotationMode || argumentMapping.isNotEmpty())) {
                            if (valueParameters != null) {
                                return applyArgumentsWithReorderingIfNeeded(argumentMapping, valueParameters, annotationMode)
                            }
                        }
                        for ((index, argument) in call.arguments.withIndex()) {
                            val valueParameter = valueParameters?.get(index)
                            val argumentExpression = convertArgument(argument, valueParameter)
                            putValueArgument(index, argumentExpression)
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
            is IrErrorCallExpressionImpl -> apply {
                for (argument in call.arguments) {
                    addArgument(visitor.convertToIrExpression(argument))
                }
            }
            else -> this
        }
    }

    private fun IrMemberAccessExpression<*>.applyArgumentsWithReorderingIfNeeded(
        argumentMapping: LinkedHashMap<FirExpression, FirValueParameter>,
        valueParameters: List<FirValueParameter>,
        annotationMode: Boolean
    ): IrExpression {
        // Assuming compile-time constants only inside annotation, we don't need a block to reorder arguments to preserve semantics.
        // But, we still need to pick correct indices for named arguments.
        if (!annotationMode &&
            needArgumentReordering(argumentMapping.values, valueParameters)
        ) {
            return IrBlockImpl(startOffset, endOffset, type, IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL).apply {
                for ((argument, parameter) in argumentMapping) {
                    val parameterIndex = valueParameters.indexOf(parameter)
                    val irArgument = convertArgument(argument, parameter)
                    if (irArgument.hasNoSideEffects()) {
                        putValueArgument(parameterIndex, irArgument)
                    } else {
                        val tempVar = declarationStorage.declareTemporaryVariable(irArgument, parameter.name.asString()).apply {
                            parent = conversionScope.parentFromStack()
                        }
                        this.statements.add(tempVar)
                        putValueArgument(parameterIndex, IrGetValueImpl(startOffset, endOffset, tempVar.symbol, null))
                    }
                }
                this.statements.add(this@applyArgumentsWithReorderingIfNeeded)
            }
        } else {
            for ((argument, parameter) in argumentMapping) {
                val argumentExpression = convertArgument(argument, parameter, annotationMode)
                putValueArgument(valueParameters.indexOf(parameter), argumentExpression)
            }
            if (annotationMode) {
                for ((index, parameter) in valueParameters.withIndex()) {
                    if (parameter.isVararg && !argumentMapping.containsValue(parameter)) {
                        val elementType = parameter.returnTypeRef.toIrType()
                        putValueArgument(
                            index,
                            IrVarargImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                elementType,
                                elementType.toArrayOrPrimitiveArrayType(irBuiltIns)
                            )
                        )
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
        annotationMode: Boolean = false
    ): IrExpression {
        var irArgument = visitor.convertToIrExpression(argument, annotationMode)
        if (parameter != null) {
            with(visitor.implicitCastInserter) {
                irArgument = irArgument.cast(argument, argument.typeRef, parameter.returnTypeRef)
            }
        }
        with(adapterGenerator) {
            irArgument = irArgument.applySuspendConversionIfNeeded(argument, parameter)
        }
        return irArgument
            .applySamConversionIfNeeded(argument, parameter)
            .applyAssigningArrayElementsToVarargInNamedForm(argument, parameter)
    }

    private fun IrExpression.applySamConversionIfNeeded(
        argument: FirExpression,
        parameter: FirValueParameter?,
        shouldUnwrapVarargType: Boolean = false
    ): IrExpression {
        if (parameter == null) {
            return this
        }
        if (this is IrVararg) {
            // element-wise SAM conversion if and only if we can build 1-to-1 mapping for elements.
            if (argument !is FirVarargArgumentsExpression || argument.arguments.size != elements.size) {
                return this
            }
            val argumentMapping = this.elements.zip(argument.arguments).toMap()
            // [IrElementTransformer] is not preferred, since it's hard to visit vararg elements only.
            val irVarargElements = elements as MutableList<IrVarargElement>
            irVarargElements.replaceAll { irVarargElement ->
                if (irVarargElement is IrExpression) {
                    val firVarargArgument =
                        argumentMapping[irVarargElement] ?: error("Can't find the original FirExpression for ${irVarargElement.render()}")
                    irVarargElement.applySamConversionIfNeeded(firVarargArgument, parameter, shouldUnwrapVarargType = true)
                } else
                    irVarargElement
            }
            return this
        }
        if (!needSamConversion(argument, parameter)) {
            return this
        }
        var samType = parameter.returnTypeRef.toIrType()
        if (shouldUnwrapVarargType) {
            samType = samType.getArrayElementType(irBuiltIns)
        }
        // Make sure the converted IrType owner indeed has a single abstract method, since FunctionReferenceLowering relies on it.
        if (!samType.isSamType) return this
        return IrTypeOperatorCallImpl(this.startOffset, this.endOffset, samType, IrTypeOperator.SAM_CONVERSION, samType, this)
    }

    private fun needSamConversion(argument: FirExpression, parameter: FirValueParameter): Boolean {
        // If the expected type is a built-in functional type, we don't need SAM conversion.
        val expectedType = argument.getExpectedTypeForSAMConversion(parameter)
        if (expectedType is ConeTypeParameterType || expectedType.isBuiltinFunctionalType(session)) {
            return false
        }
        // On the other hand, the actual type should be a functional type.
        return argument.isFunctional(session)
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

    internal fun IrExpression.applyTypeArguments(access: FirQualifiedAccess): IrExpression {
        return when (this) {
            is IrMemberAccessExpression<*> -> {
                val argumentsCount = access.typeArguments.size
                if (argumentsCount <= typeArgumentsCount) {
                    apply {
                        for ((index, argument) in access.typeArguments.withIndex()) {
                            val typeParameter = access.findTypeParameter(index)
                            val argumentFirType = (argument as FirTypeProjectionWithVariance).typeRef
                            val argumentIrType = if (typeParameter?.isReified == true) {
                                argumentFirType.approximatedIfNeededOrSelf(approximator, Visibilities.Public).toIrType()
                            } else {
                                argumentFirType.toIrType()
                            }
                            putTypeArgument(index, argumentIrType)
                        }
                    }
                } else {
                    val name = if (this is IrCallImpl) symbol.owner.name else "???"
                    IrErrorExpressionImpl(
                        startOffset, endOffset, type,
                        "Cannot bind $argumentsCount type arguments to $name call with $typeArgumentsCount type parameters"
                    )
                }
            }
            is IrBlockImpl -> apply {
                if (statements.isNotEmpty()) {
                    val lastStatement = statements.last()
                    if (lastStatement is IrExpression) {
                        statements[statements.size - 1] = lastStatement.applyTypeArguments(access)
                    }
                }
            }
            else -> this
        }
    }

    private fun FirQualifiedAccess.findTypeParameter(index: Int): FirTypeParameter? =
        ((calleeReference as? FirResolvedNamedReference)?.resolvedSymbol?.fir as? FirTypeParametersOwner)?.typeParameters?.get(index)

    private fun FirQualifiedAccess.findIrDispatchReceiver(explicitReceiverExpression: IrExpression?): IrExpression? =
        findIrReceiver(explicitReceiverExpression, isDispatch = true)

    private fun FirQualifiedAccess.findIrExtensionReceiver(explicitReceiverExpression: IrExpression?): IrExpression? =
        findIrReceiver(explicitReceiverExpression, isDispatch = false)

    internal fun FirQualifiedAccess.findIrReceiver(explicitReceiverExpression: IrExpression?, isDispatch: Boolean): IrExpression? {
        val firReceiver = if (isDispatch) dispatchReceiver else extensionReceiver
        if (firReceiver == explicitReceiver) {
            return explicitReceiverExpression
        }
        if (firReceiver is FirResolvedQualifier) {
            return convertToGetObject(firReceiver, this as? FirCallableReferenceAccess)
        }
        return firReceiver.takeIf { it !is FirNoReceiverExpression }?.let { visitor.convertToIrExpression(it) }
            ?: explicitReceiverExpression
            ?: run {
                if (this is FirCallableReferenceAccess) return null
                val name = if (isDispatch) "Dispatch" else "Extension"
                error("$name receiver expected: ${render()} to ${calleeReference.render()}")
            }
    }

    private fun IrExpression.applyReceivers(qualifiedAccess: FirQualifiedAccess, explicitReceiverExpression: IrExpression?): IrExpression {
        return when (this) {
            is IrMemberAccessExpression<*> -> {
                val ownerFunction =
                    symbol.owner as? IrFunction
                        ?: (symbol.owner as? IrProperty)?.getter
                if (ownerFunction?.dispatchReceiverParameter != null) {
                    dispatchReceiver = qualifiedAccess.findIrDispatchReceiver(explicitReceiverExpression)
                }
                if (ownerFunction?.extensionReceiverParameter != null) {
                    extensionReceiver = qualifiedAccess.findIrExtensionReceiver(explicitReceiverExpression)
                }
                this
            }
            is IrFieldAccessExpression -> {
                val ownerField = symbol.owner
                if (!ownerField.isStatic) {
                    receiver = qualifiedAccess.findIrDispatchReceiver(explicitReceiverExpression)
                }
                this
            }
            is IrBlockImpl -> apply {
                if (statements.isNotEmpty()) {
                    val lastStatement = statements.last()
                    if (lastStatement is IrExpression) {
                        statements[statements.size - 1] = lastStatement.applyReceivers(qualifiedAccess, explicitReceiverExpression)
                    }
                }
            }
            else -> this
        }
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

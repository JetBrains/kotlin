/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.inference.*
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SyntheticSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

abstract class ResolutionStage {
    abstract suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext)
}

abstract class CheckerStage : ResolutionStage()

internal object CheckExplicitReceiverConsistency : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val receiverKind = candidate.explicitReceiverKind
        val explicitReceiver = callInfo.explicitReceiver
        // TODO: add invoke cases
        when (receiverKind) {
            NO_EXPLICIT_RECEIVER -> {
                if (explicitReceiver != null && explicitReceiver !is FirResolvedQualifier && !explicitReceiver.isSuperReferenceExpression()) {
                    return sink.yieldDiagnostic(InapplicableWrongReceiver)
                }
            }
            EXTENSION_RECEIVER, DISPATCH_RECEIVER -> {
                if (explicitReceiver == null) {
                    return sink.yieldDiagnostic(InapplicableWrongReceiver)
                }
            }
            BOTH_RECEIVERS -> {
                if (explicitReceiver == null) {
                    return sink.yieldDiagnostic(InapplicableWrongReceiver)
                }
                // Here we should also check additional invoke receiver
            }
        }
    }
}

internal sealed class CheckReceivers : ResolutionStage() {
    object Dispatch : CheckReceivers() {
        override fun ExplicitReceiverKind.shouldBeCheckedAgainstImplicit(): Boolean {
            return this == EXTENSION_RECEIVER // For NO_EXPLICIT_RECEIVER we can check extension receiver only
        }

        override fun ExplicitReceiverKind.shouldBeCheckedAgainstExplicit(): Boolean {
            return this == DISPATCH_RECEIVER || this == BOTH_RECEIVERS
        }

        override fun Candidate.getReceiverType(context: ResolutionContext): ConeKotlinType? {
            return dispatchReceiverValue?.type
        }
    }

    object Extension : CheckReceivers() {
        override fun ExplicitReceiverKind.shouldBeCheckedAgainstImplicit(): Boolean {
            return this == DISPATCH_RECEIVER || this == NO_EXPLICIT_RECEIVER
        }

        override fun ExplicitReceiverKind.shouldBeCheckedAgainstExplicit(): Boolean {
            return this == EXTENSION_RECEIVER || this == BOTH_RECEIVERS
        }

        override fun Candidate.getReceiverType(context: ResolutionContext): ConeKotlinType? {
            val callableSymbol = symbol as? FirCallableSymbol<*> ?: return null
            val callable = callableSymbol.fir
            val receiverType = callable.receiverTypeRef?.coneType
            if (receiverType != null) return receiverType
            val returnTypeRef = callable.returnTypeRef as? FirResolvedTypeRef ?: return null
            if (!returnTypeRef.type.isExtensionFunctionType(context.session)) return null
            return (returnTypeRef.type.typeArguments.firstOrNull() as? ConeKotlinTypeProjection)?.type
        }
    }

    abstract fun Candidate.getReceiverType(context: ResolutionContext): ConeKotlinType?

    abstract fun ExplicitReceiverKind.shouldBeCheckedAgainstExplicit(): Boolean

    abstract fun ExplicitReceiverKind.shouldBeCheckedAgainstImplicit(): Boolean

    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val expectedReceiverType = candidate.getReceiverType(context)
        val explicitReceiverExpression = callInfo.explicitReceiver
        val explicitReceiverKind = candidate.explicitReceiverKind

        if (expectedReceiverType != null) {
            if (explicitReceiverExpression != null &&
                explicitReceiverKind.shouldBeCheckedAgainstExplicit() &&
                !explicitReceiverExpression.isSuperReferenceExpression()
            ) {
                candidate.resolveArgumentExpression(
                    candidate.csBuilder,
                    argument = explicitReceiverExpression,
                    expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType),
                    expectedTypeRef = explicitReceiverExpression.typeRef,
                    sink = sink,
                    context = context,
                    isReceiver = true,
                    isDispatch = this is Dispatch
                )
                sink.yieldIfNeed()
            } else {
                val argumentExtensionReceiverValue = candidate.implicitExtensionReceiverValue
                if (argumentExtensionReceiverValue != null && explicitReceiverKind.shouldBeCheckedAgainstImplicit()) {
                    candidate.resolvePlainArgumentType(
                        candidate.csBuilder,
                        argumentType = argumentExtensionReceiverValue.type,
                        expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType.type),
                        sink = sink,
                        context = context,
                        isReceiver = true,
                        isDispatch = this is Dispatch
                    )
                    sink.yieldIfNeed()
                }
            }
        }
    }
}

private fun FirExpression.isSuperReferenceExpression(): Boolean {
    return if (this is FirQualifiedAccessExpression) {
        val calleeReference = calleeReference
        calleeReference is FirSuperReference
    } else false
}

internal object MapArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val symbol = candidate.symbol as? FirFunctionSymbol<*> ?: return sink.reportDiagnostic(HiddenCandidate)
        val function = symbol.fir

        val mapping = mapArguments(callInfo.arguments, function)
        candidate.argumentMapping = mapping.toArgumentToParameterMapping()

        mapping.diagnostics.forEach(sink::reportDiagnostic)
        sink.yieldIfNeed()
    }
}

internal object CheckArguments : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val argumentMapping =
            candidate.argumentMapping ?: error("Argument should be already mapped while checking arguments!")
        for (argument in callInfo.arguments) {
            val parameter = argumentMapping[argument]
            candidate.resolveArgument(
                argument,
                parameter,
                isReceiver = false,
                sink = sink,
                context = context
            )
            if (candidate.system.hasContradiction) {
                sink.yieldDiagnostic(InapplicableCandidate)
            }
            sink.yieldIfNeed()
        }
    }
}

internal object EagerResolveOfCallableReferences : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (candidate.postponedAtoms.isEmpty()) return
        for (atom in candidate.postponedAtoms) {
            if (atom is ResolvedCallableReferenceAtom) {
                if (!context.bodyResolveComponents.callResolver.resolveCallableReference(candidate.csBuilder, atom)) {
                    sink.yieldDiagnostic(InapplicableCandidate)
                }
            }
        }
    }
}

internal object CheckCallableReferenceExpectedType : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val outerCsBuilder = callInfo.outerCSBuilder ?: return
        val expectedType = callInfo.expectedType
        if (candidate.symbol !is FirCallableSymbol<*>) return

        val resultingReceiverType = when (callInfo.lhs) {
            is DoubleColonLHS.Type -> callInfo.lhs.type.takeIf { callInfo.explicitReceiver !is FirResolvedQualifier }
            else -> null
        }

        val fir: FirCallableDeclaration<*> = candidate.symbol.fir

        val returnTypeRef = context.bodyResolveComponents.returnTypeCalculator.tryCalculateReturnType(fir)
        // If the expected type is a suspend function type and the current argument of interest is a function reference, we need to do
        // "suspend conversion." Here, during resolution, we bypass constraint system by making resulting type be KSuspendFunction.
        // Then, during conversion, we need to create an adapter function and replace the function reference created here with an adapted
        // callable reference.
        // TODO: should refer to LanguageVersionSettings.SuspendConversion
        val requireSuspendConversion = expectedType?.isSuspendFunctionType(callInfo.session) == true
        val resultingType: ConeKotlinType = when (fir) {
            is FirFunction -> {
                // Do not adapt references against KCallable type. It's impossible map defaults/vararg to absent parameters of KCallable.
                if (expectedType?.isKCallable(callInfo.session) == true) {
                    createFunctionalType(
                        fir.valueParameters.map { it.returnTypeRef.coneType }, resultingReceiverType, returnTypeRef.coneType,
                        isSuspend = (fir as? FirSimpleFunction)?.isSuspend == true,
                        isKFunctionType = true
                    )
                } else {
                    callInfo.session.createAdaptedKFunctionType(
                        fir, resultingReceiverType, returnTypeRef,
                        expectedParameterTypes = expectedType?.typeArguments?.dropLast(1),
                        isSuspend = (fir as? FirSimpleFunction)?.isSuspend == true || requireSuspendConversion,
                        expectedReturnType =
                        extractInputOutputTypesFromCallableReferenceExpectedType(expectedType, callInfo.session)?.outputType
                    )
                }
            }
            is FirVariable<*> -> createKPropertyType(fir, resultingReceiverType, returnTypeRef)
            else -> ConeKotlinErrorType(ConeSimpleDiagnostic("Unknown callable kind: ${fir::class}", DiagnosticKind.UnknownCallableKind))
        }.let(candidate.substitutor::substituteOrSelf)
        candidate.usesSuspendConversion = requireSuspendConversion
        candidate.resultingTypeForCallableReference = resultingType
        candidate.outerConstraintBuilderEffect = fun ConstraintSystemOperation.() {
            addOtherSystem(candidate.system.asReadOnlyStorage())

            val position = SimpleConstraintSystemConstraintPosition //TODO

            if (expectedType != null) {
                addSubtypeConstraint(resultingType, expectedType, position)
            }

            val declarationReceiverType: ConeKotlinType? =
                (fir as? FirCallableMemberDeclaration<*>)?.receiverTypeRef?.coneType
                    ?.let(candidate.substitutor::substituteOrSelf)

            if (resultingReceiverType != null && declarationReceiverType != null) {
                addSubtypeConstraint(resultingReceiverType, declarationReceiverType, position)
            }
        }

        var isApplicable = true

        outerCsBuilder.runTransaction {
            candidate.outerConstraintBuilderEffect!!(this)

            isApplicable = !hasContradiction

            false
        }

        if (!isApplicable) {
            sink.yieldDiagnostic(InapplicableCandidate)
        }
    }
}

private fun createKPropertyType(
    propertyOrField: FirVariable<*>,
    receiverType: ConeKotlinType?,
    returnTypeRef: FirResolvedTypeRef
): ConeKotlinType {
    val propertyType = returnTypeRef.type
    return createKPropertyType(
        receiverType, propertyType, isMutable = propertyOrField.isVar
    )
}

private fun FirSession.createAdaptedKFunctionType(
    function: FirFunction<*>,
    receiverType: ConeKotlinType?,
    returnTypeRef: FirResolvedTypeRef,
    expectedParameterTypes: List<ConeTypeProjection>?,
    isSuspend: Boolean,
    expectedReturnType: ConeKotlinType?
): ConeKotlinType {
    // The similar adaptations: defaults and coercion-to-unit happen at org.jetbrains.kotlin.resolve.calls.components.CallableReferencesCandidateFactory.getCallableReferenceAdaptation

    fun ConeKotlinType?.isPotentiallyArray(): Boolean =
        this != null && (this is ConeTypeVariableType || this.arrayElementType() != null)

    fun ConeKotlinType?.isPotentiallyCompatible(other: ConeKotlinType): Boolean =
        this != null &&
                (this is ConeTypeVariableType || other is ConeTypeVariableType ||
                        AbstractTypeChecker.isSubtypeOf(typeContext, this, other))

    // TODO: refactor to iterative version?
    fun adaptable(
        remainingParameters: List<FirValueParameter>,
        remainingExpectedParameterTypes: List<ConeTypeProjection>,
        beingSpread: FirValueParameter? = null,
    ): List<ConeKotlinType>? {
        if (remainingParameters.isEmpty() && remainingExpectedParameterTypes.isEmpty()) {
            return emptyList()
        }
        if (remainingParameters.isEmpty()) {
            assert(remainingExpectedParameterTypes.isNotEmpty())
            return null
        }
        if (remainingExpectedParameterTypes.isEmpty()) {
            val valueParameter = remainingParameters.first()
            return if (valueParameter.defaultValue != null || valueParameter.isVararg) {
                // Keep moving to the next expected parameter type by using the default value or assuming nothing for vararg
                adaptable(remainingParameters.drop(1), remainingExpectedParameterTypes)
            } else {
                null
            }
        }
        assert(remainingParameters.isNotEmpty() && remainingExpectedParameterTypes.isNotEmpty())
        val valueParameter = remainingParameters.first()
        val valueParameterConeType = valueParameter.returnTypeRef.coneType
        val expectedParameterType = (remainingExpectedParameterTypes.first() as? ConeKotlinTypeProjection)?.type
        if (valueParameter.isVararg) {
            val valueParameterArrayElementType = valueParameterConeType.arrayElementType()!!
            // vararg as array
            // NB: vararg should not be used in a mixed way, e.g., once spread, followed by an array use.
            if (beingSpread == null && expectedParameterType.isPotentiallyArray()) {
                val varargAsArray = adaptable(remainingParameters.drop(1), remainingExpectedParameterTypes.drop(1))
                if (varargAsArray != null) {
                    return listOf(valueParameterConeType) + varargAsArray
                }
            }
            // element case. spread or not
            if (beingSpread == valueParameter || expectedParameterType.isPotentiallyCompatible(valueParameterArrayElementType)) {
                val keepSpreading = adaptable(remainingParameters, remainingExpectedParameterTypes.drop(1), beingSpread = valueParameter)
                if (keepSpreading != null) {
                    return listOf(valueParameterArrayElementType) + keepSpreading
                }
                val endOfSpread = adaptable(remainingParameters.drop(1), remainingExpectedParameterTypes.drop(1))
                if (endOfSpread != null) {
                    return listOf(valueParameterArrayElementType) + endOfSpread
                }
            }
            val nothingPassedToVararg = adaptable(remainingParameters.drop(1), remainingExpectedParameterTypes)
            if (nothingPassedToVararg != null) {
                return nothingPassedToVararg
            }
            // Otherwise, this vararg is not adaptable.
            return null
        }
        if (expectedParameterType.isPotentiallyCompatible(valueParameterConeType)) {
            val next = adaptable(remainingParameters.drop(1), remainingExpectedParameterTypes.drop(1))
            if (next != null) {
                return listOf(valueParameterConeType) + next
            }
            // Try using the default value if any
            if (valueParameter.defaultValue != null) {
                val trialWithDefaultValue = adaptable(remainingParameters.drop(1), remainingExpectedParameterTypes)
                if (trialWithDefaultValue != null) {
                    return trialWithDefaultValue
                }
            }
        }
        // Not adaptable due to the type mismatch or lack of default value.
        return null
    }

    val parameterTypes =
        expectedParameterTypes?.let {
            adaptable(function.valueParameters, if (receiverType != null) it.drop(1) else it)
        } ?: function.valueParameters.map { it.returnTypeRef.coneType }

    val returnType =
        if (expectedReturnType != null && typeContext.run { expectedReturnType.isUnit() })
            expectedReturnType
        else
            returnTypeRef.type

    return createFunctionalType(
        parameterTypes,
        receiverType = receiverType,
        rawReturnType = returnType,
        isKFunctionType = true,
        isSuspend = isSuspend
    )
}

internal object DiscriminateSynthetics : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (candidate.symbol is SyntheticSymbol) {
            sink.reportDiagnostic(ResolvedWithLowPriority)
        }
    }
}

internal object CheckVisibility : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val visibilityChecker = callInfo.session.visibilityChecker
        val symbol = candidate.symbol
        val declaration = symbol.fir
        if (declaration is FirMemberDeclaration) {
            if (!checkVisibility(declaration, symbol, sink, candidate, visibilityChecker)) {
                return
            }
        }

        if (declaration is FirConstructor) {
            val ownerClassId = declaration.symbol.callableId.classId!!
            val provider = declaration.session.firSymbolProvider
            val classSymbol = provider.getClassLikeSymbolByFqName(ownerClassId)

            if (classSymbol is FirRegularClassSymbol) {
                if (classSymbol.fir.classKind.isSingleton) {
                    sink.yieldDiagnostic(HiddenCandidate)
                }
                checkVisibility(classSymbol.fir, classSymbol, sink, candidate, visibilityChecker)
            }
        }
    }

    private suspend fun checkVisibility(
        declaration: FirMemberDeclaration,
        symbol: AbstractFirBasedSymbol<*>,
        sink: CheckerSink,
        candidate: Candidate,
        visibilityChecker: FirVisibilityChecker
    ): Boolean {
        if (!visibilityChecker.isVisible(declaration, symbol, candidate)) {
            sink.yieldDiagnostic(HiddenCandidate)
            return false
        }
        return true
    }
}

internal object CheckLowPriorityInOverloadResolution : CheckerStage() {
    private val LOW_PRIORITY_IN_OVERLOAD_RESOLUTION_CLASS_ID: ClassId =
        ClassId(FqName("kotlin.internal"), Name.identifier("LowPriorityInOverloadResolution"))

    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val annotations = when (val fir = candidate.symbol.fir) {
            is FirSimpleFunction -> fir.annotations
            is FirProperty -> fir.annotations
            else -> return
        }

        val hasLowPriorityAnnotation = annotations.any {
            val lookupTag = it.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.lookupTag ?: return@any false
            lookupTag.classId == LOW_PRIORITY_IN_OVERLOAD_RESOLUTION_CLASS_ID
        }

        if (hasLowPriorityAnnotation) {
            sink.reportDiagnostic(ResolvedWithLowPriority)
        }
    }
}

internal object PostponedVariablesInitializerResolutionStage : ResolutionStage() {
    val BUILDER_INFERENCE_CLASS_ID: ClassId = ClassId.fromString("kotlin/BuilderInference")

    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val argumentMapping = candidate.argumentMapping ?: return
        // TODO: convert type argument mapping to map [FirTypeParameterSymbol, FirTypedProjection?]
        if (candidate.typeArgumentMapping is TypeArgumentMapping.Mapped) return
        for (parameter in argumentMapping.values) {
            if (!parameter.hasBuilderInferenceMarker()) continue
            val type = parameter.returnTypeRef.coneType
            val receiverType = type.receiverType(callInfo.session) ?: continue

            for (freshVariable in candidate.freshVariables) {
                candidate.typeArgumentMapping
                if (candidate.csBuilder.isPostponedTypeVariable(freshVariable)) continue
                if (freshVariable !is TypeParameterBasedTypeVariable) continue
                val typeParameterSymbol = freshVariable.typeParameterSymbol
                val typeHasVariable = receiverType.contains {
                    (it as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol == typeParameterSymbol
                }
                if (typeHasVariable) {
                    candidate.csBuilder.markPostponedVariable(freshVariable)
                }
            }
        }
    }

    private fun FirValueParameter.hasBuilderInferenceMarker(): Boolean {
        return this.hasAnnotation(BUILDER_INFERENCE_CLASS_ID)
    }
}

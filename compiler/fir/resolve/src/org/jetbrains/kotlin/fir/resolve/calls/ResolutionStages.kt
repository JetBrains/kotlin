/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.directExpansionType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.*
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.FirUnstableSmartcastTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasForConstructor
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SyntheticCallableId.ACCEPT_SPECIFIC_TYPE
import org.jetbrains.kotlin.fir.symbols.SyntheticSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.calls.inference.isSubtypeConstraintCompatible
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.descriptorUtil.DYNAMIC_EXTENSION_FQ_NAME
import org.jetbrains.kotlin.types.AbstractNullabilityChecker
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.util.OperatorNameConventions

abstract class ResolutionStage {
    abstract suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext)
}

abstract class CheckerStage : ResolutionStage()

internal object CheckExplicitReceiverConsistency : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val receiverKind = candidate.explicitReceiverKind
        val explicitReceiver = callInfo.explicitReceiver
        when (receiverKind) {
            NO_EXPLICIT_RECEIVER -> {
                if (explicitReceiver != null && explicitReceiver !is FirResolvedQualifier && !explicitReceiver.isSuperReferenceExpression()) {
                    return sink.yieldDiagnostic(InapplicableWrongReceiver(actualType = explicitReceiver.resolvedType))
                }
            }
            EXTENSION_RECEIVER, DISPATCH_RECEIVER -> {
                if (explicitReceiver == null) {
                    return sink.yieldDiagnostic(InapplicableWrongReceiver())
                }
            }
            BOTH_RECEIVERS -> {
                if (explicitReceiver == null) {
                    return sink.yieldDiagnostic(InapplicableWrongReceiver())
                }
            }
        }
    }
}

object CheckExtensionReceiver : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val callSite = callInfo.callSite

        if (callSite is FirImplicitInvokeCall) {
            val isInvokeFromExtensionFunctionType = candidate.isInvokeFromExtensionFunctionType
            val isImplicitInvokeCallWithExplicitReceiver = callSite.isCallWithExplicitReceiver

            // We do allow automatic conversion in the other direction, though
            if (!isInvokeFromExtensionFunctionType && isImplicitInvokeCallWithExplicitReceiver) {
                sink.reportDiagnostic(NoReceiverAllowed)
            }
        }

        val expectedReceiverType = candidate.getExpectedReceiverType() ?: return
        val expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType.type)

        // Probably, we should add an assertion here since we check consistency on the level of scope tower levels
        if (candidate.givenExtensionReceiverOptions.isEmpty()) return

        val preparedReceivers = candidate.givenExtensionReceiverOptions.map {
            candidate.prepareReceivers(it, expectedType, context)
        }

        if (preparedReceivers.size == 1) {
            resolveExtensionReceiver(preparedReceivers, candidate, expectedType, sink, context)
            return
        }

        val successfulReceivers = preparedReceivers.filter {
            candidate.system.isSubtypeConstraintCompatible(it.type, expectedType, SimpleConstraintSystemConstraintPosition)
        }

        when (successfulReceivers.size) {
            0 -> sink.yieldDiagnostic(InapplicableWrongReceiver())
            1 -> resolveExtensionReceiver(successfulReceivers, candidate, expectedType, sink, context)
            else -> sink.yieldDiagnostic(MultipleContextReceiversApplicableForExtensionReceivers())
        }
    }

    private suspend fun resolveExtensionReceiver(
        receivers: List<ReceiverDescription>,
        candidate: Candidate,
        expectedType: ConeKotlinType,
        sink: CheckerSink,
        context: ResolutionContext
    ) {
        val receiver = receivers.single()
        candidate.resolvePlainArgumentType(
            candidate.csBuilder,
            receiver.expression,
            argumentType = receiver.type,
            expectedType = expectedType,
            sink = sink,
            context = context,
            isReceiver = true,
            isDispatch = false,
        )

        candidate.chosenExtensionReceiver = receiver.expression

        sink.yieldIfNeed()
    }

    private fun Candidate.getExpectedReceiverType(): ConeKotlinType? {
        val callableSymbol = symbol as? FirCallableSymbol<*> ?: return null
        return callableSymbol.fir.receiverParameter?.typeRef?.coneType
    }
}

private fun Candidate.prepareReceivers(
    argumentExtensionReceiver: FirExpression,
    expectedType: ConeKotlinType,
    context: ResolutionContext,
): ReceiverDescription {
    val argumentType = captureFromTypeParameterUpperBoundIfNeeded(
        argumentType = argumentExtensionReceiver.resolvedType,
        expectedType = expectedType,
        session = context.session
    ).let { prepareCapturedType(it, context) }

    return ReceiverDescription(argumentExtensionReceiver, argumentType)
}

private class ReceiverDescription(
    val expression: FirExpression,
    val type: ConeKotlinType,
)

object CheckDispatchReceiver : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val explicitReceiverExpression = callInfo.explicitReceiver
        if (explicitReceiverExpression.isSuperCall()) {
            val status = candidate.symbol.fir as? FirMemberDeclaration
            if (status?.modality == Modality.ABSTRACT) {
                sink.reportDiagnostic(ResolvedWithLowPriority)
            }
        }

        val dispatchReceiverValueType = candidate.dispatchReceiver?.resolvedType ?: return

        // TODO (KT-63959): Actually, we should treat stub types as non-nullable for the isReceiverNullable check
        // Otherwise, we won't able to resolve to member toString/hashCode due to UnsafeCall error
        // It was possible in K1, due to the fact that K1 doesn't use AbstractNullabilityChecker directly
        // But, AbstractNullabilityChecker.isSubtypeOfAny doesn't respect stubTypeEqualToAnything
        val isStubType = dispatchReceiverValueType is ConeStubTypeForChainInference
        val isReceiverNullable =
            !AbstractNullabilityChecker.isSubtypeOfAny(context.session.typeContext, dispatchReceiverValueType) && !isStubType


        val isCandidateFromUnstableSmartcast =
            (candidate.originScope as? FirUnstableSmartcastTypeScope)?.isSymbolFromUnstableSmartcast(candidate.symbol) == true

        val smartcastedReceiver = when (explicitReceiverExpression) {
            is FirCheckNotNullCall -> explicitReceiverExpression.argument
            else -> explicitReceiverExpression
        } as? FirSmartCastExpression

        if (smartcastedReceiver != null &&
            !smartcastedReceiver.isStable &&
            (isCandidateFromUnstableSmartcast || (isReceiverNullable && !smartcastedReceiver.smartcastType.coneType.canBeNull(callInfo.session)))
        ) {
            val dispatchReceiverType = (candidate.symbol as? FirCallableSymbol<*>)?.dispatchReceiverType?.let {
                context.session.typeApproximator.approximateToSuperType(
                    it,
                    TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
                ) ?: it
            }
            val targetType =
                dispatchReceiverType ?: smartcastedReceiver.smartcastType.coneType
            sink.yieldDiagnostic(
                UnstableSmartCast(
                    smartcastedReceiver,
                    targetType,
                    context.session.typeContext.isTypeMismatchDueToNullability(
                        smartcastedReceiver.originalExpression.resolvedType,
                        targetType
                    )
                )
            )
        } else if (isReceiverNullable) {
            sink.yieldDiagnostic(UnsafeCall(dispatchReceiverValueType))
        }
    }
}

object CheckContextReceivers : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val contextReceiverExpectedTypes = (candidate.symbol as? FirCallableSymbol<*>)?.fir?.contextReceivers?.map {
            candidate.substitutor.substituteOrSelf(it.typeRef.coneType)
        }?.takeUnless { it.isEmpty() } ?: return

        if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) {
            sink.reportDiagnostic(UnsupportedContextualDeclarationCall)
            return
        }

        val receiverGroups: List<List<FirExpression>> =
            context.bodyResolveContext.towerDataContext.towerDataElements.asReversed().mapNotNull { towerDataElement ->
                towerDataElement.implicitReceiver?.receiverExpression?.let(::listOf)
                    ?: towerDataElement.contextReceiverGroup?.map { it.receiverExpression }
            }

        val resultingContextReceiverArguments = mutableListOf<FirExpression>()
        for (expectedType in contextReceiverExpectedTypes) {
            val matchingReceivers = candidate.findClosestMatchingReceivers(expectedType, receiverGroups, context)
            when (matchingReceivers.size) {
                0 -> {
                    sink.reportDiagnostic(NoApplicableValueForContextReceiver(expectedType))
                    return
                }
                1 -> {
                    val matchingReceiver = matchingReceivers.single()
                    resultingContextReceiverArguments.add(matchingReceiver.expression)
                    candidate.system.addSubtypeConstraint(matchingReceiver.type, expectedType, SimpleConstraintSystemConstraintPosition)
                }
                else -> {
                    sink.reportDiagnostic(AmbiguousValuesForContextReceiverParameter(expectedType))
                    return
                }
            }
        }

        candidate.contextReceiverArguments = resultingContextReceiverArguments
    }
}

object TypeVariablesInExplicitReceivers : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (callInfo.callSite.isAnyOfDelegateOperators()) return

        val explicitReceiver = callInfo.explicitReceiver ?: return checkOtherCases(candidate)

        val typeVariableType = explicitReceiver.resolvedType.obtainTypeVariable() ?: return checkOtherCases(candidate)
        val typeParameter =
            (typeVariableType.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag)?.typeParameterSymbol?.fir
                ?: return checkOtherCases(candidate)

        sink.reportDiagnostic(TypeVariableAsExplicitReceiver(explicitReceiver, typeParameter))
    }

    private fun checkOtherCases(candidate: Candidate) {
        require(candidate.chosenExtensionReceiverExpression()?.resolvedType?.obtainTypeVariable() == null) {
            "Found TV in extension receiver of $candidate"
        }

        require(candidate.dispatchReceiverExpression()?.resolvedType?.obtainTypeVariable() == null) {
            "Found TV in dispatch receiver of $candidate"
        }
    }

    private fun ConeKotlinType.obtainTypeVariable(): ConeTypeVariableType? = when (this) {
        is ConeFlexibleType -> lowerBound.obtainTypeVariable()
        is ConeTypeVariableType -> this
        is ConeDefinitelyNotNullType -> original.obtainTypeVariable()
        is ConeIntersectionType -> intersectedTypes.firstNotNullOfOrNull { it.obtainTypeVariable() }
        else -> null
    }
}

private fun Candidate.findClosestMatchingReceivers(
    expectedType: ConeKotlinType,
    receiverGroups: List<List<FirExpression>>,
    context: ResolutionContext,
): List<ReceiverDescription> {
    for (receiverGroup in receiverGroups) {
        val currentResult =
            receiverGroup
                .map { prepareReceivers(it, expectedType, context) }
                .filter { system.isSubtypeConstraintCompatible(it.type, expectedType, SimpleConstraintSystemConstraintPosition) }

        if (currentResult.isNotEmpty()) return currentResult
    }

    return emptyList()
}

/**
 * See https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-dsl-marker/ for more details and
 * /compiler/testData/diagnostics/tests/resolve/dslMarker for the test files.
 */
object CheckDslScopeViolation : ResolutionStage() {
    private val dslMarkerClassId = ClassId.fromString("kotlin/DslMarker")

    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        fun checkReceiver(receiver: FirExpression?) {
            val thisReference = receiver?.toReference(context.session) as? FirThisReference ?: return
            if (thisReference.isImplicit) {
                checkImpl(
                    candidate,
                    sink,
                    context,
                    { getDslMarkersOfImplicitReceiver(thisReference.boundSymbol, receiver.resolvedType, context) }
                ) {
                    // Here we rely on the fact that receiver expression of implicit receiver value can not be changed
                    //   during resolution of one single call
                    it.receiverExpression == receiver
                }
            }
        }
        checkReceiver(candidate.dispatchReceiver)
        checkReceiver(candidate.chosenExtensionReceiver)

        // For value of builtin functional type with implicit extension receiver, the receiver is passed as the first argument rather than
        // an extension receiver of the `invoke` call. Hence, we need to specially handle this case.
        // For example, consider the following
        // ```
        // @DslMarker
        // annotation class MyDsl
        //
        // @MyDsl
        // class X
        // fun x(block: X.() -> Unit) {}
        //
        // @MyDsl
        // class A
        // fun a(block: A.() -> Unit) {}
        //
        // val useX: X.() -> Unit
        //
        // fun test() {
        //   x {
        //     a {
        //       useX() // DSL_SCOPE_VIOLATION because `useX` needs "extension receiver" `X`.
        //     }
        //   }
        // }
        // ```
        // `useX()` is a call to `invoke` with `useX` as the dispatch receiver. In the FIR tree, extension receiver is represented as an
        // implicit `this` expression passed as the first argument.
        if (candidate.dispatchReceiver?.resolvedType?.fullyExpandedType(context.session)?.isSomeFunctionType(context.session) == true &&
            (candidate.symbol as? FirNamedFunctionSymbol)?.name == OperatorNameConventions.INVOKE
        ) {
            val firstArg = candidate.argumentMapping?.keys?.firstOrNull() as? FirThisReceiverExpression ?: return
            if (!firstArg.isImplicit) return
            checkImpl(
                candidate,
                sink,
                context,
                { firstArg.getDslMarkersOfThisReceiverExpression(context) }
            ) { it.boundSymbol == firstArg.calleeReference.boundSymbol }
        }
    }

    /**
     * Checks whether the implicit receiver (represented as an object of type `T`) violates DSL scope rules.
     */
    private fun checkImpl(
        candidate: Candidate,
        sink: CheckerSink,
        context: ResolutionContext,
        dslMarkersProvider: () -> Set<ClassId>,
        isImplicitReceiverMatching: (ImplicitReceiverValue<*>) -> Boolean,
    ) {
        val resolvedReceiverIndex = context.bodyResolveContext.implicitReceiverStack.indexOfFirst { isImplicitReceiverMatching(it) }
        if (resolvedReceiverIndex == -1) return
        val closerReceivers = context.bodyResolveContext.implicitReceiverStack.drop(resolvedReceiverIndex + 1)
        if (closerReceivers.isEmpty()) return
        val dslMarkers = dslMarkersProvider()
        if (dslMarkers.isEmpty()) return
        if (closerReceivers.any { receiver -> receiver.getDslMarkersOfImplicitReceiver(context).any { it in dslMarkers } }) {
            sink.reportDiagnostic(DslScopeViolation(candidate.symbol))
        }
    }

    private fun ImplicitReceiverValue<*>.getDslMarkersOfImplicitReceiver(context: ResolutionContext): Set<ClassId> {
        return CheckDslScopeViolation.getDslMarkersOfImplicitReceiver(boundSymbol, type, context)
    }

    private fun getDslMarkersOfImplicitReceiver(
        boundSymbol: FirBasedSymbol<*>?,
        type: ConeKotlinType,
        context: ResolutionContext,
    ): Set<ClassId> {
        return buildSet {
            (boundSymbol as? FirAnonymousFunctionSymbol)?.fir?.matchingParameterFunctionType?.let {
                // collect annotations in the function type at declaration site. For example, the `@A` and `@B` in the following code.
                // ```
                // fun <T> body(block: @A ((@B T).() -> Unit)) { ... }
                // ```

                // Collect the annotation on the function type, or `@A` in the example above.
                collectDslMarkerAnnotations(context, it.attributes.customAnnotations)

                // Collect the annotation on the extension receiver, or `@B` in the example above.
                if (CompilerConeAttributes.ExtensionFunctionType in it.attributes) {
                    it.typeArguments.firstOrNull()?.type?.let { receiverType ->
                        collectDslMarkerAnnotations(context, receiverType)
                    }
                }
            }

            // Collect annotations on the actual receiver type.
            collectDslMarkerAnnotations(context, type)
        }
    }

    private fun FirThisReceiverExpression.getDslMarkersOfThisReceiverExpression(context: ResolutionContext): Set<ClassId> {
        return buildSet {
            collectDslMarkerAnnotations(context, resolvedType)
        }
    }

    private fun MutableSet<ClassId>.collectDslMarkerAnnotations(context: ResolutionContext, type: ConeKotlinType) {
        collectDslMarkerAnnotations(context, type.attributes.customAnnotations)
        when (type) {
            is ConeFlexibleType -> {
                collectDslMarkerAnnotations(context, type.lowerBound)
                collectDslMarkerAnnotations(context, type.upperBound)
            }
            is ConeCapturedType -> {
                if (type.constructor.projection.kind == ProjectionKind.OUT) {
                    type.constructor.supertypes?.forEach { collectDslMarkerAnnotations(context, it) }
                }
            }
            is ConeDefinitelyNotNullType -> collectDslMarkerAnnotations(context, type.original)
            is ConeIntersectionType -> type.intersectedTypes.forEach { collectDslMarkerAnnotations(context, it) }
            is ConeClassLikeType -> {
                val classDeclaration = type.toSymbol(context.session) ?: return
                collectDslMarkerAnnotations(context, classDeclaration.resolvedAnnotationsWithClassIds)
                when (classDeclaration) {
                    is FirClassSymbol -> {
                        for (superType in classDeclaration.resolvedSuperTypes) {
                            collectDslMarkerAnnotations(context, superType)
                        }
                    }
                    is FirTypeAliasSymbol -> {
                        type.directExpansionType(context.session)?.let {
                            collectDslMarkerAnnotations(context, it)
                        }
                    }
                }
            }
            else -> return
        }
    }

    private fun MutableSet<ClassId>.collectDslMarkerAnnotations(context: ResolutionContext, annotations: Collection<FirAnnotation>) {
        for (annotation in annotations) {
            val annotationClass =
                annotation.annotationTypeRef.coneType.fullyExpandedType(context.session).toSymbol(context.session) as? FirClassSymbol
                    ?: continue
            if (annotationClass.hasAnnotation(dslMarkerClassId, context.session)) {
                add(annotationClass.classId)
            }
        }
    }
}

private fun FirExpression?.isSuperCall(): Boolean {
    if (this !is FirQualifiedAccessExpression) return false
    return calleeReference is FirSuperReference
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
        val mapping = context.bodyResolveComponents.mapArguments(
            callInfo.arguments,
            function,
            candidate.originScope,
            callSiteIsOperatorCall = (callInfo.callSite as? FirFunctionCall)?.origin == FirFunctionCallOrigin.Operator
        )
        candidate.argumentMapping = mapping.toArgumentToParameterMapping()
        candidate.numDefaults = mapping.numDefaults()

        mapping.diagnostics.forEach(sink::reportDiagnostic)
        sink.yieldIfNeed()
    }
}

internal object CheckArguments : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        candidate.symbol.lazyResolveToPhase(FirResolvePhase.STATUS)
        val argumentMapping =
            candidate.argumentMapping ?: error("Argument should be already mapped while checking arguments!")
        val isInvokeFromExtensionFunctionType = candidate.isInvokeFromExtensionFunctionType

        for ((index, argument) in callInfo.arguments.withIndex()) {
            candidate.resolveArgument(
                callInfo,
                argument,
                argumentMapping[argument],
                isReceiver = index == 0 && isInvokeFromExtensionFunctionType,
                sink = sink,
                context = context
            )
        }

        when {
            candidate.system.hasContradiction && callInfo.arguments.isNotEmpty() -> {
                sink.yieldDiagnostic(InapplicableCandidate)
            }

            // Logic description: only candidates from Kotlin, but using Java SAM types, are discriminated
            candidate.shouldHaveLowPriorityDueToSAM(context.bodyResolveComponents) -> {
                if (argumentMapping.values.any {
                        val coneType = it.returnTypeRef.coneType
                        context.bodyResolveComponents.samResolver.isSamType(coneType) &&
                                // Candidate is not from Java, so no flexible types are possible here
                                coneType.toRegularClassSymbol(context.session)?.isJavaOrEnhancement == true
                    }
                ) {
                    sink.markCandidateForCompatibilityResolve(context)
                }
            }
        }
    }
}

private val Candidate.isInvokeFromExtensionFunctionType: Boolean
    get() = explicitReceiverKind == DISPATCH_RECEIVER
            && dispatchReceiver?.resolvedType?.fullyExpandedType(this.callInfo.session)?.isExtensionFunctionType == true
            && (symbol as? FirNamedFunctionSymbol)?.name == OperatorNameConventions.INVOKE

internal fun Candidate.shouldHaveLowPriorityDueToSAM(bodyResolveComponents: BodyResolveComponents): Boolean {
    if (!usesSAM || isJavaApplicableCandidate()) return false
    return argumentMapping!!.values.any {
        val coneType = it.returnTypeRef.coneType
        bodyResolveComponents.samResolver.isSamType(coneType) &&
                // Candidate is not from Java, so no flexible types are possible here
                coneType.toRegularClassSymbol(bodyResolveComponents.session)?.isJavaOrEnhancement == true
    }
}

private fun Candidate.isJavaApplicableCandidate(): Boolean {
    val symbol = symbol as? FirFunctionSymbol ?: return false
    if (symbol.isJavaOrEnhancement) return true
    if (originScope !is FirTypeScope) return false
    // Note: constructor can also be Java applicable with enhancement origin, but it doesn't have overridden functions
    // See samConstructorVsFun.kt diagnostic test
    if (symbol !is FirNamedFunctionSymbol) return false

    var result = false

    originScope.processOverriddenFunctions(symbol) {
        if (it.isJavaOrEnhancement) {
            result = true
            ProcessorAction.STOP
        } else {
            ProcessorAction.NEXT
        }
    }

    return result
}

private fun CheckerSink.markCandidateForCompatibilityResolve(context: ResolutionContext) {
    if (context.session.languageVersionSettings.supportsFeature(LanguageFeature.DisableCompatibilityModeForNewInference)) return
    reportDiagnostic(LowerPriorityToPreserveCompatibilityDiagnostic)
}

internal object EagerResolveOfCallableReferences : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (candidate.postponedAtoms.isEmpty()) return
        for (atom in candidate.postponedAtoms) {
            if (atom is ResolvedCallableReferenceAtom) {
                val (applicability, success) =
                    context.bodyResolveComponents.callResolver.resolveCallableReference(
                        candidate, atom, hasSyntheticOuterCall = candidate.callInfo.name == ACCEPT_SPECIFIC_TYPE.callableName
                    )
                if (!success) {
                    // If the resolution was unsuccessful, we ensure that an error will be reported for the callable reference
                    // during completion by using the `resultingReference` of the postponed atom.
                    // We assert that the `resultingReference` is set to an error reference and the atom is in fact postponed.
                    check(atom.resultingReference is FirErrorReferenceWithCandidate)
                    if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) check(atom in candidate.postponedAtoms)

                    sink.yieldDiagnostic(UnsuccessfulCallableReferenceAtom)
                } else when (applicability) {
                    CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY ->
                        sink.reportDiagnostic(LowerPriorityToPreserveCompatibilityDiagnostic)
                    else -> {
                    }
                }
            }
        }
    }
}

internal object DiscriminateSynthetics : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (candidate.symbol is SyntheticSymbol) {
            sink.reportDiagnostic(ResolvedWithSynthetic)
        }
    }
}

internal object CheckVisibility : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val visibilityChecker = callInfo.session.visibilityChecker
        val symbol = candidate.symbol
        val declaration = symbol.fir
        if (declaration is FirMemberDeclaration && declaration !is FirConstructor) {
            if (!visibilityChecker.isVisible(declaration, candidate)) {
                sink.yieldDiagnostic(VisibilityError)
                return
            }

            if (symbol is FirPropertySymbol && callInfo.callSite is FirVariableAssignment) {
                val setterFir = symbol.setterSymbol?.fir ?: symbol.originalForSubstitutionOverride?.setterSymbol?.fir
                if (setterFir != null && !visibilityChecker.isVisible(setterFir, candidate)) {
                    sink.yieldDiagnostic(SetterVisibilityError)
                    return
                }
            }
        }

        if (declaration is FirConstructor) {
            val classSymbol = declaration.returnTypeRef.coneTypeUnsafe<ConeClassLikeType>().lookupTag.toSymbol(context.session)

            if (classSymbol is FirRegularClassSymbol) {
                if (classSymbol.fir.classKind.isSingleton) {
                    sink.yieldDiagnostic(HiddenCandidate)
                }

                val visible = visibilityChecker.isVisible(
                    declaration,
                    candidate.callInfo,
                    dispatchReceiver = null
                )
                if (!visible) {
                    sink.yieldDiagnostic(VisibilityError)
                }
            }

            val typeAlias = declaration.typeAliasForConstructor
            if (typeAlias != null) {
                if (!visibilityChecker.isVisible(typeAlias.fir, candidate)) {
                    sink.yieldDiagnostic(VisibilityError)
                }
            }
        }
    }
}

internal object CheckLowPriorityInOverloadResolution : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val annotations = when (val fir = candidate.symbol.fir) {
            is FirSimpleFunction -> fir.annotations
            is FirProperty -> fir.annotations
            is FirConstructor -> fir.annotations
            else -> return
        }

        if (hasLowPriorityAnnotation(annotations)) {
            sink.reportDiagnostic(ResolvedWithLowPriority)
        }
    }
}

internal object CheckIncompatibleTypeVariableUpperBounds : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) =
        with(candidate.system.asConstraintSystemCompleterContext()) {
            for (variableWithConstraints in candidate.system.notFixedTypeVariables.values) {
                val upperTypes = variableWithConstraints.constraints.extractUpperTypesToCheckIntersectionEmptiness()

                // TODO: consider reporting errors on bounded type variables by incompatible types but with other lower constraints, KT-59676
                if (upperTypes.size <= 1 || variableWithConstraints.constraints.any { it.kind.isLower() })
                    continue

                val emptyIntersectionTypeInfo = candidate.system.getEmptyIntersectionTypeKind(upperTypes) ?: continue
                if (variableWithConstraints.constraints.any {
                        it.kind == ConstraintKind.EQUALITY &&
                                it.position.initialConstraint.position is ConeExplicitTypeParameterConstraintPosition
                    }
                ) {
                    return
                }
                sink.yieldDiagnostic(
                    @Suppress("UNCHECKED_CAST")
                    InferredEmptyIntersectionDiagnostic(
                        upperTypes as List<ConeKotlinType>,
                        emptyIntersectionTypeInfo.casingTypes.toList() as List<ConeKotlinType>,
                        variableWithConstraints.typeVariable as ConeTypeVariable,
                        emptyIntersectionTypeInfo.kind,
                        isError = context.session.languageVersionSettings.supportsFeature(
                            LanguageFeature.ForbidInferringTypeVariablesIntoEmptyIntersection
                        )
                    )
                )
            }
        }
}

internal object CheckCallModifiers : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (callInfo.callSite is FirFunctionCall) {
            when (val functionSymbol = candidate.symbol) {
                is FirNamedFunctionSymbol -> when {
                    callInfo.callSite.origin == FirFunctionCallOrigin.Infix && !functionSymbol.fir.isInfix ->
                        sink.reportDiagnostic(InfixCallOfNonInfixFunction(functionSymbol))
                    callInfo.callSite.origin == FirFunctionCallOrigin.Operator && !functionSymbol.fir.isOperator ->
                        sink.reportDiagnostic(OperatorCallOfNonOperatorFunction(functionSymbol))
                    callInfo.isImplicitInvoke && !functionSymbol.fir.isOperator ->
                        sink.reportDiagnostic(OperatorCallOfNonOperatorFunction(functionSymbol))
                }
                is FirConstructorSymbol -> {
                    if (callInfo.callSite.origin == FirFunctionCallOrigin.Operator) {
                        sink.reportDiagnostic(OperatorCallOfConstructor(functionSymbol))
                    }
                }
            }
        }
    }
}

internal object CheckHiddenDeclaration : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val symbol = candidate.symbol as? FirCallableSymbol<*> ?: return
        /** Actual declarations are checked by [FirDeprecationChecker] */
        if (symbol.isActual) return
        val deprecation = symbol.getDeprecation(context.session, callInfo.callSite)
        if (deprecation?.deprecationLevel == DeprecationLevelValue.HIDDEN || isHiddenForThisCallSite(symbol, callInfo, candidate, context.session, sink)) {
            sink.yieldDiagnostic(HiddenCandidate)
        }
    }

    private fun isHiddenForThisCallSite(
        symbol: FirCallableSymbol<*>,
        callInfo: CallInfo,
        candidate: Candidate,
        session: FirSession,
        sink: CheckerSink,
    ): Boolean {
        /**
         * The logic for synthetic properties itself is in [FirSyntheticPropertiesScope.computeGetterCompatibility].
         */
        if (symbol is FirSimpleSyntheticPropertySymbol && symbol.deprecatedOverrideOfHidden) {
            sink.reportDiagnostic(CallToDeprecatedOverrideOfHidden)
        }

        if (symbol.fir.dispatchReceiverType == null || symbol !is FirNamedFunctionSymbol) return false
        val isSuperCall = callInfo.callSite.isSuperCall(session)
        if (symbol.hiddenStatusOfCall(isSuperCall, isCallToOverride = false) == CallToPotentiallyHiddenSymbolResult.Hidden) return true

        val scope = candidate.originScope as? FirTypeScope ?: return false

        var hidden = false
        var deprecated = false
        scope.processOverriddenFunctions(symbol) {
            val result = it.hiddenStatusOfCall(isSuperCall, isCallToOverride = true)
            if (result != CallToPotentiallyHiddenSymbolResult.Visible) {
                if (result == CallToPotentiallyHiddenSymbolResult.Hidden) {
                    hidden = true
                } else if (result == CallToPotentiallyHiddenSymbolResult.VisibleWithDeprecation) {
                    deprecated = true
                }
                ProcessorAction.STOP
            } else {
                ProcessorAction.NEXT
            }
        }

        if (deprecated) {
            sink.reportDiagnostic(CallToDeprecatedOverrideOfHidden)
        }

        return hidden
    }
}

internal fun FirElement.isSuperCall(session: FirSession): Boolean =
    this is FirQualifiedAccessExpression && explicitReceiver?.toReference(session) is FirSuperReference

private val DYNAMIC_EXTENSION_ANNOTATION_CLASS_ID: ClassId = ClassId.topLevel(DYNAMIC_EXTENSION_FQ_NAME)

internal object ProcessDynamicExtensionAnnotation : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (candidate.symbol.origin === FirDeclarationOrigin.DynamicScope) return
        val extensionReceiver = candidate.chosenExtensionReceiver ?: return
        val argumentIsDynamic = extensionReceiver.resolvedType is ConeDynamicType
        val parameterIsDynamic = (candidate.symbol as? FirCallableSymbol)?.resolvedReceiverTypeRef?.type is ConeDynamicType
        if (parameterIsDynamic != argumentIsDynamic ||
            parameterIsDynamic && !candidate.symbol.hasAnnotation(DYNAMIC_EXTENSION_ANNOTATION_CLASS_ID, context.session)
        ) {
            candidate.addDiagnostic(HiddenCandidate)
        }
    }
}

internal object LowerPriorityIfDynamic : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        when {
            candidate.symbol.origin is FirDeclarationOrigin.DynamicScope ->
                candidate.addDiagnostic(LowerPriorityForDynamic)
            candidate.callInfo.isImplicitInvoke && candidate.callInfo.explicitReceiver?.resolvedType is ConeDynamicType ->
                candidate.addDiagnostic(LowerPriorityForDynamic)
        }
    }
}

internal object ConstraintSystemForks : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (candidate.system.hasContradiction) return

        candidate.system.checkIfForksMightBeSuccessfullyResolved()?.let { csError ->
            sink.yieldDiagnostic(InferenceError(csError))
        }
    }
}

internal object TypeParameterAsCallable : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val symbol = candidate.symbol
        if (symbol is FirTypeParameterSymbol && !(callInfo.isUsedAsGetClassReceiver && symbol.isReified)) {
            sink.yieldDiagnostic(TypeParameterAsExpression)
        }
    }
}

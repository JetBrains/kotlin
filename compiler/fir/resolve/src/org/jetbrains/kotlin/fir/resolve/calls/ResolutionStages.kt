/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInfix
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.matchingParameterFunctionType
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.directExpansionType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.inference.ResolvedCallableReferenceAtom
import org.jetbrains.kotlin.fir.resolve.inference.csBuilder
import org.jetbrains.kotlin.fir.resolve.inference.hasBuilderInferenceAnnotation
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SyntheticSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.calls.inference.isSubtypeConstraintCompatible
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.descriptorUtil.DYNAMIC_EXTENSION_FQ_NAME
import org.jetbrains.kotlin.types.AbstractNullabilityChecker
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
        // TODO: add invoke cases
        when (receiverKind) {
            NO_EXPLICIT_RECEIVER -> {
                if (explicitReceiver != null && explicitReceiver !is FirResolvedQualifier && !explicitReceiver.isSuperReferenceExpression()) {
                    return sink.yieldDiagnostic(InapplicableWrongReceiver(actualType = explicitReceiver.typeRef.coneTypeSafe()))
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
                // Here we should also check additional invoke receiver
            }
        }
    }
}

object CheckExtensionReceiver : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
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
        argumentType = argumentExtensionReceiver.typeRef.coneType,
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

        val dispatchReceiverValueType = candidate.dispatchReceiver?.typeRef?.coneType ?: return
        val isReceiverNullable = !AbstractNullabilityChecker.isSubtypeOfAny(context.session.typeContext, dispatchReceiverValueType)

        val isCandidateFromUnstableSmartcast =
            (candidate.originScope as? FirUnstableSmartcastTypeScope)?.isSymbolFromUnstableSmartcast(candidate.symbol) == true

        val smartcastedReceiver = when (explicitReceiverExpression) {
            is FirCheckNotNullCall -> explicitReceiverExpression.argument
            else -> explicitReceiverExpression
        } as? FirSmartCastExpression

        if (smartcastedReceiver != null &&
            !smartcastedReceiver.isStable &&
            (isCandidateFromUnstableSmartcast || (isReceiverNullable && !smartcastedReceiver.smartcastType.canBeNull))
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
                    context.session.typeContext.isTypeMismatchDueToNullability(smartcastedReceiver.originalExpression.typeRef.coneType, targetType)
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
            val thisReference = receiver?.toReference() as? FirThisReference ?: return
            if (thisReference.isImplicit) {
                checkImpl(
                    candidate,
                    sink,
                    context,
                    { getDslMarkersOfImplicitReceiver(thisReference.boundSymbol, receiver.typeRef.coneType, context) }
                ) {
                    // TODO: is there better alternative?
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
        if (candidate.dispatchReceiver?.typeRef?.coneType?.fullyExpandedType(context.session)?.isSomeFunctionType(context.session) == true &&
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
            collectDslMarkerAnnotations(context, typeRef.coneType)
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
        for (argument in callInfo.arguments) {
            candidate.resolveArgument(
                callInfo,
                argument,
                argumentMapping[argument],
                isReceiver = false,
                sink = sink,
                context = context
            )
        }

        when {
            candidate.system.hasContradiction && callInfo.arguments.isNotEmpty() -> {
                sink.yieldDiagnostic(InapplicableCandidate)
            }

            candidate.usesSAM && !candidate.isJavaApplicableCandidate() -> {
                sink.markCandidateForCompatibilityResolve(context)
            }
        }
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
                    context.bodyResolveComponents.callResolver.resolveCallableReference(candidate.csBuilder, atom)
                if (!success) {
                    sink.yieldDiagnostic(InapplicableCandidate)
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
        if (candidate.symbol is FirPropertySymbol && candidate.symbol.source?.kind is KtFakeSourceElementKind.EnumGeneratedDeclaration) {
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
        }

        if (declaration is FirConstructor) {
            // TODO: Should be some other form
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

                // TODO: consider reporting errors on bounded type variables by incompatible types but with other lower constraints
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

internal object PostponedVariablesInitializerResolutionStage : ResolutionStage() {

    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val argumentMapping = candidate.argumentMapping ?: return
        // TODO: convert type argument mapping to map [FirTypeParameterSymbol, FirTypedProjection?]
        if (candidate.typeArgumentMapping is TypeArgumentMapping.Mapped) return
        for (parameter in argumentMapping.values) {
            if (!parameter.hasBuilderInferenceAnnotation(context.session)) continue
            val type = parameter.returnTypeRef.coneType
            val receiverType = type.receiverType(callInfo.session) ?: continue
            val dontUseBuilderInferenceIfPossible =
                context.session.languageVersionSettings.supportsFeature(LanguageFeature.UseBuilderInferenceOnlyIfNeeded)
            if (dontUseBuilderInferenceIfPossible) continue

            for (freshVariable in candidate.freshVariables) {
                if (candidate.csBuilder.isPostponedTypeVariable(freshVariable)) continue
                if (freshVariable !is ConeTypeParameterBasedTypeVariable) continue
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
}

internal object CheckCallModifiers : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (callInfo.callSite is FirFunctionCall) {
            val functionSymbol = candidate.symbol as? FirNamedFunctionSymbol ?: return
            when {
                callInfo.callSite.origin == FirFunctionCallOrigin.Infix && !functionSymbol.fir.isInfix ->
                    sink.reportDiagnostic(InfixCallOfNonInfixFunction(functionSymbol))
                callInfo.callSite.origin == FirFunctionCallOrigin.Operator && !functionSymbol.fir.isOperator ->
                    sink.reportDiagnostic(OperatorCallOfNonOperatorFunction(functionSymbol))
                callInfo.isImplicitInvoke && !functionSymbol.fir.isOperator ->
                    sink.reportDiagnostic(OperatorCallOfNonOperatorFunction(functionSymbol))
            }
        }
    }
}

internal object CheckDeprecatedSinceKotlin : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val symbol = candidate.symbol as? FirCallableSymbol<*> ?: return
        val deprecation = symbol.getDeprecation(context.session, callInfo.callSite)
        if (deprecation?.deprecationLevel == DeprecationLevelValue.HIDDEN || isHiddenForThisCallSite(symbol, callInfo, candidate)) {
            sink.yieldDiagnostic(HiddenCandidate)
        }
    }

    private fun isHiddenForThisCallSite(
        symbol: FirCallableSymbol<*>,
        callInfo: CallInfo,
        candidate: Candidate,
    ): Boolean {
        if (callInfo.callSite.isSuperCall()) return false
        if (symbol.fir.dispatchReceiverType == null || symbol !is FirNamedFunctionSymbol) return false
        if (symbol.fir.isHiddenEverywhereBesideSuperCalls == true) return true

        val scope = candidate.originScope as? FirTypeScope ?: return false

        var result = false
        scope.processOverriddenFunctions(symbol) {
            if (it.fir.isHiddenEverywhereBesideSuperCalls == true) {
                result = true
                ProcessorAction.STOP
            } else {
                ProcessorAction.NEXT
            }
        }

        return result
    }

    private fun FirElement.isSuperCall(): Boolean =
        this is FirQualifiedAccessExpression && explicitReceiver?.calleeReference is FirSuperReference
}

private val DYNAMIC_EXTENSION_ANNOTATION_CLASS_ID: ClassId = ClassId.topLevel(DYNAMIC_EXTENSION_FQ_NAME)

internal object ProcessDynamicExtensionAnnotation : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (candidate.symbol.origin === FirDeclarationOrigin.DynamicScope) return
        val extensionReceiver = candidate.chosenExtensionReceiver ?: return
        val argumentIsDynamic = extensionReceiver.typeRef.coneType is ConeDynamicType
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
            candidate.callInfo.isImplicitInvoke && candidate.callInfo.explicitReceiver?.typeRef?.coneTypeSafe<ConeDynamicType>() != null ->
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

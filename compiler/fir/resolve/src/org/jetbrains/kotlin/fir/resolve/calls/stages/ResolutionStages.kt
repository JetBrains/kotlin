/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.stages

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInfix
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.*
import org.jetbrains.kotlin.fir.resolve.inference.csBuilder
import org.jetbrains.kotlin.fir.resolve.inference.isAnyOfDelegateOperators
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.FirUnstableSmartcastTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasForConstructor
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SyntheticCallableId.ACCEPT_SPECIFIC_TYPE
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.inference.isSubtypeConstraintCompatible
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.runTransaction
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.DISPATCH_RECEIVER
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.descriptorUtil.DYNAMIC_EXTENSION_FQ_NAME
import org.jetbrains.kotlin.types.AbstractNullabilityChecker
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.util.OperatorNameConventions

abstract class ResolutionStage {
    abstract suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext)
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

        val expectedReceiverType = candidate.symbol.getExpectedReceiverType() ?: return
        val expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType)

        // Probably, we should add an assertion here since we check consistency on the level of scope tower levels
        if (candidate.givenExtensionReceiverOptions.isEmpty()) return

        val preparedReceivers = candidate.givenExtensionReceiverOptions.map {
            prepareReceivers(it, expectedType, context)
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
        val (atom, type) = receivers.single()
        ArgumentCheckingProcessor.resolvePlainArgumentType(
            candidate,
            atom,
            argumentType = type,
            expectedType = expectedType,
            sink = sink,
            context = context,
            isReceiver = true,
            isDispatch = false,
            sourceForReceiver = candidate.callInfo.callSite.source
        )

        // TODO: store atoms for receivers in candidate
        candidate.chosenExtensionReceiver = atom

        sink.yieldIfNeed()
    }
}

private fun prepareReceivers(
    argumentExtensionReceiver: ConeResolutionAtom,
    expectedType: ConeKotlinType,
    context: ResolutionContext,
): ReceiverDescription {
    val argumentType = captureFromTypeParameterUpperBoundIfNeeded(
        argumentType = argumentExtensionReceiver.expression.resolvedType,
        expectedType = expectedType,
        session = context.session
    ).let { prepareCapturedType(it, context) }
        .let {
            when (it) {
                is ConeIntegerConstantOperatorType -> it.possibleTypes.first()
                else -> it
            }
        }

    return ReceiverDescription(argumentExtensionReceiver, argumentType)
}

private data class ReceiverDescription(
    val atom: ConeResolutionAtom,
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

        val dispatchReceiverValueType = candidate.dispatchReceiver?.expression?.resolvedType ?: return

        val isReceiverNullable = !AbstractNullabilityChecker.isSubtypeOfAny(context.session.typeContext, dispatchReceiverValueType)


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
                    isCastToNotNull = context.session.typeContext.isTypeMismatchDueToNullability(
                        smartcastedReceiver.originalExpression.resolvedType,
                        targetType
                    ),
                    isImplicitInvokeReceiver = callInfo.isImplicitInvoke,
                )
            )
        } else if (isReceiverNullable) {
            sink.yieldDiagnostic(InapplicableNullableReceiver(dispatchReceiverValueType))
        }
    }
}

object CheckContextArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val contextExpectedTypes = (candidate.symbol as? FirCallableSymbol<*>)?.fir?.contextParameters?.map {
            candidate.substitutor.substituteOrSelf(it.returnTypeRef.coneType)
        }?.takeUnless { it.isEmpty() } ?: return

        if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.ContextParameters) &&
            !context.session.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)
        ) {
            sink.reportDiagnostic(UnsupportedContextualDeclarationCall)
            return
        }

        val receiverGroups: List<List<FirExpression>> =
            context.bodyResolveContext.towerDataContext.towerDataElements.asReversed().mapNotNull { towerDataElement ->
                towerDataElement.implicitReceiver?.receiverExpression?.let(::listOf)
                    ?: towerDataElement.implicitContextGroup?.map { it.computeExpression() }
            }

        val resultingContextArguments = mutableListOf<ConeResolutionAtom>()
        for (expectedType in contextExpectedTypes) {
            val matchingReceivers = candidate.findClosestMatchingReceivers(expectedType, receiverGroups, context)
            when (matchingReceivers.size) {
                0 -> {
                    sink.reportDiagnostic(NoContextArgument(expectedType))
                    return
                }
                1 -> {
                    val matchingReceiver = matchingReceivers.single()
                    resultingContextArguments.add(matchingReceiver.atom)
                    candidate.system.addSubtypeConstraint(matchingReceiver.type, expectedType, SimpleConstraintSystemConstraintPosition)
                }
                else -> {
                    sink.reportDiagnostic(AmbiguousContextArgument(expectedType))
                    return
                }
            }
        }

        candidate.contextArguments = resultingContextArguments
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
                .map { prepareReceivers(ConeResolutionAtom.createRawAtom(it), expectedType, context) }
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
        checkReceiver(candidate.dispatchReceiver?.expression)
        checkReceiver(candidate.chosenExtensionReceiver?.expression)

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
        if (
            candidate.dispatchReceiver?.expression?.resolvedType
                ?.fullyExpandedType(context.session)
                ?.isSomeFunctionType(context.session) == true
            && (candidate.symbol as? FirNamedFunctionSymbol)?.name == OperatorNameConventions.INVOKE
        ) {
            val firstArg = candidate.argumentMapping.keys.firstOrNull()?.expression as? FirThisReceiverExpression ?: return
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
        val implicitReceivers = context.bodyResolveContext.implicitValueStorage.implicitReceivers
        val resolvedReceiverIndex = implicitReceivers.indexOfFirst { isImplicitReceiverMatching(it) }
        if (resolvedReceiverIndex == -1) return
        val closerReceivers = implicitReceivers.drop(resolvedReceiverIndex + 1)
        if (closerReceivers.isEmpty()) return
        val dslMarkers = dslMarkersProvider()
        if (dslMarkers.isEmpty()) return
        if (closerReceivers.any { receiver -> receiver.getDslMarkersOfImplicitReceiver(context).any { it in dslMarkers } }) {
            sink.reportDiagnostic(DslScopeViolation(candidate.symbol))
        }
    }

    private fun ImplicitReceiverValue<*>.getDslMarkersOfImplicitReceiver(context: ResolutionContext): Set<ClassId> {
        return getDslMarkersOfImplicitReceiver(boundSymbol, type, context)
    }

    private fun getDslMarkersOfImplicitReceiver(
        boundSymbol: FirThisOwnerSymbol<*>?,
        type: ConeKotlinType,
        context: ResolutionContext,
    ): Set<ClassId> {
        val callableSymbol = (boundSymbol as? FirReceiverParameterSymbol)?.containingDeclarationSymbol
        return buildSet {
            (callableSymbol as? FirAnonymousFunctionSymbol)?.fir?.matchingParameterFunctionType?.let {
                // collect annotations in the function type at declaration site. For example, the `@A` and `@B` in the following code.
                // ```
                // fun <T> body(block: @A ((@B T).() -> Unit)) { ... }
                // ```

                // Collect the annotation on the function type, or `@A` in the example above.
                collectDslMarkerAnnotations(context, it.customAnnotations)

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
        val originalType = type.abbreviatedTypeOrSelf
        collectDslMarkerAnnotations(context, originalType.customAnnotations)
        when (originalType) {
            is ConeFlexibleType -> {
                collectDslMarkerAnnotations(context, originalType.lowerBound)
                collectDslMarkerAnnotations(context, originalType.upperBound)
            }
            is ConeCapturedType -> {
                if (originalType.constructor.projection.kind == ProjectionKind.OUT) {
                    originalType.constructor.supertypes?.forEach { collectDslMarkerAnnotations(context, it) }
                }
            }
            is ConeDefinitelyNotNullType -> collectDslMarkerAnnotations(context, originalType.original)
            is ConeIntersectionType -> originalType.intersectedTypes.forEach { collectDslMarkerAnnotations(context, it) }
            is ConeClassLikeType -> {
                val classDeclaration = originalType.toSymbol(context.session) ?: return
                collectDslMarkerAnnotations(context, classDeclaration.resolvedAnnotationsWithClassIds)
                when (classDeclaration) {
                    is FirClassSymbol -> {
                        for (superType in classDeclaration.resolvedSuperTypes) {
                            collectDslMarkerAnnotations(context, superType)
                        }
                    }
                    is FirTypeAliasSymbol -> {
                        originalType.directExpansionType(context.session)?.let {
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
                annotation.annotationTypeRef.coneType.fullyExpandedType(context.session).toClassSymbol(context.session)
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

internal object MapArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val symbol = candidate.symbol as? FirFunctionSymbol<*> ?: return sink.reportDiagnostic(HiddenCandidate)
        val function = symbol.fir
        // We could write simply
        // val arguments = callInfo.argumentAtoms
        // but, we have to re-create atoms here for each candidate, otherwise in a large number of tests
        // we can encounter a problem "subAtom already initialized". For example:
        //
        //   class A<K>
        //   fun <K> A<K>.foo(k: K) = k // (1)
        //   fun <K> A<K>.foo(a: () -> Unit) = 2 // (2)
        //   fun test(){
        //     A<Int>().foo {} // (1)
        //   }
        //
        // We have two different candidates for the 'foo' call here, so we initialize subAtom first time
        // inside 'preprocessLambdaArgument' -> 'createLambdaWithTypeVariableAsExpectedTypeAtomIfNeeded' for a non-functional candidate,
        // and then try to re-initialize it second time inside 'preprocessLambdaArgument' -> 'createResolvedLambdaAtom' for a functional one
        //
        // So the pattern is "lambda at use-site with two different candidates"
        val arguments = callInfo.arguments.map { ConeResolutionAtom.createRawAtom(it) }
        val mapping = context.bodyResolveComponents.mapArguments(
            arguments,
            function,
            candidate.originScope,
            callSiteIsOperatorCall = (callInfo.callSite as? FirFunctionCall)?.origin == FirFunctionCallOrigin.Operator
        )
        candidate.initializeArgumentMapping(
            arguments.unwrapNamedArgumentsForDynamicCall(function),
            mapping.toArgumentToParameterMapping()
        )
        candidate.numDefaults = mapping.numDefaults()
        mapping.diagnostics.forEach(sink::reportDiagnostic)
        sink.yieldIfNeed()
    }

    private fun List<ConeResolutionAtom>.unwrapNamedArgumentsForDynamicCall(function: FirFunction): List<ConeResolutionAtom> {
        if (function.origin != FirDeclarationOrigin.DynamicScope) return this
        return map {
            if (it is ConeResolutionAtomWithSingleChild && it.expression is FirNamedArgumentExpression) {
                it.subAtom ?: error("SubAtom for named argument is null")
            } else {
                it
            }
        }
    }
}

internal val Candidate.isInvokeFromExtensionFunctionType: Boolean
    get() = explicitReceiverKind == DISPATCH_RECEIVER
            && dispatchReceiver?.expression?.resolvedType?.fullyExpandedType(this.callInfo.session)?.isExtensionFunctionType == true
            && (symbol as? FirNamedFunctionSymbol)?.name == OperatorNameConventions.INVOKE

internal fun Candidate.shouldHaveLowPriorityDueToSAM(bodyResolveComponents: BodyResolveComponents): Boolean {
    if (!usesSamConversion || isJavaApplicableCandidate()) return false
    return argumentMapping.values.any {
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

internal object EagerResolveOfCallableReferences : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (candidate.postponedAtoms.isEmpty()) return
        for (atom in candidate.postponedAtoms) {
            if (atom is ConeResolvedCallableReferenceAtom) {
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

                    sink.yieldDiagnostic(UnsuccessfulCallableReferenceArgument)
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

internal object DiscriminateSyntheticProperties : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (candidate.symbol is FirSimpleSyntheticPropertySymbol) {
            sink.reportDiagnostic(ResolvedWithSynthetic)
        }
    }
}

internal object CheckVisibility : ResolutionStage() {
    private suspend fun CheckerSink.yieldVisibilityError(callInfo: CallInfo) {
        // The containing declarations structure with the code fragment is always the following:
        // FirFile
        //     FirCodeFragment
        //         ....
        // See also PsiRawFirBuilder
        yieldDiagnostic(if (callInfo.containingDeclarations.getOrNull(1) is FirCodeFragment) FragmentVisibilityError else VisibilityError)
    }

    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val visibilityChecker = callInfo.session.visibilityChecker
        val declaration = candidate.symbol.fir as? FirMemberDeclaration ?: return

        if (declaration is FirConstructor) {
            val classSymbol = declaration.returnTypeRef.coneType.classLikeLookupTagIfAny?.toSymbol(context.session)
            if (classSymbol is FirRegularClassSymbol && classSymbol.fir.classKind.isSingleton) {
                sink.yieldDiagnostic(HiddenCandidate)
            }
        }

        if (!visibilityChecker.isVisible(declaration, candidate)) {
            sink.yieldVisibilityError(callInfo)
        } else {
            (declaration as? FirConstructor)?.typeAliasForConstructor?.let { typeAlias ->
                if (!visibilityChecker.isVisible(typeAlias.fir, candidate)) {
                    sink.yieldVisibilityError(callInfo)
                }
            }
        }
    }
}

internal object CheckLowPriorityInOverloadResolution : ResolutionStage() {
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

internal object CheckCallModifiers : ResolutionStage() {
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

        if (symbol.isDeprecatedHidden(context, callInfo) ||
            (symbol is FirConstructorSymbol && symbol.typeAliasForConstructor?.isDeprecatedHidden(context, callInfo) == true) ||
            isHiddenForThisCallSite(symbol, callInfo, candidate, context.session, sink)
        ) {
            sink.yieldDiagnostic(HiddenCandidate)
        }
    }

    private fun FirBasedSymbol<*>.isDeprecatedHidden(context: ResolutionContext, callInfo: CallInfo): Boolean {
        val deprecation = getDeprecation(context.session, callInfo.callSite)
        return deprecation?.deprecationLevel == DeprecationLevelValue.HIDDEN
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
        val extensionReceiver = candidate.chosenExtensionReceiver?.expression ?: return
        val argumentIsDynamic = extensionReceiver.resolvedType is ConeDynamicType
        val parameterIsDynamic = (candidate.symbol as? FirCallableSymbol)?.resolvedReceiverTypeRef?.coneType is ConeDynamicType
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

        if (candidate.system.areThereContradictionsInForks()) {
            check(!context.session.languageVersionSettings.supportsFeature(LanguageFeature.ConsiderForkPointsWhenCheckingContradictions)) {
                "This part should only work for obsolete language-version settings"
            }
            // resolving constraints would lead to regular errors reported
            candidate.system.resolveForkPointsConstraints()

            for (error in candidate.system.errors) {
                sink.reportDiagnostic(InferenceError(error))
            }
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

internal object CheckLambdaAgainstTypeVariableContradiction : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.CheckLambdaAgainstTypeVariableContradictionInResolution)) return

        val csBuilder = candidate.csBuilder

        // No need to report additional errors if we already have a contradiction.
        if (csBuilder.hasContradiction) {
            return
        }

        for (postponedAtom in candidate.postponedAtoms) {
            if (postponedAtom !is ConeLambdaWithTypeVariableAsExpectedTypeAtom) continue
            postponedAtom.checkForContradiction(csBuilder, context, sink)
        }
    }

    /**
     * General idea: if we have a lambda argument against a type variable parameter, we can reject some candidates without analyzing the
     * lambda by checking if _any_ function type can satisfy the constraints of the type variable.
     * This makes some code green that would otherwise report an overload resolution ambiguity.
     */
    private fun ConeLambdaWithTypeVariableAsExpectedTypeAtom.checkForContradiction(
        csBuilder: NewConstraintSystemImpl,
        context: ResolutionContext,
        sink: CheckerSink,
    ) {
        // If there's a function type constraint on the type variable,
        // we can get an incorrect contradiction by adding a constraint on Function<Nothing>.
        // Example:
        // Existing constraint `Inv<Function0<Any?>> <: Inv<T>` => `T == Function0<Any?>`
        // Adding `Function<Nothing> <: T` leads to a contradiction `Function<Nothing> </: Function0<Any?>`.
        if (hasFunctionTypeConstraint(csBuilder, context)) {
            return
        }

        // We use Function<Nothing> as our representative type for "some function type".
        val lambdaType = StandardClassIds.Function
            .constructClassLikeType(arrayOf(context.session.builtinTypes.nothingType.coneType))

        var shouldReportError = false

        // We don't add the constraint to the system in the end, we only check for contradictions and roll back the transaction.
        // This ensures we don't get any issues if a different function type constraint is added later, e.g., during completion.
        csBuilder.runTransaction {
            addSubtypeConstraint(lambdaType, expectedType, ConeArgumentConstraintPosition(anonymousFunction))
            shouldReportError = hasContradiction
            false
        }

        if (shouldReportError) {
            sink.reportDiagnostic(
                ArgumentTypeMismatch(
                    expectedType,
                    lambdaType,
                    expression,
                    context.session.typeContext.isTypeMismatchDueToNullability(lambdaType, expectedType)
                )
            )
        }
    }

    private fun ConeLambdaWithTypeVariableAsExpectedTypeAtom.hasFunctionTypeConstraint(
        csBuilder: NewConstraintSystemImpl,
        context: ResolutionContext,
    ): Boolean {
        val typeConstructor = expectedType.typeConstructor(context.typeContext)
        val variableWithConstraints = csBuilder.currentStorage().notFixedTypeVariables[typeConstructor] ?: return false
        return variableWithConstraints.constraints.any { (it.type as ConeKotlinType).isSomeFunctionType(context.session) }
    }
}

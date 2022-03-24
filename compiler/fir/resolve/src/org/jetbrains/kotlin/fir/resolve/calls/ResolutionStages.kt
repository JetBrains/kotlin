/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirVisibilityChecker
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.matchingParameterFunctionType
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.resolve.directExpansionType
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.inference.ResolvedCallableReferenceAtom
import org.jetbrains.kotlin.fir.resolve.inference.csBuilder
import org.jetbrains.kotlin.fir.resolve.inference.hasBuilderInferenceAnnotation
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.FirUnstableSmartcastTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.processOverriddenFunctions
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.SyntheticSymbol
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
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
        val expectedReceiverType = candidate.getReceiverType(context) ?: return

        val argumentExtensionReceiverValue = candidate.extensionReceiverValue ?: return
        val expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType.type)
        val argumentType = captureFromTypeParameterUpperBoundIfNeeded(
            argumentType = argumentExtensionReceiverValue.type,
            expectedType = expectedType,
            session = context.session
        )
        candidate.resolvePlainArgumentType(
            candidate.csBuilder,
            argumentExtensionReceiverValue.receiverExpression,
            argumentType = argumentType,
            expectedType = expectedType,
            sink = sink,
            context = context,
            isReceiver = true,
            isDispatch = false,
        )

        sink.yieldIfNeed()
    }

    private fun Candidate.getReceiverType(context: ResolutionContext): ConeKotlinType? {
        val callableSymbol = symbol as? FirCallableSymbol<*> ?: return null
        val callable = callableSymbol.fir
        val receiverType = callable.receiverTypeRef?.coneType
        if (receiverType != null) return receiverType
        val returnTypeRef = callable.returnTypeRef as? FirResolvedTypeRef ?: return null
        if (!returnTypeRef.type.isExtensionFunctionType(context.session)) return null
        return (returnTypeRef.type.typeArguments.firstOrNull() as? ConeKotlinTypeProjection)?.type
    }
}

object CheckDispatchReceiver : ResolutionStage() {
    @OptIn(SymbolInternals::class)
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val explicitReceiverExpression = callInfo.explicitReceiver
        if (explicitReceiverExpression.isSuperCall()) {
            val status = candidate.symbol.fir as? FirMemberDeclaration
            if (status?.modality == Modality.ABSTRACT) {
                sink.reportDiagnostic(ResolvedWithLowPriority)
            }
        }

        val dispatchReceiverValueType = candidate.dispatchReceiverValue?.type ?: return
        val isReceiverNullable = !AbstractNullabilityChecker.isSubtypeOfAny(context.session.typeContext, dispatchReceiverValueType)

        val isCandidateFromUnstableSmartcast =
            (candidate.originScope as? FirUnstableSmartcastTypeScope)?.isSymbolFromUnstableSmartcast(candidate.symbol) == true

        val smartcastedReceiver = when (explicitReceiverExpression) {
            is FirCheckNotNullCall -> explicitReceiverExpression.argument
            else -> explicitReceiverExpression
        } as? FirExpressionWithSmartcast

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
                    context.session.typeContext.isTypeMismatchDueToNullability(smartcastedReceiver.originalType.coneType, targetType)
                )
            )
        } else if (isReceiverNullable) {
            sink.yieldDiagnostic(UnsafeCall(dispatchReceiverValueType))
        }
    }
}

/**
 * See https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-dsl-marker/ for more details and
 * /compiler/testData/diagnostics/tests/resolve/dslMarker for the test files.
 */
object CheckDslScopeViolation : ResolutionStage() {
    private val dslMarkerClassId = ClassId.fromString("kotlin/DslMarker")

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        fun checkReceiverValue(receiverValue: ReceiverValue?) {
            if (receiverValue is ImplicitReceiverValue<*>) {
                receiverValue.checkImpl(
                    candidate,
                    sink,
                    context,
                    { receiverValue.getDslMarkersOfImplicitReceiver(context) }
                ) { a, b -> a == b }
            }
        }
        checkReceiverValue(candidate.dispatchReceiverValue)
        checkReceiverValue(candidate.extensionReceiverValue)

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
        if (candidate.dispatchReceiverValue?.type?.fullyExpandedType(context.session)?.isBuiltinFunctionalType(context.session) == true &&
            (candidate.symbol as? FirNamedFunctionSymbol)?.name == OperatorNameConventions.INVOKE
        ) {
            val firstArg = candidate.argumentMapping?.keys?.firstOrNull() as? FirThisReceiverExpression ?: return
            if (!firstArg.isImplicit) return
            firstArg.checkImpl(
                candidate,
                sink,
                context,
                { firstArg.getDslMarkersOfThisReceiverExpression(context) }
            ) { receiver, thisExpression -> receiver.boundSymbol == thisExpression.calleeReference.boundSymbol }
        }
    }

    /**
     * Checks whether the implicit receiver (represented as an object of type `T`) violates DSL scope rules.
     */
    private fun <T> T.checkImpl(
        candidate: Candidate,
        sink: CheckerSink,
        context: ResolutionContext,
        dslMarkersProvider: () -> Set<ClassId>,
        isImplicitReceiverMatching: (ImplicitReceiverValue<*>, T) -> Boolean,
    ) {
        val resolvedReceiverIndex = context.bodyResolveContext.implicitReceiverStack.indexOfFirst { isImplicitReceiverMatching(it, this) }
        if (resolvedReceiverIndex == -1) return
        val closerReceivers = context.bodyResolveContext.implicitReceiverStack.drop(resolvedReceiverIndex + 1)
        if (closerReceivers.isEmpty()) return
        val dslMarkers = dslMarkersProvider()
        if (dslMarkers.isEmpty()) return
        if (closerReceivers.any { receiver -> receiver.getDslMarkersOfImplicitReceiver(context).any { it in dslMarkers } }) {
            sink.reportDiagnostic(DslScopeViolation(candidate.symbol))
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun ImplicitReceiverValue<*>.getDslMarkersOfImplicitReceiver(context: ResolutionContext): Set<ClassId> {
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

    @OptIn(ExperimentalStdlibApi::class)
    private fun FirThisReceiverExpression.getDslMarkersOfThisReceiverExpression(context: ResolutionContext): Set<ClassId> {
        return buildSet {
            collectDslMarkerAnnotations(context, typeRef.coneType)
        }
    }

    @OptIn(SymbolInternals::class)
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
                val classDeclaration = type.toSymbol(context.session)?.fir ?: return
                collectDslMarkerAnnotations(context, classDeclaration.annotations)
                when (classDeclaration) {
                    is FirClass -> {
                        for (superType in classDeclaration.superConeTypes) {
                            collectDslMarkerAnnotations(context, superType)
                        }
                    }
                    is FirTypeAlias -> {
                        type.directExpansionType(context.session)?.let {
                            collectDslMarkerAnnotations(context, it)
                        }
                    }
                }
            }
            else -> return
        }
    }

    @OptIn(SymbolInternals::class)
    private fun MutableSet<ClassId>.collectDslMarkerAnnotations(context: ResolutionContext, annotations: Collection<FirAnnotation>) {
        for (annotation in annotations) {
            val annotationClass =
                annotation.annotationTypeRef.coneType.fullyExpandedType(context.session).toSymbol(context.session)?.fir as? FirClass
                    ?: continue
            if (annotationClass.hasAnnotation(dslMarkerClassId)) {
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

        val mapping = context.bodyResolveComponents.mapArguments(callInfo.arguments, function, candidate.originScope)
        candidate.argumentMapping = mapping.toArgumentToParameterMapping()
        candidate.numDefaults = mapping.numDefaults()

        mapping.diagnostics.forEach(sink::reportDiagnostic)
        sink.yieldIfNeed()
    }
}

internal object CheckArguments : CheckerStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        candidate.symbol.ensureResolved(FirResolvePhase.STATUS)
        val argumentMapping =
            candidate.argumentMapping ?: error("Argument should be already mapped while checking arguments!")
        for (argument in callInfo.arguments) {
            val parameter = argumentMapping[argument]
            candidate.resolveArgument(
                callInfo,
                argument,
                parameter,
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
    val symbol = symbol as? FirNamedFunctionSymbol ?: return false
    if (symbol.origin == FirDeclarationOrigin.Enhancement) return true
    if (originScope !is FirTypeScope) return false

    var result = false

    originScope.processOverriddenFunctions(symbol) {
        if (it.origin == FirDeclarationOrigin.Enhancement) {
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
            if (!checkVisibility(declaration, sink, candidate, visibilityChecker)) {
                return
            }
        }

        if (declaration is FirConstructor) {
            // TODO: Should be some other form
            val classSymbol = declaration.returnTypeRef.coneTypeUnsafe<ConeClassLikeType>().lookupTag.toSymbol(context.session)

            if (classSymbol is FirRegularClassSymbol) {
                if (classSymbol.fir.classKind.isSingleton) {
                    sink.yieldDiagnostic(VisibilityError)
                }
                checkVisibility(classSymbol.fir, sink, candidate, visibilityChecker)
            }
        }
    }

    private suspend fun <T : FirMemberDeclaration> checkVisibility(
        declaration: T,
        sink: CheckerSink,
        candidate: Candidate,
        visibilityChecker: FirVisibilityChecker
    ): Boolean {
        if (!visibilityChecker.isVisible(declaration, candidate)) {
            sink.yieldDiagnostic(VisibilityError)
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
            is FirConstructor -> fir.annotations
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

    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val argumentMapping = candidate.argumentMapping ?: return
        // TODO: convert type argument mapping to map [FirTypeParameterSymbol, FirTypedProjection?]
        if (candidate.typeArgumentMapping is TypeArgumentMapping.Mapped) return
        for (parameter in argumentMapping.values) {
            if (!parameter.hasBuilderInferenceAnnotation()) continue
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
        val deprecation = symbol.getDeprecation(callInfo.callSite)
        if (deprecation != null && deprecation.deprecationLevel == DeprecationLevelValue.HIDDEN) {
            sink.yieldDiagnostic(HiddenCandidate)
        }
    }
}

internal object LowerPriorityIfDynamic : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (candidate.symbol.origin is FirDeclarationOrigin.DynamicScope) {
            candidate.addDiagnostic(LowerPriorityForDynamic)
        }
    }
}

internal object ConstraintSystemForks : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (candidate.system.hasContradiction) return

        candidate.system.processForkConstraints()

        if (candidate.system.hasContradiction) {
            sink.yieldDiagnostic(candidate.system.errors.firstOrNull()?.let(::InferenceError) ?: InapplicableCandidate)
        }
    }
}

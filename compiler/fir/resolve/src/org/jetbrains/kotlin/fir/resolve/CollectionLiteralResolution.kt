/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.declarations.isDeprecationLevelHidden
import org.jetbrains.kotlin.fir.declarations.processAllDeclaredCallables
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.ConeAtomWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ConeCollectionLiteralAtom
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CallInfo
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CallKind
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CheckerSinkImpl
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.ImplicitInvokeMode
import org.jetbrains.kotlin.fir.resolve.calls.candidate.createErrorReferenceWithErrorCandidate
import org.jetbrains.kotlin.fir.resolve.calls.stages.ArgumentCheckingProcessor
import org.jetbrains.kotlin.fir.resolve.inference.CollectionLiteralBounds
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.resolve.CollectionNames
import org.jetbrains.kotlin.util.OperatorNameConventions

context(context: ResolutionContext, outerCandidateContext: CollectionLiteralOuterCandidateContext)
fun runCollectionLiteralResolution(
    atom: ConeCollectionLiteralAtom,
    precalculatedBounds: CollectionLiteralBounds?,
) {
    val originalExpression = atom.expression
    val classForResolution = when (precalculatedBounds) {
        is CollectionLiteralBounds.SingleBound -> precalculatedBounds.bound
        is CollectionLiteralBounds.NonTvExpected -> precalculatedBounds.bound
        // it means CL is here through regular resolve of postponed atoms with all input types known
        null -> atom.expectedType?.getClassRepresentativeForCollectionLiteralResolution()
        else -> null
    }

    val resolvedThroughRegularStrategies = tryAllCLResolutionStrategies {
        val preparedCall = prepareRawCall(originalExpression, classForResolution) ?: return@tryAllCLResolutionStrategies null
        resolveCollectionLiteralToPreparedCall(preparedCall)
    }

    val resolvedCall = when {
        resolvedThroughRegularStrategies != null -> resolvedThroughRegularStrategies
        precalculatedBounds is CollectionLiteralBounds.Ambiguity -> {
            resolveCollectionLiteralToErrorCall(
                precalculatedBounds.toConeDiagnostic(),
                atom,
            )
        }
        else -> {
            val preparedCall = prepareFunctionCallForFallback(originalExpression)
            resolveCollectionLiteralToPreparedCall(preparedCall)
        }
    }

    postprocessCollectionLiteralCall(resolvedCall, atom)
}

context(context: ResolutionContext, outerCandidateContext: CollectionLiteralOuterCandidateContext)
private fun resolveCollectionLiteralToPreparedCall(
    preparedCall: FirFunctionCall,
): FirFunctionCall {
    var call = preparedCall
    call = context.bodyResolveComponents.callResolver.resolveCallAndSelectCandidate(
        call,
        ResolutionMode.ContextDependent,
        outerCandidateContext,
    )
    call = context.bodyResolveComponents.callCompleter.completeCall(
        call,
        ResolutionMode.ContextDependent,
        skipEvenPartialCompletion = true,
    )

    return call
}

context(context: ResolutionContext, outerCandidateContext: CollectionLiteralOuterCandidateContext)
private fun resolveCollectionLiteralToErrorCall(
    diagnostic: ConeDiagnostic,
    collectionLiteralAtom: ConeCollectionLiteralAtom,
): FirFunctionCall {
    val components = context.bodyResolveComponents
    val collectionLiteral = collectionLiteralAtom.expression
    val callInfo = CallInfo(
        collectionLiteral,
        CallKind.CollectionLiteral,
        OperatorNameConventions.OF,
        explicitReceiver = null,
        argumentList = collectionLiteral.argumentList,
        isUsedAsGetClassReceiver = false,
        typeArguments = emptyList(),
        session = context.session,
        containingFile = components.file,
        containingDeclarations = components.containingDeclarations,
        resolutionMode = ResolutionMode.ContextDependent,
        origin = FirFunctionCallOrigin.Operator,
        implicitInvokeMode = ImplicitInvokeMode.None,
        containingCandidateForCollectionLiteral = outerCandidateContext.containingCandidate,
    )

    val errorReference = createErrorReferenceWithErrorCandidate(
        callInfo = callInfo,
        diagnostic = diagnostic,
        source = collectionLiteralAtom.expression.source?.fakeElement(KtFakeSourceElementKind.CalleeReferenceForOperatorOfCall),
        resolutionContext = context,
        resolutionStageRunner = components.resolutionStageRunner,
    )

    var call = buildFunctionCall {
        annotations.addAll(collectionLiteral.annotations)
        source = collectionLiteral.source
        argumentList = collectionLiteral.argumentList
        calleeReference = errorReference
        origin = FirFunctionCallOrigin.Operator
    }

    call = components.callCompleter.completeCall(
        call,
        ResolutionMode.ContextDependent,
        skipEvenPartialCompletion = true,
    )

    return call
}

context(context: ResolutionContext, outerCandidateContext: CollectionLiteralOuterCandidateContext)
private fun postprocessCollectionLiteralCall(
    replacementForCL: FirFunctionCall,
    collectionLiteralAtom: ConeCollectionLiteralAtom,
) {
    val originalExpression = collectionLiteralAtom.expression
    val candidateForCL = (replacementForCL.calleeReference as? FirNamedReferenceWithCandidate)?.candidate
        ?: error("Collection literal is expected to be resolved to a call with named candidate.")
    val containingCandidate = outerCandidateContext.containingCandidate
    // 0. When entering the function, `candidateForCL` passed all the stages of `CallKind.CollectionLiteral`.
    // Its system is not yet merged back to containing call's system.
    // Constraint typeOf(replacementForCL) <: expectedType of CL is not added to either of the systems.

    // 1. Set `subAtom`. It is used for traversal over the atom tree after CL has been resolved.
    collectionLiteralAtom.subAtom = ConeAtomWithCandidate(replacementForCL, candidateForCL)

    // 2. Store resolved version of CL. It is then used in the completion results writer to update FIR representation.
    collectionLiteralAtom.containingCallCandidate.setUpdatedCollectionLiteral(originalExpression, replacementForCL)

    // 3. Add constraints from expected type.
    // NB: note the candidate whose system we expand. It needs to be CL since its system is more precise at that point.
    ArgumentCheckingProcessor.resolveArgumentExpression(
        candidateForCL,
        collectionLiteralAtom.subAtom!!,
        collectionLiteralAtom.expectedType,
        outerCandidateContext.checkerSink ?: CheckerSinkImpl(containingCandidate),
        context = context,
        isReceiver = false,
        isDispatch = false,
    )

    // 4. Run additional resolution stages for collection literals.
    // Notably, they include eager resolve for nested collection literals.
    // This is why it is important that we replace the containing call's system later --
    // the system of CL candidate might be expanded even further during these stages.
    context.bodyResolveComponents.resolutionStageRunner.processCandidate(
        candidateForCL,
        context,
        stopOnFirstError = false,
        runAdditionalStages = true,
    )

    // 5. Update resolved reference of the candidate with new ConeDiagnostic, if needed.
    updateCalleeReferenceWithNewErrorsIfNeeded(replacementForCL, candidateForCL)

    // 6. All the diagnostics collected for CL candidate (both from additional stages and basic ones)
    // need to be remapped. Remap preserves the exact applicability if it `isSuccess`.
    outerCandidateContext.checkerSink?.let {
        candidateForCL.remapResolutionDiagnosticsToOuterCandidate(it)
    }

    // 7. Finally, the outer system can be updated.
    containingCandidate.system.replaceContentWith(candidateForCL.system.currentStorage())
}

context(context: ResolutionContext)
private fun prepareFunctionCallForFallback(collectionLiteral: FirCollectionLiteral): FirFunctionCall {
    val packageName = StandardNames.COLLECTIONS_PACKAGE_FQ_NAME
    val functionName = CollectionNames.Factories.LIST_OF

    return context.bodyResolveComponents.buildCollectionLiteralCallForStdlibType(packageName, functionName, collectionLiteral)
}

abstract class CollectionLiteralResolutionStrategy(protected val context: ResolutionContext) {
    protected val components: BodyResolveComponents get() = context.bodyResolveComponents

    internal abstract fun declaresOperatorOf(expectedType: FirRegularClassSymbol): Boolean

    abstract fun prepareRawCall(
        collectionLiteral: FirCollectionLiteral,
        expectedClass: FirRegularClassSymbol?
    ): FirFunctionCall?
}

private class CollectionLiteralResolutionStrategyThroughCompanion(context: ResolutionContext) :
    CollectionLiteralResolutionStrategy(context) {

    private fun FirCallableSymbol<*>.isOperatorOf(): Boolean {
        return this is FirNamedFunctionSymbol && this.isOperator && name == OperatorNameConventions.OF
    }

    private fun FirCallableSymbol<*>.isVisible(receiver: FirResolvedQualifier): Boolean {
        return context.session.visibilityChecker.isVisible(
            fir,
            context.session,
            context.bodyResolveComponents.file,
            context.bodyResolveComponents.containingDeclarations,
            dispatchReceiver = receiver,
        )
    }

    private fun FirRegularClassSymbol.declaresVisibleOf(receiver: FirResolvedQualifier): Boolean {
        var result: Boolean? = null
        processAllDeclaredCallables(context.session) { declaration ->
            if (result != null) return@processAllDeclaredCallables
            if (declaration.isOperatorOf() && !declaration.isDeprecationLevelHidden(context.session)) {
                result = declaration.isVisible(receiver)
            }
        }
        return result ?: false
    }

    private fun buildReceiverIfThereIsVisibleOf(
        expectedClass: FirRegularClassSymbol?,
        collectionLiteral: FirCollectionLiteral?,
    ): FirResolvedQualifier? {
        val companionObjectSymbol = expectedClass?.resolvedCompanionObjectSymbol ?: return null
        val companionAsImplicitReceiver = companionObjectSymbol.toImplicitResolvedQualifierReceiver(
            components,
            collectionLiteral?.source?.fakeElement(KtFakeSourceElementKind.DesugaredReceiverForOperatorOfCall),
        )

        return companionAsImplicitReceiver.takeIf { companionObjectSymbol.declaresVisibleOf(it) }
    }

    override fun declaresOperatorOf(expectedType: FirRegularClassSymbol): Boolean {
        return buildReceiverIfThereIsVisibleOf(expectedType, null) != null
    }

    override fun prepareRawCall(
        collectionLiteral: FirCollectionLiteral,
        expectedClass: FirRegularClassSymbol?
    ): FirFunctionCall? {
        val companionReceiver = buildReceiverIfThereIsVisibleOf(expectedClass, collectionLiteral) ?: return null

        val functionCall = buildFunctionCall {
            annotations.addAll(collectionLiteral.annotations)
            explicitReceiver = companionReceiver
            source = collectionLiteral.source
            calleeReference = buildSimpleNamedReference {
                source = collectionLiteral.source?.fakeElement(KtFakeSourceElementKind.CalleeReferenceForOperatorOfCall)
                name = OperatorNameConventions.OF
            }
            argumentList = collectionLiteral.argumentList
            origin = FirFunctionCallOrigin.Operator
        }

        return functionCall
    }
}

private class CollectionLiteralResolutionStrategyForStdlibType(context: ResolutionContext) : CollectionLiteralResolutionStrategy(context) {
    override fun declaresOperatorOf(expectedType: FirRegularClassSymbol): Boolean {
        return toCollectionOfFactoryPackageAndName(expectedType, context.session) != null
    }

    override fun prepareRawCall(
        collectionLiteral: FirCollectionLiteral,
        expectedClass: FirRegularClassSymbol?,
    ): FirFunctionCall? {
        if (expectedClass == null) return null
        val (packageName, functionName) = toCollectionOfFactoryPackageAndName(expectedClass, context.session) ?: return null

        return components.buildCollectionLiteralCallForStdlibType(packageName, functionName, collectionLiteral)
    }
}

context(context: ResolutionContext)
fun <T : Any> tryAllCLResolutionStrategies(attempt: CollectionLiteralResolutionStrategy.() -> T?): T? {
    CollectionLiteralResolutionStrategyThroughCompanion(context).attempt()?.let { return it }
    return CollectionLiteralResolutionStrategyForStdlibType(context).attempt()
}

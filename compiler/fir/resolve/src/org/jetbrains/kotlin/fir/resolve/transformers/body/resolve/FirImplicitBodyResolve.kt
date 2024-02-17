/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.FirRegularTowerDataContexts
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.AdapterForResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.runContractResolveForLocalClass
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.scopes.impl.originalForWrappedIntegerOperator
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment

@OptIn(AdapterForResolveProcessor::class)
class FirImplicitTypeBodyResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession, FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {
    override val transformer = FirImplicitTypeBodyResolveTransformerAdapter(session, scopeSession)
}

@AdapterForResolveProcessor
class FirImplicitTypeBodyResolveTransformerAdapter(session: FirSession, scopeSession: ScopeSession) : FirTransformer<Any?>() {
    private val implicitBodyResolveComputationSession = ImplicitBodyResolveComputationSession()
    private val returnTypeCalculator = ReturnTypeCalculatorWithJump(scopeSession, implicitBodyResolveComputationSession)

    private val transformer = FirImplicitAwareBodyResolveTransformer(
        session,
        scopeSession,
        implicitBodyResolveComputationSession,
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE, implicitTypeOnly = true,
        returnTypeCalculator
    )

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        return element
    }

    override fun transformFile(file: FirFile, data: Any?): FirFile {
        return withFileAnalysisExceptionWrapping(file) {
            file.transform(transformer, ResolutionMode.ContextIndependent)
        }
    }
}

fun <F : FirClassLikeDeclaration> F.runContractAndBodiesResolutionForLocalClass(
    components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    resolutionMode: ResolutionMode,
    localClassesNavigationInfo: LocalClassesNavigationInfo
): F {
    val currentReturnTypeCalculator = components.context.returnTypeCalculator as? ReturnTypeCalculatorWithJump
    val prevDesignation = currentReturnTypeCalculator?.designationMapForLocalClasses ?: emptyMap()

    val (designationMap, targetedClasses) = localClassesNavigationInfo.run {
        (prevDesignation + designationMap) to
                (parentForClass.keys + this@runContractAndBodiesResolutionForLocalClass) + components.context.targetedLocalClasses
    }

    val implicitBodyResolveComputationSession =
        currentReturnTypeCalculator?.implicitBodyResolveComputationSession ?: ImplicitBodyResolveComputationSession()

    val returnTypeCalculator = ReturnTypeCalculatorWithJump(
        components.scopeSession,
        implicitBodyResolveComputationSession,
        designationMap,
        outerTransformer = components.transformer,
    )

    val newContext = components.context.createSnapshotForLocalClasses(returnTypeCalculator, targetedClasses)
    returnTypeCalculator.outerBodyResolveContext = newContext

    runContractResolveForLocalClass(components.session, components.scopeSession, components.context, targetedClasses)

    val transformer = FirImplicitAwareBodyResolveTransformer(
        components.session, components.scopeSession,
        implicitBodyResolveComputationSession,
        FirResolvePhase.BODY_RESOLVE,
        implicitTypeOnly = false,
        returnTypeCalculator,
        outerBodyResolveContext = newContext,
    )

    return this.transform(transformer, resolutionMode)
}

open class FirImplicitAwareBodyResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    private val implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession,
    phase: FirResolvePhase,
    implicitTypeOnly: Boolean,
    returnTypeCalculator: ReturnTypeCalculatorWithJump,
    outerBodyResolveContext: BodyResolveContext? = null,
) : FirBodyResolveTransformer(
    session,
    phase,
    implicitTypeOnly,
    scopeSession,
    returnTypeCalculator,
    outerBodyResolveContext,
) {
    override fun transformForeignAnnotationCall(symbol: FirBasedSymbol<*>, annotationCall: FirAnnotationCall): FirAnnotationCall {
        val outerTransformer = (returnTypeCalculator as ReturnTypeCalculatorWithJump).outerTransformer
        return outerTransformer?.transformForeignAnnotationCall(symbol, annotationCall) ?: annotationCall
    }

    /**
     * This is required to avoid transformations of class annotations
     */
    override fun transformDeclarationContent(
        declaration: FirDeclaration,
        data: ResolutionMode,
    ): FirDeclaration = if (implicitTypeOnly && declaration is FirRegularClass && !declaration.isLocal) {
        declaration.transformDeclarations(this, data)
    } else {
        super.transformDeclarationContent(declaration, data)
    }

    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: ResolutionMode
    ): FirSimpleFunction {
        return computeCachedTransformationResult(simpleFunction) {
            super.transformSimpleFunction(simpleFunction, data)
        }
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): FirProperty {
        return computeCachedTransformationResult(property) {
            super.transformProperty(property, data)
        }
    }

    private fun <D : FirCallableDeclaration> computeCachedTransformationResult(
        member: D,
        transform: () -> D
    ): D {
        if (!implicitTypeOnly && member.returnTypeRef is FirResolvedTypeRef) {
            return transform()
        }

        val canHaveDeepImplicitTypeRefs = member is FirProperty && member.backingField?.returnTypeRef is FirResolvedTypeRef == false
        if (member.returnTypeRef is FirResolvedTypeRef && !canHaveDeepImplicitTypeRefs) return member
        val symbol = member.symbol
        val status = implicitBodyResolveComputationSession.getStatus(symbol)
        if (status is ImplicitBodyResolveComputationStatus.Computed) {
            @Suppress("UNCHECKED_CAST")
            return status.transformedDeclaration as D
        }

        // If somebody has started resolution recursively (from ReturnTypeCalculator), one has to track it's not being computed already
        require(status is ImplicitBodyResolveComputationStatus.NotComputed) {
            "Unexpected status in transformCallableMember ($status) for ${member.render()}"
        }

        return implicitBodyResolveComputationSession.compute(symbol, transform)
    }
}

open class ReturnTypeCalculatorWithJump(
    protected val scopeSession: ScopeSession,
    val implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession,
    val designationMapForLocalClasses: Map<FirCallableDeclaration, List<FirClassLikeDeclaration>> = mapOf(),
    val outerTransformer: FirAbstractBodyResolveTransformerDispatcher? = null,
) : ReturnTypeCalculator() {
    override val callableCopyTypeCalculator: CallableCopyTypeCalculator = CallableCopyTypeCalculatorWithJump()

    @OptIn(PrivateForInline::class)
    var outerBodyResolveContext: BodyResolveContext? = null
        set(context) {
            field = context
            outerTowerDataContexts = context?.regularTowerDataContexts
        }

    var outerTowerDataContexts: FirRegularTowerDataContexts? = null

    override fun tryCalculateReturnTypeOrNull(declaration: FirCallableDeclaration): FirResolvedTypeRef {
        // Local declarations must be handled by `ReturnTypeCalculatorForFullBodyResolve` to avoid resolution cycles in LL FIR.
        if (declaration.visibility == Visibilities.Local) {
            return ReturnTypeCalculatorForFullBodyResolve.Default.tryCalculateReturnType(declaration)
        }

        if (declaration is FirValueParameter && declaration.returnTypeRef is FirImplicitTypeRef) {
            declaration.replaceReturnTypeRef(
                buildErrorTypeRef {
                    diagnostic = ConeSimpleDiagnostic("Unsupported: implicit VP type")
                }
            )
        }

        if (declaration is FirSimpleFunction) {
            // Effectively this logic is redundant now, because all methods of Int have an explicit return type,
            // so explicit call here just to be sure (probably some method from Int will have an implicit type)
            declaration.originalForWrappedIntegerOperator?.let {
                tryCalculateReturnTypeOrNull(it.fir)
            }
        }

        resolvedToContractsIfNecessary(declaration)

        val returnTypeRef = declaration.returnTypeRef
        if (returnTypeRef is FirResolvedTypeRef) return returnTypeRef

        if (declaration is FirSyntheticProperty) {
            return tryCalculateReturnType(declaration.getter.delegate)
        }

        val unwrappedDelegate = declaration.delegatedWrapperData?.wrapped
        if (unwrappedDelegate != null) {
            return tryCalculateReturnType(unwrappedDelegate).also {
                if (declaration.returnTypeRef is FirImplicitTypeRef) {
                    declaration.replaceReturnTypeRef(it)
                }
            }
        }

        if (declaration.isCopyCreatedInScope) {
            val resolvedTypeRef = callableCopyTypeCalculator.computeReturnType(declaration)
            requireWithAttachment(
                resolvedTypeRef is FirResolvedTypeRef,
                { "Unexpected return type: ${resolvedTypeRef?.let { it::class.simpleName }}" },
            ) {
                withFirEntry("declaration", declaration)
            }

            return resolvedTypeRef
        }

        return computeReturnTypeRef(declaration)
    }

    private fun resolvedToContractsIfNecessary(declaration: FirCallableDeclaration) {
        val canHaveContracts = when {
            declaration is FirProperty && !declaration.isLocal -> true
            declaration is FirSimpleFunction && !declaration.isLocal -> true
            else -> false
        }

        if (canHaveContracts) {
            declaration.lazyResolveToPhase(FirResolvePhase.CONTRACTS)
        }
    }


    protected fun recursionInImplicitTypeRef(): FirErrorTypeRef = buildErrorTypeRef {
        diagnostic = ConeSimpleDiagnostic("cycle", DiagnosticKind.RecursionInImplicitTypes)
    }

    private fun computeReturnTypeRef(declaration: FirCallableDeclaration): FirResolvedTypeRef {
        val computedReturnType = when (val status = implicitBodyResolveComputationSession.getStatus(declaration.symbol)) {
            is ImplicitBodyResolveComputationStatus.Computed -> status.resolvedTypeRef
            is ImplicitBodyResolveComputationStatus.Computing -> recursionInImplicitTypeRef()
            else -> null
        }

        (computedReturnType ?: declaration.returnTypeRef as? FirResolvedTypeRef)?.let { return it }
        val symbol = declaration.symbol
        require(!symbol.isCopyCreatedInScope) {
            "callableCopySubstitution was not calculated for callable copy: " +
                    "$symbol with origin ${declaration.origin} and return type ${declaration.returnTypeRef}"
        }

        return resolveDeclaration(declaration)
    }

    @OptIn(PrivateForInline::class)
    protected open fun resolveDeclaration(declaration: FirCallableDeclaration): FirResolvedTypeRef {
        // To properly transform and resolve declaration's type, we need to use its module's session
        val session = declaration.moduleData.session
        val symbol = declaration.symbol

        val (designation, outerBodyResolveContext) = if (declaration in designationMapForLocalClasses) {
            designationMapForLocalClasses.getValue(declaration) to outerBodyResolveContext
        } else {
            (outerTransformer?.returnTypeCalculator as? ReturnTypeCalculatorWithJump)?.resolveDeclaration(declaration)?.let {
                return it
            }

            val provider = session.firProvider
            val file = provider.getFirCallableContainerFile(symbol)

            val outerClasses = generateSequence(symbol.containingClassLookupTag()?.classId) { classId ->
                classId.outerClassId
            }.mapTo(mutableListOf()) { provider.getFirClassifierByFqName(it) }

            if (file == null || outerClasses.any { it == null }) {
                return buildErrorTypeRef {
                    diagnostic = ConeSimpleDiagnostic(
                        "Cannot calculate return type (local class/object?)",
                        DiagnosticKind.InferenceError
                    )
                }
            }
            (listOf(file) + outerClasses.filterNotNull().asReversed()) to null
        }

        val previousTowerDataContexts = outerBodyResolveContext?.regularTowerDataContexts
        outerBodyResolveContext?.regularTowerDataContexts = outerTowerDataContexts!!

        val transformer = FirDesignatedBodyResolveTransformerForReturnTypeCalculator(
            (designation.drop(1) + declaration).iterator(),
            session,
            scopeSession,
            implicitBodyResolveComputationSession,
            this,
            outerBodyResolveContext
        )

        designation.first().transform<FirElement, ResolutionMode>(transformer, ResolutionMode.ContextDependent)

        val transformedDeclaration = transformer.lastResult as? FirCallableDeclaration
            ?: error("Unexpected lastResult: ${transformer.lastResult?.render()}")

        val newReturnTypeRef = transformedDeclaration.returnTypeRef
        require(newReturnTypeRef is FirResolvedTypeRef) { transformedDeclaration.render() }
        if (previousTowerDataContexts != null) {
            outerBodyResolveContext.regularTowerDataContexts = previousTowerDataContexts
        }
        return newReturnTypeRef
    }

    private inner class CallableCopyTypeCalculatorWithJump : CallableCopyTypeCalculator.AbstractCallableCopyTypeCalculator() {
        override fun FirCallableDeclaration.getResolvedTypeRef(): FirResolvedTypeRef? {
            return this@ReturnTypeCalculatorWithJump.computeReturnTypeRef(this)
        }
    }
}

open class FirDesignatedBodyResolveTransformerForReturnTypeCalculator(
    private val designation: Iterator<FirElement>,
    session: FirSession,
    scopeSession: ScopeSession,
    implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession,
    returnTypeCalculator: ReturnTypeCalculatorWithJump,
    outerBodyResolveContext: BodyResolveContext? = null
) : FirImplicitAwareBodyResolveTransformer(
    session,
    scopeSession,
    implicitBodyResolveComputationSession,
    FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
    implicitTypeOnly = true,
    returnTypeCalculator,
    outerBodyResolveContext
) {
    var lastResult: FirElement? = null

    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration {
        if (designation.hasNext()) {
            val result = designation.next().transform<FirDeclaration, ResolutionMode>(this, data)
            if (!designation.hasNext() && lastResult == null) {
                lastResult = result
            }
            return declaration
        }

        return super.transformDeclarationContent(declaration, data)
    }
}

open class ImplicitBodyResolveComputationSession {
    private val implicitBodyResolveStatusMap = hashMapOf<FirCallableSymbol<*>, ImplicitBodyResolveComputationStatus>()

    internal fun getStatus(symbol: FirCallableSymbol<*>): ImplicitBodyResolveComputationStatus {
        if (symbol is FirSyntheticPropertySymbol) {
            val fir = symbol.fir
            if (fir is FirSyntheticProperty) {
                return getStatus(fir.getter.delegate.symbol)
            }
        }
        return implicitBodyResolveStatusMap[symbol] ?: ImplicitBodyResolveComputationStatus.NotComputed
    }

    protected open fun <D : FirCallableDeclaration> executeTransformation(symbol: FirCallableSymbol<*>, transformation: () -> D): D {
        return transformation()
    }

    internal fun <D : FirCallableDeclaration> compute(symbol: FirCallableSymbol<*>, transformation: () -> D): D {
        startComputing(symbol)
        val result = executeTransformation(symbol, transformation)
        storeResult(symbol, result)
        return result
    }

    private fun startComputing(symbol: FirCallableSymbol<*>) {
        require(implicitBodyResolveStatusMap[symbol] == null) {
            "Unexpected static in startComputing for $symbol: ${implicitBodyResolveStatusMap[symbol]}"
        }

        implicitBodyResolveStatusMap[symbol] = ImplicitBodyResolveComputationStatus.Computing
    }

    private fun storeResult(
        symbol: FirCallableSymbol<*>,
        transformedDeclaration: FirCallableDeclaration
    ) {
        require(implicitBodyResolveStatusMap[symbol] == ImplicitBodyResolveComputationStatus.Computing) {
            "Unexpected static in storeResult for $symbol: ${implicitBodyResolveStatusMap[symbol]}"
        }

        val returnTypeRef = transformedDeclaration.returnTypeRef
        require(returnTypeRef is FirResolvedTypeRef) {
            "Not FirResolvedTypeRef (${transformedDeclaration.returnTypeRef.render()}) in storeResult for: ${symbol.fir.render()}"
        }

        implicitBodyResolveStatusMap[symbol] = ImplicitBodyResolveComputationStatus.Computed(returnTypeRef, transformedDeclaration)
    }
}

internal sealed class ImplicitBodyResolveComputationStatus {
    object NotComputed : ImplicitBodyResolveComputationStatus()
    object Computing : ImplicitBodyResolveComputationStatus()

    class Computed(
        val resolvedTypeRef: FirResolvedTypeRef,
        val transformedDeclaration: FirCallableDeclaration
    ) : ImplicitBodyResolveComputationStatus()
}

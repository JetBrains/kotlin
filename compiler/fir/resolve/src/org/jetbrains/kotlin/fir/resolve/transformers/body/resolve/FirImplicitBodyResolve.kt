/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.runContractResolveForLocalClass
import org.jetbrains.kotlin.fir.symbols.impl.FirAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

@OptIn(AdapterForResolveProcessor::class)
class FirImplicitTypeBodyResolveProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession) {
    override val transformer = FirImplicitTypeBodyResolveTransformerAdapter(session, scopeSession)
}

@AdapterForResolveProcessor
class FirImplicitTypeBodyResolveTransformerAdapter(session: FirSession, scopeSession: ScopeSession) : FirTransformer<Nothing?>() {
    private val implicitBodyResolveComputationSession = ImplicitBodyResolveComputationSession()
    private val returnTypeCalculator = ReturnTypeCalculatorWithJump(session, scopeSession, implicitBodyResolveComputationSession).also {
        scopeSession.returnTypeCalculator = it
    }

    private val transformer = FirImplicitAwareBodyResolveTransformer(
        session,
        scopeSession,
        implicitBodyResolveComputationSession,
        FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE, implicitTypeOnly = true,
        returnTypeCalculator
    )

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        return file.transform(transformer, ResolutionMode.ContextIndependent)
    }
}

fun <F : FirClass<F>> F.runContractAndBodiesResolutionForLocalClass(
    components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    resolutionMode: ResolutionMode,
    localClassesNavigationInfo: LocalClassesNavigationInfo,
): F {
    val (designationMap, targetedClasses) = localClassesNavigationInfo.run {
        designationMap to parentForClass.keys + this@runContractAndBodiesResolutionForLocalClass
    }

    val implicitBodyResolveComputationSession =
        ((components.returnTypeCalculator as? ReturnTypeCalculatorWithJump)?.implicitBodyResolveComputationSession
            ?: ImplicitBodyResolveComputationSession())
    val returnTypeCalculator = ReturnTypeCalculatorWithJump(
        components.session,
        components.scopeSession,
        implicitBodyResolveComputationSession,
        designationMap,
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
        outerBodyResolveContext = newContext
    )

    val graphBuilder = components.context.dataFlowAnalyzerContext.graphBuilder
    val members = localClassesNavigationInfo.allMembers
    graphBuilder.prepareForLocalClassMembers(members)

    return this.transform<F, ResolutionMode>(transformer, resolutionMode).single.also {
        graphBuilder.cleanAfterForLocalClassMembers(members)
    }
}

fun createReturnTypeCalculatorForIDE(
    session: FirSession,
    scopeSession: ScopeSession,
    createTransformer: (
        designation: Iterator<FirElement>,
        FirSession,
        ScopeSession,
        ImplicitBodyResolveComputationSession,
        ReturnTypeCalculator,
        BodyResolveContext?
    ) -> FirDesignatedBodyResolveTransformerForReturnTypeCalculator
): ReturnTypeCalculator =
    ReturnTypeCalculatorWithJump(session, scopeSession, ImplicitBodyResolveComputationSession(), createTransformer = createTransformer)

open class FirImplicitAwareBodyResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    private val implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession,
    phase: FirResolvePhase,
    implicitTypeOnly: Boolean,
    returnTypeCalculator: ReturnTypeCalculator,
    outerBodyResolveContext: BodyResolveContext? = null
) : FirBodyResolveTransformer(
    session,
    phase,
    implicitTypeOnly,
    scopeSession,
    returnTypeCalculator,
    outerBodyResolveContext
) {
    override fun transformSimpleFunction(
        simpleFunction: FirSimpleFunction,
        data: ResolutionMode
    ): CompositeTransformResult<FirSimpleFunction> {
        return computeCachedTransformationResult(simpleFunction) {
            super.transformSimpleFunction(simpleFunction, data)
        }
    }

    override fun transformProperty(property: FirProperty, data: ResolutionMode): CompositeTransformResult<FirProperty> {
        return computeCachedTransformationResult(property) {
            super.transformProperty(property, data)
        }
    }

    private fun <D : FirCallableMemberDeclaration<D>> computeCachedTransformationResult(
        member: D,
        transform: () -> CompositeTransformResult<D>
    ): CompositeTransformResult<D> {
        if (!implicitTypeOnly && member.returnTypeRef is FirResolvedTypeRef) {
            return transform()
        }

        if (member.returnTypeRef is FirResolvedTypeRef) return member.compose()
        val symbol = member.symbol
        val status = implicitBodyResolveComputationSession.getStatus(symbol)
        if (status is ImplicitBodyResolveComputationStatus.Computed) {
            @Suppress("UNCHECKED_CAST")
            return status.transformedDeclaration.compose() as CompositeTransformResult<D>
        }

        // If somebody has started resolution recursively (from ReturnTypeCalculator), one has to track it's not being computed already
        require(status is ImplicitBodyResolveComputationStatus.NotComputed) {
            "Unexpected status in transformCallableMember ($status) for ${member.render()}"
        }

        implicitBodyResolveComputationSession.startComputing(symbol)
        val result = transform()
        implicitBodyResolveComputationSession.storeResult(symbol, result.single)

        return result
    }
}

private class ReturnTypeCalculatorWithJump(
    private val session: FirSession,
    private val scopeSession: ScopeSession,
    val implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession,
    val designationMapForLocalClasses: Map<FirCallableMemberDeclaration<*>, List<FirClass<*>>> = mapOf(),
    private val createTransformer: (
        designation: Iterator<FirElement>,
        session: FirSession,
        scopeSession: ScopeSession,
        implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession,
        returnTypeCalculator: ReturnTypeCalculator,
        outerBodyResolveContext: BodyResolveContext?
    ) -> FirDesignatedBodyResolveTransformerForReturnTypeCalculator = ::FirDesignatedBodyResolveTransformerForReturnTypeCalculator,
) : ReturnTypeCalculator {

    var outerBodyResolveContext: BodyResolveContext? = null

    override fun tryCalculateReturnType(declaration: FirTypedDeclaration): FirResolvedTypeRef {
        if (declaration is FirValueParameter && declaration.returnTypeRef is FirImplicitTypeRef) {
            // TODO?
            declaration.transformReturnTypeRef(
                TransformImplicitType,
                buildErrorTypeRef {
                    diagnostic = ConeSimpleDiagnostic("Unsupported: implicit VP type")
                }
            )
        }

        val returnTypeRef = declaration.returnTypeRef
        if (returnTypeRef is FirResolvedTypeRef) return returnTypeRef

        require(declaration is FirCallableMemberDeclaration<*>) { "${declaration::class}: ${declaration.render()}" }

        if (declaration is FirSyntheticProperty) {
            return tryCalculateReturnType(declaration.getter.delegate)
        }

        if (declaration.origin == FirDeclarationOrigin.IntersectionOverride) {
            val result = tryCalculateReturnType(declaration.symbol.overriddenSymbol!!.fir)
            declaration.replaceReturnTypeRef(result)
            return result
        }

        return when (val status = implicitBodyResolveComputationSession.getStatus(declaration.symbol)) {
            is ImplicitBodyResolveComputationStatus.Computed -> status.resolvedTypeRef
            is ImplicitBodyResolveComputationStatus.Computing ->
                buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic("cycle", DiagnosticKind.RecursionInImplicitTypes) }
            else -> computeReturnTypeRef(declaration)
        }
    }

    private fun computeReturnTypeRef(declaration: FirCallableMemberDeclaration<*>): FirResolvedTypeRef {
        val symbol = declaration.symbol
        val id = symbol.callableId

        val provider = session.firProvider

        val (designation, outerBodyResolveContext) = if (declaration in designationMapForLocalClasses) {
            designationMapForLocalClasses.getValue(declaration) to outerBodyResolveContext
        } else {
            val file = provider.getFirCallableContainerFile(symbol)

            val outerClasses = generateSequence(id.classId) { classId ->
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

        val transformer = createTransformer(
            (designation.drop(1) + declaration).iterator(),
            session,
            scopeSession,
            implicitBodyResolveComputationSession,
            this,
            outerBodyResolveContext
        )

        designation.first().transform<FirElement, ResolutionMode>(transformer, ResolutionMode.ContextDependent)

        val transformedDeclaration = transformer.lastResult as? FirCallableMemberDeclaration<*>
            ?: error("Unexpected lastResult: ${transformer.lastResult?.render()}")

        val newReturnTypeRef = transformedDeclaration.returnTypeRef
        require(newReturnTypeRef is FirResolvedTypeRef) { transformedDeclaration.render() }
        return newReturnTypeRef
    }
}

open class FirDesignatedBodyResolveTransformerForReturnTypeCalculator(
    private val designation: Iterator<FirElement>,
    session: FirSession,
    scopeSession: ScopeSession,
    implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession,
    returnTypeCalculator: ReturnTypeCalculator,
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

    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): CompositeTransformResult<FirDeclaration> {
        if (designation.hasNext()) {
            val result = designation.next().transform<FirDeclaration, ResolutionMode>(this, data).single
            if (!designation.hasNext() && lastResult == null) {
                lastResult = result
            }
            return declaration.compose()
        }

        return super.transformDeclarationContent(declaration, data)
    }
}

class ImplicitBodyResolveComputationSession {
    private val implicitBodyResolveStatusMap = hashMapOf<FirCallableSymbol<*>, ImplicitBodyResolveComputationStatus>()

    internal fun getStatus(symbol: FirCallableSymbol<*>): ImplicitBodyResolveComputationStatus {
        if (symbol is FirAccessorSymbol) {
            val fir = symbol.fir
            if (fir is FirSyntheticProperty) {
                return getStatus(fir.getter.delegate.symbol)
            }
        }
        return implicitBodyResolveStatusMap[symbol] ?: ImplicitBodyResolveComputationStatus.NotComputed
    }

    fun startComputing(symbol: FirCallableSymbol<*>) {
        require(implicitBodyResolveStatusMap[symbol] == null) {
            "Unexpected static in startComputing for $symbol: ${implicitBodyResolveStatusMap[symbol]}"
        }

        implicitBodyResolveStatusMap[symbol] = ImplicitBodyResolveComputationStatus.Computing
    }

    fun storeResult(
        symbol: FirCallableSymbol<*>,
        transformedDeclaration: FirCallableMemberDeclaration<*>
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
        val transformedDeclaration: FirCallableMemberDeclaration<*>
    ) : ImplicitBodyResolveComputationStatus()
}

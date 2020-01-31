/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.resolve.transformers.body.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.TransformImplicitType
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

@Deprecated("It is temp", level = DeprecationLevel.WARNING, replaceWith = ReplaceWith("TODO(\"что-то нормальное\")"))
class FirImplicitTypeBodyResolveTransformerAdapter(private val scopeSession: ScopeSession) : FirTransformer<Nothing?>() {
    private val implicitBodyResolveComputationSession = ImplicitBodyResolveComputationSession()

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        val session = file.session
        scopeSession.returnTypeCalculator = ReturnTypeCalculatorWithJump(session, scopeSession, implicitBodyResolveComputationSession)
        val transformer = FirImplicitBodyResolveTransformer(
            session,
            scopeSession,
            implicitBodyResolveComputationSession
        )
        return file.transform(transformer, ResolutionMode.ContextIndependent)
    }
}

fun createReturnTypeCalculatorForIDE(session: FirSession, scopeSession: ScopeSession): ReturnTypeCalculator =
    ReturnTypeCalculatorWithJump(session, scopeSession, ImplicitBodyResolveComputationSession())

// TODO: This class is unused because it's effectively unneeded while we have mutable trees
// Once we decide to switch to persistent trees this, it should be applied to each file
// If the decision is to stay with mutable trees, this class should be removed
private class FirApplyInferredDeclarationTypesTransformer(
    private val implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession
) : FirDefaultTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        file.replaceResolvePhase(FirResolvePhase.SUPER_TYPES)

        return (file.transformChildren(this, null) as FirFile).compose()
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirStatement> {
        return (regularClass.transformChildren(this, null) as FirRegularClass).compose()
    }

    override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        if (simpleFunction.returnTypeRef !is FirImplicitTypeRef) return simpleFunction.compose()

        val transformedDeclaration = implicitBodyResolveComputationSession.getComputedResult(simpleFunction.symbol).transformedDeclaration
        return transformedDeclaration.compose()
    }

    override fun transformProperty(property: FirProperty, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        if (property.returnTypeRef !is FirImplicitTypeRef) return property.compose()

        val transformedDeclaration = implicitBodyResolveComputationSession.getComputedResult(property.symbol).transformedDeclaration
        return transformedDeclaration.compose()
    }
}

private open class FirImplicitBodyResolveTransformer(
    session: FirSession,
    scopeSession: ScopeSession,
    private val implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession
) : FirBodyResolveTransformer(
    session,
    phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE,
    implicitTypeOnly = true,
    scopeSession = scopeSession,
    returnTypeCalculator = ReturnTypeCalculatorWithJump(session, scopeSession, implicitBodyResolveComputationSession)
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
    private val implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession
) : ReturnTypeCalculator {

    override fun tryCalculateReturnType(declaration: FirTypedDeclaration): FirResolvedTypeRef {
        if (declaration is FirValueParameter && declaration.returnTypeRef is FirImplicitTypeRef) {
            // TODO?
            declaration.transformReturnTypeRef(
                TransformImplicitType,
                buildErrorTypeRef {
                    diagnostic = FirSimpleDiagnostic("Unsupported: implicit VP type")
                }
            )
        }

        val returnTypeRef = declaration.returnTypeRef
        if (returnTypeRef is FirResolvedTypeRef) return returnTypeRef

        require(declaration is FirCallableMemberDeclaration<*>) { "${declaration::class}: ${declaration.render()}" }

        return when (val status = implicitBodyResolveComputationSession.getStatus(declaration.symbol)) {
            is ImplicitBodyResolveComputationStatus.Computed -> status.resolvedTypeRef
            is ImplicitBodyResolveComputationStatus.Computing ->
                buildErrorTypeRef {diagnostic = FirSimpleDiagnostic("cycle", DiagnosticKind.RecursionInImplicitTypes) }
            else -> computeReturnTypeRef(declaration)
        }
    }

    private fun computeReturnTypeRef(declaration: FirCallableMemberDeclaration<*>): FirResolvedTypeRef {
        val symbol = declaration.symbol
        val id = symbol.callableId

        val provider = session.firProvider

        val file = provider.getFirCallableContainerFile(symbol)

        val outerClasses = generateSequence(id.classId) { classId ->
            classId.outerClassId
        }.mapTo(mutableListOf()) { provider.getFirClassifierByFqName(it) }

        if (file == null || outerClasses.any { it == null }) {
            return buildErrorTypeRef {
                diagnostic = FirSimpleDiagnostic(
                    "Cannot calculate return type (local class/object?)",
                    DiagnosticKind.InferenceError
                )
            }
        }
        val designation = (listOf(file) + outerClasses.filterNotNull().asReversed() + listOf(declaration))
        val transformer = FirDesignatedBodyResolveTransformerForReturnTypeCalculator(
            designation.iterator(),
            file.session,
            scopeSession,
            implicitBodyResolveComputationSession
        )

        file.transform<FirElement, ResolutionMode>(transformer, ResolutionMode.ContextDependent)

        val transformedDeclaration = transformer.lastResult as? FirCallableMemberDeclaration<*>
            ?: error("Unexpected lastResult: ${transformer.lastResult?.render()}")

        val newReturnTypeRef = transformedDeclaration.returnTypeRef
        require(newReturnTypeRef is FirResolvedTypeRef) { transformedDeclaration.render() }
        return newReturnTypeRef
    }
}

private class FirDesignatedBodyResolveTransformerForReturnTypeCalculator(
    private val designation: Iterator<FirElement>,
    session: FirSession,
    scopeSession: ScopeSession,
    implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession
) : FirImplicitBodyResolveTransformer(
    session,
    scopeSession,
    implicitBodyResolveComputationSession
) {

    var lastResult: FirElement? = null

    override fun <E : FirElement> transformElement(element: E, data: ResolutionMode): CompositeTransformResult<E> {
        if (designation.hasNext()) {
            val result = designation.next().transform<E, ResolutionMode>(this, data).single
            if (!designation.hasNext()) {
                lastResult = result
            }
            return result.compose()
        }
        return super.transformElement(element, data)
    }

    override fun transformDeclaration(declaration: FirDeclaration, data: ResolutionMode): CompositeTransformResult<FirDeclaration> {
        return components.withContainer(declaration) {
            declaration.replaceResolvePhase(transformerPhase)
            transformElement(declaration, data)
        }
    }
}

private class ImplicitBodyResolveComputationSession {
    private val implicitBodyResolveStatusMap = hashMapOf<FirCallableSymbol<*>, ImplicitBodyResolveComputationStatus>()

    fun getStatus(symbol: FirCallableSymbol<*>): ImplicitBodyResolveComputationStatus {
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
            "Not FirResolvedTypeRef (${transformedDeclaration.receiverTypeRef?.render()}) in storeResult for: ${symbol.fir.render()}"
        }

        implicitBodyResolveStatusMap[symbol] = ImplicitBodyResolveComputationStatus.Computed(returnTypeRef, transformedDeclaration)
    }

    fun getComputedResult(symbol: FirCallableSymbol<*>): ImplicitBodyResolveComputationStatus.Computed {
        val status = getStatus(symbol)
        require(status is ImplicitBodyResolveComputationStatus.Computed) {
            "Unexpected status $status for ${symbol.fir.render()}"
        }

        return status
    }
}

private sealed class ImplicitBodyResolveComputationStatus {
    object NotComputed : ImplicitBodyResolveComputationStatus()
    object Computing : ImplicitBodyResolveComputationStatus()

    class Computed(
        val resolvedTypeRef: FirResolvedTypeRef,
        val transformedDeclaration: FirCallableMemberDeclaration<*>
    ) : ImplicitBodyResolveComputationStatus()
}

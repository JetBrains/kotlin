package org.jetbrains.kotlin.idea.fir.low.level.api.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotationResolveStatus
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirTowerDataContextCollector
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.ImplicitBodyResolveComputationSession
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.createReturnTypeCalculatorForIDE
import org.jetbrains.kotlin.idea.fir.low.level.api.FirPhaseRunner
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator

internal class FirFileAnnotationsResolveTransformer(
    private val firFile: FirFile,
    private val annotations: List<FirAnnotationCall>,
    session: FirSession,
    scopeSession: ScopeSession,
    implicitBodyResolveComputationSession: ImplicitBodyResolveComputationSession = ImplicitBodyResolveComputationSession(),
    firTowerDataContextCollector: FirTowerDataContextCollector? = null,
) : FirBodyResolveTransformer(
    session = session,
    phase = FirResolvePhase.BODY_RESOLVE,
    implicitTypeOnly = false,
    scopeSession = scopeSession,
    returnTypeCalculator = createReturnTypeCalculatorForIDE(
        session,
        scopeSession,
        implicitBodyResolveComputationSession,
        ::FirIdeDesignatedBodyResolveTransformerForReturnTypeCalculator
    ),
    firTowerDataContextCollector = firTowerDataContextCollector
), FirLazyTransformerForIDE {

    override fun transformDeclarationContent(declaration: FirDeclaration, data: ResolutionMode): FirDeclaration {
        require(declaration is FirFile) { "Unexpected declaration ${declaration::class.simpleName}" }
        annotations.forEach {
            if (it.resolveStatus != FirAnnotationResolveStatus.Resolved) {
                it.visitNoTransform(this, data)
            }
        }
        return declaration
    }

    override fun transformDeclaration(phaseRunner: FirPhaseRunner) {
        if (annotations.all { it.resolveStatus == FirAnnotationResolveStatus.Resolved }) return
        check(firFile.resolvePhase >= FirResolvePhase.IMPORTS) { "Invalid file resolve phase ${firFile.resolvePhase}" }

        firFile.accept(this, ResolutionMode.ContextDependent)
        check(annotations.all { it.resolveStatus == FirAnnotationResolveStatus.Resolved }) { "Annotation was not resolved" }
    }

    override fun needReplacePhase(firDeclaration: FirDeclaration): Boolean = false
}
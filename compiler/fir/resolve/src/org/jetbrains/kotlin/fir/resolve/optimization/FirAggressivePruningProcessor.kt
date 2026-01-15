package org.jetbrains.kotlin.fir.resolve.optimization

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveProcessor
import org.jetbrains.kotlin.fir.visitors.FirTransformer

import org.jetbrains.kotlin.fir.resolve.transformers.AdapterForResolveProcessor

/**
 * Optimizes HeaderMode processing by stripping unnecessary private members. It performs a
 * reachability analysis to identify the minimal subset of internal structures required by
 * the public interface and inline definitions, then pruning any private members that are
 * unreachable.
 *
 * @param delegate The underlying body resolve processor that performs the actual resolution.
 */
@OptIn(AdapterForResolveProcessor::class)
class FirAggressivePruningProcessor(
    private val delegate: FirBodyResolveProcessor
) : FirTransformerBasedResolveProcessor(delegate.session, delegate.scopeSession, FirResolvePhase.BODY_RESOLVE) {

    override val transformer: FirTransformer<Nothing?>
        get() = delegate.transformer

    override fun processFile(file: FirFile) {
        delegate.processFile(file)

        val marker = FirReachabilityAnalyzer(delegate.session)
        val reachable = marker.collectReachableSymbols(file)

        val transformer = FirPruningTransformer(reachable)
        file.transform<FirFile, Nothing?>(transformer, data = null)
    }

    override fun beforePhase() {
        delegate.beforePhase()
    }

    override fun afterPhase() {
        delegate.afterPhase()
    }
}

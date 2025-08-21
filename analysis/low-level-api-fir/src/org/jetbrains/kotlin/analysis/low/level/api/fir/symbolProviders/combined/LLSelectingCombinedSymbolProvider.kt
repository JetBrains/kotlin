/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.combined

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.utils.errors.withKaModuleEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

/**
 * A combined symbol provider which *selects* a subset of its [providers] to delegate to by an element's [KaModule]. For example, a
 * selecting symbol provider might perform an index access, get a number of candidate PSI elements, and delegate to the appropriate provider
 * for the element directly.
 *
 * Classpath order must be preserved with [selectFirstElementInClasspathOrder] in case a single result is required.
 */
internal abstract class LLSelectingCombinedSymbolProvider<PROVIDER : FirSymbolProvider>(
    session: FirSession,
    project: Project,
    override val providers: List<PROVIDER>,
) : LLCombinedSymbolProvider<PROVIDER>(session) {
    /**
     * Maps each [KaModule] (corresponding uniquely to a symbol provider) to its index in [providers]. This determines the [KaModule]
     * precedence, which must be checked in case of multiple candidates to preserve classpath order.
     */
    private val moduleToIndex: Map<KaModule, Int> = buildMap {
        providers.forEachIndexed { index, provider ->
            val module = provider.session.llFirModuleData.ktModule
            if (module in this) {
                errorWithAttachment("`${module::class.simpleName}` must not be associated with multiple symbol providers.") {
                    withKaModuleEntry("module", module)
                }
            }
            put(module, index)
        }
    }

    /**
     * Cache [KotlinProjectStructureProvider] to avoid service access when getting [KaModule]s.
     */
    @KaCachedService
    private val projectStructureProvider: KotlinProjectStructureProvider = KotlinProjectStructureProvider.getInstance(project)

    private val contextualModule = session.llFirModuleData.ktModule

    protected fun getModule(element: PsiElement): KaModule {
        return projectStructureProvider.getModule(element, contextualModule)
    }

    /**
     * Selects the element with the highest module precedence in [candidates], returning the element and the provider to which resolution
     * should be delegated. This is a post-processing step that preserves classpath order when, for example, an index access with a combined
     * scope isn't guaranteed to return the first element in classpath order.
     */
    protected inline fun <CANDIDATE> selectFirstElementInClasspathOrder(
        candidates: Collection<CANDIDATE>,
        getElement: (CANDIDATE) -> PsiElement?,
    ): Pair<CANDIDATE, PROVIDER>? {
        if (candidates.isEmpty()) return null

        // We're using a custom implementation instead of `minBy` since we need the precedence as well to find the provider at the end.
        var currentCandidate: CANDIDATE? = null
        var currentPrecedence: Int = Int.MAX_VALUE

        for (candidate in candidates) {
            val element = getElement(candidate) ?: continue
            val module = getModule(element)

            // If `module` cannot be found in the map, `candidate` cannot be processed by any of the available providers, because none of
            // them belong to the correct module. We can skip in that case, because iterating through all providers wouldn't lead to any
            // results for `candidate`.
            val precedence = moduleToIndex[module] ?: continue
            if (precedence < currentPrecedence) {
                currentCandidate = candidate
                currentPrecedence = precedence
            }
        }

        val candidate = currentCandidate ?: return null

        // `currentPrecedence` will always be a valid index at this point, because the precedence was taken from `moduleToIndex`, which
        // corresponds directly to the indices in `providers`.
        val provider = providers[currentPrecedence]

        return Pair(candidate, provider)
    }

    protected fun getProviderByModule(module: KaModule): PROVIDER? =
        moduleToIndex[module]?.let { providers[it] }
}

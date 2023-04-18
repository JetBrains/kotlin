/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.ProjectStructureProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.utils.mapToIndex

/**
 * A combined symbol provider which *selects* a subset of its [providers] to delegate to by an element's [KtModule]. For example, a
 * selecting symbol provider might perform an index access, get a number of candidate PSI elements, and delegate to the appropriate provider
 * for the element directly.
 *
 * Classpath order must be preserved with [selectFirstElementInClasspathOrder] in case a single result is required.
 */
abstract class LLFirSelectingCombinedSymbolProvider<PROVIDER : FirSymbolProvider>(
    session: FirSession,
    project: Project,
    protected val providers: List<PROVIDER>,
) : FirSymbolProvider(session) {
    protected val providersByKtModule: Map<KtModule, PROVIDER> =
        providers
            .groupingBy { it.session.llFirModuleData.ktModule }
            // `reduce` invokes the `error` operation if it encounters a second element.
            .reduce { module, _, _ -> error("$module must have a unique symbol provider.") }

    /**
     * [KtModule] precedence must be checked in case of multiple candidates to preserve classpath order.
     */
    private val modulePrecedenceMap: Map<KtModule, Int> = providers.map { it.session.llFirModuleData.ktModule }.mapToIndex()

    /**
     * Cache [ProjectStructureProvider] to avoid service access when getting [KtModule]s.
     */
    private val projectStructureProvider: ProjectStructureProvider = ProjectStructureProvider.getInstance(project)

    private val contextualModule = session.llFirModuleData.ktModule

    protected fun getModule(element: PsiElement): KtModule {
        return projectStructureProvider.getModule(element, contextualModule)
    }

    /**
     * Selects the element with the highest module precedence in [candidates], returning the element and the provider to which resolution
     * should be delegated. This is a post-processing step that preserves classpath order when, for example, an index access with a combined
     * scope isn't guaranteed to return the first element in classpath order.
     */
    protected fun <CANDIDATE> selectFirstElementInClasspathOrder(
        candidates: Collection<CANDIDATE>,
        getElement: (CANDIDATE) -> PsiElement?,
    ): Pair<CANDIDATE, PROVIDER>? {
        if (candidates.isEmpty()) return null

        // We're using a custom implementation instead of `minBy` so that `ktModule` doesn't need to be fetched twice.
        var currentCandidate: CANDIDATE? = null
        var currentPrecedence: Int = Int.MAX_VALUE
        var currentKtModule: KtModule? = null

        for (candidate in candidates) {
            val element = getElement(candidate) ?: continue
            val ktModule = getModule(element)

            // If `ktModule` cannot be found in the map, `candidate` cannot be processed by any of the available providers, because none of
            // them belong to the correct module. We can skip in that case, because iterating through all providers wouldn't lead to any
            // results for `candidate`.
            val precedence = modulePrecedenceMap[ktModule] ?: continue
            if (precedence < currentPrecedence) {
                currentCandidate = candidate
                currentPrecedence = precedence
                currentKtModule = ktModule
            }
        }

        val candidate = currentCandidate ?: return null
        val ktModule = currentKtModule ?: error("`currentKtModule` must not be `null` when `currentCandidate` has been found.")

        // The provider will always be found at this point, because `modulePrecedenceMap` contains the same keys as `providersByKtModule`
        // and a precedence for `currentKtModule` must have been found in the previous step.
        val provider = providersByKtModule.getValue(ktModule)

        return Pair(candidate, provider)
    }
}

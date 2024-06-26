/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.platform.KaEngineService

/**
 * [KaSourceModificationService] is an **engine service** which handles cache invalidation after source code changes:
 *
 * - For local changes (in-block modification), the service invalidates local caches for e.g. classes. This level of granularity cannot be
 *   reached by any module-level event (see [KotlinModificationTopics]), because the specific entity to invalidate needs to be discovered
 *   via the changed [PsiElement].
 * - For non-local changes (out-of-block modification), the service publishes a
 *   module out-of-block [modification event][KotlinModificationTopics.MODULE_OUT_OF_BLOCK_MODIFICATION].
 *
 * The service performs change locality detection to classify whether a change to a given [PsiElement] is an in-block or out-of-block
 * modification (or no modification in case of a whitespace/comment change).
 *
 * An element may be submitted to consideration before or after the actual modification is performed, but service implementations don't
 * guarantee that the invalidation behavior is the same for before/after instances of the same modification.
 *
 * While out-of-block modification events can be published to the message bus directly, there is currently no infrastructure for publishing
 * in-block modification. Hence, platforms will need to use [KaSourceModificationService] for more granular cache invalidation, or as a last
 * resort publish out-of-block modification events for all changes.
 */
public interface KaSourceModificationService : KaEngineService {
    /**
     * Handles cache invalidation before/after [element] has been modified according to the [modificationType], as described in the KDoc of
     * [KaSourceModificationService]. The function must be called from a write action.
     *
     * If [KaElementModificationType.Unknown] is specified, the service must process the change pessimistically, so specifying a narrower
     * modification type is usually beneficial.
     *
     * Here are some examples for which [element] to pass:
     *
     * - [element] should be a [KtNamedFunction][org.jetbrains.kotlin.psi.KtNamedFunction] after dropping the function's
     *   [body][org.jetbrains.kotlin.psi.KtBlockExpression].
     * - [element] should be a [KtBlockExpression][org.jetbrains.kotlin.psi.KtBlockExpression] after replacing one body expression with
     *   another.
     * - [element] should be a [KtBlockExpression][org.jetbrains.kotlin.psi.KtBlockExpression] after adding a body to a function without a
     *   body.
     * - If [element] is the parent of an already removed element, [KaElementModificationType.ElementRemoved] should contain the removed
     *   element.
     */
    public fun handleElementModification(element: PsiElement, modificationType: KaElementModificationType)

    /**
     * Returns the farthest ancestor [PsiElement] of [element] which would be affected by an in-block modification to [element], or `null`
     * if it's uncertain.
     */
    public fun ancestorAffectedByInBlockModification(element: PsiElement): PsiElement?

    public companion object {
        public fun getInstance(project: Project): KaSourceModificationService = project.service()
    }
}

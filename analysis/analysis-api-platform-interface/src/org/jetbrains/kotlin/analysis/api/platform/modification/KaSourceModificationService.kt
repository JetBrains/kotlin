/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.KaEngineService

/**
 * [KaSourceModificationService] is an **engine service** which handles cache invalidation after source code changes:
 *
 * - For local changes (in-block modification and whitespace), the service invalidates local caches for, e.g., classes. This level of
 *   granularity cannot be reached by any module-level event (see [KotlinModificationEvent]), because the specific entity to invalidate
 *   needs to be discovered via the changed [PsiElement].
 * - For non-local changes (out-of-block modification), the service publishes a module out-of-block
 *   [modification event][KotlinModuleOutOfBlockModificationEvent].
 *
 * The service performs change locality detection to classify a change to a given [PsiElement] in terms of [KaSourceModificationLocality].
 *
 * An element may be submitted to consideration before or after the actual modification is performed, but service implementations don't
 * guarantee that the invalidation behavior is the same for before/after instances of the same modification.
 *
 * While out-of-block modification events can be published to the message bus directly, there is currently no infrastructure for publishing
 * in-block modification. Hence, platforms will need to use [KaSourceModificationService] for more granular cache invalidation, or as a last
 * resort publish out-of-block modification events for all changes.
 */
@KaPlatformInterface
public interface KaSourceModificationService : KaEngineService {
    /**
     * Classifies the modification of [element] and its [modificationType] in terms of [KaSourceModificationLocality], as described in the
     * KDoc of [KaSourceModificationService]. The function may be called before and after [element]'s modification.
     *
     * If [KaElementModificationType.Unknown] is specified, the service must classify the modification pessimistically, so specifying a
     * narrower modification type is usually beneficial.
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
     * - If [element] is the replacement, [KaElementModificationType.ElementReplaced] should contain the replaced element.
     */
    public fun detectLocality(element: PsiElement, modificationType: KaElementModificationType): KaSourceModificationLocality

    /**
     * Handles the cache invalidation for [element]'s modification based on the detected [modificationLocality].
     *
     * The function must be called from a write action.
     *
     * @param modificationLocality The modification locality detected by [detectLocality]. It *must* have been provided by this service. No
     *  other [KaSourceModificationLocality] should be passed to the function.
     *
     * @see detectLocality
     */
    public fun handleInvalidation(element: PsiElement, modificationLocality: KaSourceModificationLocality)

    /**
     * Returns the farthest ancestor [PsiElement] of [element] which would be affected by an in-block modification to [element], or `null`
     * if it's uncertain.
     */
    public fun ancestorAffectedByInBlockModification(element: PsiElement): PsiElement?

    @KaPlatformInterface
    public companion object {
        public fun getInstance(project: Project): KaSourceModificationService = project.service()
    }
}

/**
 * Detects the modification locality of [element] and handles the corresponding cache invalidation.
 *
 * This is a convenience function that combines [KaSourceModificationService.detectLocality] and
 * [KaSourceModificationService.handleInvalidation].
 *
 * The function must be called from a write action.
 *
 * @see KaSourceModificationService.detectLocality
 * @see KaSourceModificationService.handleInvalidation
 */
public fun KaSourceModificationService.handleElementModification(element: PsiElement, modificationType: KaElementModificationType) {
    val modificationLocality = detectLocality(element, modificationType)
    handleInvalidation(element, modificationLocality)
}

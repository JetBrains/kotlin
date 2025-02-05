/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.restrictedAnalysis

import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.KotlinOptionalPlatformComponent

/**
 * Allows the platform to communicate to the Analysis API about restricted analysis. When analysis is restricted, the platform typically
 * lacks full, up-to-date information about the project. For example, [declaration providers][org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider]
 * might return incomplete results. In addition, the available information might change without associated [modification events][org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTopics].
 *
 * As a consequence, results returned by the Analysis API might be incomplete or incorrect. Handling such results is the responsibility of
 * the Analysis API user. The user should be aware of the platform's conventions around its restricted analysis mode and develop
 * accordingly.
 *
 * In IntelliJ, restricted analysis mode is synonymous with its [dumb mode](https://plugins.jetbrains.com/docs/intellij/indexing-and-psi-stubs.html#dumb-mode),
 * which is typically enabled when files are being re-indexed.
 *
 * Any exceptions which occur during restricted analysis are wrapped in [KaRestrictedAnalysisException] (see its KDoc for more information).
 *
 * If [KotlinRestrictedAnalysisService] is not registered, it's assumed that the platform has no conception of restricted analysis. When
 * there is a restricted analysis mode, but analysis should be disallowed during the mode, [KotlinRestrictedAnalysisService] should still be
 * implemented, setting [isRestrictedAnalysisAllowed] to `false`.
 *
 * ### Implementation notes
 *
 * It is the responsibility of the platform to ensure that [declaration providers][org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider]
 * and [package providers][org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider] are adapted to restricted analysis.
 * This also extends to other platform components which might be affected by restricted analysis.
 *
 * The platform must ensure that [modification events][org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTopics] are
 * published when *entering* and *exiting* restricted analysis mode:
 *
 * - The platform must publish modification events for all [KaModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaModule]s which
 *   might be affected by incomplete, inconsistent, or changing information when *entering* restricted analysis mode. The kind of
 *   modification event (module state modification, out-of-block modification) to publish depends on the event which triggered the
 *   restricted analysis mode.
 *     - Generally, the usual modification events published by the platform in reaction to project changes should be sufficient to cover
 *       this requirement.
 * - A [global module state modification event][org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTopics.GLOBAL_MODULE_STATE_MODIFICATION]
 *   must be published when *exiting* restricted analysis mode, to mitigate potential issues with inconsistent cache states that accumulated
 *   during restricted analysis.
 */
public interface KotlinRestrictedAnalysisService : KotlinOptionalPlatformComponent {
    /**
     * Whether the Analysis API platform is currently in *restricted analysis mode*.
     */
    public val isAnalysisRestricted: Boolean

    /**
     * Whether Analysis API access during restricted analysis mode is allowed by the platform. Such analysis activity is also called simply
     * *restricted analysis*.
     *
     * Depending on this setting, the Analysis API will act differently during restricted analysis mode:
     *
     * - When restricted analysis is allowed, requests to the Analysis API will be accepted, and analysis will be performed according to
     *   the rules of restricted analysis (see [KotlinRestrictedAnalysisService]).
     * - Otherwise, the Analysis API will reject the request using [rejectRestrictedAnalysis].
     */
    public val isRestrictedAnalysisAllowed: Boolean

    /**
     * Throws an exception after Analysis API access was rejected during restricted analysis mode.
     */
    public fun rejectRestrictedAnalysis(): Nothing

    public companion object {
        public fun getInstance(project: Project): KotlinRestrictedAnalysisService? = project.serviceOrNull()
    }
}

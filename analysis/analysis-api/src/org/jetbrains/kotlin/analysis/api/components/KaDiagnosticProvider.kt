/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DeprecatedCallableAddReplaceWith")

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaObsoleteComponentApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaDiagnosticProvider : KaSessionComponent {
    /**
     * Collects diagnostics for the given element.
     *
     * **Caution:** The result might not include diagnostics that are reported for child elements, as well as diagnostics provided by the
     * checkers of containing elements. Therefore, the API might not return all expected diagnostics for an element.
     * [KtFile.collectDiagnostics] should be preferred at the current time.
     *
     * Deprecated: Use [directDiagnostics][org.jetbrains.kotlin.analysis.api.diagnostics.directDiagnostics] instead. The [filter] becomes a
     * [withCheckers][org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics.withCheckers] modifier of the returned query:
     *
     * ```kotlin
     * element.directDiagnostics().withCheckers(KaDiagnosticCheckerKind.COMMON, KaDiagnosticCheckerKind.EXTENDED)
     * ```
     */
    @KaExperimentalApi
    @Deprecated(
        "Use the 'org.jetbrains.kotlin.analysis.api.diagnostics.directDiagnostics' endpoint instead." +
                " The 'filter' argument becomes a 'withCheckers' modifier of the returned 'KaDiagnostics'."
    )
    public fun KtElement.diagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>>

    /**
     * Collects diagnostics for the given element.
     *
     * **Caution:** The result might not include diagnostics that are reported for child elements, as well as diagnostics provided by the
     * checkers of containing elements. Therefore, the API might not return all expected diagnostics for an element.
     * [KtFile.collectDiagnostics] should be preferred at the current time.
     *
     * Deprecated: Use [directDiagnostics][org.jetbrains.kotlin.analysis.api.diagnostics.directDiagnostics] instead. The [filter] becomes a
     * [withCheckers][org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics.withCheckers] modifier of the returned query:
     *
     * ```kotlin
     * element.directDiagnostics().withCheckers(KaDiagnosticCheckerKind.COMMON, KaDiagnosticCheckerKind.EXTENDED)
     * ```
     */
    @KaExperimentalApi
    @Deprecated(
        "Use the 'org.jetbrains.kotlin.analysis.api.diagnostics.directDiagnostics' endpoint instead." +
                " The 'filter' argument becomes a 'withCheckers' modifier of the returned 'KaDiagnostics'."
    )
    public fun KtElement.directDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>>

    /**
     * Collects all diagnostics for the given file.
     *
     * Eager version of [KtFile.diagnostics].
     *
     * @see KtFile.diagnostics
     */
    public fun KtFile.collectDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>>

    /**
     * Returns a [Sequence] of all diagnostics for the given file.
     *
     * This is a [Sequence]-based version of [collectDiagnostics].
     *
     * Deprecated: Use [diagnostics][org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics] instead. The [filter] becomes a
     * [withCheckers][org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics.withCheckers] modifier of the returned query:
     *
     * ```kotlin
     * file.diagnostics().withCheckers(KaDiagnosticCheckerKind.COMMON, KaDiagnosticCheckerKind.EXTENDED)
     * ```
     *
     * @see collectDiagnostics
     */
    @KaExperimentalApi
    @Deprecated(
        "Use the 'org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics' endpoint instead." +
                " The 'filter' argument becomes a 'withCheckers' modifier of the returned 'KaDiagnostics'."
    )
    public fun KtFile.diagnostics(filter: KaDiagnosticCheckerFilter): Sequence<KaDiagnosticWithPsi<*>>

    /**
     * Returns a [Sequence] of all diagnostics for the given file, including those that would normally be suppressed
     * (e.g. by `@Suppress` annotations).
     *
     * Deprecated: Use [diagnostics][org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics] with
     * [includingSuppressed][org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics.includingSuppressed] instead. The [filter] becomes
     * a [withCheckers][org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics.withCheckers] modifier of the returned query:
     *
     * ```kotlin
     * file.diagnostics().withCheckers(KaDiagnosticCheckerKind.COMMON, KaDiagnosticCheckerKind.EXTENDED).includingSuppressed()
     * ```
     *
     * @see diagnostics
     */
    @KaExperimentalApi
    @Deprecated(
        "Use the 'org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics' endpoint with 'includingSuppressed()' instead." +
                " The 'filter' argument becomes a 'withCheckers' modifier of the returned 'KaDiagnostics'."
    )
    public fun KtFile.diagnosticsIgnoringSuppression(filter: KaDiagnosticCheckerFilter): Sequence<KaDiagnosticWithPsi<*>>
}

/**
 * [KaDiagnosticCheckerFilter] controls which kinds of diagnostics are included in the result of diagnostic collection.
 */
@KaObsoleteComponentApi
public enum class KaDiagnosticCheckerFilter {
    /**
     * Includes diagnostics only from the compiler's common checkers.
     */
    ONLY_COMMON_CHECKERS,

    /**
     * Includes diagnostics from extended checkers (that typically run only in the IDE).
     */
    ONLY_EXTENDED_CHECKERS,

    /**
     * Includes diagnostics from experimental checkers.
     *
     * Their role is to be run in the IDE similar to [ONLY_EXTENDED_CHECKERS] with the following differences:
     * * They might have false positives
     * * They might be slow
     */
    ONLY_EXPERIMENTAL_CHECKERS,

    /**
     * Includes diagnostics from both common and extended checkers.
     */
    EXTENDED_AND_COMMON_CHECKERS,
}

/**
 * Collects diagnostics for the given element.
 *
 * **Caution:** The result might not include diagnostics that are reported for child elements, as well as diagnostics provided by the
 * checkers of containing elements. Therefore, the API might not return all expected diagnostics for an element.
 * [KtFile.collectDiagnostics] should be preferred at the current time.
 *
 * Deprecated: Use [directDiagnostics][org.jetbrains.kotlin.analysis.api.diagnostics.directDiagnostics] instead. The [filter] becomes a
 * [withCheckers][org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics.withCheckers] modifier of the returned query:
 *
 * ```kotlin
 * element.directDiagnostics().withCheckers(KaDiagnosticCheckerKind.COMMON, KaDiagnosticCheckerKind.EXTENDED)
 * ```
 */
@KaExperimentalApi
@Deprecated(
    "Use the 'org.jetbrains.kotlin.analysis.api.diagnostics.directDiagnostics' endpoint instead." +
            " The 'filter' argument becomes a 'withCheckers' modifier of the returned 'KaDiagnostics'."
)
context(session: KaSession)
public fun KtElement.diagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> {
    @Suppress("DEPRECATION")
    return with(session) {
        diagnostics(
            filter = filter,
        )
    }
}

/**
 * Collects diagnostics for the given element.
 *
 * **Caution:** The result might not include diagnostics that are reported for child elements, as well as diagnostics provided by the
 * checkers of containing elements. Therefore, the API might not return all expected diagnostics for an element.
 * [KtFile.collectDiagnostics] should be preferred at the current time.
 *
 * Deprecated: Use [directDiagnostics][org.jetbrains.kotlin.analysis.api.diagnostics.directDiagnostics] instead. The [filter] becomes a
 * [withCheckers][org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics.withCheckers] modifier of the returned query:
 *
 * ```kotlin
 * element.directDiagnostics().withCheckers(KaDiagnosticCheckerKind.COMMON, KaDiagnosticCheckerKind.EXTENDED)
 * ```
 */
@KaExperimentalApi
@Deprecated(
    "Use the 'org.jetbrains.kotlin.analysis.api.diagnostics.directDiagnostics' endpoint instead." +
            " The 'filter' argument becomes a 'withCheckers' modifier of the returned 'KaDiagnostics'."
)
context(session: KaSession)
public fun KtElement.directDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> {
    @Suppress("DEPRECATION")
    return with(session) {
        directDiagnostics(
            filter = filter,
        )
    }
}

/**
 * Collects all diagnostics for the given file.
 *
 * Eager version of [KtFile.diagnostics].
 *
 * @see KtFile.diagnostics
 */
@KaObsoleteComponentApi
context(session: KaSession)
public fun KtFile.collectDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> {
    return with(session) {
        collectDiagnostics(
            filter = filter,
        )
    }
}

/**
 * Returns a [Sequence] of all diagnostics for the given file.
 *
 * This is a [Sequence]-based version of [collectDiagnostics].
 *
 * Deprecated: Use [diagnostics][org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics] instead. The [filter] becomes a
 * [withCheckers][org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics.withCheckers] modifier of the returned query:
 *
 * ```kotlin
 * file.diagnostics().withCheckers(KaDiagnosticCheckerKind.COMMON, KaDiagnosticCheckerKind.EXTENDED)
 * ```
 *
 * @see collectDiagnostics
 */
@KaExperimentalApi
@Deprecated(
    "Use the 'org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics' endpoint instead." +
            " The 'filter' argument becomes a 'withCheckers' modifier of the returned 'KaDiagnostics'."
)
context(session: KaSession)
public fun KtFile.diagnostics(filter: KaDiagnosticCheckerFilter): Sequence<KaDiagnosticWithPsi<*>> {
    @Suppress("DEPRECATION")
    return with(session) {
        diagnostics(
            filter = filter,
        )
    }
}

/**
 * Returns a [Sequence] of all diagnostics for the given file, including those that would normally be suppressed
 * (e.g. by `@Suppress` annotations).
 *
 * Deprecated: Use [diagnostics][org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics] with
 * [includingSuppressed][org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics.includingSuppressed] instead. The [filter] becomes a
 * [withCheckers][org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics.withCheckers] modifier of the returned query:
 *
 * ```kotlin
 * file.diagnostics().withCheckers(KaDiagnosticCheckerKind.COMMON, KaDiagnosticCheckerKind.EXTENDED).includingSuppressed()
 * ```
 *
 * @see diagnostics
 */
@KaExperimentalApi
@Deprecated(
    "Use the 'org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics' endpoint with 'includingSuppressed()' instead." +
            " The 'filter' argument becomes a 'withCheckers' modifier of the returned 'KaDiagnostics'."
)
context(session: KaSession)
public fun KtFile.diagnosticsIgnoringSuppression(filter: KaDiagnosticCheckerFilter): Sequence<KaDiagnosticWithPsi<*>> {
    @Suppress("DEPRECATION")
    return with(session) {
        diagnosticsIgnoringSuppression(
            filter = filter,
        )
    }
}

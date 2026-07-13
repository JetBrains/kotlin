/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaResolver

/**
 * Describes how [context-sensitive resolution](https://github.com/Kotlin/KEEP/issues/379) (CSR) relates to a
 * [org.jetbrains.kotlin.psi.KtSimpleNameExpression].
 *
 * It tells both whether the name is already resolved through CSR and whether CSR *could* be used to drop an otherwise
 * redundant qualifier or import. IDE inspections and shorteners can use this information to suggest removing such
 * qualifiers and imports.
 *
 * The information is available even when the `-Xcontext-sensitive-resolution` feature is not enabled.
 *
 * The status of a particular name is obtained via the `contextSensitiveResolutionStatus` property declared in [KaResolver].
 */
@KaExperimentalApi
public sealed interface KaContextSensitiveResolutionStatus {
    /**
     * Context-sensitive resolution is neither used by, nor applicable to, the name.
     */
    @KaExperimentalApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface NotAvailable : KaContextSensitiveResolutionStatus

    /**
     * The name is already resolved through context-sensitive resolution.
     *
     * #### Example
     *
     * ```kotlin
     * enum class Foo { BAR }
     *
     * fun usage(foo: Foo) {}
     *
     * fun main() {
     *     usage(BAR) // 'BAR' is resolved against the expected type 'Foo'
     * }
     * ```
     */
    @KaExperimentalApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface Used : KaContextSensitiveResolutionStatus

    /**
     * The name is resolved through an explicit qualifier which can be removed: context-sensitive resolution would
     * resolve the same symbol without it.
     *
     * #### Example
     *
     * ```kotlin
     * enum class Foo { BAR }
     *
     * fun usage(): Foo {
     *     return Foo.BAR // the 'Foo.' qualifier can be removed
     * }
     * ```
     */
    @KaExperimentalApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface QualifierCanBeRemoved : KaContextSensitiveResolutionStatus

    /**
     * The name is resolved through an import which can be removed: context-sensitive resolution would resolve the same
     * symbol without it.
     *
     * #### Example
     *
     * ```kotlin
     * import Foo.BAR
     *
     * enum class Foo { BAR }
     *
     * fun usage(): Foo {
     *     return BAR // the 'import Foo.BAR' can be removed
     * }
     * ```
     */
    @KaExperimentalApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface ImportCanBeRemoved : KaContextSensitiveResolutionStatus

    /**
     * Exists only to keep `when` expressions over [KaContextSensitiveResolutionStatus] non-exhaustive, so that new
     * variants can be added later without breaking clients. This status is never returned.
     */
    @Suppress("unused")
    @KaExperimentalApi
    private data object Unknown : KaContextSensitiveResolutionStatus
}

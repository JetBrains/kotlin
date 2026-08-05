/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import kotlin.reflect.KClass

/**
 * The severity of a [KaDiagnostic].
 */
public enum class KaSeverity {
    ERROR,
    WARNING,
    INFO
}

/**
 * A diagnostic message reported by the compiler checker.
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaDiagnostic : KaLifetimeOwner {
    public val diagnosticClass: KClass<*>

    /**
     * A technical name identifying the diagnostic.
     *
     * This is the same name as reported by the compiler when the `-Xrender-internal-diagnostic-names` compiler flag is enabled.
     *
     * #### Example
     *
     * ```kotlin
     * fun test() {
     *     foo(1)
     * }
     * ```
     *
     * Because `foo` is not defined, the compiler reports an `UNRESOLVED_REFERENCE` error. This would also be the [factoryName] of the
     * [KaDiagnostic].
     */
    public val factoryName: String

    /**
     * The severity of the diagnostic (e.g. [error][KaSeverity.ERROR] or [warning][KaSeverity.WARNING]), which is determined from the
     * compiler's classification of the diagnostic.
     */
    public val severity: KaSeverity

    /**
     * Whether the diagnostic is suppressed at its use site, e.g., by a `@Suppress` annotation.
     *
     * Suppressed diagnostics are not reported by the compiler, so they should not be presented to the user as is. By default, diagnostic
     * collection filters them out; [KaDiagnostics.ignoreSuppressed] yields them as well.
     *
     * The property is only meaningful for diagnostics obtained from diagnostic collection. For diagnostics obtained in another way, such as
     * diagnostics of an unresolved call, the property is always `false`.
     *
     * #### Example
     *
     * ```kotlin
     * @Suppress("UNUSED_VARIABLE")
     * fun foo() {
     *     val unused = 0
     * }
     * ```
     *
     * The `UNUSED_VARIABLE` diagnostic reported on `unused` is suppressed.
     */
    @KaExperimentalApi
    public val isSuppressed: Boolean

    /**
     * The human-readable message rendered by the compiler to describe the error, warning, or info.
     */
    public val defaultMessage: String
}

/**
 * A [KaDiagnostic] reported on a [PsiElement] of type [PSI].
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaDiagnosticWithPsi<out PSI : PsiElement> : KaDiagnostic {
    /**
     * The PSI element that the diagnostic is reported on.
     */
    public val psi: PSI

    /**
     * The text ranges within the [psi] element where the diagnostic occurs.
     */
    public val textRanges: Collection<TextRange>

    public override val diagnosticClass: KClass<out KaDiagnosticWithPsi<PSI>>
}

/**
 * Returns a formatted string of the diagnostic's [factory name][KaDiagnostic.factoryName] and [default message][KaDiagnostic.defaultMessage].
 */
public fun KaDiagnostic.getDefaultMessageWithFactoryName(): String {
    return "[$factoryName] $defaultMessage"
}

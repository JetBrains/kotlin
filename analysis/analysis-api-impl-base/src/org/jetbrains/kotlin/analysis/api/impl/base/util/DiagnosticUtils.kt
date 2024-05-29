/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.diagnostics.KaSeverity
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.Severity.*
import kotlin.reflect.KClass

class KaNonBoundToPsiErrorDiagnostic(
    override val factoryName: String,
    override val defaultMessage: String,
    override val token: KaLifetimeToken,
) : KaDiagnostic {
    override val severity: KaSeverity
        get() = withValidityAssertion { KaSeverity.ERROR }

    override val diagnosticClass: KClass<*>
        get() = KaNonBoundToPsiErrorDiagnostic::class
}

fun Severity.toAnalysisApiSeverity(): KaSeverity {
    return when (this) {
        ERROR -> KaSeverity.ERROR
        WARNING -> KaSeverity.WARNING
        INFO -> KaSeverity.INFO
    }
}
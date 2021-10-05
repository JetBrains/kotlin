/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.config.LanguageVersionSettings

interface DiagnosticContext {
    val containingFilePath: String?

    fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean

    val languageVersionSettings: LanguageVersionSettings
}

abstract class MutableDiagnosticContext : DiagnosticContext {
    abstract fun addSuppressedDiagnostics(
        diagnosticNames: Collection<String>,
        allInfosSuppressed: Boolean,
        allWarningsSuppressed: Boolean,
        allErrorsSuppressed: Boolean
    ): DiagnosticContext
}

abstract class DiagnosticReporter {
    abstract fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext)
}

open class KtDiagnosticReporterWithContext(
    val diagnosticReporter: DiagnosticReporter,
    val languageVersionSettings: LanguageVersionSettings
) : DiagnosticReporter() {
    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) = diagnosticReporter.report(diagnostic, context)

    fun at(sourceElement: AbstractKtSourceElement?, containingFilePath: String): DiagnosticContextImpl =
        DiagnosticContextImpl(sourceElement, containingFilePath)

    open inner class DiagnosticContextImpl(
        val sourceElement: AbstractKtSourceElement?,
        override val containingFilePath: String
    ) : DiagnosticContext {

        override fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean {
            return false
//            TODO("Not yet implemented")
        }

        override val languageVersionSettings: LanguageVersionSettings
            get() = this@KtDiagnosticReporterWithContext.languageVersionSettings

        @OptIn(InternalDiagnosticFactoryMethod::class)
        fun report(
            factory: KtDiagnosticFactory0,
            positioningStrategy: AbstractSourceElementPositioningStrategy? = null
        ) {
            sourceElement?.let { report(factory.on(it, positioningStrategy), this) }
        }

        @OptIn(InternalDiagnosticFactoryMethod::class)
        fun <A : Any> report(
            factory: KtDiagnosticFactory1<A>,
            a: A,
            positioningStrategy: AbstractSourceElementPositioningStrategy? = null
        ) {
            sourceElement?.let { report(factory.on(it, a, positioningStrategy), this) }
        }
    }
}
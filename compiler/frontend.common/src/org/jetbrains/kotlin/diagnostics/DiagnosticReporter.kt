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

abstract class DiagnosticReporter {
    abstract fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext)

    open fun checkAndCommitReportsOn(element: AbstractKtSourceElement, context: DiagnosticContext?) {
    }
}

open class KtDiagnosticReporterWithContext(
    val diagnosticReporter: DiagnosticReporter,
    val languageVersionSettings: LanguageVersionSettings
) : DiagnosticReporter() {
    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) = diagnosticReporter.report(diagnostic, context)

    override fun checkAndCommitReportsOn(element: AbstractKtSourceElement, context: DiagnosticContext?) {
        diagnosticReporter.checkAndCommitReportsOn(element, context)
    }

    open fun at(sourceElement: AbstractKtSourceElement?, containingFilePath: String): DiagnosticContextImpl =
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

        fun report(factory: KtDiagnosticFactory0) {
            sourceElement?.let {
                reportOn(it, factory, this)
                checkAndCommitReportsOn(it, this)
            }
        }

        fun <A : Any> report(factory: KtDiagnosticFactory1<A>, a: A) {
            sourceElement?.let {
                reportOn(it, factory, a, this)
                checkAndCommitReportsOn(it, this)
            }
        }

        fun <A : Any, B : Any> report(factory: KtDiagnosticFactory2<A, B>, a: A, b: B) {
            sourceElement?.let {
                reportOn(it, factory, a, b, this)
                checkAndCommitReportsOn(it, this)
            }
        }

        fun <A : Any, B : Any, C : Any> report(factory: KtDiagnosticFactory3<A, B, C>, a: A, b: B, c: C) {
            sourceElement?.let {
                reportOn(it, factory, a, b, c, this)
                checkAndCommitReportsOn(it, this)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DiagnosticContextImpl) return false

            if (sourceElement != other.sourceElement) return false
            if (containingFilePath != other.containingFilePath) return false
            if (languageVersionSettings != other.languageVersionSettings) return false

            return true
        }

        override fun hashCode(): Int {
            var result = sourceElement?.hashCode() ?: 0
            result = 31 * result + containingFilePath.hashCode()
            result = 31 * result + languageVersionSettings.hashCode()
            return result
        }
    }
}

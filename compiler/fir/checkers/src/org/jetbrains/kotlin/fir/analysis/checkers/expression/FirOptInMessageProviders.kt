/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirOptInUsageBaseChecker.Experimentality.Severity

abstract class FirOptInDiagnosticMessageProvider {
    abstract fun buildDiagnosticMessage(markerName: String, severity: Severity, customMessage: String? = null): String

    protected fun getMessageVerb(severity: Severity): String = when (severity) {
        Severity.WARNING -> "should"
        Severity.ERROR -> "must"
    }

    protected fun StringBuilder.buildMessageWithPrefix(
        prefix: String,
        customMessage: String?,
    ): StringBuilder {
        this.append(prefix)
        if (customMessage != null && customMessage.isNotBlank()) {
            this.append(buildCustomMessage(customMessage))
        } else {
            this.append(".")
        }
        this.append(" ")
        return this
    }

    private fun buildCustomMessage(customMessage: String): String {
        val message = ": $customMessage".trim()
        return if (message.endsWithSentenceTerminator()) message else "$message."
    }

    private fun String.endsWithSentenceTerminator(): Boolean = this.trim().matches(Regex(".*[.!?]$"))
}


object FirOptInUsagesDiagnosticMessageProvider : FirOptInDiagnosticMessageProvider() {
    override fun buildDiagnosticMessage(
        markerName: String,
        severity: Severity,
        customMessage: String?,
    ): String {
        val diagnosticMessage = StringBuilder()
        val verb = getMessageVerb(severity)

        diagnosticMessage.buildMessageWithPrefix("This declaration requires opt-in to be used", customMessage)

        diagnosticMessage.append("The usage $verb be annotated with '@$markerName' or '@OptIn($markerName::class)'")
        return diagnosticMessage.toString()
    }

}


class FirOptInInheritanceDiagnosticMessageProvider(private val isSubclassOptInApplicable: Boolean) : FirOptInDiagnosticMessageProvider() {
    override fun buildDiagnosticMessage(
        markerName: String,
        severity: Severity,
        customMessage: String?,
    ): String {
        val diagnosticMessage = StringBuilder()
        val verb = getMessageVerb(severity)

        diagnosticMessage.buildMessageWithPrefix("This class or interface requires opt-in to be implemented", customMessage)

        when {
            isSubclassOptInApplicable -> diagnosticMessage.append("The implementation $verb be annotated with '@$markerName', '@OptIn($markerName::class)' or '@SubclassOptInRequired($markerName::class)'")
            else -> diagnosticMessage.append("The implementation $verb be annotated with '@$markerName' or '@OptIn($markerName::class)'")
        }
        return diagnosticMessage.toString()
    }
}

class FirOptInOverrideDiagnosticMessageProvider(private val supertypeName: String) : FirOptInDiagnosticMessageProvider() {
    override fun buildDiagnosticMessage(
        markerName: String,
        severity: Severity,
        customMessage: String?,
    ): String {
        val diagnosticMessage = StringBuilder()
        val verb = getMessageVerb(severity)

        diagnosticMessage.buildMessageWithPrefix(
            "Base declaration of supertype '$supertypeName' requires opt-in to be overridden",
            customMessage
        )

        diagnosticMessage.append("The overriding declaration $verb be annotated with '@$markerName' or '@OptIn($markerName::class)'")
        return diagnosticMessage.toString()
    }

}
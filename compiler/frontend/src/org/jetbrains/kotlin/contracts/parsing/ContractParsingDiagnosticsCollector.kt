/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.parsing

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingTrace

interface ContractParsingDiagnosticsCollector {
    fun unsupportedFeature(languageVersionSettings: LanguageVersionSettings)
    fun contractNotAllowed(message: String)
    fun badDescription(message: String, reportOn: KtElement)
    fun addFallbackErrorIfNecessary()

    fun flushDiagnostics()
    fun hasErrors(): Boolean


    object EMPTY : ContractParsingDiagnosticsCollector {
        override fun contractNotAllowed(message: String) {}
        override fun badDescription(message: String, reportOn: KtElement) {}
        override fun unsupportedFeature(languageVersionSettings: LanguageVersionSettings) {}
        override fun addFallbackErrorIfNecessary() { }

        override fun flushDiagnostics() {}

        override fun hasErrors(): Boolean = false
    }
}

class TraceBasedCollector(private val bindingTrace: BindingTrace, mainCall: KtExpression) : ContractParsingDiagnosticsCollector {
    constructor(callContext: ContractCallContext) : this(callContext.trace, callContext.contractCallExpression)

    private val reportedErrors: MutableList<Diagnostic> = mutableListOf()
    private val mainCallReportTarget = (mainCall as? KtCallExpression)?.calleeExpression ?: mainCall

    override fun contractNotAllowed(message: String) {
        reportedErrors += Errors.CONTRACT_NOT_ALLOWED.on(mainCallReportTarget, message)
    }

    override fun badDescription(message: String, reportOn: KtElement) {
        reportedErrors += Errors.ERROR_IN_CONTRACT_DESCRIPTION.on(reportOn, message)
    }

    override fun unsupportedFeature(languageVersionSettings: LanguageVersionSettings) {
        reportedErrors += Errors.UNSUPPORTED_FEATURE.on(
            mainCallReportTarget,
            LanguageFeature.AllowContractsForCustomFunctions to languageVersionSettings
        )
    }

    override fun addFallbackErrorIfNecessary() {
        if (reportedErrors.isEmpty())
            reportedErrors += Errors.ERROR_IN_CONTRACT_DESCRIPTION.on(mainCallReportTarget, "Error in contract description")
    }

    override fun flushDiagnostics() {
        reportedErrors.forEach { bindingTrace.report(it) }
    }


    override fun hasErrors(): Boolean = reportedErrors.isNotEmpty()
}
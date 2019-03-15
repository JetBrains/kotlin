/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers.diagnostics.factories

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.checkers.diagnostics.DebugInfoDiagnostic
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.PositioningStrategies
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory

class DebugInfoDiagnosticFactory0 : DiagnosticFactory0<PsiElement>,
    DebugInfoDiagnosticFactory {
    private val name: String
    override val withExplicitDefinitionOnly: Boolean

    override fun createDiagnostic(
        expression: KtExpression,
        bindingContext: BindingContext,
        dataFlowValueFactory: DataFlowValueFactory?,
        languageVersionSettings: LanguageVersionSettings?,
        moduleDescriptor: ModuleDescriptorImpl?
    ): Diagnostic {
        return DebugInfoDiagnostic(expression, this)
    }

    private constructor(name: String, severity: Severity = Severity.ERROR) : super(severity, PositioningStrategies.DEFAULT) {
        this.name = name
        this.withExplicitDefinitionOnly = false
    }

    private constructor(name: String, severity: Severity, withExplicitDefinitionOnly: Boolean) : super(
        severity,
        PositioningStrategies.DEFAULT
    ) {
        this.name = name
        this.withExplicitDefinitionOnly = withExplicitDefinitionOnly
    }

    override fun getName(): String {
        return "DEBUG_INFO_$name"
    }

    companion object {
        val SMARTCAST = DebugInfoDiagnosticFactory0("SMARTCAST", Severity.INFO)
        val IMPLICIT_RECEIVER_SMARTCAST =
            DebugInfoDiagnosticFactory0("IMPLICIT_RECEIVER_SMARTCAST", Severity.INFO)
        val CONSTANT = DebugInfoDiagnosticFactory0("CONSTANT", Severity.INFO)
        val LEAKING_THIS = DebugInfoDiagnosticFactory0("LEAKING_THIS")
        val IMPLICIT_EXHAUSTIVE =
            DebugInfoDiagnosticFactory0("IMPLICIT_EXHAUSTIVE", Severity.INFO)
        val ELEMENT_WITH_ERROR_TYPE = DebugInfoDiagnosticFactory0("ELEMENT_WITH_ERROR_TYPE")
        val UNRESOLVED_WITH_TARGET = DebugInfoDiagnosticFactory0("UNRESOLVED_WITH_TARGET")
        val MISSING_UNRESOLVED = DebugInfoDiagnosticFactory0("MISSING_UNRESOLVED")
        val DYNAMIC = DebugInfoDiagnosticFactory0("DYNAMIC", Severity.INFO)
    }
}
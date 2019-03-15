/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers.diagnostics.factories

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.PositioningStrategies
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory

class DebugInfoDiagnosticFactory1 : DiagnosticFactory1<PsiElement, String>,
    DebugInfoDiagnosticFactory {
    private val name: String

    override fun getName(): String {
        return "DEBUG_INFO_$name"
    }

    override val withExplicitDefinitionOnly: Boolean

    override fun createDiagnostic(
        expression: KtExpression,
        bindingContext: BindingContext,
        dataFlowValueFactory: DataFlowValueFactory?,
        languageVersionSettings: LanguageVersionSettings?,
        moduleDescriptor: ModuleDescriptorImpl?
    ) = when (name) {
        "EXPRESSION_TYPE" -> {
            val (type, dataFlowTypes) = CheckerTestUtil.getTypeInfo(
                expression,
                bindingContext,
                dataFlowValueFactory,
                languageVersionSettings,
                moduleDescriptor
            )

            this.on(expression, Renderers.renderExpressionType(type, dataFlowTypes))
        }
        else -> throw NotImplementedError("Creation diagnostic '$name' isn't supported.")
    }

    protected constructor(name: String, severity: Severity) : super(severity, PositioningStrategies.DEFAULT) {
        this.name = name
        this.withExplicitDefinitionOnly = false
    }

    protected constructor(name: String, severity: Severity, withExplicitDefinitionOnly: Boolean) : super(
        severity,
        PositioningStrategies.DEFAULT
    ) {
        this.name = name
        this.withExplicitDefinitionOnly = withExplicitDefinitionOnly
    }

    companion object {
        val EXPRESSION_TYPE = create(
            "EXPRESSION_TYPE",
            Severity.INFO,
            true
        )

        fun create(name: String, severity: Severity): DebugInfoDiagnosticFactory1 {
            return DebugInfoDiagnosticFactory1(name, severity)
        }

        fun create(name: String, severity: Severity, withExplicitDefinitionOnly: Boolean): DebugInfoDiagnosticFactory1 {
            return DebugInfoDiagnosticFactory1(name, severity, withExplicitDefinitionOnly)
        }
    }
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
    private val privateName: String

    override var name: String?
        get() = "DEBUG_INFO_$privateName"
        set(_) {}

    override val withExplicitDefinitionOnly: Boolean

    override fun createDiagnostic(
        expression: KtExpression,
        bindingContext: BindingContext,
        dataFlowValueFactory: DataFlowValueFactory?,
        languageVersionSettings: LanguageVersionSettings?,
        moduleDescriptor: ModuleDescriptorImpl?
    ) = when (privateName) {
        EXPRESSION_TYPE.privateName -> {
            val (type, dataFlowTypes) = CheckerTestUtil.getTypeInfo(
                expression,
                bindingContext,
                dataFlowValueFactory,
                languageVersionSettings,
                moduleDescriptor
            )

            this.on(expression, Renderers.renderExpressionType(type, dataFlowTypes))
        }
        CALL.privateName -> {
            val (fqName, typeCall) = CheckerTestUtil.getCallDebugInfo(expression, bindingContext)
            this.on(expression, Renderers.renderCallInfo(fqName, typeCall))
        }
        else -> throw NotImplementedError("Creation diagnostic '$name' isn't supported.")
    }

    private constructor(name: String, severity: Severity) : super(severity, PositioningStrategies.DEFAULT) {
        this.privateName = name
        this.withExplicitDefinitionOnly = false
    }

    private constructor(name: String, severity: Severity, withExplicitDefinitionOnly: Boolean) : super(
        severity,
        PositioningStrategies.DEFAULT
    ) {
        this.privateName = name
        this.withExplicitDefinitionOnly = withExplicitDefinitionOnly
    }

    companion object {
        val EXPRESSION_TYPE = create(
            "EXPRESSION_TYPE",
            Severity.INFO,
            true
        )
        val CALL = create(
            "CALL",
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
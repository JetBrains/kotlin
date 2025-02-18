/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariableType
import org.jetbrains.kotlin.fir.types.contains
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirAnalysisHandler
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class LLDiagnosticParameterChecker(testServices: TestServices) : FirAnalysisHandler(testServices) {
    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        for (part in info.partsForDependsOnModules) {
            val facade = part.firAnalyzerFacade as LowLevelFirAnalyzerFacade
            val diagnostics = facade.runCheckers().values.flatten()

            for (diagnostic in diagnostics) {
                checkDiagnosticIsSuitableForFirIde(diagnostic as KtPsiDiagnostic)
            }
        }
    }

    private fun checkDiagnosticIsSuitableForFirIde(diagnostic: KtPsiDiagnostic) {
        val parameters = diagnostic.allParameters()
        for (parameter in parameters) {
            checkDiagnosticParameter(diagnostic, parameter)
        }
    }

    private fun checkDiagnosticParameter(diagnostic: KtPsiDiagnostic, parameter: Any?) {
        when (parameter) {
            is ConeKotlinType -> checkType(parameter, diagnostic as KtDiagnostic)
        }
    }

    private fun checkType(parameter: ConeKotlinType, diagnostic: KtDiagnostic) {
        val containsTypeVariableType = parameter.contains { it is ConeTypeVariableType }
        if (containsTypeVariableType) {
            val rendered = RootDiagnosticRendererFactory(diagnostic).render(diagnostic)
            testServices.assertions.fail {
                "ConeTypeVariableType should not be exposed from diagnostic. But it was for ${diagnostic.factoryName} $rendered"
            }
        }
    }

    private fun KtPsiDiagnostic.allParameters(): List<Any?> = when (this) {
        is KtPsiDiagnosticWithParameters1<*> -> listOf(a)
        is KtPsiDiagnosticWithParameters2<*, *> -> listOf(a, b)
        is KtPsiDiagnosticWithParameters3<*, *, *> -> listOf(a, b, c)
        is KtPsiDiagnosticWithParameters4<*, *, *, *> -> listOf(a, b, c, d)
        is KtPsiSimpleDiagnostic -> emptyList()
        else -> errorWithAttachment("Unexpected diagnostic ${this::class}, $factoryName") {
            withPsiEntry("onElement", psiElement)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}

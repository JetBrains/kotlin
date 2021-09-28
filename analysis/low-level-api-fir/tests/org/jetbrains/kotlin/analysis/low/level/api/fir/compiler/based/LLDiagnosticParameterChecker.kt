/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler.based

import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariableType
import org.jetbrains.kotlin.fir.types.contains
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirAnalysisHandler
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

internal class LLDiagnosticParameterChecker(testServices: TestServices) : FirAnalysisHandler(testServices) {
    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        val diagnostics = info.firAnalyzerFacade.runCheckers().values.flatten()

        for (diagnostic in diagnostics) {
            checkDiagnosticIsSuitableForFirIde(diagnostic)
        }
    }

    private fun checkDiagnosticIsSuitableForFirIde(diagnostic: FirDiagnostic) {
        val parameters = diagnostic.allParameters()
        for (parameter in parameters) {
            checkDiagnosticParameter(diagnostic, parameter)
        }
    }

    private fun checkDiagnosticParameter(diagnostic: FirDiagnostic, parameter: Any?) {
        when (parameter) {
            is ConeKotlinType -> checkType(parameter, diagnostic)
        }
    }

    private fun checkType(parameter: ConeKotlinType, diagnostic: FirDiagnostic) {
        val containsTypeVariableType = parameter.contains { it is ConeTypeVariableType }
        if (containsTypeVariableType) {
            val rendered = FirDefaultErrorMessages.getRendererForDiagnostic(diagnostic).render(diagnostic)
            testServices.assertions.fail {
                "ConeTypeVariableType should not be exposed from diagnostic. But it was for ${diagnostic.factoryName} $rendered"
            }
        }
    }

    private fun FirDiagnostic.allParameters(): List<Any?> = when (this) {
        is FirPsiDiagnosticWithParameters1<*> -> listOf(a)
        is FirPsiDiagnosticWithParameters2<*, *> -> listOf(a, b)
        is FirPsiDiagnosticWithParameters3<*, *, *> -> listOf(a, b, c)
        is FirPsiDiagnosticWithParameters4<*, *, *, *> -> listOf(a, b, c, d)
        is FirPsiSimpleDiagnostic -> emptyList()
        else -> error("Unexpected diagnostic $this")
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}

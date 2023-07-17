/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration

abstract class AbstractFirWithInterpreterDiagnosticsTest(val parser: FirParser) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    override fun TestConfigurationBuilder.configuration() {
        configureFirParser(parser)
        baseFirDiagnosticTestConfiguration()

        facadeStep(::Fir2IrResultsConverter)
        irHandlersStep {
            useHandlers(
                ::IrDiagnosticsHandler
            )
        }
    }
}

open class AbstractFirPsiWithInterpreterDiagnosticsTest : AbstractFirWithInterpreterDiagnosticsTest(FirParser.Psi)

open class AbstractFirLightTreeWithInterpreterDiagnosticsTest : AbstractFirWithInterpreterDiagnosticsTest(FirParser.LightTree)

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend



//open class AbstractPhasedJvmTest : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
//    override fun TestConfigurationBuilder.configuration() {
//        baseFirDiagnosticTestConfiguration(frontendFacade = ::PhasedJvmFrontedFacade)
//
//        globalDefaults {
//            artifactKind = ArtifactKinds.Jvm
//            this.targetBackend = targetBackend
//            dependencyKind = DependencyKind.Binary
//        }
//
//        defaultDirectives {
//            LATEST_EXPECTED_TIER with TestTierLabel.BACKEND
//            FIR_PARSER with FirParser.Psi
//        }
//
//        configureFirHandlersStep {
//            useHandlers(
////                ::FirDiagnosticsHandler,
//                ::NoFirCompilationErrorsHandler
//            )
//        }
//        facadeStep(::PhasedJvmFir2IrFacade)
//        facadeStep(::PhasedJvmIrBackendFacade)
//
//        useAfterAnalysisCheckers(::PhasedPipelineChecker)
//        enableMetaInfoHandler()
//    }
//}
//
//class SomePhasedTest : AbstractPhasedJvmTest() {
//    @Test
//    fun testAddTestForFalsePositiveDuplicateLabelInWhen() {
//        runTest("compiler/testData/diagnostics/tests/addTestForFalsePositiveDuplicateLabelInWhen.kt");
//    }
//}

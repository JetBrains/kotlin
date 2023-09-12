/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.frontend.fir.handlers.toMetaInfos
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.dependencyProvider
import org.jetbrains.kotlin.test.services.globalMetadataInfoHandler

inline fun IrBackendInput.processAllIrModuleFragments(
    module: TestModule,
    processor: (irModuleFragment: IrModuleFragment, moduleName: String) -> Unit
) {
    dependentIrModuleFragments.forEach { processor(it, it.name.asString()) }
    processor(irModuleFragment, module.name)
}

fun BinaryArtifactHandler<*>.reportKtDiagnostics(module: TestModule, ktDiagnosticReporter: BaseDiagnosticsCollector) {
    val globalMetadataInfoHandler = testServices.globalMetadataInfoHandler
    val firParser = module.directives.singleOrZeroValue(FirDiagnosticsDirectives.FIR_PARSER)
    val lightTreeComparingModeEnabled = firParser != null && FirDiagnosticsDirectives.COMPARE_WITH_LIGHT_TREE in module.directives
    val lightTreeEnabled = firParser == FirParser.LightTree

    val processedModules = mutableSetOf<TestModule>()

    fun processModule(module: TestModule) {
        if (!processedModules.add(module)) return
        for (testFile in module.files) {
            val ktDiagnostics = ktDiagnosticReporter.diagnosticsByFilePath["/${testFile.name}"] ?: continue
            ktDiagnostics.forEach {
                val metaInfos = it.toMetaInfos(module, testFile, globalMetadataInfoHandler, lightTreeEnabled, lightTreeComparingModeEnabled)
                globalMetadataInfoHandler.addMetadataInfosForFile(testFile, metaInfos)
            }
        }
        for ((moduleName, _, _) in module.dependsOnDependencies) {
            val dependantModule = testServices.dependencyProvider.getTestModule(moduleName)
            processModule(dependantModule)
        }
    }

    processModule(module)
}

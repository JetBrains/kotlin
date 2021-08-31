/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.checkers.CheckerDebugInfoReporter
import org.jetbrains.kotlin.checkers.utils.DebugInfoUtil
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.RECURSIVE_ALL
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension

class DynamicCallsDumpHandler(testServices: TestServices) : ClassicFrontendAnalysisHandler(testServices) {
    companion object {
        private const val DYNAMIC_PREFIX = ".dynamic.txt"
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(DiagnosticsDirectives)

    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper(moduleHeaderTemplate = "// -- Module: <%s> --")

    override fun processModule(module: TestModule, info: ClassicFrontendOutputArtifact) {
        val dynamicCallDescriptors = mutableListOf<DeclarationDescriptor>()
        for (ktFile in info.ktFiles.values) {
            DebugInfoUtil.markDebugAnnotations(
                ktFile,
                info.analysisResult.bindingContext,
                CheckerDebugInfoReporter(
                    dynamicCallDescriptors,
                    markDynamicCalls = true,
                    debugAnnotations = mutableListOf(),
                    withNewInference = info.languageVersionSettings.supportsFeature(LanguageFeature.NewInference),
                    platform = null
                )
            )
        }
        val serializer = RecursiveDescriptorComparator(RECURSIVE_ALL)
        val builder = dumper.builderForModule(module)
        for (descriptor in dynamicCallDescriptors) {
            builder.append(serializer.serializeRecursively(descriptor))
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        val expectedFile = testServices.moduleStructure.originalTestDataFiles.first().withExtension(DYNAMIC_PREFIX)
        if (expectedFile.exists()) {
            val resultDump = dumper.generateResultingDump()
            assertions.assertEqualsToFile(expectedFile, resultDump)
        }
    }
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.createFilesWithGeneratedDeclarations
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.extensions.generatedMembers
import org.jetbrains.kotlin.fir.extensions.generatedNestedClassifiers
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper

class FirDumpHandler(
    testServices: TestServices
) : FirAnalysisHandler(testServices) {
    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper()

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    @OptIn(SymbolInternals::class)
    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        if (FirDiagnosticsDirectives.FIR_DUMP !in module.directives) return
        val builderForModule = dumper.builderForModule(module)
        val firFiles = info.firFiles

        val allFiles = buildList {
            addAll(firFiles.values)
            addAll(info.session.createFilesWithGeneratedDeclarations())
        }

        val renderer = FirRendererWithGeneratedDeclarations(info.session, builderForModule)
        allFiles.forEach {
            it.accept(renderer)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (dumper.isEmpty()) return
        // TODO: change according to multiple testdata files
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val expectedFile = testDataFile.parentFile.resolve("${testDataFile.nameWithoutFirExtension}.fir.txt")
        val actualText = dumper.generateResultingDump()
        assertions.assertEqualsToFile(expectedFile, actualText, message = { "Content is not equal" })
    }

    private class FirRendererWithGeneratedDeclarations(
        val session: FirSession,
        builder: StringBuilder,
    ) : FirRenderer(builder, modeWithPackageDirective) {
        companion object {
            val modeWithPackageDirective = RenderMode.Normal.copy(renderPackageDirective = true)
        }

        override fun renderClassDeclarations(regularClass: FirRegularClass) {
            val allDeclarations = buildList {
                addAll(regularClass.declarations)
                addAll(regularClass.generatedMembers(session))
                addAll(regularClass.generatedNestedClassifiers(session))
            }
            allDeclarations.renderDeclarations()
        }
    }
}

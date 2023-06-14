/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.createFilesWithGeneratedDeclarations
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.extensions.generatedMembers
import org.jetbrains.kotlin.fir.extensions.generatedNestedClassifiers
import org.jetbrains.kotlin.fir.renderer.FirClassMemberRenderer
import org.jetbrains.kotlin.fir.renderer.FirPackageDirectiveRenderer
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.renderer.FirSymbolRendererWithStaticFlag
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
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

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module
            if (FirDiagnosticsDirectives.FIR_DUMP !in currentModule.directives) return
            val builderForModule = dumper.builderForModule(currentModule)
            val firFiles = info.mainFirFiles

            val allFiles = buildList {
                addAll(firFiles.values)
                addAll(part.session.createFilesWithGeneratedDeclarations())
            }
            part.session.lazyDeclarationResolver.startResolvingPhase(FirResolvePhase.BODY_RESOLVE)

            val renderer = FirRenderer(
                builder = builderForModule,
                packageDirectiveRenderer = FirPackageDirectiveRenderer(),
                classMemberRenderer = FirClassMemberRendererWithGeneratedDeclarations(part.session),
                referencedSymbolRenderer = FirSymbolRendererWithStaticFlag()
            )
            allFiles.forEach {
                renderer.renderElementAsString(it)
            }
            part.session.lazyDeclarationResolver.finishResolvingPhase(FirResolvePhase.BODY_RESOLVE)
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

    private class FirClassMemberRendererWithGeneratedDeclarations(val session: FirSession) : FirClassMemberRenderer() {
        override fun render(regularClass: FirRegularClass) {
            val allDeclarations = buildList {
                addAll(regularClass.declarations)
                addAll(regularClass.generatedMembers(session))
                addAll(regularClass.generatedNestedClassifiers(session))
            }
            render(allDeclarations)
        }
    }
}

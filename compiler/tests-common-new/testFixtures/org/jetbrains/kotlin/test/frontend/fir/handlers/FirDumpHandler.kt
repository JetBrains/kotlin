/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.utils.createFilesWithGeneratedDeclarations
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.extensions.generatedMembers
import org.jetbrains.kotlin.fir.extensions.generatedNestedClassifiers
import org.jetbrains.kotlin.fir.renderer.FirClassMemberRenderer
import org.jetbrains.kotlin.fir.renderer.FirPackageDirectiveRenderer
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.renderer.FirSymbolRendererWithStaticFlag
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.handlers.assertFileDoesntExist
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.CHECK_BYTECODE_LISTING
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.DISABLE_TYPEALIAS_EXPANSION
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.USE_LATEST_LANGUAGE_VERSION
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.impl.testConfiguration
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import java.io.File

@OptIn(DirectDeclarationsAccess::class)
class FirDumpHandler(
    testServices: TestServices
) : FirAnalysisHandler(testServices) {
    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper()
    private var byteCodeListingEnabled = false

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(FirDiagnosticsDirectives)

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        if (module.directives.shouldSkip()) return
        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module
            byteCodeListingEnabled = byteCodeListingEnabled || CHECK_BYTECODE_LISTING in module.directives
            val isFirDumpEnabled =
                expectedFile().exists() || FirDiagnosticsDirectives.FIR_DUMP in currentModule.directives

            if (!isFirDumpEnabled) return

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
        if (testServices.moduleStructure.allDirectives.shouldSkip()) return
        val expectedFile = expectedFile()

        if (dumper.isEmpty()) {
            assertions.assertFileDoesntExist(expectedFile, FirDiagnosticsDirectives.FIR_DUMP)
        } else {
            val actualText = dumper.generateResultingDump()
            assertions.assertEqualsToFile(expectedFile, actualText, message = { "Content is not equal" })
        }
    }

    private fun expectedFile(): File {
        // TODO: change according to multiple testdata files
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val extension = if (byteCodeListingEnabled) ".fir2.txt" else ".fir.txt"
        val originalExpectedFilePath = testDataFile.parentFile.resolve("${testDataFile.nameWithoutFirExtension}$extension").path

        @OptIn(TestInfrastructureInternals::class)
        val expectedFilePath = testServices.testConfiguration
            .metaTestConfigurators
            .fold(originalExpectedFilePath) { fileName, configurator ->
                configurator.transformTestDataPath(fileName)
            }

        return File(expectedFilePath)
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

    private fun RegisteredDirectives.shouldSkip(): Boolean {
        // disabled typealias mode is used only for sanity checks for tests
        // there is no need to duplicate dumps for them (and they may differ from regular ones, as
        // types in resolved type ref won't be expanded)
        return DISABLE_TYPEALIAS_EXPANSION in this || USE_LATEST_LANGUAGE_VERSION in this
    }
}

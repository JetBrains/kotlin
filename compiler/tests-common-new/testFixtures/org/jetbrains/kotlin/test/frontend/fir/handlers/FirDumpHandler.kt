/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.cli.pipeline.FrontendFilesForPluginsGenerationPipelinePhase
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.extensions.generatedMembers
import org.jetbrains.kotlin.fir.extensions.generatedNestedClassifiers
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.renderer.*
import org.jetbrains.kotlin.fir.symbols.lazyDeclarationResolver
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.CHECK_BYTECODE_LISTING
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.DISABLE_TYPEALIAS_EXPANSION
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.DISABLE_FIR_DUMP_HANDLER
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.EXPLICITLY_GENERATE_PLUGIN_FILES
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.RENDER_SPECIFIC_FIR_DECLARATION_ATTRIBUTES
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.USE_LATEST_LANGUAGE_VERSION
import org.jetbrains.kotlin.test.directives.TestDumpDirectives
import org.jetbrains.kotlin.test.directives.assertEqualsToDump
import org.jetbrains.kotlin.test.directives.getClassifiedDumpFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirOutputPartForDependsOnModule
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper

@OptIn(DirectDeclarationsAccess::class)
class FirDumpHandler(
    testServices: TestServices
) : FirAnalysisHandler(testServices) {
    private val dumper: MultiModuleInfoDumper = MultiModuleInfoDumper()
    private var byteCodeListingEnabled = false

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(TestDumpDirectives, FirDiagnosticsDirectives)

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        if (module.directives.shouldSkip()) return
        for (part in info.partsForDependsOnModules) {
            val currentModule = part.module
            byteCodeListingEnabled = byteCodeListingEnabled || CHECK_BYTECODE_LISTING in module.directives
            val isFirDumpEnabled = FirDiagnosticsDirectives.FIR_DUMP in currentModule.directives ||
                    testServices.moduleStructure.getClassifiedDumpFile(getDumpExtension()).exists()

            if (!isFirDumpEnabled) return

            val builderForModule = dumper.builderForModule(currentModule)

            val allFiles = collectFilesForRendering(module, info, part)
            part.session.lazyDeclarationResolver.startResolvingPhase(FirResolvePhase.BODY_RESOLVE)

            val declarationRenderer = if (RENDER_SPECIFIC_FIR_DECLARATION_ATTRIBUTES in module.directives) {
                FirDeclarationRendererWithSpecificAttributes(module.directives[RENDER_SPECIFIC_FIR_DECLARATION_ATTRIBUTES].toSet())
            } else {
                FirDeclarationRenderer()
            }

            val renderer = FirRenderer(
                builder = builderForModule,
                packageDirectiveRenderer = FirPackageDirectiveRenderer(),
                classMemberRenderer = FirClassMemberRendererWithGeneratedDeclarations(part.session),
                referencedSymbolRenderer = FirSymbolRendererWithStaticFlag(),
                declarationRenderer = declarationRenderer,
            )
            allFiles.forEach {
                renderer.renderElementAsString(it)
            }
            part.session.lazyDeclarationResolver.finishResolvingPhase(FirResolvePhase.BODY_RESOLVE)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (testServices.moduleStructure.allDirectives.shouldSkip()) return
        val actualText = if (dumper.isEmpty()) null else dumper.generateResultingDump()
        assertEqualsToDump(getDumpExtension(), actualText)
    }

    private fun getDumpExtension(): String = if (byteCodeListingEnabled) ".fir2.txt" else ".fir.txt"

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
        return DISABLE_TYPEALIAS_EXPANSION in this || USE_LATEST_LANGUAGE_VERSION in this || DISABLE_FIR_DUMP_HANDLER in this
    }

    companion object {
        fun collectFilesForRendering(
            module: TestModule,
            info: FirOutputArtifact,
            part: FirOutputPartForDependsOnModule,
        ): List<FirFile> {
            return buildList {
                // collect only files belonging to the specific parts and exclude additional test files
                info.allFirFiles.filterTo(this) { file ->
                    file.moduleData == part.session.moduleData &&
                            info.allFirFilesByTestFile.entries.none {
                                it.value == file && it.key.isAdditional
                            }
                }
                if (EXPLICITLY_GENERATE_PLUGIN_FILES in module.directives) {
                    addAll(FrontendFilesForPluginsGenerationPipelinePhase.createFilesWithGeneratedDeclarations(part.session))
                }
            }
        }
    }
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.buildPackageDirective
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildFile
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import java.lang.StringBuilder

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

        val symbolProvider = info.session.symbolProvider
        val declarationGenerators = info.session.extensionService.declarationGenerators
        val topLevelClasses = declarationGenerators.flatMap { it.getTopLevelClassIds() }.groupBy { it.packageFqName }
        val topLevelCallables = declarationGenerators.flatMap { it.getTopLevelCallableIds() }.groupBy { it.packageName }

        val allFiles = buildList {
            addAll(firFiles.values)
            for (packageFqName in (topLevelClasses.keys + topLevelCallables.keys)) {
                this += buildFile {
                    origin = FirDeclarationOrigin.Synthetic
                    moduleData = info.session.moduleData
                    packageDirective = buildPackageDirective {
                        this.packageFqName = packageFqName
                    }
                    name = "### GENERATED DECLARATIONS ###"
                    declarations += topLevelCallables.getOrDefault(packageFqName, emptyList())
                        .flatMap { symbolProvider.getTopLevelCallableSymbols(packageFqName, it.callableName) }
                        .map { it.fir }
                    declarations += topLevelClasses.getOrDefault(packageFqName, emptyList())
                        .mapNotNull { symbolProvider.getClassLikeSymbolByClassId(it)?.fir }
                }
            }
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

                @OptIn(SymbolInternals::class)
                fun addGeneratedDeclaration(symbol: FirBasedSymbol<*>) {
                    val declaration = symbol.fir
                    if (declaration.origin.generated) {
                        add(declaration)
                    }
                }

                val scope = session.declaredMemberScope(regularClass)
                for (callableName in scope.getCallableNames()) {
                    scope.processFunctionsByName(callableName) {
                        addGeneratedDeclaration(it)
                    }
                    scope.processPropertiesByName(callableName) {
                        addGeneratedDeclaration(it)
                    }
                }

                for (classifierName in scope.getClassifierNames()) {
                    scope.processClassifiersByName(classifierName) {
                        addGeneratedDeclaration(it)
                    }
                }
            }
            allDeclarations.renderDeclarations()
        }
    }
}

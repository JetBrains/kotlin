/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.imports

import org.jetbrains.kotlin.analysis.api.impl.base.imports.KaDefaultImportImpl
import org.jetbrains.kotlin.analysis.api.imports.KaDefaultImport
import org.jetbrains.kotlin.analysis.api.imports.KaDefaultImportPriority
import org.jetbrains.kotlin.analysis.api.imports.getDefaultImports
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.scopes.impl.DefaultImportPriority
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirSingleLevelDefaultStarImportingScope
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractKaDefaultImportsProviderTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val sourceModule = mainModule.ktModule as KaSourceModule

        checkImportsFromKaDefaultImportProvider(sourceModule, testServices)
        checkImportsFromResolve(sourceModule, mainFile, testServices)
        checkExcludedImports(sourceModule, testServices)
    }

    private fun checkImportsFromKaDefaultImportProvider(sourceModule: KaSourceModule, testServices: TestServices) {
        val imports = sourceModule.targetPlatform.getDefaultImports(sourceModule.project).defaultImports
        val actual = renderDefaultImports(imports)
        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = ".default.txt")
    }

    private fun checkImportsFromResolve(sourceModule: KaSourceModule, mainFile: KtFile, testServices: TestServices) {
        val importsUsedInResolve = getDefaultImportsUsedInResolve(sourceModule, mainFile)
        val actual = renderDefaultImports(importsUsedInResolve)
        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = ".default.txt")
    }

    private fun checkExcludedImports(sourceModule: KaSourceModule, testServices: TestServices) {
        val excludedImports = sourceModule.targetPlatform.getDefaultImports(sourceModule.project).excludedFromDefaultImports
        val actual = prettyPrint {
            if (excludedImports.isEmpty()) {
                appendLine("<EMPTY>")
            }
            printCollection(excludedImports.sortedBy { it.fqName.asString() }, separator = "\n") {
                append(it.fqName.asString())
            }
        }
        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = ".excluded.txt")
    }


    private fun renderDefaultImports(imports: List<KaDefaultImport>): String = prettyPrint {
        printCollection(KaDefaultImportPriority.entries, separator = "\n") { priority ->
            appendLine("${priority.name}:")
            withIndent {
                val importsWithCurrentPriority =
                    imports.filter { it.priority == priority }
                        .sortedWith(
                            compareBy(
                                { it.importPath.fqName.asString() },
                                { it.importPath.isAllUnder },
                                { it.priority.toString() },
                            )
                        )
                if (importsWithCurrentPriority.isNotEmpty()) {
                    printCollection(importsWithCurrentPriority, separator = "\n") {
                        append(it.asString())
                    }
                } else {
                    append("<EMPTY>")
                }
            }
        }
    }

    private fun getDefaultImportsUsedInResolve(kaModule: KaModule, ktFile: KtFile): List<KaDefaultImport> {
        val resolutionFacade = kaModule.getResolutionFacade(kaModule.project)
        val context = ContextCollector.process(
            ktFile.getOrBuildFirFile(resolutionFacade),
            SessionHolderImpl.Companion.createWithEmptyScopeSession(resolutionFacade.useSiteFirSession),
            ktFile,
        )!!

        return context.towerDataContext.towerDataElements
            .flatMap { it.getAvailableScopes() }
            .flatMap { if (it is FirDefaultStarImportingScope) listOf(it.first, it.second) else listOf(it) }
            .flatMap { scope ->
                when (scope) {
                    is FirSingleLevelDefaultStarImportingScope -> {
                        scope.starImports.map { import ->
                            KaDefaultImportImpl(
                                ImportPath(import.importedFqName!!, isAllUnder = true),
                                scope.priority.asKaDefaultImportPriority()
                            )
                        }
                    }
                    is FirDefaultSimpleImportingScope -> {
                        scope.simpleImports.values.flatten().map { import ->
                            KaDefaultImportImpl(
                                ImportPath(import.importedFqName!!, isAllUnder = false),
                                scope.priority.asKaDefaultImportPriority()
                            )
                        }
                    }
                    else -> emptyList()
                }
            }

    }

    private fun DefaultImportPriority.asKaDefaultImportPriority() = when (this) {
        DefaultImportPriority.HIGH -> KaDefaultImportPriority.HIGH
        DefaultImportPriority.LOW -> KaDefaultImportPriority.LOW
    }

    fun KaDefaultImport.asString() = buildString {
        append("import ${importPath.fqName.asString()}")
        if (importPath.isAllUnder) {
            append(".*")
        }
    }
}
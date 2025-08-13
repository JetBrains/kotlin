/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLResolutionFacadeService
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

/**
 * A base class for *unit testing* of LL FIR symbol providers.
 *
 * While symbol providers are central to the Analysis API and thus well covered by general Analysis API tests, unit testing allows focusing
 * in on specific scenarios and symbol provider implementations.
 *
 * Note: The test only supports [FirSymbolProvider.hasPackage] for now, but can be extended to cover other symbol provider methods.
 */
abstract class AbstractSymbolProviderTest : AbstractAnalysisApiBasedTest() {
    private object Directives : SimpleDirectivesContainer() {
        val HAS_PACKAGE by stringDirective(
            description = "Instructs the test to call `hasPackage` on the symbol provider with the given package name.",
            applicability = DirectiveApplicability.Any,
        )
    }

    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    /**
     * The test output would get confusing if we support multiple symbol provider instances from the main module, so this functions has to
     * pick a single symbol provider.
     *
     * @see findSymbolProvidersOfType
     */
    protected abstract fun findTestSymbolProvider(mainModule: KtTestModule): FirSymbolProvider

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val hasPackageTargets =
            getAllDirectivesWithFiles(Directives.HAS_PACKAGE, testServices)
                .map { parsePackageFqName(it.trim()) }

        val symbolProvider = findTestSymbolProvider(mainModule)

        val actualText = prettyPrint {
            for (packageFqName in hasPackageTargets) {
                appendLine("HAS_PACKAGE '$packageFqName':")
                withIndent {
                    appendLine(symbolProvider.hasPackage(packageFqName).toString())
                }
                appendLine()
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actualText)
    }

    private fun getAllDirectivesWithFiles(
        directive: StringDirective,
        testServices: TestServices,
    ): List<String> {
        val testModuleStructure = testServices.ktTestModuleStructure.testModuleStructure
        val structureDirectives = testModuleStructure.allDirectives[directive]
        val fileDirectives = testModuleStructure.modules.flatMap { testModule -> testModule.files.flatMap { it.directives[directive] } }
        return structureDirectives + fileDirectives
    }

    private fun parsePackageFqName(value: String): FqName = if (value == "<root>") FqName.ROOT else FqName(value)

    internal inline fun <reified T> KaModule.findSymbolProvidersOfType(): List<T> {
        val resolutionFacade = LLResolutionFacadeService.getInstance(project).getResolutionFacade(this)
        val useSiteSession = resolutionFacade.useSiteFirSession

        val moduleSymbolProvider = useSiteSession.symbolProvider as? LLModuleWithDependenciesSymbolProvider
            ?: error("Expected `${LLModuleWithDependenciesSymbolProvider::class.simpleName}` as the module-level symbol provider.")

        return buildList {
            addAll(moduleSymbolProvider.providers.filterIsInstance<T>())
            addAll(moduleSymbolProvider.dependencyProvider.providers.filterIsInstance<T>())
        }
    }
}

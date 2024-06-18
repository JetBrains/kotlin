/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirLibraryBinaryDecompiledTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirDeclarationForCompiledElementSearcher
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.CompiledLibraryProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleDecompiler
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleDecompilerDirectory
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.service

abstract class AbstractDeserializedContainerSourceFromLibraryFirTest : AbstractAnalysisApiBasedTest() {
    override val configurator get() = AnalysisApiFirLibraryBinaryDecompiledTestConfigurator

    override fun configureTest(builder: TestConfigurationBuilder) {
        builder.forTestsMatching("*/metadata/*") {
            this.defaultsProviderBuilder.targetPlatform = CommonPlatforms.defaultCommonPlatform
            this.useAdditionalService<TestModuleDecompiler> { TestModuleDecompilerDirectory() }
        }

        builder.forTestsMatching("*/js/*") {
            this.defaultsProviderBuilder.targetPlatform = JsPlatforms.defaultJsPlatform
        }

        builder.forTestsMatching("*/jvm/*") {
            this.defaultsProviderBuilder.targetPlatform = JvmPlatforms.defaultJvmPlatform
        }

        super.configureTest(builder)
        with(builder) {
            useDirectives(Directives)
            useAdditionalServices(service(::CompiledLibraryProvider))
        }
    }

    private fun renderDeserializedContainerSource(fir: FirDeclaration, declaration: KtDeclaration): String {
        val str = """
           KT element: ${declaration::class.simpleName}
           KT element text: ${declaration.text}
           FIR element: ${fir::class.simpleName}
           File name: ${declaration.containingKtFile.name}
           FIR container source: ${fir.renderContainerSource()}
        """.trimIndent()
        return str
    }

    private fun FirDeclaration.renderContainerSource(): String =
        render((this as? FirCallableDeclaration)?.containerSource)

    private fun render(containerSource: DeserializedContainerSource?) =
        containerSource?.let { "${containerSource::class.simpleName} ${containerSource.presentableString}" } ?: "null"

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val moduleStructure = testServices.moduleStructure
        val declaration = getElementToSearch(mainFile, moduleStructure)
            ?: error("Declaration of the type ${moduleStructure.allDirectives[Directives.DECLARATION_TYPE]} was not found in the test data")

        val ktModule = mainModule.ktModule
        val resolveSession = LLFirResolveSessionService.getInstance(mainFile.project).getFirResolveSessionForBinaryModule(ktModule)
        val symbolProvider = resolveSession.getSessionFor(ktModule).symbolProvider
        val fir = FirDeclarationForCompiledElementSearcher(symbolProvider).findNonLocalDeclaration(declaration)

        testServices.assertions.assertEqualsToTestDataFileSibling(renderDeserializedContainerSource(fir, declaration))
    }

    private fun getElementToSearch(ktFile: KtFile, moduleStructure: TestModuleStructure): KtDeclaration? {
        val expectedType = moduleStructure.allDirectives[Directives.DECLARATION_TYPE].firstOrNull()
            ?: error("Compiled code should have element type specified")
        @Suppress("UNCHECKED_CAST") val expectedClass = Class.forName(expectedType) as Class<out PsiElement>
        return findFirstDeclarationWithClass(ktFile.declarations, expectedClass)
    }

    private object Directives : SimpleDirectivesContainer() {
        val DECLARATION_TYPE by stringDirective("DECLARATION_TYPE")
    }
}

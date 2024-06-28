/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirLibraryBinaryDecompiledTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.FirDeclarationForCompiledElementSearcher
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.CompiledLibraryProvider
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleDecompiler
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleDecompilerDirectory
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationContainer
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.service
import kotlin.collections.firstOrNull
import kotlin.collections.forEach

abstract class AbstractDeserializedContainerSourceFirTest : AbstractAnalysisApiBasedTest() {
    override val configurator get() = AnalysisApiFirLibraryBinaryDecompiledTestConfigurator

    override fun configureTest(builder: TestConfigurationBuilder) {
        builder.forTestsMatching("analysis/low-level-api-fir/testData/getOrBuildFirForFileFacade/metadata/*") {
            this.defaultsProviderBuilder.targetPlatform = CommonPlatforms.defaultCommonPlatform
            this.useAdditionalService<TestModuleDecompiler> { TestModuleDecompilerDirectory() }
        }

        builder.forTestsMatching("analysis/low-level-api-fir/testData/getOrBuildFirForFileFacade/js/*") {
            this.defaultsProviderBuilder.targetPlatform = JsPlatforms.defaultJsPlatform
        }

        builder.forTestsMatching("analysis/low-level-api-fir/testData/getOrBuildFirForFileFacade/jvm/*") {
            this.defaultsProviderBuilder.targetPlatform = JvmPlatforms.defaultJvmPlatform
        }

        super.configureTest(builder)
        with(builder) {
            useAdditionalServices(service(::CompiledLibraryProvider))
        }
    }

    private fun renderDeserializedContainerSource(fir: FirDeclaration, declaration: KtDeclaration): String {
        val str = ""
        return str
    }

    private fun renderContainerSource(fir: FirDeclaration): String {
        return when (fir) {
            is FirFunction -> fir.containerSource?.render().orEmpty()
            is FirProperty -> fir.containerSource?.render().orEmpty()
            else -> ""
        }
    }

    private fun DeserializedContainerSource.render() =
        "${this::class.simpleName} ${this.presentableString}"

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        val declaration = getElementToSearch(mainFile)

        val ktModule = mainModule.ktModule
        val resolveSession = LLFirResolveSessionService.getInstance(mainFile.project).getFirResolveSessionForBinaryModule(ktModule)
        val symbolProvider = resolveSession.getSessionFor(ktModule).symbolProvider
        val fir = FirDeclarationForCompiledElementSearcher(symbolProvider).findNonLocalDeclaration(declaration)

        testServices.assertions.assertEqualsToTestDataFileSibling(renderDeserializedContainerSource(fir, declaration))
    }

    private fun getElementToSearch(ktFile: KtFile): KtDeclaration {
        var functionOrProperty: KtDeclaration? = null

        object : KtVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                functionOrProperty = function
            }

            override fun visitProperty(property: KtProperty) {
                functionOrProperty = property
            }
        }.visitKtFile(ktFile)

        return functionOrProperty ?: error("Expected to find one function or property in the test file to check its container source")
    }
}
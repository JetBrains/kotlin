/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinContentScopeRefiner
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.util.*

/**
 * This test targets `ContentScopeProvider` and `KaResolutionScopeProvider`.
 *
 * The test file for the test should contain normal module declarations.
 * Each module can have a dedicated refiner registered.
 * Refiner for a module should have the same name with `_REFINER` suffix added.
 * Each file inside refiner module must have at least one of the following directives:
 *  SHADOWED - this file will be shadowed for this module
 *  ADDED - this file will be added for this module
 */
open class AbstractContentAndResolutionScopesProvidersTest : AbstractAnalysisApiBasedTest() {
    private var refinerToRegister: DummyContentScopeRefiner = DummyContentScopeRefiner()
    override var configurator: ContentScopeProviderConfigurator = ContentScopeProviderConfigurator(listOf(refinerToRegister))

    override fun doTest(testServices: TestServices) {
        val namesToModules: MutableMap<String, KtTestModule> = mutableMapOf()
        val namesToFiles: MutableMap<String, List<Pair<VirtualFile, TestFile>>> = mutableMapOf()

        testServices.ktTestModuleStructure.mainModules.associateTo(namesToModules) {
            val ktModule = it.ktModule
            (ktModule as KaSourceModule).name to it
        }

        namesToFiles.putAll(
            namesToModules.map { (name, module) ->
                // Empty modules still contain dummy files that have "module_" prefix
                val psiFiles = module.files.filter { !it.name.startsWith("module_") }.sortedBy { it.name }.map { it.virtualFile }
                val testFiles = module.testModule.files.filter { !it.name.startsWith("module_") }.sortedBy { it.name }
                name to psiFiles.zip(testFiles)
            }
        )

        val workingModules = namesToModules.filter { !it.key.endsWith(REFINER_MODULE_SUFFIX) }

        val baseContentScopes = namesToFiles.filter { !it.key.endsWith(REFINER_MODULE_SUFFIX) }.map { (moduleName, files) ->
            moduleName to files.map { it.first }
        }.toMap()

        val refiners = namesToFiles.filter { it.key.endsWith(REFINER_MODULE_SUFFIX) }.map {
            it.key.removeSuffix(REFINER_MODULE_SUFFIX) to it.value
        }

        val enlargementScopes =
            refiners.associate { (moduleName, files) ->
                moduleName to files.filter { (_, testFile) -> testFile.directives.contains(Directives.ADDED) }
                    .map { (virtualFile, _) ->
                        val baseScope = baseContentScopes[moduleName] ?: error("scope for $moduleName not found")
                        baseScope.firstOrNull { it.name == virtualFile.name } ?: virtualFile
                    }
            }

        val restrictionScopes =
            refiners.associate { (moduleName, files) ->
                moduleName to files.filter { (_, testFile) -> testFile.directives.contains(Directives.SHADOWED) }
                    .map { (virtualFile, _) ->
                        val baseScope = baseContentScopes[moduleName] ?: error("scope for $moduleName not found")
                        baseScope.firstOrNull { it.name == virtualFile.name } ?: virtualFile
                    }
            }

        val inputData = InputData(baseContentScopes, enlargementScopes, restrictionScopes)

        val computedContentScopes = workingModules.map { (moduleName, _) ->
            val scope = TreeSet(virtualFilesComparator).apply {
                inputData.moduleToInputBaseContentScope[moduleName]?.let { addAll(it) }
                inputData.moduleToInputEnlargementScope[moduleName]?.let { addAll(it) }
                inputData.moduleToInputShadowedScope[moduleName]?.let { removeAll(it) }
            }

            moduleName to scope
        }.toMap()

        val moduleData = workingModules.map { (moduleName, testModule) ->
            val inputBaseContentScope = inputData.moduleToInputBaseContentScope[moduleName] ?: listOf()
            val inputShadowedScope = inputData.moduleToInputShadowedScope[moduleName] ?: listOf()
            val inputEnlargementScope = inputData.moduleToInputEnlargementScope[moduleName] ?: listOf()
            val expectedContentScope = computedContentScopes[moduleName] ?: TreeSet()
            moduleName to ModuleData(
                moduleName,
                testModule,
                inputBaseContentScope,
                expectedContentScope,
                inputEnlargementScope,
                inputShadowedScope
            )
        }.toMap()

        moduleData.forEach {
            it.value.dependencies.addAll(it.value.testModule.testModule.allDependencies.mapNotNull { dependencyDescription ->
                val name = dependencyDescription.dependencyModule.name
                moduleData[name]
            })
        }

        refinerToRegister.apply {
            enlargementScope.clear()
            restrictionScope.clear()
            enlargementScope.putAll(enlargementScopes)
            restrictionScope.putAll(restrictionScopes)
        }

        testContentScope(moduleData, testServices)
        testResolutionScope(moduleData, testServices)
    }

    private fun testContentScope(
        moduleData: Map<String, ModuleData>,
        testServices: TestServices
    ) {
        val stringBuilder = StringBuilder()

        moduleData.forEach { (currentModuleName, currentModuleData) ->
            val contentScope = currentModuleData.testModule.ktModule.contentScope
            println("Module ${currentModuleName}: $contentScope")
            currentModuleData.expectedContentScope.forEach {
                testServices.assertions.assertTrue(contentScope.contains(it)) { "File ${it.name} should be in scope" }
            }

            currentModuleData.inputShadowedScope.forEach {
                testServices.assertions.assertFalse(contentScope.contains(it)) { "File ${it.name} should not be in scope" }
            }

            stringBuilder.appendLine("Module ${currentModuleName}:")
            stringBuilder.appendLine(currentModuleData.expectedContentScope.joinToString(separator = "\n") { "    ${it.path}" })
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(
            actual = "Resulting Content Scopes:\n$stringBuilder",
            extension = ".content.scope"
        )
    }

    private fun testResolutionScope(
        moduleData: Map<String, ModuleData>,
        testServices: TestServices
    ) {
        val stringBuilder = StringBuilder()

        moduleData.forEach { (currentModuleName, currentModuleData) ->
            val mockResolutionScope = buildSet<VirtualFile> {
                addAll(currentModuleData.expectedContentScope)
                addAll(currentModuleData.dependencies.flatMap { it.expectedContentScope })
            }

            val mainModuleResolutionScope =
                KaResolutionScopeProvider.getInstance(currentModuleData.testModule.ktModule.project)
                    .getResolutionScope(currentModuleData.testModule.ktModule)

            mockResolutionScope.forEach {
                testServices.assertions.assertTrue(mainModuleResolutionScope.contains(it)) { "File ${it.name} should be in scope" }
            }

            currentModuleData.inputShadowedScope.forEach {
                testServices.assertions.assertFalse(mainModuleResolutionScope.contains(it)) { "File ${it.name} should not be in scope" }
            }

            stringBuilder.appendLine("Module ${currentModuleName}:")
            stringBuilder.appendLine(mockResolutionScope.joinToString(separator = "\n") { "    ${it.path}" })
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(
            actual = "Resulting Resolution Scope:\n$stringBuilder",
            extension = ".resolution.scope"
        )
    }

    object Directives : SimpleDirectivesContainer() {
        val SHADOWED by stringDirective("This file is shadowed in 'KotlinContentScopeRefiner'", DirectiveApplicability.File)
        val ADDED by stringDirective("This file is added in 'KotlinContentScopeRefiner'", DirectiveApplicability.File)
    }

    private data class InputData(
        val moduleToInputBaseContentScope: Map<String, List<VirtualFile>>,
        val moduleToInputEnlargementScope: Map<String, List<VirtualFile>>,
        val moduleToInputShadowedScope: Map<String, List<VirtualFile>>,
    )

    private data class ModuleData(
        val name: String,
        val testModule: KtTestModule,
        val inputBaseContentScope: List<VirtualFile>,
        val expectedContentScope: TreeSet<VirtualFile>,
        val inputEnlargementScope: List<VirtualFile>,
        val inputShadowedScope: List<VirtualFile>,
        val dependencies: MutableList<ModuleData> = mutableListOf()
    )

    companion object {
        private val virtualFilesComparator = Comparator<VirtualFile> { a, b ->
            a.name.compareTo(b.name)
        }

        private const val REFINER_MODULE_SUFFIX = "_REFINER"
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }
}


class ContentScopeProviderConfigurator(private val scopeRefinersToRegister: List<DummyContentScopeRefiner> = listOf()) :
    AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false) {
    override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = buildList {
            addAll(super.serviceRegistrars)
            add(ContentScopeProviderRegistrar(scopeRefinersToRegister))
        }
}

private class ContentScopeProviderRegistrar(private val scopeRefinersToRegister: List<DummyContentScopeRefiner>) :
    AnalysisApiTestServiceRegistrar() {
    override fun registerProjectModelServices(project: MockProject, disposable: Disposable, testServices: TestServices) {
        val extensionPoint = project.extensionArea.getExtensionPoint(KotlinContentScopeRefiner.EP_NAME)
        scopeRefinersToRegister.forEach { refiner ->
            extensionPoint.registerExtension(refiner, disposable)
        }
    }
}

class DummyContentScopeRefiner(
    val enlargementScope: MutableMap<String, List<VirtualFile>> = mutableMapOf(),
    val restrictionScope: MutableMap<String, List<VirtualFile>> = mutableMapOf()
) : KotlinContentScopeRefiner {
    override fun getEnlargementScopes(module: KaModule): List<GlobalSearchScope> {
        val moduleName = (module as? KaSourceModule)?.name ?: return emptyList()
        val files = enlargementScope[moduleName] ?: return emptyList()
        val scope = GlobalSearchScope.filesScope(
            module.project,
            files
        )
        return listOf(scope)
    }

    override fun getRestrictionScopes(module: KaModule): List<GlobalSearchScope> {
        val moduleName = (module as? KaSourceModule)?.name ?: return emptyList()
        val files = restrictionScope[moduleName] ?: return emptyList()
        val scope = GlobalSearchScope.filesScope(
            module.project,
            files
        )
        return listOf(scope)
    }
}

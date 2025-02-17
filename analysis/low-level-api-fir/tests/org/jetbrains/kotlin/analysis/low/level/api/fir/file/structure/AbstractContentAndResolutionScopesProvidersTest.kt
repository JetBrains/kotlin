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
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestFile
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.util.*

/**
 * This test targets `ContentScopeProvider` and `KaResolutionScopeProvider`.
 *
 * The test file for the test should contain normal module declarations.
 * Each module can have a dedicated refiner registered.
 * Refiner for a module should have the same name with `_REFINER` suffix added.
 * Each file inside refiner modules must have at least one of the following directives:
 *  'SHADOWED' - this file will be shadowed for this module
 *  'ADDED' - this file will be added for this module
 * It is also possible to put the 'SHADOWED' directive directly on file in the working module.
 *
 * Please note that for clarity of the output, all file names should be unique.
 * This doesn't apply to the same file being used in a working module and in the corresponding refiner module.
 *
 * Test output consists of two files:
 *  - One file with '.content.scope' extension lists all content scopes of all presented working modules
 *  - One file with '.resolution.scope' extension lists all resolution scopes of all presented working modules
 */
open class AbstractContentAndResolutionScopesProvidersTest : AbstractAnalysisApiBasedTest() {
    private var refinerToRegister: DummyContentScopeRefiner = DummyContentScopeRefiner()
    override var configurator: AnalysisApiFirSourceTestConfigurator = ContentScopeProviderConfigurator(refinerToRegister)

    override fun doTest(testServices: TestServices) {
        val namesToModules = testServices.ktTestModuleStructure.mainModules.associateBy {
            val ktModule = it.ktModule
            (ktModule as KaSourceModule).name
        }

        val namesToModulesWithFiles =
            namesToModules.map { (name, module) ->
                // Empty modules still contain dummy files that have "module_" prefix
                val testFiles = module.testFiles.filter { !it.testFile.name.startsWith("module_") }.map { ktTestFile ->
                    KtTestFileWithVirtualFile(ktTestFile, ktTestFile.psiFile?.virtualFile ?: error("No virtual file found for $ktTestFile"))
                }
                name to TestModuleWithFiles(module, testFiles)
            }.toMap()

        val workingModules = namesToModulesWithFiles.filter { isWorkingModule(it.key) }.toSortedMap()

        val refinerModules = namesToModulesWithFiles.filter { !isWorkingModule(it.key) }.map {
            it.key.removeSuffix(REFINER_MODULE_SUFFIX) to it.value
        }.toMap().toSortedMap()

        buildMap<String, MutableList<String>> {
            workingModules.forEach { (moduleName, module) ->
                this.put(moduleName, module.files.map { it.virtualFile.name }.toMutableList())
            }

            refinerModules.forEach { (moduleName, module) ->
                this[moduleName]?.addAll(module.files.map { it.virtualFile.name })
            }
        }.flatMap {
            it.value.distinct()
        }.let { fileNames ->
            testServices.assertions.assertTrue(fileNames.distinct().size == fileNames.size) {
                "All files across working modules are expected to be unique"
            }
        }

        testServices.assertions.assertTrue(workingModules.flatMap { it.value.files }
                                               .none { it.ktTestFile.testFile.directives.contains(Directives.ADDED) }) {
            "Files from working modules cannot have 'ADDED' directive, add it to the corresponding refiner"
        }

        val baseContentScopes =
            workingModules.map { (moduleName, module) ->
                moduleName to module.files.map { it.virtualFile }
            }.toMap()

        val enlargementScopes =
            refinerModules.map { (moduleName, module) ->
                moduleName to module.files.filter { file -> file.ktTestFile.testFile.directives.contains(Directives.ADDED) }
                    .map { file ->
                        val baseScope = baseContentScopes[moduleName] ?: error("module '$moduleName' not found")
                        baseScope.firstOrNull { it.name == file.virtualFile.name } ?: file.virtualFile
                    }
            }.toMap()

        val restrictionScopes = workingModules.entries.zip(refinerModules.entries) { workingModule, refiner ->
            workingModule.key to workingModule.value.files + refiner.value.files
        }.associate { (moduleName, files) ->
            moduleName to files.filter { file -> file.ktTestFile.testFile.directives.contains(Directives.SHADOWED) }
                .map { file ->
                    val baseScope = baseContentScopes[moduleName] ?: error("module '$moduleName' not found")
                    baseScope.firstOrNull { it.name == file.virtualFile.name } ?: file.virtualFile
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

        val moduleData = workingModules.map { (moduleName, testModuleWithFiles) ->
            val inputBaseContentScope = inputData.moduleToInputBaseContentScope[moduleName] ?: listOf()
            val inputShadowedScope = inputData.moduleToInputShadowedScope[moduleName] ?: listOf()
            val inputEnlargementScope = inputData.moduleToInputEnlargementScope[moduleName] ?: listOf()
            val expectedContentScope = computedContentScopes[moduleName] ?: TreeSet()
            moduleName to ModuleData(
                moduleName,
                testModuleWithFiles,
                inputBaseContentScope,
                expectedContentScope,
                inputEnlargementScope,
                inputShadowedScope
            )
        }.toMap()

        moduleData.forEach {
            it.value.dependencies.addAll(it.value.moduleWithFiles.ktTestModule.testModule.allDependencies.mapNotNull { dependencyDescription ->
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
            val contentScope = currentModuleData.moduleWithFiles.ktTestModule.ktModule.contentScope
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
            val mockResolutionScope = buildSet {
                addAll(currentModuleData.expectedContentScope)
                addAll(currentModuleData.dependencies.flatMap { it.expectedContentScope })
            }

            val kaModule = currentModuleData.moduleWithFiles.ktTestModule.ktModule

            val mainModuleResolutionScope =
                KaResolutionScopeProvider.getInstance(kaModule.project)
                    .getResolutionScope(kaModule)

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
        val moduleWithFiles: TestModuleWithFiles,
        val inputBaseContentScope: List<VirtualFile>,
        val expectedContentScope: TreeSet<VirtualFile>,
        val inputEnlargementScope: List<VirtualFile>,
        val inputShadowedScope: List<VirtualFile>,
        val dependencies: MutableList<ModuleData> = mutableListOf()
    )

    private data class KtTestFileWithVirtualFile(
        val ktTestFile: KtTestFile<*>,
        val virtualFile: VirtualFile
    )

    private data class TestModuleWithFiles(
        val ktTestModule: KtTestModule,
        val files: List<KtTestFileWithVirtualFile>
    )

    private fun isWorkingModule(moduleName: String): Boolean = !moduleName.endsWith(REFINER_MODULE_SUFFIX)

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


private class ContentScopeProviderConfigurator(private val scopeRefinerToRegister: DummyContentScopeRefiner) :
    AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false) {
    override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = buildList {
            addAll(super.serviceRegistrars)
            add(ContentScopeProviderRegistrar(scopeRefinerToRegister))
        }
}

private class ContentScopeProviderRegistrar(private val scopeRefinerToRegister: DummyContentScopeRefiner) :
    AnalysisApiTestServiceRegistrar() {
    override fun registerProjectModelServices(project: MockProject, disposable: Disposable, testServices: TestServices) {
        val extensionPoint = project.extensionArea.getExtensionPoint(KotlinContentScopeRefiner.EP_NAME)
        extensionPoint.registerExtension(scopeRefinerToRegister, disposable)
    }
}

private class DummyContentScopeRefiner(
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

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils.getAllVirtualFilesFromRoot
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinContentScopeRefiner
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.hasFallbackDependencies
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestFile
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.model.nameWithoutExtension
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.utils.addToStdlib.runIf
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
 *
 * ### Library modules
 *
 * To support binary library modules, which contain compiled `.class` files, test files are mapped to class files. For example, consider the
 * following library module:
 *
 * ```
 * // MODULE: LIBRARY
 * // MODULE_KIND: LibraryBinary
 * // FILE: a.kt
 * class a
 *
 * // FILE: b.kt
 * // SHADOWED
 * class b
 *
 * // MODULE: LIBRARY_REFINER
 * // FILE: x.kt
 * // ADDED
 * class x
 * ```
 *
 * The JAR compiled by the test infrastructure doesn't contain individual files like `a.kt`, but rather `.class` files for each class. This
 * test has a special mapping between a file `a.kt` and a class file `a.class`: `a.class` is considered to be the "compiled representation"
 * of `a.kt`. The directives of `a.kt` apply to `a.class`. In the example above, `b.class` would be shadowed because `SHADOWED` is applied
 * to `b.kt`. The resulting refined content scope would include `a.class` and `x.class`, but not `b.class`.
 *
 * This approach is technically not very correct, but pragmatic and convenient, as we would otherwise have to define a separate syntax for
 * library modules.
 */
open class AbstractContentAndResolutionScopesProvidersTest : AbstractAnalysisApiBasedTest() {
    private var refinerToRegister: DummyContentScopeRefiner = DummyContentScopeRefiner()
    override var configurator: AnalysisApiFirSourceTestConfigurator = ContentScopeProviderConfigurator(refinerToRegister)

    override fun doTest(testServices: TestServices) {
        val testModulesWithFiles =
            testServices.ktTestModuleStructure.mainModules
                .fold(emptyList<TestModuleWithFiles>()) { result, ktTestModule ->
                    result + createTestModuleWithFiles(ktTestModule, result, testServices)
                }

        val workingModules = testModulesWithFiles.filter { it.isWorkingModule }
        val refinerModules = testModulesWithFiles.filter { it.isRefinerModule }

        checkFileUniqueness(workingModules, refinerModules, testServices)

        testServices.assertions.assertTrue(
            workingModules.flatMap { it.files }.none { it.ktTestFile.testFile.directives.contains(Directives.ADDED) }
        ) {
            "Files from working modules cannot have an 'ADDED' directive. Add it to the corresponding refiner."
        }

        val baseContentScopesByKaModule =
            workingModules.associate { module ->
                module.kaModule to module.files.map { it.virtualFile }
            }

        val enlargementScopesByKaModule =
            refinerModules.associate { module ->
                val originalModule = module.originalModule ?: error("No original module found for refiner module '${module.moduleName}'.")
                originalModule.kaModule to module.files.filter { file -> file.ktTestFile.testFile.directives.contains(Directives.ADDED) }
                    .map { file ->
                        val baseScope = baseContentScopesByKaModule.getValue(originalModule.kaModule)
                        baseScope.firstOrNull { it.name == file.virtualFile.name } ?: file.virtualFile
                    }
            }

        val shadowedScopesByKaModule = workingModules.associate { module ->
            val refinerModule = refinerModules.firstOrNull { it.originalModule == module }
            val files = buildList {
                addAll(module.files)
                refinerModule?.let { addAll(it.files) }
            }
            module.kaModule to files.filter { file -> file.ktTestFile.testFile.directives.contains(Directives.SHADOWED) }
                .map { file ->
                    val baseScope = baseContentScopesByKaModule.getValue(module.kaModule)
                    baseScope.firstOrNull { it.name == file.virtualFile.name } ?: file.virtualFile
                }
        }

        val inputData = InputData(baseContentScopesByKaModule, enlargementScopesByKaModule, shadowedScopesByKaModule)

        val computedContentScopesByKaModule = workingModules.associate { module ->
            val kaModule = module.kaModule
            val scope = TreeSet(virtualFilesComparator).apply {
                inputData.moduleToInputBaseContentScope[kaModule]?.let { addAll(it) }
                inputData.moduleToInputEnlargementScope[kaModule]?.let { addAll(it) }
                inputData.moduleToInputShadowedScope[kaModule]?.let { removeAll(it) }
            }
            kaModule to scope
        }

        val moduleDataByKaModule = workingModules.associate { module ->
            val kaModule = module.kaModule
            val inputBaseContentScope = inputData.moduleToInputBaseContentScope[kaModule] ?: listOf()
            val inputShadowedScope = inputData.moduleToInputShadowedScope[kaModule] ?: listOf()
            val inputEnlargementScope = inputData.moduleToInputEnlargementScope[kaModule] ?: listOf()
            val expectedContentScope = computedContentScopesByKaModule[kaModule] ?: TreeSet()
            kaModule to ModuleData(
                module.moduleName,
                module,
                inputBaseContentScope,
                expectedContentScope,
                inputEnlargementScope,
                inputShadowedScope
            )
        }

        moduleDataByKaModule.values.forEach { moduleData ->
            moduleData.dependencies.addAll(
                moduleData.collectDependencies(moduleDataByKaModule, testServices)
            )
        }

        refinerToRegister.apply {
            enlargementScope.clear()
            shadowedScope.clear()
            enlargementScope.putAll(enlargementScopesByKaModule)
            shadowedScope.putAll(shadowedScopesByKaModule)
        }

        val moduleDataList = moduleDataByKaModule.values.sortedBy { it.name }
        testContentScope(moduleDataList, testServices)
        testResolutionScope(moduleDataList, testServices)
    }

    private fun createTestModuleWithFiles(
        ktTestModule: KtTestModule,
        existingModules: List<TestModuleWithFiles>,
        testServices: TestServices,
    ): TestModuleWithFiles {
        // Empty modules still contain dummy files that have the "module_" prefix.
        val ktTestFiles = ktTestModule.testFiles.filter { !it.testFile.name.startsWith("module_") }

        val kaModule = ktTestModule.ktModule
        val testFiles = if (kaModule is KaLibraryModule) {
            val binaryFiles = kaModule.binaryVirtualFiles.flatMap { binaryRoot ->
                getAllVirtualFilesFromRoot(binaryRoot, includeRoot = false)
            }.distinct()

            // As noted in the class's KDoc, we have a special mapping for library files: `a.kt` corresponds to `a.class` for a class
            // declaration `class a` inside `a.kt`.
            ktTestFiles.map { ktTestFile ->
                val virtualFile = binaryFiles
                    .firstOrNull { binaryFile -> binaryFile.name == ktTestFile.testFile.nameWithoutExtension + ".class" }
                    ?: error("No `.class` virtual file found for $ktTestFile from library module.")

                KtTestFileWithVirtualFile(ktTestFile, virtualFile)
            }
        } else {
            ktTestFiles.map { ktTestFile ->
                val virtualFile = ktTestFile.psiFile?.virtualFile ?: error("No virtual file found for $ktTestFile")
                KtTestFileWithVirtualFile(ktTestFile, virtualFile)
            }
        }

        val originalModule = runIf(ktTestModule.name.endsWith(REFINER_MODULE_SUFFIX)) {
            val originalModuleName = ktTestModule.name.removeSuffix(REFINER_MODULE_SUFFIX)
            existingModules.firstOrNull { it.moduleName == originalModuleName }
                ?: error(
                    "No original module found for refiner module '${ktTestModule.name}'. It should be ordered before the refiner module."
                )
        }

        return TestModuleWithFiles(ktTestModule, originalModule, testFiles)
    }

    private fun checkFileUniqueness(
        workingModules: List<TestModuleWithFiles>,
        refinerModules: List<TestModuleWithFiles>,
        testServices: TestServices,
    ) {
        buildMap {
            workingModules.forEach { module ->
                this.put(module.moduleName, module.files.map { it.virtualFile.name }.toMutableList())
            }

            refinerModules.forEach { module ->
                val originalModuleName = module.originalModule?.moduleName
                    ?: error("No original module found for refiner module ${module.moduleName}.")
                this[originalModuleName]?.addAll(module.files.map { it.virtualFile.name })
            }
        }.flatMap {
            it.value.distinct()
        }.let { fileNames ->
            testServices.assertions.assertTrue(fileNames.distinct().size == fileNames.size) {
                "All files across working modules are expected to be unique"
            }
        }
    }

    private fun ModuleData.collectDependencies(
        moduleDataByKaModule: Map<KaModule, ModuleData>,
        testServices: TestServices,
    ): List<ModuleData> {
        val testModule = moduleWithFiles.testModule

        // When the module has fallback dependencies, it effectively depends on all other library modules.
        if (testModule.hasFallbackDependencies) {
            return moduleDataByKaModule.values.filter { otherModuleData ->
                val otherKaModule = otherModuleData.moduleWithFiles.kaModule
                otherKaModule is KaLibraryModule && otherKaModule != moduleWithFiles.kaModule
            }
        }

        return testModule.allDependencies.mapNotNull { dependencyDescription ->
            val dependencyKtTestModule = testServices.ktTestModuleStructure.getKtTestModule(dependencyDescription.dependencyModule)
            moduleDataByKaModule[dependencyKtTestModule.ktModule]
        }
    }

    private fun testContentScope(
        moduleDataList: List<ModuleData>,
        testServices: TestServices
    ) {
        val stringBuilder = StringBuilder()

        moduleDataList.forEach { moduleData ->
            val contentScope = moduleData.moduleWithFiles.ktTestModule.ktModule.contentScope
            moduleData.expectedContentScope.forEach {
                testServices.assertions.assertTrue(contentScope.contains(it)) { "File ${it.name} should be in scope" }
            }

            moduleData.inputShadowedScope.forEach {
                testServices.assertions.assertFalse(contentScope.contains(it)) { "File ${it.name} should not be in scope" }
            }

            stringBuilder.appendLine("Module ${moduleData.moduleWithFiles.moduleName}:")
            stringBuilder.appendLine(moduleData.expectedContentScope.joinToString(separator = "\n") { "    ${it.name}" })
        }

        testServices.assertions.assertEqualsToTestOutputFile(
            actual = "Resulting Content Scopes:\n$stringBuilder",
            extension = ".content.scope"
        )
    }

    private fun testResolutionScope(
        moduleDataList: List<ModuleData>,
        testServices: TestServices
    ) {
        val stringBuilder = StringBuilder()

        moduleDataList.forEach { moduleData ->
            val mockResolutionScope = buildSet {
                addAll(moduleData.expectedContentScope)
                addAll(moduleData.dependencies.flatMap { it.expectedContentScope })
            }

            val kaModule = moduleData.moduleWithFiles.ktTestModule.ktModule

            val mainModuleResolutionScope =
                KaResolutionScopeProvider.getInstance(kaModule.project)
                    .getResolutionScope(kaModule)

            mockResolutionScope.forEach {
                testServices.assertions.assertTrue(mainModuleResolutionScope.contains(it)) { "File ${it.name} should be in scope" }
            }

            moduleData.inputShadowedScope.forEach {
                testServices.assertions.assertFalse(mainModuleResolutionScope.contains(it)) { "File ${it.name} should not be in scope" }
            }

            stringBuilder.appendLine("Module ${moduleData.moduleWithFiles.moduleName}:")
            stringBuilder.appendLine(mockResolutionScope.joinToString(separator = "\n") { "    ${it.name}" })
        }

        testServices.assertions.assertEqualsToTestOutputFile(
            actual = "Resulting Resolution Scope:\n$stringBuilder",
            extension = ".resolution.scope"
        )
    }

    object Directives : SimpleDirectivesContainer() {
        val SHADOWED by stringDirective("This file is shadowed in 'KotlinContentScopeRefiner'", DirectiveApplicability.File)
        val ADDED by stringDirective("This file is added in 'KotlinContentScopeRefiner'", DirectiveApplicability.File)
    }

    private data class InputData(
        val moduleToInputBaseContentScope: Map<KaModule, List<VirtualFile>>,
        val moduleToInputEnlargementScope: Map<KaModule, List<VirtualFile>>,
        val moduleToInputShadowedScope: Map<KaModule, List<VirtualFile>>,
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

    /**
     * @param originalModule If this [TestModuleWithFiles] is a *refiner module*, [originalModule] points to the module refined by this
     *  module.
     */
    private data class TestModuleWithFiles(
        val ktTestModule: KtTestModule,
        val originalModule: TestModuleWithFiles?,
        val files: List<KtTestFileWithVirtualFile>,
    ) {
        val moduleName: String get() = ktTestModule.name
        val kaModule: KaModule get() = ktTestModule.ktModule
        val testModule: TestModule get() = ktTestModule.testModule

        val isWorkingModule: Boolean get() = originalModule == null
        val isRefinerModule: Boolean get() = originalModule != null
    }

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
    val enlargementScope: MutableMap<KaModule, List<VirtualFile>> = mutableMapOf(),
    val shadowedScope: MutableMap<KaModule, List<VirtualFile>> = mutableMapOf(),
) : KotlinContentScopeRefiner {
    override fun getEnlargementScopes(module: KaModule): List<GlobalSearchScope> {
        val files = enlargementScope[module] ?: return emptyList()
        val scope = GlobalSearchScope.filesScope(
            module.project,
            files
        )
        return listOf(scope)
    }

    override fun getRestrictionScopes(module: KaModule): List<GlobalSearchScope> {
        val files = shadowedScope[module] ?: return emptyList()
        val scope = GlobalSearchScope.filesScope(
            module.project,
            files
        )
        return listOf(GlobalSearchScope.notScope(scope))
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.impl.base.util.LibraryUtils.getAllVirtualFilesFromJar
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScopeProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinContentScopeRefiner
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.AnalysisApiServiceRegistrar
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestFile
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.test.framework.services.environmentManager
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleCompiler
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.nameWithoutExtension
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.isKtFile
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * This test checks the membership of [content scopes][KaModule.contentScope] and [resolution scopes][KaResolutionScopeProvider]. It
 * incorporates a custom [KotlinContentScopeRefiner] for added/shadowed files.
 *
 * The test supports multiple modules and checks all of their content and resolution scopes. The test data can declare the module structure
 * as usual with `MODULE` and `FILE` directives.
 *
 * To add/shadow files in a module, the module can have a dedicated refiner. It should have the same name as the module with a `_REFINER`
 * suffix. Each file inside the refiner module must have at least one of the following directives:
 *
 * - [SHADOWED][Directives.SHADOWED] - the file will be shadowed for this module.
 * - [ADDED][Directives.ADDED] - the file will be added for this module
 *
 * It is also possible to put the `SHADOWED` directive directly on a file in the working module.
 *
 * The test output consists of two files which authoritatively record the *actual* scopes computed by the Analysis API:
 *
 * - A `.content.scope` file lists, per working module, each relevant file marked with `+` if it is in the module's content scope, or `-` if
 *   it is not.
 * - A `.resolution.scope` file does the same for each module's resolution scope.
 *
 * The relevant files for a module are its own files plus the files of its refiner module. For the resolution scope, the files of all
 * working modules are considered, as a resolution scope may include files from dependency modules and we should also test unrelated files
 * against the resolution scope.
 *
 * Please note that for clarity of the output, all file names should be unique. However, two files in a working module and its corresponding
 * refiner module may share a name.
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
 *
 * Other files marked with the [LIBRARY_RESOURCE][TestModuleCompiler.Directives.LIBRARY_RESOURCE] directive are embedded verbatim in the JAR
 * regardless of their extension (e.g., a mock Scala `.tasty` file, or a source file erroneously placed in a binary root). For example, this
 * allows testing which file types a library restriction scope admits or excludes.
 */
abstract class AbstractContentAndResolutionScopesProvidersTest : AbstractAnalysisApiBasedTest() {
    private val contentScopeRefiner: DummyContentScopeRefiner = DummyContentScopeRefiner()

    override val configurator: AnalysisApiTestConfigurator = ContentScopeProviderConfigurator(contentScopeRefiner)

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useDirectives(Directives)
    }

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

        val baseFilesByKaModule =
            workingModules.associate { module ->
                module.kaModule to module.files.map { it.virtualFile }
            }

        val addedFilesByKaModule = refinerModules.associate { module ->
            val originalModule = module.originalModule ?: error("No original module found for refiner module '${module.moduleName}'.")
            val baseFiles = baseFilesByKaModule.getValue(originalModule.kaModule)
            val addedFiles = module.files
                .filter { file -> file.ktTestFile.testFile.directives.contains(Directives.ADDED) }
                .map { file ->
                    baseFiles.firstOrNull { it.name == file.virtualFile.name } ?: file.virtualFile
                }
            originalModule.kaModule to addedFiles
        }

        val shadowedFilesByKaModule = workingModules.associate { module ->
            val refinerModule = refinerModules.firstOrNull { it.originalModule == module }
            val files = buildList {
                addAll(module.files)
                refinerModule?.let { addAll(it.files) }
            }
            val baseFiles = baseFilesByKaModule.getValue(module.kaModule)
            val shadowedFiles = files
                .filter { file -> file.ktTestFile.testFile.directives.contains(Directives.SHADOWED) }
                .map { file ->
                    baseFiles.firstOrNull { it.name == file.virtualFile.name } ?: file.virtualFile
                }
            module.kaModule to shadowedFiles
        }

        contentScopeRefiner.setFiles(addedFilesByKaModule, shadowedFilesByKaModule)

        // The files relevant to a working module are its base content files, plus the files added or shadowed via its refiner. The test
        // probes whether each of these files actually ends up in the module's scopes (see `testContentScope` and `testResolutionScope`).
        val candidateFilesByKaModule = workingModules.associate { module ->
            val kaModule = module.kaModule
            val files = buildSet {
                baseFilesByKaModule[kaModule]?.let(::addAll)
                addedFilesByKaModule[kaModule]?.let(::addAll)
                shadowedFilesByKaModule[kaModule]?.let(::addAll)
            }
            kaModule to files.sortedWith(virtualFilesComparator)
        }

        val sortedWorkingModules = workingModules.sortedBy { it.moduleName }
        testContentScope(sortedWorkingModules, candidateFilesByKaModule, testServices)
        testResolutionScope(sortedWorkingModules, candidateFilesByKaModule, testServices)
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
            val jarFileSystem = testServices.environmentManager.getApplicationEnvironment().jarFileSystem as CoreJarFileSystem
            val binaryFiles = kaModule.binaryRoots.flatMap { binaryRoot ->
                getAllVirtualFilesFromJar(binaryRoot, jarFileSystem, includeRoot = false)
            }

            // As noted in the class's KDoc, we have a special mapping for library files: `a.kt` corresponds to `a.class` for a class
            // declaration `class a` inside `a.kt`.
            //
            // `LIBRARY_RESOURCE` files are embedded verbatim in the JAR regardless of their extension (e.g. a mock `.tasty` file, or a
            // source file erroneously placed in a classes root), so they are mapped by their exact name rather than to a `.class` file.
            ktTestFiles.map { ktTestFile ->
                val isLibraryResource = ktTestFile.testFile.directives.contains(TestModuleCompiler.Directives.LIBRARY_RESOURCE)
                val virtualFile = if (!isLibraryResource && ktTestFile.testFile.isKtFile) {
                    binaryFiles
                        .firstOrNull { binaryFile -> binaryFile.name == ktTestFile.testFile.nameWithoutExtension + ".class" }
                        ?: error("No `.class` virtual file found for $ktTestFile from library module.")
                } else {
                    binaryFiles
                        .firstOrNull { binaryFile -> binaryFile.name == ktTestFile.testFile.name }
                        ?: error("No virtual file found for $ktTestFile from library module.")
                }

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
                this[module.moduleName] = module.files.map { it.virtualFile.name }.toMutableList()
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

    private fun testContentScope(
        workingModules: List<TestModuleWithFiles>,
        candidateFilesByKaModule: Map<KaModule, List<VirtualFile>>,
        testServices: TestServices,
    ) {
        val stringBuilder = StringBuilder()

        workingModules.forEach { module ->
            val contentScope = module.kaModule.contentScope
            stringBuilder.appendLine("Module ${module.moduleName}:")
            candidateFilesByKaModule.getValue(module.kaModule).forEach { file ->
                stringBuilder.appendLine(formatMembership(file, contentScope.contains(file)))
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(
            actual = "Resulting Content Scopes:\n$stringBuilder",
            extension = ".content.scope"
        )
    }

    private fun testResolutionScope(
        workingModules: List<TestModuleWithFiles>,
        candidateFilesByKaModule: Map<KaModule, List<VirtualFile>>,
        testServices: TestServices,
    ) {
        // Every module's resolution scope is probed against the candidate files of *all* working modules, not just the module's own files
        // and those of its dependencies. A resolution scope legitimately includes dependency files, so those must be covered. But probing
        // unrelated files (e.g. a `main.kt` against a library that does not depend on it) is intentional too: it asserts that the
        // resolution scope does *not* over-include files it shouldn't, which is coverage we'd lose if we only probed related files.
        val allCandidateFiles = candidateFilesByKaModule.values.flatten().distinct().sortedWith(virtualFilesComparator)

        val stringBuilder = StringBuilder()

        workingModules.forEach { module ->
            val resolutionScope = KaResolutionScopeProvider.getInstance(module.kaModule.project).getResolutionScope(module.kaModule)
            stringBuilder.appendLine("Module ${module.moduleName}:")
            allCandidateFiles.forEach { file ->
                stringBuilder.appendLine(formatMembership(file, resolutionScope.contains(file)))
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(
            actual = "Resulting Resolution Scope:\n$stringBuilder",
            extension = ".resolution.scope"
        )
    }

    private fun formatMembership(file: VirtualFile, isInScope: Boolean): String =
        "    ${if (isInScope) "+" else "-"} ${file.name}"

    object Directives : SimpleDirectivesContainer() {
        val SHADOWED by stringDirective("This file is shadowed in 'KotlinContentScopeRefiner'", DirectiveApplicability.File)
        val ADDED by stringDirective("This file is added in 'KotlinContentScopeRefiner'", DirectiveApplicability.File)
    }

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

        val isWorkingModule: Boolean get() = originalModule == null
        val isRefinerModule: Boolean get() = originalModule != null
    }

    companion object {
        private val virtualFilesComparator = Comparator<VirtualFile> { a, b ->
            a.name.compareTo(b.name)
        }

        private const val REFINER_MODULE_SUFFIX = "_REFINER"
    }
}


private class ContentScopeProviderConfigurator(
    private val contentScopeRefiner: DummyContentScopeRefiner,
) : LLSourceLikeTestConfigurator() {
    override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>
        get() = buildList {
            addAll(super.serviceRegistrars)
            add(ContentScopeProviderRegistrar(contentScopeRefiner))
        }
}

private class ContentScopeProviderRegistrar(
    private val contentScopeRefiner: DummyContentScopeRefiner,
) : AnalysisApiTestServiceRegistrar() {
    override fun registerProjectModelServices(project: MockProject, disposable: Disposable, testServices: TestServices) {
        val extensionPoint = project.extensionArea.getExtensionPoint(KotlinContentScopeRefiner.EP_NAME)
        extensionPoint.registerExtension(contentScopeRefiner, disposable)
    }
}

private class DummyContentScopeRefiner : KotlinContentScopeRefiner {
    private val addedFilesByKaModule = mutableMapOf<KaModule, List<VirtualFile>>()
    private val shadowedFilesByKaModule = mutableMapOf<KaModule, List<VirtualFile>>()

    fun setFiles(
        added: Map<KaModule, List<VirtualFile>>,
        shadowed: Map<KaModule, List<VirtualFile>>,
    ) {
        addedFilesByKaModule.clear()
        addedFilesByKaModule.putAll(added)
        shadowedFilesByKaModule.clear()
        shadowedFilesByKaModule.putAll(shadowed)
    }

    override fun getEnlargementScopes(module: KaModule): List<GlobalSearchScope> {
        val files = addedFilesByKaModule[module] ?: return emptyList()
        val scope = GlobalSearchScope.filesScope(module.project, files)
        return listOf(scope)
    }

    override fun getRestrictionScopes(module: KaModule): List<GlobalSearchScope> {
        val files = shadowedFilesByKaModule[module] ?: return emptyList()
        val scope = GlobalSearchScope.filesScope(module.project, files)
        return listOf(GlobalSearchScope.notScope(scope))
    }
}

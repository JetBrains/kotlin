/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.*
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.platform.IdePlatform

class MessageCollector {
    private val builder = StringBuilder()

    fun report(message: String) {
        builder.append(message).append('\n')
    }

    fun check() {
        val message = builder.toString()
        if (message.isNotEmpty()) {
            assert(false) { message }
        }
    }
}

class ProjectInfo(project: Project, private val projectPath: String) {
    private val messageCollector = MessageCollector()
    private val moduleManager = ModuleManager.getInstance(project)
    private val expectedModuleNames = HashSet<String>()
    private var allModulesAsserter: (ModuleInfo.() -> Unit)? = null

    fun allModules(body: ModuleInfo.() -> Unit) {
        assert(allModulesAsserter == null)
        allModulesAsserter = body
    }

    fun module(name: String, body: ModuleInfo.() -> Unit = {}) {
        val module = moduleManager.findModuleByName(name)
        if (module == null) {
            messageCollector.report("No module found: '$name'")
            return
        }
        val moduleInfo = ModuleInfo(module, messageCollector, projectPath)
        allModulesAsserter?.let { moduleInfo.it() }
        moduleInfo.run(body)
        expectedModuleNames += name
    }

    fun run(body: ProjectInfo.() -> Unit = {}) {
        body()

        val actualNames = moduleManager.modules.map { it.name }.sorted()
        val expectedNames = expectedModuleNames.sorted()
        if (actualNames != expectedNames) {
            messageCollector.report("Expected module list $expectedNames doesn't match the actual one: $actualNames")
        }

        messageCollector.check()
    }
}

class ModuleInfo(
    val module: Module,
    private val messageCollector: MessageCollector,
    private val projectPath: String
) {
    private val rootModel = module.rootManager
    private val expectedDependencyNames = HashSet<String>()
    private val expectedSourceRoots = HashSet<String>()
    private val sourceFolderByPath by lazy {
        rootModel.contentEntries.asSequence()
            .flatMap { it.sourceFolders.asSequence() }
            .mapNotNull {
                val path = it.file?.path ?: return@mapNotNull null
                FileUtil.getRelativePath(projectPath, path, '/')!! to it
            }
            .toMap()
    }

    fun languageVersion(version: String) {
        val actualVersion = module.languageVersionSettings.languageVersion.versionString
        if (actualVersion != version) {
            messageCollector.report("Module '${module.name}': expected language version '$version' but found '$actualVersion'")
        }
    }

    fun apiVersion(version: String) {
        val actualVersion = module.languageVersionSettings.apiVersion.versionString
        if (actualVersion != version) {
            messageCollector.report("Module '${module.name}': expected API version '$version' but found '$actualVersion'")
        }
    }

    fun platform(platform: IdePlatform<*, *>) {
        val actualPlatform = module.platform
        if (actualPlatform != platform) {
            messageCollector.report(
                "Module '${module.name}': expected platform '${platform.description}' but found '${actualPlatform?.description}'"
            )
        }
    }

    fun additionalArguments(arguments: String?) {
        val actualArguments = KotlinFacet.get(module)?.configuration?.settings?.compilerSettings?.additionalArguments
        if (actualArguments != arguments) {
            messageCollector.report(
                "Module '${module.name}': expected additional arguments '$arguments' but found '$actualArguments'"
            )
        }
    }

    fun libraryDependency(libraryName: String, scope: DependencyScope) {
        val libraryEntry = rootModel.orderEntries.filterIsInstance<LibraryOrderEntry>().singleOrNull { it.libraryName == libraryName }
        if (libraryEntry == null) {
            messageCollector.report("Module '${module.name}': No library dependency found: '$libraryName'")
            return
        }
        checkDependencyScope(libraryEntry, scope)
        expectedDependencyNames += libraryEntry.presentableName
    }

    fun moduleDependency(moduleName: String, scope: DependencyScope) {
        val moduleEntry = rootModel.orderEntries.filterIsInstance<ModuleOrderEntry>().singleOrNull { it.moduleName == moduleName }
        if (moduleEntry == null) {
            messageCollector.report("Module '${module.name}': No module dependency found: '$moduleName'")
            return
        }
        checkDependencyScope(moduleEntry, scope)
        expectedDependencyNames += moduleEntry.presentableName
    }

    fun sourceFolder(pathInProject: String, rootType: JpsModuleSourceRootType<*>) {
        val sourceFolder = sourceFolderByPath[pathInProject]
        if (sourceFolder == null) {
            messageCollector.report("Module '${module.name}': No source folder found: '$pathInProject'")
            return
        }
        expectedSourceRoots += pathInProject
        val actualRootType = sourceFolder.rootType
        if (actualRootType != rootType) {
            messageCollector.report(
                "Module '${module.name}', source root '$pathInProject': Expected root type $rootType doesn't match the actual one: $actualRootType"
            )
            return
        }
    }

    fun inheritProjectOutput() {
        val isInherited = CompilerModuleExtension.getInstance(module)?.isCompilerOutputPathInherited ?: true
        if (!isInherited) {
            messageCollector.report("Module '${module.name}': project output is not inherited")
        }
    }

    fun outputPath(pathInProject: String, isProduction: Boolean) {
        val compilerModuleExtension = CompilerModuleExtension.getInstance(module)
        val url = if (isProduction) compilerModuleExtension?.compilerOutputUrl else compilerModuleExtension?.compilerOutputUrlForTests
        val actualPathInProject = url?.let {
            FileUtil.getRelativePath(
                projectPath,
                JpsPathUtil.urlToPath(
                    it
                ),
                '/'
            )
        }
        if (actualPathInProject != pathInProject) {
            messageCollector.report(
                "Module '${module.name}': Expected output path $pathInProject doesn't match the actual one: $actualPathInProject"
            )
            return
        }
    }

    fun run(body: ModuleInfo.() -> Unit = {}) {
        body()

        val actualDependencyNames = rootModel
            .orderEntries
            .filter { it is ModuleOrderEntry || it is LibraryOrderEntry }
            .map { it.presentableName }
            .sorted()
        val expectedDependencyNames = expectedDependencyNames.sorted()
        if (actualDependencyNames != expectedDependencyNames) {
            messageCollector.report("Module '${module.name}': Expected dependency list $expectedDependencyNames doesn't match the actual one: $actualDependencyNames")
        }

        val actualSourceRoots = sourceFolderByPath.keys.sorted()
        val expectedSourceRoots = expectedSourceRoots.sorted()
        if (actualSourceRoots != expectedSourceRoots) {
            messageCollector.report("Module '${module.name}': Expected source root list $expectedSourceRoots doesn't match the actual one: $actualSourceRoots")
        }
    }

    private fun checkDependencyScope(library: ExportableOrderEntry, scope: DependencyScope) {
        val actualScope = library.scope
        if (actualScope != scope) {
            messageCollector.report("Module '${module.name}': Dependency '${library.presentableName}': expected scope '$scope' but found '$actualScope'")
        }
    }
}

fun checkProjectStructure(project: Project, projectPath: String, body: ProjectInfo.() -> Unit = {}) {
    ProjectInfo(project, projectPath).run(body)
}
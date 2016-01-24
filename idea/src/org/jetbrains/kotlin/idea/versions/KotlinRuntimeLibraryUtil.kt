/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.versions

import com.google.common.collect.Sets
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.PathUtil.getLocalFile
import com.intellij.util.PathUtil.getLocalPath
import com.intellij.util.containers.MultiMap
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ScalarIndexExtension
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.configuration.KotlinJavaModuleConfigurator
import org.jetbrains.kotlin.idea.configuration.KotlinJsModuleConfigurator
import org.jetbrains.kotlin.idea.configuration.getConfiguratorByName
import org.jetbrains.kotlin.idea.framework.JSLibraryStdPresentationProvider
import org.jetbrains.kotlin.idea.framework.JavaRuntimePresentationProvider
import org.jetbrains.kotlin.idea.framework.LibraryPresentationProviderUtil
import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.IOException

fun getLibraryRootsWithAbiIncompatibleKotlinClasses(module: Module): Collection<VirtualFile> {
    return getLibraryRootsWithAbiIncompatibleVersion(
            module, KotlinMetadataVersionIndex,
            { version -> !version.isCompatible() })
}

fun getLibraryRootsWithAbiIncompatibleForKotlinJs(module: Module): Collection<VirtualFile> {
    return getLibraryRootsWithAbiIncompatibleVersion(
            module, KotlinJavaScriptAbiVersionIndex,
            { version -> !KotlinJavascriptMetadataUtils.isAbiVersionCompatible(version.minor) })       // TODO: support major.minor.patch version in JS metadata
}


fun updateLibraries(
        project: Project,
        libraries: Collection<Library>) {
    ApplicationManager.getApplication().invokeLater {
        val kJvmConfigurator = getConfiguratorByName(KotlinJavaModuleConfigurator.NAME) as KotlinJavaModuleConfigurator?
        assert(kJvmConfigurator != null) { "Configurator with given name doesn't exists: " + KotlinJavaModuleConfigurator.NAME }

        val kJsConfigurator = getConfiguratorByName(KotlinJsModuleConfigurator.NAME) as KotlinJsModuleConfigurator?
        assert(kJsConfigurator != null) { "Configurator with given name doesn't exists: " + KotlinJsModuleConfigurator.NAME }

        for (library in libraries) {
            if (LibraryPresentationProviderUtil.isDetected(JavaRuntimePresentationProvider.getInstance(), library)) {
                updateJar(project, JavaRuntimePresentationProvider.getRuntimeJar(library), LibraryJarDescriptor.RUNTIME_JAR)
                updateJar(project, JavaRuntimePresentationProvider.getReflectJar(library), LibraryJarDescriptor.REFLECT_JAR)
                updateJar(project, JavaRuntimePresentationProvider.getTestJar(library), LibraryJarDescriptor.TEST_JAR)

                if (kJvmConfigurator!!.changeOldSourcesPathIfNeeded(project, library)) {
                    kJvmConfigurator.copySourcesToPathFromLibrary(project, library)
                }
                else {
                    updateJar(project, JavaRuntimePresentationProvider.getRuntimeSrcJar(library), LibraryJarDescriptor.RUNTIME_SRC_JAR)
                }
            }
            else if (LibraryPresentationProviderUtil.isDetected(JSLibraryStdPresentationProvider.getInstance(), library)) {
                updateJar(project, JSLibraryStdPresentationProvider.getJsStdLibJar(library), LibraryJarDescriptor.JS_STDLIB_JAR)

                if (kJsConfigurator!!.changeOldSourcesPathIfNeeded(project, library)) {
                    kJsConfigurator.copySourcesToPathFromLibrary(project, library)
                }
                else {
                    updateJar(project, JSLibraryStdPresentationProvider.getJsStdLibSrcJar(library), LibraryJarDescriptor.JS_STDLIB_SRC_JAR)
                }
            }
        }
    }
}

private fun updateJar(
        project: Project,
        fileToReplace: VirtualFile?,
        libraryJarDescriptor: LibraryJarDescriptor) {
    if (fileToReplace == null && !libraryJarDescriptor.shouldExist) {
        return
    }

    val paths = PathUtil.getKotlinPathsForIdeaPlugin()
    val jarPath: File = when (libraryJarDescriptor) {
        LibraryJarDescriptor.RUNTIME_JAR -> paths.runtimePath
        LibraryJarDescriptor.REFLECT_JAR -> paths.reflectPath
        LibraryJarDescriptor.TEST_JAR -> paths.kotlinTestPath
        LibraryJarDescriptor.RUNTIME_SRC_JAR -> paths.runtimeSourcesPath
        LibraryJarDescriptor.JS_STDLIB_JAR -> paths.jsStdLibJarPath
        LibraryJarDescriptor.JS_STDLIB_SRC_JAR -> paths.jsStdLibSrcJarPath
    }

    if (!jarPath.exists()) {
        showRuntimeJarNotFoundDialog(project, libraryJarDescriptor.jarName)
        return
    }

    val localJar = getLocalJar(fileToReplace)
    assert(localJar != null)

    replaceFile(jarPath, localJar!!)
}

fun findAllUsedLibraries(project: Project): MultiMap<Library, Module> {
    val libraries = MultiMap<Library, Module>()

    for (module in ModuleManager.getInstance(project).modules) {
        val moduleRootManager = ModuleRootManager.getInstance(module)

        for (entry in moduleRootManager.orderEntries.filterIsInstance<LibraryOrderEntry>()) {
            val library = entry.library ?: continue

            libraries.putValue(library, module)

            // TODO: search js libraries as well
        }
    }

    return libraries
}

private enum class LibraryJarDescriptor private constructor(val jarName: String, val shouldExist: Boolean) {
    RUNTIME_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_JAR, true),
    REFLECT_JAR(PathUtil.KOTLIN_JAVA_REFLECT_JAR, false),
    TEST_JAR(PathUtil.KOTLIN_TEST_JAR, false),
    RUNTIME_SRC_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_SRC_JAR, false),
    JS_STDLIB_JAR(PathUtil.JS_LIB_JAR_NAME, true),
    JS_STDLIB_SRC_JAR(PathUtil.JS_LIB_SRC_JAR_NAME, false)
}

@JvmOverloads fun bundledRuntimeVersion(pluginVersion: String = KotlinPluginUtil.getPluginVersion()): String {
    var placeToSplit = -1

    val ideaPatternIndex = StringUtil.indexOf(pluginVersion, "Idea")
    if (ideaPatternIndex >= 2 && Character.isDigit(pluginVersion[ideaPatternIndex - 2])) {
        placeToSplit = ideaPatternIndex - 1
    }

    val ijPatternIndex = StringUtil.indexOf(pluginVersion, "IJ")
    if (ijPatternIndex >= 2 && Character.isDigit(pluginVersion[ijPatternIndex - 2])) {
        placeToSplit = ijPatternIndex - 1
    }

    if (placeToSplit == -1) {
        for (i in 1..pluginVersion.length - 1) {
            val ch = pluginVersion[i]
            if (Character.isLetter(ch) && pluginVersion[i - 1] == '.') {
                placeToSplit = i - 1
                break
            }
        }
    }

    return if (placeToSplit != -1) pluginVersion.substring(0, placeToSplit) else pluginVersion
}

fun getLocalJar(kotlinRuntimeJar: VirtualFile?): VirtualFile? {
    if (kotlinRuntimeJar == null) return null

    val localJarFile = JarFileSystem.getInstance().getVirtualFileForJar(kotlinRuntimeJar)
    if (localJarFile != null) {
        return localJarFile
    }
    return kotlinRuntimeJar
}

internal fun replaceFile(updatedFile: File, replacedJarFile: VirtualFile) {
    try {
        val replacedFile = getLocalFile(replacedJarFile)

        val localPath = getLocalPath(replacedFile)
        assert(localPath != null) { "Should be called for replacing valid root file: " + replacedJarFile }

        val libraryJarPath = File(localPath)

        if (FileUtil.filesEqual(updatedFile, libraryJarPath)) {
            return
        }

        FileUtil.copy(updatedFile, libraryJarPath)
        replacedFile.refresh(false, true)
    }
    catch (e: IOException) {
        throw AssertionError(e)
    }
}

private fun getLibraryRootsWithAbiIncompatibleVersion(
        module: Module,
        index: ScalarIndexExtension<BinaryVersion>,
        checkVersion: (BinaryVersion) -> Boolean): Collection<VirtualFile> {
    val id = index.name

    val moduleWithAllDependencies = setOf(module) + ModuleUtil.getAllDependentModules(module)
    val moduleWithAllDependentLibraries = GlobalSearchScope.union(
            moduleWithAllDependencies.map { it.moduleWithLibrariesScope }.toTypedArray())

    val allVersions = FileBasedIndex.getInstance().getAllKeys(id, module.project)
    val badVersions = allVersions.filter(checkVersion).toHashSet()
    val badRoots = Sets.newHashSet<VirtualFile>()
    val fileIndex = ProjectFileIndex.SERVICE.getInstance(module.project)

    for (version in badVersions) {
        val indexedFiles = FileBasedIndex.getInstance().getContainingFiles(
                id, version, moduleWithAllDependentLibraries)

        for (indexedFile in indexedFiles) {
            val libraryRoot = fileIndex.getClassRootForFile(indexedFile)
            assert(libraryRoot != null) { "Only library roots were requested, " + "and only class files should be indexed with KotlinAbiVersionIndex key. " + "File: " + indexedFile.path }
            badRoots.add(getLocalFile(libraryRoot!!))
        }
    }

    return badRoots
}

fun showRuntimeJarNotFoundDialog(project: Project, jarName: String) {
    Messages.showErrorDialog(project,
                             jarName + " is not found. Make sure plugin is properly installed.",
                             "No Runtime Found")
}

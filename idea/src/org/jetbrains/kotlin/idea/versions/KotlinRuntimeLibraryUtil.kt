/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.PathUtil.getLocalFile
import com.intellij.util.PathUtil.getLocalPath
import com.intellij.util.containers.MultiMap
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ScalarIndexExtension
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.configuration.KotlinJavaModuleConfigurator
import org.jetbrains.kotlin.idea.configuration.KotlinJsModuleConfigurator
import org.jetbrains.kotlin.idea.configuration.createConfigureKotlinNotificationCollector
import org.jetbrains.kotlin.idea.configuration.getConfiguratorByName
import org.jetbrains.kotlin.idea.framework.*
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.util.runWithAlternativeResolveEnabled
import org.jetbrains.kotlin.idea.vfilefinder.JsVirtualFileFinderFactory
import org.jetbrains.kotlin.load.kotlin.JvmMetadataVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion
import org.jetbrains.kotlin.utils.JsMetadataVersion
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.addToStdlib.constant
import java.io.File
import java.io.IOException

fun getLibraryRootsWithAbiIncompatibleKotlinClasses(module: Module): Collection<BinaryVersionedFile<JvmMetadataVersion>> {
    return getLibraryRootsWithAbiIncompatibleVersion(module, JvmMetadataVersion.INSTANCE, KotlinJvmMetadataVersionIndex)
}

fun getLibraryRootsWithAbiIncompatibleForKotlinJs(module: Module): Collection<BinaryVersionedFile<JsMetadataVersion>> {
    return getLibraryRootsWithAbiIncompatibleVersion(module, JsMetadataVersion.INSTANCE, KotlinJsMetadataVersionIndex)
}

fun updateLibraries(project: Project, libraries: Collection<Library>) {
    if (project.allModules().any { module -> KotlinPluginUtil.isMavenModule(module) }) {
        Messages.showMessageDialog(project, "Automatic library version update for Maven projects is currently unsupported. Please update your pom.xml manually.",
                                   "Update Kotlin Runtime Library",
                                   Messages.getErrorIcon())
        return
    }

    if (project.allModules().any { module -> KotlinPluginUtil.isGradleModule(module) }) {
        Messages.showMessageDialog(project, "Automatic library version update for Gradle projects is currently unsupported. Please update your build.gradle manually.",
                                   "Update Kotlin Runtime Library",
                                   Messages.getErrorIcon())
        return
    }

    ApplicationManager.getApplication().invokeLater {
        val kJvmConfigurator = getConfiguratorByName(KotlinJavaModuleConfigurator.NAME) as KotlinJavaModuleConfigurator? ?:
                               error("Configurator with given name doesn't exists: " + KotlinJavaModuleConfigurator.NAME)

        val kJsConfigurator = getConfiguratorByName(KotlinJsModuleConfigurator.NAME) as KotlinJsModuleConfigurator? ?:
                              error("Configurator with given name doesn't exists: " + KotlinJsModuleConfigurator.NAME)

        val collector = createConfigureKotlinNotificationCollector(project)

        for (library in libraries) {
            if (isDetected(JavaRuntimePresentationProvider.getInstance(), library)) {
                updateJar(project, getRuntimeJar(library), LibraryJarDescriptor.RUNTIME_JAR)
                updateJar(project, getReflectJar(library), LibraryJarDescriptor.REFLECT_JAR)
                updateJar(project, getTestJar(library), LibraryJarDescriptor.TEST_JAR)

                if (kJvmConfigurator.changeOldSourcesPathIfNeeded(library, collector)) {
                    kJvmConfigurator.copySourcesToPathFromLibrary(library, collector)
                }
                else {
                    updateJar(project, getRuntimeSrcJar(library), LibraryJarDescriptor.RUNTIME_SRC_JAR)
                }
            }
            else if (isDetected(JSLibraryStdPresentationProvider.getInstance(), library)) {
                updateJar(project, getJsStdLibJar(library), LibraryJarDescriptor.JS_STDLIB_JAR)

                if (kJsConfigurator.changeOldSourcesPathIfNeeded(library, collector)) {
                    kJsConfigurator.copySourcesToPathFromLibrary(library, collector)
                }
                else {
                    updateJar(project, getJsStdLibSrcJar(library), LibraryJarDescriptor.JS_STDLIB_SRC_JAR)
                }
            }
        }

        collector.showNotification()
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
        LibraryJarDescriptor.SCRIPT_RUNTIME_JAR -> paths.scriptRuntimePath
        LibraryJarDescriptor.TEST_JAR -> paths.kotlinTestPath
        LibraryJarDescriptor.RUNTIME_SRC_JAR -> paths.runtimeSourcesPath
        LibraryJarDescriptor.JS_STDLIB_JAR -> paths.jsStdLibJarPath
        LibraryJarDescriptor.JS_STDLIB_SRC_JAR -> paths.jsStdLibSrcJarPath
    }

    if (!jarPath.exists()) {
        showRuntimeJarNotFoundDialog(project, libraryJarDescriptor.jarName)
        return
    }

    replaceFile(jarPath, getLocalJar(fileToReplace)!!)
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
    SCRIPT_RUNTIME_JAR(PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR, true),
    TEST_JAR(PathUtil.KOTLIN_TEST_JAR, false),
    RUNTIME_SRC_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_SRC_JAR, false),
    JS_STDLIB_JAR(PathUtil.JS_LIB_JAR_NAME, true),
    JS_STDLIB_SRC_JAR(PathUtil.JS_LIB_SRC_JAR_NAME, false)
}

fun bundledRuntimeVersion(): String {
    return bundledRuntimeBuildNumber ?: pluginRuntimeVersion(KotlinPluginUtil.getPluginVersion())
}

private val bundledRuntimeBuildNumber: String? by lazy {
    val file = PathUtil.getKotlinPathsForIdeaPlugin().buildNumberFile
    if (file.exists()) file.readText().trim() else null
}

private val PLUGIN_VERSIONS_SEPARATORS = arrayOf("Idea", "IJ", "release", "dev", "Studio")

fun pluginRuntimeVersion(pluginVersion: String): String {
    var placeToSplit = -1

    for (separator in PLUGIN_VERSIONS_SEPARATORS) {
        val ideaPatternIndex = StringUtil.indexOf(pluginVersion, separator)
        if (ideaPatternIndex >= 2 && Character.isDigit(pluginVersion[ideaPatternIndex - 2])) {
            placeToSplit = ideaPatternIndex - 1
            break
        }
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

        val localPath = getLocalPath(replacedFile) ?:
                        error("Should be called for replacing valid root file: $replacedJarFile")

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

data class BinaryVersionedFile<out T : BinaryVersion>(val file: VirtualFile, val version: T, val supportedVersion: T)

private fun <T : BinaryVersion> getLibraryRootsWithAbiIncompatibleVersion(
        module: Module,
        supportedVersion: T,
        index: ScalarIndexExtension<T>
): Collection<BinaryVersionedFile<T>> {
    val id = index.name

    val moduleWithAllDependencies = setOf(module) + ModuleUtil.getAllDependentModules(module)
    val moduleWithAllDependentLibraries = GlobalSearchScope.union(
            moduleWithAllDependencies.map { it.moduleWithLibrariesScope }.toTypedArray())

    val allVersions = FileBasedIndex.getInstance().getAllKeys(id, module.project)
    val badVersions = allVersions.filterNot(BinaryVersion::isCompatible).toHashSet()
    val badRoots = hashSetOf<BinaryVersionedFile<T>>()
    val fileIndex = ProjectFileIndex.SERVICE.getInstance(module.project)

    for (version in badVersions) {
        val indexedFiles = FileBasedIndex.getInstance().getContainingFiles(id, version, moduleWithAllDependentLibraries)
        for (indexedFile in indexedFiles) {
            val libraryRoot = fileIndex.getClassRootForFile(indexedFile) ?:
                    error("Only library roots were requested, and only class files should be indexed with the $id key. " +
                          "File: ${indexedFile.path}")
            badRoots.add(BinaryVersionedFile(getLocalFile(libraryRoot), version, supportedVersion))
        }
    }

    return badRoots
}

fun showRuntimeJarNotFoundDialog(project: Project, jarName: String) {
    Messages.showErrorDialog(project,
                             jarName + " is not found. Make sure plugin is properly installed.",
                             "No Runtime Found")
}

fun getKotlinJvmRuntimeMarkerClass(project: Project, scope: GlobalSearchScope): PsiClass? {
    return runReadAction {
        project.runWithAlternativeResolveEnabled {
            JavaPsiFacade.getInstance(project).findClass(KotlinBuiltIns.FQ_NAMES.unit.asString(), scope)
        }
    }
}

fun hasKotlinJsKjsmFile(project: Project, scope: GlobalSearchScope): Boolean {
    return runReadAction {
        project.runWithAlternativeResolveEnabled {
            JsVirtualFileFinderFactory.SERVICE.getInstance(project).create(scope).hasPackage(constant { FqName("kotlin.js") })
        }
    }
}

fun getStdlibArtifactId(sdk: Sdk?): String {
    val sdkVersion = sdk?.let { JavaSdk.getInstance().getVersion(it) }
    val artifactId = when (sdkVersion) {
        JavaSdkVersion.JDK_1_8, JavaSdkVersion.JDK_1_9 -> MAVEN_STDLIB_ID_JRE8
        JavaSdkVersion.JDK_1_7 -> MAVEN_STDLIB_ID_JRE7
        else -> MAVEN_STDLIB_ID
    }
    return artifactId
}

val MAVEN_STDLIB_ID = "kotlin-stdlib"
val MAVEN_STDLIB_ID_JRE7 = "kotlin-stdlib-jre7"
val MAVEN_STDLIB_ID_JRE8 = "kotlin-stdlib-jre8"
val MAVEN_JS_STDLIB_ID = "kotlin-stdlib-js"
val MAVEN_OLD_JS_STDLIB_ID = "kotlin-js-library"
val MAVEN_COMMON_STDLIB_ID = "kotlin-stdlib-common" // TODO: KotlinCommonMavenConfigurator

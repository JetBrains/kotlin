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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.*
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.PathUtil.getLocalFile
import com.intellij.util.containers.MultiMap
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.configuration.*
import org.jetbrains.kotlin.idea.framework.JavaRuntimeDetectionUtil
import org.jetbrains.kotlin.idea.framework.isExternalLibrary
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.idea.util.runWithAlternativeResolveEnabled
import org.jetbrains.kotlin.idea.vfilefinder.KotlinJavaScriptMetaFileIndex
import org.jetbrains.kotlin.idea.vfilefinder.hasSomethingInPackage
import org.jetbrains.kotlin.load.kotlin.JvmMetadataVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion
import org.jetbrains.kotlin.utils.JsMetadataVersion
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.LibraryUtils
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.util.*

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

    val kJvmConfigurator = getConfiguratorByName(KotlinJavaModuleConfigurator.NAME) as KotlinJavaModuleConfigurator? ?:
                           error("Configurator with given name doesn't exists: " + KotlinJavaModuleConfigurator.NAME)

    val kJsConfigurator = getConfiguratorByName(KotlinJsModuleConfigurator.NAME) as KotlinJsModuleConfigurator? ?:
                          error("Configurator with given name doesn't exists: " + KotlinJsModuleConfigurator.NAME)

    val collector = createConfigureKotlinNotificationCollector(project)
    val sdk = ProjectRootManager.getInstance(project).projectSdk
    // TODO use module SDK

    for (library in libraries) {
        val libraryJarDescriptors = if (JavaRuntimeDetectionUtil.getJavaRuntimeVersion(library) != null)
            kJvmConfigurator.getLibraryJarDescriptors(sdk)
        else
            kJsConfigurator.getLibraryJarDescriptors(sdk)

        for (libraryJarDescriptor in libraryJarDescriptors) {
            updateJar(project, library, libraryJarDescriptor)
        }
    }

    collector.showNotification()
}

private fun updateJar(
        project: Project,
        library: Library,
        libraryJarDescriptor: LibraryJarDescriptor) {
    val fileToReplace = libraryJarDescriptor.findExistingJar(library)

    if (fileToReplace == null && !libraryJarDescriptor.shouldExist) {
        return
    }

    val oldUrl = fileToReplace?.url
    val jarPath: File = libraryJarDescriptor.getPathInPlugin()

    if (!jarPath.exists()) {
        showRuntimeJarNotFoundDialog(project, libraryJarDescriptor.jarName)
        return
    }

    val jarFileToReplace = getLocalJar(fileToReplace)!!
    val newVFile = replaceFile(jarPath, jarFileToReplace)
    if (newVFile != null) {
        val model = library.modifiableModel
        runWriteAction {
            try {
                if (oldUrl != null) {
                    model.removeRoot(oldUrl, libraryJarDescriptor.orderRootType)
                }
                val newRoot = JarFileSystem.getInstance().getJarRootForLocalFile(newVFile)!!
                model.addRoot(newRoot, libraryJarDescriptor.orderRootType)
            }
            finally {
                model.commit()
            }
        }
    }
}

fun findAllUsedLibraries(project: Project): MultiMap<Library, Module> {
    val libraries = MultiMap<Library, Module>()

    for (module in ModuleManager.getInstance(project).modules) {
        val moduleRootManager = ModuleRootManager.getInstance(module)

        for (entry in moduleRootManager.orderEntries.filterIsInstance<LibraryOrderEntry>()) {
            val library = entry.library ?: continue

            libraries.putValue(library, module)
        }
    }

    return libraries
}

enum class LibraryJarDescriptor(val jarName: String,
                                val orderRootType: OrderRootType,
                                val shouldExist: Boolean,
                                val getPath: (KotlinPaths) -> File = { paths -> File(paths.libPath, jarName) }) {
    RUNTIME_JAR(PathUtil.KOTLIN_JAVA_STDLIB_JAR, OrderRootType.CLASSES, true, KotlinPaths::getStdlibPath) {
        override fun findExistingJar(library: Library): VirtualFile? {
            if (isExternalLibrary(library)) return null
            return JavaRuntimeDetectionUtil.getRuntimeJar(Arrays.asList(*library.getFiles(OrderRootType.CLASSES)))
        }
    },

    REFLECT_JAR(PathUtil.KOTLIN_JAVA_REFLECT_JAR, OrderRootType.CLASSES, false, KotlinPaths::getReflectPath),
    SCRIPT_RUNTIME_JAR(PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR, OrderRootType.CLASSES, true, KotlinPaths::getScriptRuntimePath),
    TEST_JAR(PathUtil.KOTLIN_TEST_JAR, OrderRootType.CLASSES, false, KotlinPaths::getKotlinTestPath),
    RUNTIME_JRE7_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_JRE7_JAR, OrderRootType.CLASSES, false),
    RUNTIME_JRE8_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_JRE8_JAR, OrderRootType.CLASSES, false),
    RUNTIME_JRE7_SOURCES_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_JRE7_SRC_JAR, OrderRootType.SOURCES, false),
    RUNTIME_JRE8_SOURCES_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_JRE8_SRC_JAR, OrderRootType.SOURCES, false),

    RUNTIME_SRC_JAR(PathUtil.KOTLIN_JAVA_STDLIB_SRC_JAR, OrderRootType.SOURCES, false, KotlinPaths::getStdlibSourcesPath) {
        override fun findExistingJar(library: Library): VirtualFile? {
            return super.findExistingJar(library) ?:
                   LibraryUtils.getJarFile(library.getFiles(orderRootType).toList(), PathUtil.KOTLIN_JAVA_STDLIB_SRC_JAR_OLD)
        }
    },
    REFLECT_SRC_JAR(PathUtil.KOTLIN_REFLECT_SRC_JAR, OrderRootType.SOURCES, false),
    TEST_SRC_JAR(PathUtil.KOTLIN_TEST_SRC_JAR, OrderRootType.SOURCES, false),

    JS_STDLIB_JAR(PathUtil.JS_LIB_JAR_NAME, OrderRootType.CLASSES, true, KotlinPaths::getJsStdLibJarPath),
    JS_STDLIB_SRC_JAR(PathUtil.JS_LIB_SRC_JAR_NAME, OrderRootType.SOURCES, false, KotlinPaths::getJsStdLibSrcJarPath);

    open fun findExistingJar(library: Library): VirtualFile? {
        if (isExternalLibrary(library)) return null
        return LibraryUtils.getJarFile(Arrays.asList(*library.getFiles(orderRootType)), jarName)
    }

    fun getPathInPlugin() = getPath(PathUtil.kotlinPathsForIdeaPlugin)
}

fun bundledRuntimeVersion(): String {
    return bundledRuntimeBuildNumber ?: pluginRuntimeVersion(KotlinPluginUtil.getPluginVersion())
}

private val bundledRuntimeBuildNumber: String? by lazy {
    val file = PathUtil.kotlinPathsForIdeaPlugin.buildNumberFile
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

internal fun replaceFile(updatedFile: File, jarFileToReplace: VirtualFile): VirtualFile? {
    val jarIoFileToReplace = File(jarFileToReplace.path)

    if (FileUtil.filesEqual(updatedFile, jarIoFileToReplace)) {
        return null
    }

    FileUtil.copy(updatedFile, jarIoFileToReplace)
    if (jarIoFileToReplace.name != updatedFile.name) {
        val newFile = File(jarIoFileToReplace.parent, updatedFile.name)
        if (!newFile.exists()) {
            if (!jarIoFileToReplace.renameTo(newFile)) {
                LOG.info("Failed to rename ${jarIoFileToReplace.path} to ${newFile.path}")
                return null
            }
            val newVFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(newFile)
            if (newVFile == null) {
                LOG.info("Failed to find ${newFile.path} in VFS")
                return null
            }
            newVFile.refresh(false, true)
            return newVFile
        }
    }
    jarFileToReplace.refresh(false, true)
    return null
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

private val KOTLIN_JS_FQ_NAME = FqName("kotlin.js")

fun hasKotlinJsKjsmFile(project: Project, scope: GlobalSearchScope): Boolean {
    return runReadAction {
        project.runWithAlternativeResolveEnabled {
            KotlinJavaScriptMetaFileIndex.hasSomethingInPackage(KOTLIN_JS_FQ_NAME, scope)
        }
    }
}

fun getStdlibArtifactId(sdk: Sdk?, version: String): String {
    if (!hasJreSpecificRuntime(version)) {
        return MAVEN_STDLIB_ID
    }

    val sdkVersion = sdk?.let { JavaSdk.getInstance().getVersion(it) }
    return when (sdkVersion) {
        JavaSdkVersion.JDK_1_8, JavaSdkVersion.JDK_1_9 -> MAVEN_STDLIB_ID_JRE8
        JavaSdkVersion.JDK_1_7 -> MAVEN_STDLIB_ID_JRE7
        else -> MAVEN_STDLIB_ID
    }
}

fun getDefaultJvmTarget(sdk: Sdk?, version: String): JvmTarget? {
    if (!hasJreSpecificRuntime(version)) {
        return null
    }
    val sdkVersion = sdk?.let { JavaSdk.getInstance().getVersion(it) }
    if (sdkVersion != null && sdkVersion.isAtLeast(JavaSdkVersion.JDK_1_8)) {
        return JvmTarget.JVM_1_8
    }
    return null
}

fun hasJreSpecificRuntime(version: String): Boolean =
        VersionComparatorUtil.compare(version, "1.1.0") >= 0 ||
        isSnapshot(version) ||
        version == "default_version" /* for tests */

val MAVEN_STDLIB_ID = "kotlin-stdlib"
val MAVEN_STDLIB_ID_JRE7 = "kotlin-stdlib-jre7"
val MAVEN_STDLIB_ID_JRE8 = "kotlin-stdlib-jre8"
val MAVEN_JS_STDLIB_ID = "kotlin-stdlib-js"
val MAVEN_OLD_JS_STDLIB_ID = "kotlin-js-library"
val MAVEN_COMMON_STDLIB_ID = "kotlin-stdlib-common" // TODO: KotlinCommonMavenConfigurator
val LOG = Logger.getInstance("org.jetbrains.kotlin.idea.versions.KotlinRuntimeLibraryUtilKt")
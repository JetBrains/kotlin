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
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.MultiMap
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.text.VersionComparatorUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.idea.framework.JavaRuntimeDetectionUtil
import org.jetbrains.kotlin.idea.framework.isExternalLibrary
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.projectStructure.version
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

enum class LibraryJarDescriptor(
    val jarName: String,
    val orderRootType: OrderRootType,
    val shouldExist: Boolean,
    val getPath: (KotlinPaths) -> File = { paths -> File(paths.libPath, jarName) }
) {
    RUNTIME_JAR(PathUtil.KOTLIN_JAVA_STDLIB_JAR, OrderRootType.CLASSES, true, KotlinPaths::getStdlibPath) {
        override fun findExistingJar(library: Library): VirtualFile? {
            if (isExternalLibrary(library)) return null
            return JavaRuntimeDetectionUtil.getRuntimeJar(Arrays.asList(*library.getFiles(OrderRootType.CLASSES)))
        }
    },

    REFLECT_JAR(PathUtil.KOTLIN_JAVA_REFLECT_JAR, OrderRootType.CLASSES, false, KotlinPaths::getReflectPath),
    SCRIPT_RUNTIME_JAR(PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR, OrderRootType.CLASSES, true, KotlinPaths::getScriptRuntimePath),
    TEST_JAR(PathUtil.KOTLIN_TEST_JAR, OrderRootType.CLASSES, false, KotlinPaths::getKotlinTestPath),

    @Deprecated("RUNTIME_JDK7_JAR should be used since 1.2")
    RUNTIME_JRE7_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_JRE7_JAR, OrderRootType.CLASSES, false),
    RUNTIME_JDK7_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_JDK7_JAR, OrderRootType.CLASSES, false),

    @Deprecated("RUNTIME_JDK8_JAR should be used since 1.2")
    RUNTIME_JRE8_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_JRE8_JAR, OrderRootType.CLASSES, false),
    RUNTIME_JDK8_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_JDK8_JAR, OrderRootType.CLASSES, false),

    @Deprecated("RUNTIME_JDK7_SOURCES_JAR should be used since 1.2")
    RUNTIME_JRE7_SOURCES_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_JRE7_SRC_JAR, OrderRootType.SOURCES, false),
    RUNTIME_JDK7_SOURCES_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_JDK7_SRC_JAR, OrderRootType.SOURCES, false),

    @Deprecated("RUNTIME_JDK8_SOURCES_JAR should be used since 1.2")
    RUNTIME_JRE8_SOURCES_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_JRE8_SRC_JAR, OrderRootType.SOURCES, false),
    RUNTIME_JDK8_SOURCES_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_JDK8_SRC_JAR, OrderRootType.SOURCES, false),

    RUNTIME_SRC_JAR(PathUtil.KOTLIN_JAVA_STDLIB_SRC_JAR, OrderRootType.SOURCES, false, KotlinPaths::getStdlibSourcesPath) {
        override fun findExistingJar(library: Library): VirtualFile? {
            return super.findExistingJar(library) ?: LibraryUtils.getJarFile(
                library.getFiles(orderRootType).toList(),
                PathUtil.KOTLIN_JAVA_STDLIB_SRC_JAR_OLD
            )
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

fun bundledRuntimeVersion(): String = KotlinCompilerVersion.VERSION


fun getLocalJar(kotlinRuntimeJar: VirtualFile?): VirtualFile? {
    if (kotlinRuntimeJar == null) return null

    val localJarFile = JarFileSystem.getInstance().getVirtualFileForJar(kotlinRuntimeJar)
    if (localJarFile != null) {
        return localJarFile
    }
    return kotlinRuntimeJar
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
        moduleWithAllDependencies.map { it.moduleWithLibrariesScope }.toTypedArray()
    )

    val allVersions = FileBasedIndex.getInstance().getAllKeys(id, module.project)
    val badVersions = allVersions.filterNot(BinaryVersion::isCompatible).toHashSet()
    val badRoots = hashSetOf<BinaryVersionedFile<T>>()
    val fileIndex = ProjectFileIndex.SERVICE.getInstance(module.project)

    for (version in badVersions) {
        val indexedFiles = FileBasedIndex.getInstance().getContainingFiles(id, version, moduleWithAllDependentLibraries)
        for (indexedFile in indexedFiles) {
            val libraryRoot = fileIndex.getClassRootForFile(indexedFile) ?: error(
                "Only library roots were requested, and only class files should be indexed with the $id key. " +
                        "File: ${indexedFile.path}"
            )
            badRoots.add(BinaryVersionedFile(com.intellij.util.PathUtil.getLocalFile(libraryRoot), version, supportedVersion))
        }
    }

    return badRoots
}

fun showRuntimeJarNotFoundDialog(project: Project, jarName: String) {
    Messages.showErrorDialog(
        project,
        jarName + " is not found. Make sure plugin is properly installed.",
        "No Runtime Found"
    )
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

    val sdkVersion = sdk?.version
    if (hasJdkLikeUpdatedRuntime(version)) {
        return when {
            sdkVersion != null && sdkVersion.isAtLeast(JavaSdkVersion.JDK_1_8) -> MAVEN_STDLIB_ID_JDK8
            sdkVersion == JavaSdkVersion.JDK_1_7 -> MAVEN_STDLIB_ID_JDK7
            else -> MAVEN_STDLIB_ID
        }
    }

    return when {
        sdkVersion != null && sdkVersion.isAtLeast(JavaSdkVersion.JDK_1_8) -> MAVEN_STDLIB_ID_JRE8
        sdkVersion == JavaSdkVersion.JDK_1_7 -> MAVEN_STDLIB_ID_JRE7
        else -> MAVEN_STDLIB_ID
    }
}

fun getDefaultJvmTarget(sdk: Sdk?, version: String): JvmTarget? {
    if (!hasJreSpecificRuntime(version)) {
        return null
    }
    val sdkVersion = sdk?.version
    if (sdkVersion != null && sdkVersion.isAtLeast(JavaSdkVersion.JDK_1_8)) {
        return JvmTarget.JVM_1_8
    }
    return null
}

fun isSnapshot(version: String): Boolean {
    return version.contains("SNAPSHOT", ignoreCase = true)
}

fun hasJdkLikeUpdatedRuntime(version: String): Boolean =
    VersionComparatorUtil.compare(version, "1.2.0-rc-39") >= 0 ||
            isSnapshot(version) ||
            version == "default_version" /* for tests */

fun hasJreSpecificRuntime(version: String): Boolean =
    VersionComparatorUtil.compare(version, "1.1.0") >= 0 ||
            isSnapshot(version) ||
            version == "default_version" /* for tests */

const val MAVEN_STDLIB_ID = PathUtil.KOTLIN_JAVA_STDLIB_NAME

const val MAVEN_STDLIB_ID_JRE7 = PathUtil.KOTLIN_JAVA_RUNTIME_JRE7_NAME
const val MAVEN_STDLIB_ID_JDK7 = PathUtil.KOTLIN_JAVA_RUNTIME_JDK7_NAME

const val MAVEN_STDLIB_ID_JRE8 = PathUtil.KOTLIN_JAVA_RUNTIME_JRE8_NAME
const val MAVEN_STDLIB_ID_JDK8 = PathUtil.KOTLIN_JAVA_RUNTIME_JDK8_NAME

const val MAVEN_JS_STDLIB_ID = PathUtil.JS_LIB_NAME
const val MAVEN_JS_TEST_ID = PathUtil.KOTLIN_TEST_JS_NAME

const val MAVEN_OLD_JS_STDLIB_ID = "kotlin-js-library"
const val MAVEN_COMMON_STDLIB_ID = "kotlin-stdlib-common" // TODO: KotlinCommonMavenConfigurator
const val MAVEN_TEST_ID = PathUtil.KOTLIN_TEST_NAME
const val MAVEN_TEST_JUNIT_ID = "kotlin-test-junit"
const val MAVEN_COMMON_TEST_ID = "kotlin-test-common"
const val MAVEN_COMMON_TEST_ANNOTATIONS_ID = "kotlin-test-annotations-common"

val LOG = Logger.getInstance("org.jetbrains.kotlin.idea.versions.KotlinRuntimeLibraryUtilKt")

data class LibInfo(
    val groupId: String,
    val name: String
)

data class DeprecatedLibInfo(
    val old: LibInfo,
    val new: LibInfo,
    val outdatedAfterVersion: String,
    val message: String
)

private fun deprecatedLib(
    oldGroupId: String,
    oldName: String,
    newGroupId: String = oldGroupId,
    newName: String = oldName,
    outdatedAfterVersion: String,
    message: String
): DeprecatedLibInfo {
    return DeprecatedLibInfo(
        old = LibInfo(groupId = oldGroupId, name = oldName),
        new = LibInfo(groupId = newGroupId, name = newName),
        outdatedAfterVersion = outdatedAfterVersion,
        message = message
    )
}

val DEPRECATED_LIBRARIES_INFORMATION = listOf(
    deprecatedLib(
        oldGroupId = "org.jetbrains.kotlin",
        oldName = PathUtil.KOTLIN_JAVA_RUNTIME_JRE7_NAME, newName = PathUtil.KOTLIN_JAVA_RUNTIME_JDK7_NAME,
        outdatedAfterVersion = "1.2.0-rc-39",
        message = "${PathUtil.KOTLIN_JAVA_RUNTIME_JRE7_NAME} is deprecated since 1.2.0 and should be replaced with ${PathUtil.KOTLIN_JAVA_RUNTIME_JDK7_NAME}"
    ),

    deprecatedLib(
        oldGroupId = "org.jetbrains.kotlin",
        oldName = PathUtil.KOTLIN_JAVA_RUNTIME_JRE8_NAME, newName = PathUtil.KOTLIN_JAVA_RUNTIME_JDK8_NAME,
        outdatedAfterVersion = "1.2.0-rc-39",
        message = "${PathUtil.KOTLIN_JAVA_RUNTIME_JRE8_NAME} is deprecated since 1.2.0 and should be replaced with ${PathUtil.KOTLIN_JAVA_RUNTIME_JDK8_NAME}"
    )
)
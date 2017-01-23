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

package org.jetbrains.kotlin.cli.jvm

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.MavenComparableVersion
import java.util.*
import java.util.jar.Manifest

internal inline fun Properties.getString(propertyName: String, otherwise: () -> String): String =
        getProperty(propertyName) ?: otherwise()

object JvmRuntimeVersionsConsistencyChecker {
    private val LOG = Logger.getInstance(JvmRuntimeVersionsConsistencyChecker::class.java)

    private fun fatal(message: String): Nothing {
        LOG.error(message)
        throw AssertionError(message)
    }

    private fun <T> T?.assertNotNull(message: () -> String): T =
            if (this == null) fatal(message()) else this

    private val VERSION_ISSUE_SEVERITY = CompilerMessageSeverity.ERROR

    private const val META_INF = "META-INF"
    private const val MANIFEST_MF = "$META_INF/MANIFEST.MF"

    private const val MANIFEST_KOTLIN_VERSION_ATTRIBUTE = "manifest.impl.attribute.kotlin.version"
    private const val MANIFEST_KOTLIN_VERSION_VALUE = "manifest.impl.value.kotlin.version"
    private const val MANIFEST_KOTLIN_RUNTIME_COMPONENT = "manifest.impl.attribute.kotlin.runtime.component"
    private const val MANIFEST_KOTLIN_RUNTIME_COMPONENT_CORE = "manifest.impl.value.kotlin.runtime.component.core"
    private const val MANIFEST_KOTLIN_RUNTIME_COMPONENT_MAIN = "manifest.impl.value.kotlin.runtime.component.main"

    private const val KOTLIN_STDLIB_MODULE = "$META_INF/kotlin-stdlib.kotlin_module"
    private const val KOTLIN_REFLECT_MODULE = "$META_INF/kotlin-reflection.kotlin_module"
    private const val KOTLIN_COMPILER_MODULE = "$META_INF/kotlin-compiler.kotlin_module"

    private val KOTLIN_VERSION_ATTRIBUTE: String
    private val CURRENT_COMPILER_VERSION: MavenComparableVersion

    private val KOTLIN_RUNTIME_COMPONENT_ATTRIBUTE: String
    private val KOTLIN_RUNTIME_COMPONENT_CORE: String
    private val KOTLIN_RUNTIME_COMPONENT_MAIN: String

    init {
        val manifestProperties: Properties = try {
            JvmRuntimeVersionsConsistencyChecker::class.java
                    .getResourceAsStream("/kotlinManifest.properties")
                    .let { input -> Properties().apply { load(input) } }
        }
        catch (e: Exception) {
            LOG.error(e)
            throw e
        }

        KOTLIN_VERSION_ATTRIBUTE = manifestProperties.getProperty(MANIFEST_KOTLIN_VERSION_ATTRIBUTE)
                .assertNotNull { "$MANIFEST_KOTLIN_VERSION_ATTRIBUTE not found in kotlinManifest.properties" }

        CURRENT_COMPILER_VERSION = run {
            val kotlinVersionString = manifestProperties.getProperty(MANIFEST_KOTLIN_VERSION_VALUE)
                    .assertNotNull { "$MANIFEST_KOTLIN_VERSION_VALUE not found in kotlinManifest.properties" }

            MavenComparableVersion(kotlinVersionString)
        }

        if (CURRENT_COMPILER_VERSION != MavenComparableVersion(LanguageVersion.LATEST)) {
            fatal("Kotlin compiler version $CURRENT_COMPILER_VERSION in kotlinManifest.properties doesn't match ${LanguageVersion.LATEST}")
        }

        KOTLIN_RUNTIME_COMPONENT_ATTRIBUTE = manifestProperties.getProperty(MANIFEST_KOTLIN_RUNTIME_COMPONENT)
                .assertNotNull { "$MANIFEST_KOTLIN_RUNTIME_COMPONENT not found in kotlinManifest.properties" }
        KOTLIN_RUNTIME_COMPONENT_CORE = manifestProperties.getProperty(MANIFEST_KOTLIN_RUNTIME_COMPONENT_CORE)
                .assertNotNull { "$MANIFEST_KOTLIN_RUNTIME_COMPONENT_CORE not found in kotlinManifest.properties" }
        KOTLIN_RUNTIME_COMPONENT_MAIN = manifestProperties.getProperty(MANIFEST_KOTLIN_RUNTIME_COMPONENT_MAIN)
                .assertNotNull { "$MANIFEST_KOTLIN_RUNTIME_COMPONENT_MAIN not found in kotlinManifest.properties" }
    }

    private class KotlinLibraryFile(val component: String, val file: VirtualFile, val version: MavenComparableVersion) {
        override fun toString(): String =
                "${file.name}:$version ($component)"
    }

    private class RuntimeJarsInfo(
            // Runtime jars with components "Main" and "Core"
            val jars: List<KotlinLibraryFile>,
            // Runtime jars with components "Core" only (a subset of [jars])
            val coreJars: List<KotlinLibraryFile>
    )

    fun checkCompilerClasspathConsistency(
            messageCollector: MessageCollector,
            languageVersionSettings: LanguageVersionSettings?,
            classpathJarRoots: List<VirtualFile>
    ) {
        val runtimeJarsInfo = collectRuntimeJarsInfo(classpathJarRoots)
        if (runtimeJarsInfo.jars.isEmpty()) return

        val languageVersion = languageVersionSettings?.let { MavenComparableVersion(it.languageVersion) } ?: CURRENT_COMPILER_VERSION

        if (checkCompilerClasspathConsistency(messageCollector, languageVersion, runtimeJarsInfo)) {
            messageCollector.issue(null, "Some runtime JAR files in the classpath have an incompatible version. " +
                                         "Remove them from the classpath or use '-Xskip-runtime-version-check' to suppress errors")
        }
    }

    private fun checkCompilerClasspathConsistency(
            messageCollector: MessageCollector,
            languageVersion: MavenComparableVersion,
            runtimeJarsInfo: RuntimeJarsInfo
    ): Boolean {
        // The "Core" jar files should not be newer than the compiler. This behavior is reserved for the future if we realise that we're
        // going to break language/library compatibility in such a way that it's easier to make the old compiler just report an error
        // in the case the new runtime library is specified in the classpath, rather than employing any other compatibility breakage tools
        // we have at our disposal (Deprecated, SinceKotlin, SinceKotlinInfo in metadata, etc.)
        if (runtimeJarsInfo.coreJars.map {
            checkNotNewerThanCompiler(messageCollector, it)
        }.any { it }) return true

        if (runtimeJarsInfo.jars.map {
            checkCompatibleWithLanguageVersion(messageCollector, it, languageVersion)
        }.any { it }) return true

        return checkMatchingVersions(messageCollector, runtimeJarsInfo)
    }

    private fun checkNotNewerThanCompiler(messageCollector: MessageCollector, jar: KotlinLibraryFile): Boolean {
        if (jar.version > CURRENT_COMPILER_VERSION) {
            messageCollector.issue(jar.file, "Runtime JAR file has version ${jar.version} which is newer than compiler version $CURRENT_COMPILER_VERSION")
            return true
        }
        return false
    }

    private fun checkCompatibleWithLanguageVersion(
            messageCollector: MessageCollector, jar: KotlinLibraryFile, languageVersion: MavenComparableVersion
    ): Boolean {
        if (jar.version < languageVersion) {
            messageCollector.issue(jar.file, "Runtime JAR file has version ${jar.version} which is older than required for language version $languageVersion")
            return true
        }
        return false
    }

    private fun checkMatchingVersions(messageCollector: MessageCollector, runtimeJarsInfo: RuntimeJarsInfo): Boolean {
        val oldestJar = runtimeJarsInfo.jars.minBy { it.version } ?: return false
        val newestJar = runtimeJarsInfo.jars.maxBy { it.version } ?: return false

        if (oldestJar.version != newestJar.version) {
            messageCollector.issue(null, buildString {
                appendln("Runtime JAR files in the classpath must have the same version. These files were found in the classpath:")
                for (jar in runtimeJarsInfo.jars) {
                    appendln("    ${jar.file.path} (version ${jar.version})")
                }
            }.trimEnd())
            return true
        }

        return false
    }

    private fun MessageCollector.issue(file: VirtualFile?, message: String) {
        report(VERSION_ISSUE_SEVERITY, message, CompilerMessageLocation.create(file?.let(VfsUtilCore::virtualToIoFile)?.path))
    }

    private fun collectRuntimeJarsInfo(classpathJarRoots: List<VirtualFile>): RuntimeJarsInfo {
        val jars = ArrayList<KotlinLibraryFile>(2)
        val coreJars = ArrayList<KotlinLibraryFile>(2)

        for (jarRoot in classpathJarRoots) {
            val manifest = try {
                val manifestFile = jarRoot.findFileByRelativePath(MANIFEST_MF) ?: continue
                Manifest(manifestFile.inputStream)
            }
            catch (e: Exception) {
                continue
            }

            val runtimeComponent = getKotlinRuntimeComponent(jarRoot, manifest) ?: continue
            val version = manifest.getKotlinLanguageVersion()

            val jarFile = VfsUtilCore.getVirtualFileForJar(jarRoot) ?: continue
            val file = KotlinLibraryFile(runtimeComponent, jarFile, version)

            if (runtimeComponent == KOTLIN_RUNTIME_COMPONENT_CORE) {
                jars.add(file)
                coreJars.add(file)
            }
            else if (runtimeComponent == KOTLIN_RUNTIME_COMPONENT_MAIN) {
                jars.add(file)
            }
        }

        return RuntimeJarsInfo(jars, coreJars)
    }

    private fun getKotlinRuntimeComponent(jar: VirtualFile, manifest: Manifest): String? {
        manifest.mainAttributes.getValue(KOTLIN_RUNTIME_COMPONENT_ATTRIBUTE)?.let { return it }

        // Do not treat kotlin-compiler and kotlin-compiler-embeddable as Kotlin runtime libraries.
        // The second condition is needed because when the compiler is built with "compiler-quick", there's no kotlin-compiler.kotlin_module
        if (jar.findFileByRelativePath(KOTLIN_COMPILER_MODULE) != null ||
            jar.findFileByRelativePath(this::class.java.name.replace('.', '/') + "." + JavaClassFileType.INSTANCE.defaultExtension) != null) return null

        if (jar.findFileByRelativePath(KOTLIN_STDLIB_MODULE) != null) return KOTLIN_RUNTIME_COMPONENT_MAIN
        if (jar.findFileByRelativePath(KOTLIN_REFLECT_MODULE) != null) return KOTLIN_RUNTIME_COMPONENT_MAIN

        return null
    }

    private fun Manifest.getKotlinLanguageVersion(): MavenComparableVersion =
            MavenComparableVersion(mainAttributes.getValue(KOTLIN_VERSION_ATTRIBUTE) ?: LanguageVersion.KOTLIN_1_0.versionString)

    private fun MavenComparableVersion(languageVersion: LanguageVersion): MavenComparableVersion =
            MavenComparableVersion(languageVersion.versionString)
}

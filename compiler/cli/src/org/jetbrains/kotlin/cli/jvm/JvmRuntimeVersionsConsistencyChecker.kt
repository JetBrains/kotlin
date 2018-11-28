/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*
import java.io.IOException
import java.util.*
import java.util.jar.Attributes
import java.util.jar.Manifest

object JvmRuntimeVersionsConsistencyChecker {
    private val LOG = Logger.getInstance(JvmRuntimeVersionsConsistencyChecker::class.java)

    private fun <T> T?.assertNotNull(lazyMessage: () -> String): T =
        this ?: lazyMessage().let { message ->
            LOG.error(message)
            throw AssertionError(message)
        }

    private const val META_INF = "META-INF"
    private const val MANIFEST_MF = "$META_INF/MANIFEST.MF"

    private const val MANIFEST_KOTLIN_VERSION_ATTRIBUTE = "manifest.impl.attribute.kotlin.version"
    private const val MANIFEST_KOTLIN_RUNTIME_COMPONENT = "manifest.impl.attribute.kotlin.runtime.component"
    private const val MANIFEST_KOTLIN_RUNTIME_COMPONENT_CORE = "manifest.impl.value.kotlin.runtime.component.core"
    private const val MANIFEST_KOTLIN_RUNTIME_COMPONENT_MAIN = "manifest.impl.value.kotlin.runtime.component.main"

    private const val KOTLIN_STDLIB_MODULE = "$META_INF/kotlin-stdlib.kotlin_module"
    private const val KOTLIN_STDLIB_JRE_7_MODULE = "$META_INF/kotlin-stdlib-jre7.kotlin_module"
    private const val KOTLIN_STDLIB_JRE_8_MODULE = "$META_INF/kotlin-stdlib-jre8.kotlin_module"
    private const val KOTLIN_REFLECT_MODULE = "$META_INF/kotlin-reflection.kotlin_module"

    private val RUNTIME_IMPLEMENTATION_TITLES = setOf(
        "kotlin-runtime", "kotlin-stdlib", "kotlin-reflect", "Kotlin Runtime", "Kotlin Standard Library", "Kotlin Reflect"
    )

    private val KOTLIN_VERSION_ATTRIBUTE: String
    private val KOTLIN_RUNTIME_COMPONENT_ATTRIBUTE: String
    private val KOTLIN_RUNTIME_COMPONENT_CORE: String
    private val KOTLIN_RUNTIME_COMPONENT_MAIN: String

    init {
        val manifestProperties: Properties = try {
            JvmRuntimeVersionsConsistencyChecker::class.java
                .getResourceAsStream("/kotlinManifest.properties")
                .let { input -> Properties().apply { load(input) } }
        } catch (e: Exception) {
            LOG.error(e)
            throw e
        }

        KOTLIN_VERSION_ATTRIBUTE = manifestProperties.getProperty(MANIFEST_KOTLIN_VERSION_ATTRIBUTE)
            .assertNotNull { "$MANIFEST_KOTLIN_VERSION_ATTRIBUTE not found in kotlinManifest.properties" }
        KOTLIN_RUNTIME_COMPONENT_ATTRIBUTE = manifestProperties.getProperty(MANIFEST_KOTLIN_RUNTIME_COMPONENT)
            .assertNotNull { "$MANIFEST_KOTLIN_RUNTIME_COMPONENT not found in kotlinManifest.properties" }
        KOTLIN_RUNTIME_COMPONENT_CORE = manifestProperties.getProperty(MANIFEST_KOTLIN_RUNTIME_COMPONENT_CORE)
            .assertNotNull { "$MANIFEST_KOTLIN_RUNTIME_COMPONENT_CORE not found in kotlinManifest.properties" }
        KOTLIN_RUNTIME_COMPONENT_MAIN = manifestProperties.getProperty(MANIFEST_KOTLIN_RUNTIME_COMPONENT_MAIN)
            .assertNotNull { "$MANIFEST_KOTLIN_RUNTIME_COMPONENT_MAIN not found in kotlinManifest.properties" }
    }

    private class KotlinLibraryFile(val file: VirtualFile, val version: MavenComparableVersion) {
        override fun toString(): String =
            "${file.name}:$version"
    }

    private class RuntimeJarsInfo(
        // Runtime jars with components "Main" and "Core"
        val jars: List<KotlinLibraryFile>,
        // Runtime jars with components "Core" only (a subset of [jars])
        val coreJars: List<KotlinLibraryFile>,
        // Library jars which have some Kotlin Runtime library bundled into them
        val otherLibrariesWithBundledRuntime: List<VirtualFile>,
        val stdlibJre7: List<KotlinLibraryFile>,
        val stdlibJre8: List<KotlinLibraryFile>
    )

    fun checkCompilerClasspathConsistency(
        messageCollector: MessageCollector,
        configuration: CompilerConfiguration,
        classpathJarRoots: List<VirtualFile>
    ) {
        val runtimeJarsInfo = collectRuntimeJarsInfo(classpathJarRoots)
        if (runtimeJarsInfo.jars.isEmpty()) return

        val languageVersionSettings = configuration.languageVersionSettings
        val currentApi = languageVersionSettings.apiVersion

        val consistency = checkCompilerClasspathConsistency(messageCollector, currentApi.version, runtimeJarsInfo)
        if (consistency is ClasspathConsistency.InconsistentWithApiVersion) {
            val actualRuntimeVersion = consistency.actualRuntimeVersion
            if (currentApi.isStable) {
                messageCollector.issue(
                    null,
                    "Runtime JAR files in the classpath have the version $actualRuntimeVersion, " +
                            "which is older than the API version ${currentApi.version}. " +
                            "Consider using the runtime of version ${currentApi.version}, or pass '-api-version $actualRuntimeVersion' " +
                            "explicitly to restrict the available APIs to the runtime of version $actualRuntimeVersion. " +
                            "You can also pass '-language-version $actualRuntimeVersion' instead, which will restrict " +
                            "not only the APIs to the specified version, but also the language features"
                )

                for (jar in consistency.incompatibleJars) {
                    messageCollector.issue(
                        jar.file,
                        "Runtime JAR file has version ${jar.version} which is older than required for API version ${currentApi.version}"
                    )
                }
            }

            val actualApi = ApiVersion.parse(actualRuntimeVersion.toString())
            if (actualApi == null) {
                messageCollector.issue(null, "Could not parse runtime JAR version: $actualRuntimeVersion")
            } else if (!languageVersionSettings.getFlag(AnalysisFlags.explicitApiVersion) && actualApi < currentApi) {
                // If there's no explicit "-api-version" AND there's an old stdlib in the classpath (older than the default value of API),
                // we infer API = the version of that stdlib.
                // Note that "no explicit -api-version" requirement is necessary because for example, in
                // "kotlinc-1.2 -language-version 1.0 -cp kotlin-runtime-1.1.jar" we should still infer API = 1.0
                val newSettings = object : LanguageVersionSettings by languageVersionSettings {
                    override val apiVersion: ApiVersion get() = actualApi
                }

                messageCollector.issue(
                    null, "Old runtime has been found in the classpath. " +
                            "Initial language version settings: $languageVersionSettings. " +
                            "Updated language version settings: $newSettings", CompilerMessageSeverity.LOGGING
                )

                configuration.languageVersionSettings = newSettings
            }
        } else if (consistency != ClasspathConsistency.Consistent) {
            messageCollector.issue(
                null,
                "Some runtime JAR files in the classpath have an incompatible version. Consider removing them from the classpath"
            )
        }

        if (configuration.languageVersionSettings.apiVersion >= ApiVersion.KOTLIN_1_2) {
            for (stdlibJre7 in runtimeJarsInfo.stdlibJre7) {
                messageCollector.issue(stdlibJre7.file, "kotlin-stdlib-jre7 is deprecated. Please use kotlin-stdlib-jdk7 instead")
            }
            for (stdlibJre8 in runtimeJarsInfo.stdlibJre8) {
                messageCollector.issue(stdlibJre8.file, "kotlin-stdlib-jre8 is deprecated. Please use kotlin-stdlib-jdk8 instead")
            }
        }

        val librariesWithBundled = runtimeJarsInfo.otherLibrariesWithBundledRuntime
        if (librariesWithBundled.isNotEmpty()) {
            messageCollector.issue(
                null,
                "Some JAR files in the classpath have the Kotlin Runtime library bundled into them. " +
                        "This may cause difficult to debug problems if there's a different version of the Kotlin Runtime library in the classpath. " +
                        "Consider removing these libraries from the classpath"
            )

            for (library in librariesWithBundled) {
                messageCollector.issue(library, "Library has Kotlin runtime bundled into it")
            }
        }
    }

    private sealed class ClasspathConsistency {
        object Consistent : ClasspathConsistency()
        class InconsistentWithApiVersion(
            val actualRuntimeVersion: MavenComparableVersion,
            val incompatibleJars: List<KotlinLibraryFile>
        ) : ClasspathConsistency()

        object InconsistentWithCompilerVersion : ClasspathConsistency()
        object InconsistentBecauseOfRuntimesWithDifferentVersions : ClasspathConsistency()
    }

    private fun checkCompilerClasspathConsistency(
        messageCollector: MessageCollector,
        apiVersion: MavenComparableVersion,
        runtimeJarsInfo: RuntimeJarsInfo
    ): ClasspathConsistency {
        // The "Core" jar files should not be newer than the compiler. This behavior is reserved for the future if we realise that we're
        // going to break language/library compatibility in such a way that it's easier to make the old compiler just report an error
        // in the case the new runtime library is specified in the classpath, rather than employing any other compatibility breakage tools
        // we have at our disposal (Deprecated, SinceKotlin, VersionRequirement in metadata, etc.)
        if (runtimeJarsInfo.coreJars.map {
            checkNotNewerThanCompiler(messageCollector, it)
        }.any { it }) return ClasspathConsistency.InconsistentWithCompilerVersion

        val jars = runtimeJarsInfo.jars
        if (jars.isEmpty()) return ClasspathConsistency.Consistent

        val runtimeVersion = checkMatchingVersionsAndGetRuntimeVersion(messageCollector, jars)
            ?: return ClasspathConsistency.InconsistentBecauseOfRuntimesWithDifferentVersions

        val jarsIncompatibleWithApiVersion = jars.filter { it.version < apiVersion }
        if (jarsIncompatibleWithApiVersion.isNotEmpty()) {
            return ClasspathConsistency.InconsistentWithApiVersion(runtimeVersion, jarsIncompatibleWithApiVersion)
        }

        return ClasspathConsistency.Consistent
    }

    private fun checkNotNewerThanCompiler(messageCollector: MessageCollector, jar: KotlinLibraryFile): Boolean {
        if (jar.version > ApiVersion.LATEST_STABLE.version) {
            messageCollector.issue(
                jar.file,
                "Runtime JAR file has version ${jar.version} which is newer than compiler version ${ApiVersion.LATEST_STABLE.version}",
                CompilerMessageSeverity.ERROR
            )
            return true
        }
        return false
    }

    // Returns the version if it's the same across all jars, or null if versions of some jars differ.
    private fun checkMatchingVersionsAndGetRuntimeVersion(
        messageCollector: MessageCollector,
        jars: List<KotlinLibraryFile>
    ): MavenComparableVersion? {
        assert(jars.isNotEmpty()) { "'jars' must not be empty" }
        val oldestVersion = jars.minBy { it.version }!!.version
        val newestVersion = jars.maxBy { it.version }!!.version

        // If the oldest version is the same as the newest version, then all jars have the same version
        if (oldestVersion == newestVersion) return oldestVersion

        messageCollector.issue(null, buildString {
            appendln("Runtime JAR files in the classpath should have the same version. These files were found in the classpath:")
            for (jar in jars) {
                appendln("    ${jar.file.path} (version ${jar.version})")
            }
        }.trimEnd())

        // If there's kotlin-stdlib of version X in the classpath and kotlin-reflect of version Y < X,
        // we suggest to provide an explicit dependency on version X.
        // TODO: report this depending on the content of the jars instead
        val minReflectJar =
            jars.filter { it.file.name.startsWith("kotlin-reflect") }.minBy { it.version }
        val maxStdlibJar =
            jars.filter { it.file.name.startsWith("kotlin-runtime") || it.file.name.startsWith("kotlin-stdlib") }.maxBy { it.version }
        if (minReflectJar != null && maxStdlibJar != null && minReflectJar.version < maxStdlibJar.version) {
            messageCollector.issue(
                null,
                "Consider providing an explicit dependency on kotlin-reflect ${maxStdlibJar.version} to prevent strange errors"
            )
        }

        return null
    }

    private fun MessageCollector.issue(
        file: VirtualFile?,
        message: String,
        severity: CompilerMessageSeverity = CompilerMessageSeverity.STRONG_WARNING
    ) {
        report(severity, message, CompilerMessageLocation.create(file?.let(VfsUtilCore::virtualToIoFile)?.path))
    }

    private fun collectRuntimeJarsInfo(classpathJarRoots: List<VirtualFile>): RuntimeJarsInfo {
        val jars = ArrayList<KotlinLibraryFile>(2)
        val coreJars = ArrayList<KotlinLibraryFile>(2)
        val otherLibrariesWithBundledRuntime = ArrayList<VirtualFile>(0)
        val stdlibJre7 = ArrayList<KotlinLibraryFile>(0)
        val stdlibJre8 = ArrayList<KotlinLibraryFile>(0)

        val visitedPaths = hashSetOf<String>()

        for (jarRoot in classpathJarRoots) {
            val fileKind = determineFileKind(jarRoot)
            if (fileKind is FileKind.Irrelevant) continue

            val jarFile = VfsUtilCore.getVirtualFileForJar(jarRoot) ?: continue
            if (!visitedPaths.add(jarFile.path)) continue

            when (fileKind) {
                is FileKind.Runtime -> {
                    val file = KotlinLibraryFile(jarFile, fileKind.version)
                    jars.add(file)
                    if (fileKind.isCoreComponent) {
                        coreJars.add(file)
                    }
                    if (fileKind.isStdlibJre7) {
                        stdlibJre7.add(file)
                    }
                    if (fileKind.isStdlibJre8) {
                        stdlibJre8.add(file)
                    }
                }
                FileKind.OldRuntime -> jars.add(KotlinLibraryFile(jarFile, ApiVersion.KOTLIN_1_0.version))
                FileKind.LibraryWithBundledRuntime -> otherLibrariesWithBundledRuntime.add(jarFile)
            }
        }

        return RuntimeJarsInfo(jars, coreJars, otherLibrariesWithBundledRuntime, stdlibJre7, stdlibJre8)
    }

    private sealed class FileKind {
        class Runtime(
            val version: MavenComparableVersion,
            val isStdlibJre7: Boolean,
            val isStdlibJre8: Boolean,
            val isCoreComponent: Boolean
        ) : FileKind()

        // Runtime library of Kotlin 1.0
        object OldRuntime : FileKind()

        object LibraryWithBundledRuntime : FileKind()

        object Irrelevant : FileKind()
    }

    private fun determineFileKind(jarRoot: VirtualFile): FileKind {
        val manifestFile = jarRoot.findFileByRelativePath(MANIFEST_MF)
        val manifest = try {
            manifestFile?.let { Manifest(it.inputStream) }
        } catch (e: IOException) {
            return FileKind.Irrelevant
        }

        val runtimeComponent = manifest?.mainAttributes?.getValue(KOTLIN_RUNTIME_COMPONENT_ATTRIBUTE)
        val isStdlibJre7 = jarRoot.findFileByRelativePath(KOTLIN_STDLIB_JRE_7_MODULE) != null
        val isStdlibJre8 = jarRoot.findFileByRelativePath(KOTLIN_STDLIB_JRE_8_MODULE) != null
        return when (runtimeComponent) {
            KOTLIN_RUNTIME_COMPONENT_MAIN ->
                FileKind.Runtime(manifest.getKotlinLanguageVersion(), isStdlibJre7, isStdlibJre8, isCoreComponent = false)
            KOTLIN_RUNTIME_COMPONENT_CORE ->
                FileKind.Runtime(manifest.getKotlinLanguageVersion(), isStdlibJre7, isStdlibJre8, isCoreComponent = true)
            null -> when {
                jarRoot.findFileByRelativePath(KOTLIN_STDLIB_MODULE) == null &&
                        jarRoot.findFileByRelativePath(KOTLIN_REFLECT_MODULE) == null -> FileKind.Irrelevant
                isGenuineKotlinRuntime(manifest) -> FileKind.OldRuntime
                else -> FileKind.LibraryWithBundledRuntime
            }
            else -> FileKind.Irrelevant
        }
    }

    // Returns true if the manifest is from the original Kotlin Runtime jar, false if it's from a library with a bundled runtime
    private fun isGenuineKotlinRuntime(manifest: Manifest?): Boolean {
        return manifest != null &&
                manifest.mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE) in RUNTIME_IMPLEMENTATION_TITLES
    }

    private fun Manifest.getKotlinLanguageVersion(): MavenComparableVersion =
        (mainAttributes.getValue(KOTLIN_VERSION_ATTRIBUTE)?.let((ApiVersion)::parse) ?: ApiVersion.KOTLIN_1_0).version
}

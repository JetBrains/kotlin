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

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.impl.light.LightJavaModule
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.jvm.modules.javaModuleInfoSourceReader
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.modules.ModuleInfoClassSelection
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRootBase
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootBase
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntry
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntryFactory
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.JavaModuleGraph
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.jvm.environment.asJvmClasspathRootId
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.ClassIdToJavaClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isValidJavaFqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo
import org.jetbrains.kotlin.resolve.jvm.modules.KOTLIN_STDLIB_MODULE_NAME
import java.io.IOException
import java.util.jar.Attributes
import java.util.jar.Manifest
import kotlin.LazyThreadSafetyMode.NONE

/**
 * @param findClassOnClasspath Resolves the annotation classes of a `module-info` anywhere on the classpath, lazily: the
 *  classpath index is built from the roots computed here, so the lookup fails if it runs while the roots are computed.
 * @param outputDirectory The identity of the output directory, which is a root of the compiled source module if it is also on the
 *  classpath or module path.
 * @param rootEntries The entries through which `module-info` files and manifests of the roots are read; the modules themselves keep
 *  the [VirtualFile]s of their roots and descriptors.
 */
class ClasspathRootsResolver(
    private val configuration: CompilerConfiguration,
    private val additionalModules: List<String>,
    private val contentRootToVirtualFile: (JvmContentRootBase) -> VirtualFile?,
    private val javaModuleFinder: CliJavaModuleFinder,
    private val requireStdlibModule: Boolean,
    private val outputDirectory: JvmClasspathRootId?,
    private val findClassOnClasspath: ClassIdToJavaClass,
    private val jdkRelease: Int?,
    hasKotlinSources: Boolean,
    private val rootEntries: JvmRootEntryFactory = JvmRootEntryFactory.VFS,
) {
    val javaModuleGraph = JavaModuleGraph(javaModuleFinder)

    // Only report Java module-related errors if there's at least one Kotlin source file in the module. Otherwise the compiler would only
    // report those errors and not the more important "no source files" error which is handled later, after the roots have been computed.
    private val reportErrors = hasKotlinSources

    private var computingRoots = false

    private val findClassOnClasspathAfterRoots: ClassIdToJavaClass = { classId ->
        check(!computingRoots) { "The classpath cannot be searched for $classId while its roots are computed" }
        findClassOnClasspath(classId)
    }

    /**
     * @param roots The roots to index: directories and archives.
     * @param singleJavaFileRoots The Java source files passed as roots, which are not indexed as directories (see `SingleJavaFileRootsIndex`).
     */
    data class RootsAndModules(val roots: List<JavaRoot>, val modules: List<JavaModule>, val singleJavaFileRoots: List<JavaRoot>)

    private data class RootWithPrefix(val root: VirtualFile, val packagePrefix: String?)

    /** A module path root with the facts about it known when it is resolved, so that they are not derived from [root] again. */
    private data class ModulePathRoot(val root: VirtualFile, val contentRoot: JvmModulePathRoot, val isJar: Boolean)

    fun convertClasspathRoots(contentRoots: List<ContentRoot>): RootsAndModules {
        val javaSourceRoots = mutableListOf<RootWithPrefix>()
        val jvmClasspathRoots = mutableListOf<VirtualFile>()
        val jvmModulePathRoots = mutableListOf<ModulePathRoot>()

        for (contentRoot in contentRoots) {
            if (contentRoot !is JvmContentRootBase) continue
            val root = contentRootToVirtualFile(contentRoot) ?: continue
            when (contentRoot) {
                is JavaSourceRoot -> javaSourceRoots += RootWithPrefix(root, contentRoot.packagePrefix)
                is JvmClasspathRootBase -> jvmClasspathRoots += root
                is JvmModulePathRoot ->
                    jvmModulePathRoots += ModulePathRoot(root, contentRoot, root.fileSystem.protocol == StandardFileSystems.JAR_PROTOCOL)
                else -> error("Unknown root type: $contentRoot")
            }
        }

        computingRoots = true
        try {
            return computeRoots(javaSourceRoots, jvmClasspathRoots, jvmModulePathRoots)
        } finally {
            computingRoots = false
        }
    }

    private fun computeRoots(
        javaSourceRoots: List<RootWithPrefix>,
        jvmClasspathRoots: List<VirtualFile>,
        jvmModulePathRoots: List<ModulePathRoot>
    ): RootsAndModules {
        val result = mutableListOf<JavaRoot>()
        val modules = mutableListOf<JavaModule>()

        val outputDirectoryRoot = outputDirectory?.let { outputDirectory ->
            (jvmClasspathRoots.asSequence() + jvmModulePathRoots.asSequence().map { it.root })
                .firstOrNull { it.asJvmClasspathRootId() == outputDirectory }
        }

        for ((root, packagePrefix) in javaSourceRoots) {
            val modularRoot = modularSourceRoot(root, outputDirectoryRoot)
            if (modularRoot != null) {
                modules += modularRoot
            } else {
                result += JavaRoot(root, JavaRoot.RootType.SOURCE, packagePrefix?.let { prefix ->
                    if (isValidJavaFqName(prefix)) FqName(prefix)
                    else null.also {
                        reportWarning("Invalid package prefix name is ignored: $prefix")
                    }
                })
            }
        }

        for (root in jvmClasspathRoots) {
            result += JavaRoot(root, JavaRoot.RootType.BINARY)
        }

        val outputDirectoryAddedAsPartOfModule =
            outputDirectory != null && modules.any { module -> module.moduleRoots.any { it.id == outputDirectory } }

        for (root in jvmModulePathRoots) {
            // Do not add output directory as a separate module if we're compiling an explicit named module.
            // It's going to be included as a root of our module in modularSourceRoot.
            if (outputDirectoryAddedAsPartOfModule && root.root.asJvmClasspathRootId() == outputDirectory) continue

            val module = modularBinaryRoot(root)
            if (module != null) {
                modules += module
            }
        }
        if (jdkRelease == null || jdkRelease >= 9) {
            addModularRoots(modules, result)
        } else {
            //TODO: see also `addJvmSdkRoots` usages, some refactoring is required with moving such logic into one place
            javaModuleFinder.nonModuleRoot?.let { nonModuleRoot ->
                result += JavaRoot(nonModuleRoot.file, JavaRoot.RootType.BINARY_SIG, id = nonModuleRoot.id)
            }
        }

        val [singleJavaFileRoots, roots] = result.partition(::isSingleJavaFileRoot)
        return RootsAndModules(roots, modules, singleJavaFileRoots)
    }

    private fun isSingleJavaFileRoot(root: JavaRoot): Boolean =
        root.type == JavaRoot.RootType.SOURCE &&
                root.file.let { file -> !file.isDirectory && file.extension == JavaFileType.DEFAULT_EXTENSION }

    private fun findSourceModuleInfo(root: VirtualFile): Pair<VirtualFile, JavaModuleInfo>? {
        val javaRoot = JavaRoot(root, JavaRoot.RootType.SOURCE)
        val rootEntry = rootEntries.createRootEntry(javaRoot)
        val moduleInfoEntry =
            when {
                rootEntry.isDirectory -> rootEntry.findChild(PsiJavaModule.MODULE_INFO_FILE)
                rootEntry.name == PsiJavaModule.MODULE_INFO_FILE -> rootEntry
                else -> null
            } ?: return null
        val moduleInfoFile = (if (rootEntry.isDirectory) root.findChild(PsiJavaModule.MODULE_INFO_FILE) else root) ?: return null

        val reader = configuration.javaModuleInfoSourceReader ?: run {
            // Not gated by `reportErrors`: this is a misconfiguration of the compiler, and silently treating a modular source root as
            // unnamed would change the accessibility results.
            report(
                CliDiagnostics.CLASSPATH_RESOLUTION_ERROR,
                "Cannot read '${PsiJavaModule.MODULE_INFO_FILE}': no reader is configured. " +
                        "Set it with 'CompilerConfiguration.putJavaModuleInfoSourceReader' or 'configureJavaModulesContentRoots'",
                moduleInfoFile
            )
            return null
        }
        // Read as UTF-8, javac's default and what java-direct uses for all Java sources.
        val text = try {
            String(moduleInfoEntry.toHandle(javaRoot).readBytes(), Charsets.UTF_8)
        } catch (_: IOException) {
            return null
        }
        val moduleInfo = reader.read(text, findClassOnClasspathAfterRoots) ?: return null

        return moduleInfoFile to moduleInfo
    }

    private fun modularSourceRoot(root: VirtualFile, outputDirectoryRoot: VirtualFile?): JavaModule.Explicit? {
        val [moduleInfoFile, moduleInfo] = findSourceModuleInfo(root) ?: return null
        val sourceRoot = JavaModule.Root(root, isBinary = false)
        val roots =
            if (outputDirectoryRoot != null)
                listOf(sourceRoot, JavaModule.Root(outputDirectoryRoot, isBinary = true))
            else listOf(sourceRoot)
        return JavaModule.Explicit(moduleInfo, roots, moduleInfoFile)
    }

    private fun modularBinaryRoot(modulePathRoot: ModulePathRoot): JavaModule? {
        val [root, contentRoot, isJar] = modulePathRoot
        val javaRoot = JavaRoot(root, JavaRoot.RootType.BINARY)
        val rootEntry = rootEntries.createRootEntry(javaRoot)
        val metaInf by lazy(NONE) { rootEntry.findChild(META_INF) }
        val manifest = lazy(NONE) { readManifestAttributes(metaInf, javaRoot) }

        val moduleInfoEntry = ModuleInfoClassSelection.select(
            rootModuleInfo = rootEntry.findChild(PsiJavaModule.MODULE_INFO_CLS_FILE)?.let { PsiJavaModule.MODULE_INFO_CLS_FILE to it },
            isJar = isJar,
            isMultiRelease = { ModuleInfoClassSelection.isMultiReleaseJar(manifest.value) },
            versionedModuleInfos = { findVersionSpecificModuleInfos(metaInf) },
        )

        if (moduleInfoEntry != null) {
            val [relativePath, entry] = moduleInfoEntry
            val moduleInfoFile = root.findFileByRelativePath(relativePath) ?: return null
            val handle = entry.toHandle(javaRoot)
            val moduleInfo = try {
                JavaModuleInfo.read(handle.readBytes(), handle.pathForDiagnostics, findClassOnClasspathAfterRoots)
            } catch (_: IOException) {
                null
            } ?: return null
            return JavaModule.Explicit(moduleInfo, listOf(JavaModule.Root(root, isBinary = true)), moduleInfoFile)
        }

        // Only .jar files can be automatic modules
        if (isJar) {
            val moduleRoot = listOf(JavaModule.Root(root, isBinary = true))

            val automaticModuleName = manifest.value?.getValue(AUTOMATIC_MODULE_NAME)
            if (automaticModuleName != null) {
                return JavaModule.Automatic(automaticModuleName, moduleRoot)
            }

            val moduleName = LightJavaModule.moduleName(contentRoot.file.nameWithoutExtension)
            if (moduleName.isEmpty()) {
                reportError("Cannot infer automatic module name for the file", VfsUtilCore.getVirtualFileForJar(root) ?: root)
                return null
            }
            return JavaModule.Automatic(moduleName, moduleRoot)
        }

        return null
    }

    /**
     * The `module-info.class` entries under `META-INF/versions/<n>/` with their paths relative to the root, keyed by `<n>`
     * (see [ModuleInfoClassSelection.select]).
     */
    private fun findVersionSpecificModuleInfos(metaInf: JvmRootEntry?): Map<String, Pair<String, JvmRootEntry>> {
        val versionsDir = metaInf?.findChild("versions") ?: return emptyMap()
        return versionsDir.children.mapNotNull { versionDir ->
            versionDir.findChild(PsiJavaModule.MODULE_INFO_CLS_FILE)?.let {
                versionDir.name to ("$META_INF/versions/${versionDir.name}/${PsiJavaModule.MODULE_INFO_CLS_FILE}" to it)
            }
        }.toMap()
    }

    private fun readManifestAttributes(metaInf: JvmRootEntry?, javaRoot: JavaRoot): Attributes? {
        val manifestEntry = metaInf?.findChild("MANIFEST.MF") ?: return null
        return try {
            Manifest(manifestEntry.toHandle(javaRoot).readBytes().inputStream()).mainAttributes
        } catch (_: IOException) {
            null
        }
    }

    private fun addModularRoots(modules: List<JavaModule>, result: MutableList<JavaRoot>) {
        // In current implementation, at most one source module is supported. This can be relaxed in the future if we support another
        // compilation mode, similar to java's --module-source-path
        val sourceModules = modules.filterIsInstance<JavaModule.Explicit>().filter(JavaModule::isSourceModule)
        if (sourceModules.size > 1) {
            for (module in sourceModules) {
                reportError("Too many source module declarations found", module.moduleInfoFile)
            }
            return
        }

        for (module in modules) {
            val existing = javaModuleFinder.findModule(module.name)
            if (existing == null) {
                javaModuleFinder.addUserModule(module)
            } else if (module.moduleRoots != existing.moduleRoots) {
                fun JavaModule.getRootFile() =
                    moduleRoots.firstOrNull()?.file?.let { VfsUtilCore.getVirtualFileForJar(it) ?: it }

                val thisFile = module.getRootFile()
                val existingFile = existing.getRootFile()
                val atExistingPath = if (existingFile == null) "" else " at: ${existingFile.path}"
                reportWarning(
                    "The root is ignored because a module with the same name '${module.name}' " +
                            "has been found earlier on the module path$atExistingPath", thisFile
                )
            }
        }

        if (javaModuleFinder.allObservableModules.none()) return

        val sourceModule = sourceModules.singleOrNull()
        val addAllModulePathToRoots = "ALL-MODULE-PATH" in additionalModules
        if (addAllModulePathToRoots && sourceModule != null) {
            reportError("-Xadd-modules=ALL-MODULE-PATH can only be used when compiling the unnamed module")
            return
        }

        val rootModules = when {
            sourceModule != null -> listOf(sourceModule.name) + additionalModules
            addAllModulePathToRoots -> modules.map(JavaModule::name)
            else -> javaModuleFinder.computeDefaultRootModules() + additionalModules
        }

        val allDependencies = javaModuleGraph.getAllDependencies(rootModules)
        // Compatibility, see KT-66622: the implicitly added stdlib used to come together with the automatic module
        // `kotlin.script.runtime`, which (as any automatic module) made all observable automatic modules part of the graph.
        // The script runtime is not added implicitly anymore, so this behavior is emulated for the implicitly added stdlib.
        // TODO(KT-66622): remove this compatibility code after the real fix
        val implicitAutomaticModulesCompatibility = KOTLIN_STDLIB_MODULE_NAME in additionalModules
        if (implicitAutomaticModulesCompatibility ||
            allDependencies.any { moduleName -> javaModuleFinder.findModule(moduleName) is JavaModule.Automatic }
        ) {
            // According to java.lang.module javadoc, if at least one automatic module is added to the module graph,
            // all observable automatic modules should be added.
            // There are no automatic modules in the JDK, so we select all automatic modules out of user modules
            for (module in modules) {
                if (module is JavaModule.Automatic) {
                    allDependencies += module.name
                }
            }
        }

        log("Loading modules: $allDependencies")

        for (moduleName in allDependencies) {
            val module = javaModuleFinder.findModule(moduleName)
            if (module == null) {
                reportError("Module $moduleName cannot be found in the module graph")
            } else {
                result.addAll(module.getJavaModuleRoots())
            }
        }

        if (requireStdlibModule && sourceModule != null && !javaModuleGraph.reads(sourceModule.name, KOTLIN_STDLIB_MODULE_NAME)) {
            reportError(
                "The Kotlin standard library is not found in the module graph. " +
                        "Please ensure you have the 'requires $KOTLIN_STDLIB_MODULE_NAME' clause in your module definition",
                sourceModule.moduleInfoFile
            )
        }
    }

    private fun reportError(message: String, file: VirtualFile? = null) {
        if (!reportErrors) return
        report(CliDiagnostics.CLASSPATH_RESOLUTION_ERROR, message, file)
    }

    private fun reportWarning(message: String, file: VirtualFile? = null) {
        report(CliDiagnostics.CLASSPATH_RESOLUTION_WARNING, message, file)
    }

    private fun report(factory: KtSourcelessDiagnosticFactory, message: String, file: VirtualFile? = null) {
        val location = file?.let { CompilerMessageLocation.create(MessageUtil.virtualFileToPath(it)) }
        configuration.report(factory, message, location)
    }

    private fun log(message: String) {
        configuration.reportLog(message)
    }

    private companion object {
        const val AUTOMATIC_MODULE_NAME = "Automatic-Module-Name"
        private const val META_INF = "META-INF"
    }
}

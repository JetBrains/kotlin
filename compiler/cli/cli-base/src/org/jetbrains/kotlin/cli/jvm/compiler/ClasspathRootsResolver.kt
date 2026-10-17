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

import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.light.LightJavaModule
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRootBase
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootBase
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.modules.ALL_UNNAMED
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.JavaModuleGraph
import org.jetbrains.kotlin.cli.jvm.modules.parseJavaModuleAccessOption
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isValidJavaFqName
import org.jetbrains.kotlin.resolve.jvm.KotlinCliJavaFileManager
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo
import org.jetbrains.kotlin.resolve.jvm.modules.KOTLIN_STDLIB_MODULE_NAME
import java.io.IOException
import java.util.jar.Attributes
import java.util.jar.Manifest
import kotlin.LazyThreadSafetyMode.NONE

class ClasspathRootsResolver(
    private val psiManager: PsiManager,
    private val configuration: CompilerConfiguration,
    private val additionalModules: List<String>,
    private val contentRootToVirtualFile: (JvmContentRootBase) -> VirtualFile?,
    private val javaModuleFinder: CliJavaModuleFinder,
    private val requireStdlibModule: Boolean,
    private val outputDirectory: VirtualFile?,
    private val javaFileManager: KotlinCliJavaFileManager,
    private val jdkRelease: Int?,
    hasKotlinSources: Boolean,
) {
    val javaModuleGraph = JavaModuleGraph(javaModuleFinder)

    private val searchScope = GlobalSearchScope.allScope(psiManager.project)

    // Only report Java module-related errors if there's at least one Kotlin source file in the module. Otherwise the compiler would only
    // report those errors and not the more important "no source files" error which is handled later, after the roots have been computed.
    private val reportErrors = hasKotlinSources

    /**
     * Values of `-Xadd-reads` in the module graph, from a module name to the modules it reads or `ALL-UNNAMED`. Computed by the first
     * call to [convertClasspathRoots].
     */
    var additionalJavaModuleReads: Map<String, Set<String>> = emptyMap()
        private set

    /**
     * Values of `-Xadd-exports` in the module graph, from `<module>/<package>` to the modules it is exported to or `ALL-UNNAMED`.
     * Computed by the first call to [convertClasspathRoots].
     */
    var additionalJavaModuleExports: Map<String, Set<String>> = emptyMap()
        private set

    /**
     * Modules read only from classpath roots that are also added to the module path (see [JvmModulePathRoot.isFromClasspath]). For javac,
     * those roots are in the unnamed module. Computed by the first call to [convertClasspathRoots].
     */
    var javaModulesFromClasspath: Set<String> = emptySet()
        private set

    // Roots passed to a later call, such as script dependencies, are resolved without the source module, so `-Xadd-reads` and
    // `-Xadd-exports` are only resolved on the first call.
    private var hasConvertedRoots = false

    data class RootsAndModules(val roots: List<JavaRoot>, val modules: List<JavaModule>)

    private data class RootWithPrefix(val root: VirtualFile, val packagePrefix: String?)

    fun convertClasspathRoots(contentRoots: List<ContentRoot>): RootsAndModules {
        val javaSourceRoots = mutableListOf<RootWithPrefix>()
        val jvmClasspathRoots = mutableListOf<VirtualFile>()
        val jvmModulePathRoots = mutableListOf<VirtualFile>()
        val jvmModulePathRootsFromClasspath = mutableSetOf<VirtualFile>()
        val jvmModulePathRootsGiven = mutableSetOf<VirtualFile>()

        for (contentRoot in contentRoots) {
            if (contentRoot !is JvmContentRootBase) continue
            val root = contentRootToVirtualFile(contentRoot) ?: continue
            when (contentRoot) {
                is JavaSourceRoot -> javaSourceRoots += RootWithPrefix(root, contentRoot.packagePrefix)
                is JvmClasspathRootBase -> jvmClasspathRoots += root
                is JvmModulePathRoot -> {
                    jvmModulePathRoots += root
                    (if (contentRoot.isFromClasspath) jvmModulePathRootsFromClasspath else jvmModulePathRootsGiven) += root
                }
                else -> error("Unknown root type: $contentRoot")
            }
        }

        // Like javac, a root given with the module path too is read from the module path.
        val rootsFromClasspath = jvmModulePathRootsFromClasspath - jvmModulePathRootsGiven
        return computeRoots(javaSourceRoots, jvmClasspathRoots, jvmModulePathRoots, rootsFromClasspath).also { hasConvertedRoots = true }
    }

    private fun computeRoots(
        javaSourceRoots: List<RootWithPrefix>,
        jvmClasspathRoots: List<VirtualFile>,
        jvmModulePathRoots: List<VirtualFile>,
        jvmModulePathRootsFromClasspath: Set<VirtualFile>,
    ): RootsAndModules {
        val result = mutableListOf<JavaRoot>()
        val modules = mutableListOf<JavaModule>()

        val hasOutputDirectoryInClasspath = outputDirectory in jvmClasspathRoots || outputDirectory in jvmModulePathRoots

        for ((root, packagePrefix) in javaSourceRoots) {
            val modularRoot = modularSourceRoot(root, hasOutputDirectoryInClasspath)
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

        val outputDirectoryAddedAsPartOfModule = modules.any { module -> module.moduleRoots.any { it.file == outputDirectory } }

        for (root in jvmModulePathRoots) {
            // Do not add output directory as a separate module if we're compiling an explicit named module.
            // It's going to be included as a root of our module in modularSourceRoot.
            if (outputDirectoryAddedAsPartOfModule && root == outputDirectory) continue

            val module = modularBinaryRoot(root)
            if (module != null) {
                modules += module
            }
        }
        if (jdkRelease == null || jdkRelease >= 9) {
            addModularRoots(modules, result, jvmModulePathRootsFromClasspath)
        } else {
            //TODO: see also `addJvmSdkRoots` usages, some refactoring is required with moving such logic into one place
            result += JavaRoot(javaModuleFinder.nonModuleRoot.file, JavaRoot.RootType.BINARY_SIG)
        }

        return RootsAndModules(result, modules)
    }

    private fun findSourceModuleInfo(root: VirtualFile): Pair<VirtualFile, PsiJavaModule>? {
        val moduleInfoFile =
            when {
                root.isDirectory -> root.findChild(PsiJavaModule.MODULE_INFO_FILE)
                root.name == PsiJavaModule.MODULE_INFO_FILE -> root
                else -> null
            } ?: return null

        val psiFile = psiManager.findFile(moduleInfoFile) ?: return null
        val psiJavaModule = psiFile.children.singleOrNull { it is PsiJavaModule } as? PsiJavaModule ?: return null

        return moduleInfoFile to psiJavaModule
    }

    private fun modularSourceRoot(root: VirtualFile, hasOutputDirectoryInClasspath: Boolean): JavaModule.Explicit? {
        val [moduleInfoFile, psiJavaModule] = findSourceModuleInfo(root) ?: return null
        val sourceRoot = JavaModule.Root(root, isBinary = false)
        val roots =
            if (hasOutputDirectoryInClasspath)
                listOf(sourceRoot, JavaModule.Root(outputDirectory!!, isBinary = true))
            else listOf(sourceRoot)
        return JavaModule.Explicit(JavaModuleInfo.create(psiJavaModule), roots, moduleInfoFile)
    }

    private fun modularBinaryRoot(root: VirtualFile): JavaModule? {
        val isJar = root.fileSystem.protocol == StandardFileSystems.JAR_PROTOCOL
        val manifest = lazy(NONE) { readManifestAttributes(root) }

        val moduleInfoFile =
            root.findChild(PsiJavaModule.MODULE_INFO_CLS_FILE)
                ?: if (isJar) tryLoadVersionSpecificModuleInfo(root, manifest) else null

        if (moduleInfoFile != null) {
            val moduleInfo = JavaModuleInfo.read(moduleInfoFile) {
                javaFileManager.findClass(JavaClassFinder.Request(it), searchScope)
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

            val originalFile = VfsUtilCore.virtualToIoFile(root)
            val moduleName = LightJavaModule.moduleName(originalFile.nameWithoutExtension)
            if (moduleName.isEmpty()) {
                reportError("Cannot infer automatic module name for the file", VfsUtilCore.getVirtualFileForJar(root) ?: root)
                return null
            }
            return JavaModule.Automatic(moduleName, moduleRoot)
        }

        return null
    }

    private fun tryLoadVersionSpecificModuleInfo(root: VirtualFile, manifest: Lazy<Attributes?>): VirtualFile? {
        val versionsDir = root.findChild("META-INF")?.findChild("versions") ?: return null

        val isMultiReleaseJar = manifest.value?.getValue(IS_MULTI_RELEASE)?.equals("true", ignoreCase = true)
        if (isMultiReleaseJar != true) return null

        val versions = versionsDir.children.filter {
            val version = it.name.toIntOrNull()
            version != null && version >= 9
        }.sortedBy { it.name.toInt() }
        for (version in versions) {
            val file = version.findChild(PsiJavaModule.MODULE_INFO_CLS_FILE)
            if (file != null) return file
        }
        return null
    }

    private fun readManifestAttributes(jarRoot: VirtualFile): Attributes? {
        val manifestFile = jarRoot.findChild("META-INF")?.findChild("MANIFEST.MF")
        return try {
            manifestFile?.inputStream?.let(::Manifest)?.mainAttributes
        } catch (_: IOException) {
            null
        }
    }

    private fun addModularRoots(modules: List<JavaModule>, result: MutableList<JavaRoot>, rootsFromClasspath: Set<VirtualFile>) {
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
        if (!hasConvertedRoots) {
            // A module of the same name given with the module path is added first, so the root from the classpath is ignored.
            javaModulesFromClasspath = modules.mapTo(mutableSetOf(), JavaModule::name).filterTo(mutableSetOf()) { name ->
                javaModuleFinder.findModule(name)?.moduleRoots.orEmpty().any { it.file in rootsFromClasspath }
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

        if (!hasConvertedRoots) {
            // Like javac, which puts the classpath roots in the unnamed module, the modules read from them are left out.
            additionalJavaModuleReads = collectJavaModuleAccessOptions(allDependencies - javaModulesFromClasspath, isExports = false)
            additionalJavaModuleExports = collectJavaModuleAccessOptions(allDependencies - javaModulesFromClasspath, isExports = true)
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

        if (requireStdlibModule && sourceModule != null && !javaModuleGraph.reads(sourceModule.name, KOTLIN_STDLIB_MODULE_NAME) &&
            !(KOTLIN_STDLIB_MODULE_NAME in allDependencies &&
                    javaModuleGraph.readsAdditionally(additionalJavaModuleReads, sourceModule.name, KOTLIN_STDLIB_MODULE_NAME))
        ) {
            reportError(
                "The Kotlin standard library is not found in the module graph. " +
                        "Please ensure you have the 'requires $KOTLIN_STDLIB_MODULE_NAME' clause in your module definition",
                sourceModule.moduleInfoFile
            )
        }
    }

    // Like javac, report and leave out the modules in -Xadd-reads and -Xadd-exports that are not in the module graph, and reject system
    // modules on the left-hand side with -Xjdk-release.
    private fun collectJavaModuleAccessOptions(modulesInGraph: Set<String>, isExports: Boolean): Map<String, Set<String>> {
        val flag = if (isExports) "-Xadd-exports" else "-Xadd-reads"
        val key = if (isExports) JVMConfigurationKeys.ADDITIONAL_JAVA_MODULE_EXPORTS else JVMConfigurationKeys.ADDITIONAL_JAVA_MODULE_READS

        fun isInGraph(name: String): Boolean =
            (name == ALL_UNNAMED || name in modulesInGraph).also { if (!it) reportWarning("Module name in `$flag` not found: $name") }

        val result = HashMap<String, MutableSet<String>>()
        for (value in configuration.getList(key)) {
            val option = parseJavaModuleAccessOption(value, isExports) ?: continue
            val source = option.source ?: continue
            val moduleName = source.substringBefore('/')
            if (!isInGraph(moduleName)) continue
            if (jdkRelease != null && javaModuleFinder.systemModules.any { it.name == moduleName }) {
                reportError(
                    if (isExports) "Exporting a package from system module $moduleName is not allowed with `-Xjdk-release`"
                    else "Adding read edges for system module $moduleName is not allowed with `-Xjdk-release`"
                )
                continue
            }
            result.getOrPut(source, ::HashSet) += option.otherModules.filter(::isInGraph)
        }
        return result
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
        const val IS_MULTI_RELEASE = "Multi-Release"
    }
}

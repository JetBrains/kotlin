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
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.JavaModuleGraph
import org.jetbrains.kotlin.config.ContentRoot
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isValidJavaFqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo
import org.jetbrains.kotlin.resolve.jvm.modules.KOTLIN_STDLIB_MODULE_NAME
import java.io.IOException
import java.util.jar.Attributes
import java.util.jar.Manifest
import kotlin.LazyThreadSafetyMode.NONE

class ClasspathRootsResolver(
        private val psiManager: PsiManager,
        private val messageCollector: MessageCollector?,
        private val additionalModules: List<String>,
        private val contentRootToVirtualFile: (JvmContentRoot) -> VirtualFile?,
        private val javaModuleFinder: CliJavaModuleFinder,
        private val requireStdlibModule: Boolean,
        private val outputDirectory: VirtualFile?
) {
    val javaModuleGraph = JavaModuleGraph(javaModuleFinder)

    data class RootsAndModules(val roots: List<JavaRoot>, val modules: List<JavaModule>)

    private data class RootWithPrefix(val root: VirtualFile, val packagePrefix: String?)

    fun convertClasspathRoots(contentRoots: List<ContentRoot>): RootsAndModules {
        val javaSourceRoots = mutableListOf<RootWithPrefix>()
        val jvmClasspathRoots = mutableListOf<VirtualFile>()
        val jvmModulePathRoots = mutableListOf<VirtualFile>()

        for (contentRoot in contentRoots) {
            if (contentRoot !is JvmContentRoot) continue
            val root = contentRootToVirtualFile(contentRoot) ?: continue
            when (contentRoot) {
                is JavaSourceRoot -> javaSourceRoots += RootWithPrefix(root, contentRoot.packagePrefix)
                is JvmClasspathRoot -> jvmClasspathRoots += root
                is JvmModulePathRoot -> jvmModulePathRoots += root
                else -> error("Unknown root type: $contentRoot")
            }
        }

        return computeRoots(javaSourceRoots, jvmClasspathRoots, jvmModulePathRoots)
    }

    private fun computeRoots(
            javaSourceRoots: List<RootWithPrefix>,
            jvmClasspathRoots: List<VirtualFile>,
            jvmModulePathRoots: List<VirtualFile>
    ): RootsAndModules {
        val result = mutableListOf<JavaRoot>()
        val modules = mutableListOf<JavaModule>()

        val hasOutputDirectoryInClasspath = outputDirectory in jvmClasspathRoots || outputDirectory in jvmModulePathRoots

        for ((root, packagePrefix) in javaSourceRoots) {
            val modularRoot = modularSourceRoot(root, hasOutputDirectoryInClasspath)
                    if (modularRoot != null) {
                        modules += modularRoot
                    }
                    else {
                result += JavaRoot(root, JavaRoot.RootType.SOURCE, packagePrefix?.let { prefix ->
                            if (isValidJavaFqName(prefix)) FqName(prefix)
                            else null.also {
                                report(STRONG_WARNING, "Invalid package prefix name is ignored: $prefix")
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

        addModularRoots(modules, result)

        return RootsAndModules(result, modules)
    }

/*
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
        val (moduleInfoFile, psiJavaModule) = findSourceModuleInfo(root) ?: return null
        val sourceRoot = JavaModule.Root(root, isBinary = false)
        val roots =
                if (hasOutputDirectoryInClasspath)
                    listOf(sourceRoot, JavaModule.Root(outputDirectory!!, isBinary = true))
                else listOf(sourceRoot)
        return JavaModule.Explicit(JavaModuleInfo.create(psiJavaModule), roots, moduleInfoFile)
    }

    private fun modularBinaryRoot(root: VirtualFile): JavaModule? {
        val isJar = root.fileSystem.protocol == StandardFileSystems.JAR_PROTOCOL
        val manifest: Attributes? by lazy(NONE) { readManifestAttributes(root) }

        val moduleInfoFile =
                root.findChild(PsiJavaModule.MODULE_INFO_CLS_FILE)
                ?: root.takeIf { isJar }?.findFileByRelativePath(MULTI_RELEASE_MODULE_INFO_CLS_FILE)?.takeIf {
                    manifest?.getValue(IS_MULTI_RELEASE)?.equals("true", ignoreCase = true) == true
                }

        if (moduleInfoFile != null) {
            val moduleInfo = JavaModuleInfo.read(moduleInfoFile) ?: return null
            return JavaModule.Explicit(moduleInfo, listOf(JavaModule.Root(root, isBinary = true)), moduleInfoFile)
        }

        // Only .jar files can be automatic modules
        if (isJar) {
            val moduleRoot = listOf(JavaModule.Root(root, isBinary = true))

            val automaticModuleName = manifest?.getValue(AUTOMATIC_MODULE_NAME)
            if (automaticModuleName != null) {
                return JavaModule.Automatic(automaticModuleName, moduleRoot)
            }

            val originalFile = VfsUtilCore.virtualToIoFile(root)
            val moduleName = LightJavaModule.moduleName(originalFile.nameWithoutExtension)
            if (moduleName.isEmpty()) {
                report(ERROR, "Cannot infer automatic module name for the file", VfsUtilCore.getVirtualFileForJar(root) ?: root)
                return null
            }
            return JavaModule.Automatic(moduleName, moduleRoot)
        }

        return null
    }
*/

    private fun readManifestAttributes(jarRoot: VirtualFile): Attributes? {
        val manifestFile = jarRoot.findChild("META-INF")?.findChild("MANIFEST.MF")
        return try {
            manifestFile?.inputStream?.let(::Manifest)?.mainAttributes
        }
        catch (e: IOException) {
            null
        }
    }

    private fun addModularRoots(modules: List<JavaModule>, result: MutableList<JavaRoot>) {
        // In current implementation, at most one source module is supported. This can be relaxed in the future if we support another
        // compilation mode, similar to java's --module-source-path
        val sourceModules = modules.filterIsInstance<JavaModule.Explicit>().filter(JavaModule::isSourceModule)
        if (sourceModules.size > 1) {
            for (module in sourceModules) {
                report(ERROR, "Too many source module declarations found", module.moduleInfoFile)
            }
            return
        }

        for (module in modules) {
            val existing = javaModuleFinder.findModule(module.name)
            if (existing == null) {
                javaModuleFinder.addUserModule(module)
            }
            else if (module.moduleRoots != existing.moduleRoots) {
                fun JavaModule.getRootFile() =
                        moduleRoots.firstOrNull()?.file?.let { VfsUtilCore.getVirtualFileForJar(it) ?: it }

                val thisFile = module.getRootFile()
                val existingFile = existing.getRootFile()
                val atExistingPath = if (existingFile == null) "" else " at: ${existingFile.path}"
                report(STRONG_WARNING, "The root is ignored because a module with the same name '${module.name}' " +
                                       "has been found earlier on the module path$atExistingPath", thisFile)
            }
        }

        if (javaModuleFinder.allObservableModules.none()) return

        val sourceModule = sourceModules.singleOrNull()
        val addAllModulePathToRoots = "ALL-MODULE-PATH" in additionalModules
        if (addAllModulePathToRoots && sourceModule != null) {
            report(ERROR, "-Xadd-modules=ALL-MODULE-PATH can only be used when compiling the unnamed module")
            return
        }

        val rootModules = when {
            sourceModule != null -> listOf(sourceModule.name) + additionalModules
            addAllModulePathToRoots -> modules.map(JavaModule::name)
            else -> computeDefaultRootModules() + additionalModules
        }

        val allDependencies = javaModuleGraph.getAllDependencies(rootModules)
        if (allDependencies.any { moduleName -> javaModuleFinder.findModule(moduleName) is JavaModule.Automatic }) {
            // According to java.lang.module javadoc, if at least one automatic module is added to the module graph,
            // all observable automatic modules should be added.
            // There are no automatic modules in the JDK, so we select all automatic modules out of user modules
            for (module in modules) {
                if (module is JavaModule.Automatic) {
                    allDependencies += module.name
                }
            }
        }

        report(LOGGING, "Loading modules: $allDependencies")

        for (moduleName in allDependencies) {
            val module = javaModuleFinder.findModule(moduleName)
            if (module == null) {
                report(ERROR, "Module $moduleName cannot be found in the module graph")
            }
            else {
                for ((root, isBinary) in module.moduleRoots) {
                    result.add(JavaRoot(root, if (isBinary) JavaRoot.RootType.BINARY else JavaRoot.RootType.SOURCE))
                }
            }
        }

        if (requireStdlibModule && sourceModule != null && KOTLIN_STDLIB_MODULE_NAME !in allDependencies) {
            report(
                    ERROR,
                    "The Kotlin standard library is not found in the module graph. " +
                    "Please ensure you have the 'requires $KOTLIN_STDLIB_MODULE_NAME' clause in your module definition",
                    sourceModule.moduleInfoFile
            )
        }
    }

    // See http://openjdk.java.net/jeps/261
    private fun computeDefaultRootModules(): List<String> {
        val result = arrayListOf<String>()

        val systemModules = javaModuleFinder.systemModules.associateBy(JavaModule::name)
        val javaSeExists = "java.se" in systemModules
        if (javaSeExists) {
            // The java.se module is a root, if it exists.
            result.add("java.se")
        }

        fun JavaModule.Explicit.exportsAtLeastOnePackageUnqualified(): Boolean = moduleInfo.exports.any { it.toModules.isEmpty() }

        if (!javaSeExists) {
            // If it does not exist then every java.* module on the upgrade module path or among the system modules
            // that exports at least one package, without qualification, is a root.
            for ((name, module) in systemModules) {
                if (name.startsWith("java.") && module.exportsAtLeastOnePackageUnqualified()) {
                    result.add(name)
                }
            }
        }

        for ((name, module) in systemModules) {
            // Every non-java.* module on the upgrade module path or among the system modules that exports at least one package,
            // without qualification, is also a root.
            if (!name.startsWith("java.") && module.exportsAtLeastOnePackageUnqualified()) {
                result.add(name)
            }
        }

        return result
    }

    private fun report(severity: CompilerMessageSeverity, message: String, file: VirtualFile? = null) {
        if (messageCollector == null) {
            throw IllegalStateException("${if (file != null) file.path + ":" else ""}$severity: $message (no MessageCollector configured)")
        }
        messageCollector.report(
                severity, message,
                if (file == null) null else CompilerMessageLocation.create(MessageUtil.virtualFileToPath(file))
        )
    }

    private companion object {
        //const val MULTI_RELEASE_MODULE_INFO_CLS_FILE = "META-INF/versions/9/${PsiJavaModule.MODULE_INFO_CLS_FILE}"
        const val AUTOMATIC_MODULE_NAME = "Automatic-Module-Name"
        const val IS_MULTI_RELEASE = "Multi-Release"
    }
}

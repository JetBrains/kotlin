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
import com.intellij.openapi.vfs.VirtualFileManager
//import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiManager
//import com.intellij.psi.impl.light.LightJavaModule
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
import java.io.File
import java.io.IOException
import java.util.jar.Attributes
import java.util.jar.Manifest
import kotlin.LazyThreadSafetyMode.NONE

internal class ClasspathRootsResolver(
        private val psiManager: PsiManager,
        private val messageCollector: MessageCollector?,
        private val additionalModules: List<String>,
        private val contentRootToVirtualFile: (JvmContentRoot) -> VirtualFile?
) {
    val javaModuleFinder = CliJavaModuleFinder(VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JRT_PROTOCOL))
    val javaModuleGraph = JavaModuleGraph(javaModuleFinder)

    data class RootsAndModules(val roots: List<JavaRoot>, val modules: List<JavaModule>)

    fun convertClasspathRoots(contentRoots: List<ContentRoot>): RootsAndModules {
        val result = mutableListOf<JavaRoot>()

        val modules = ArrayList<JavaModule>()

        for (contentRoot in contentRoots) {
            if (contentRoot !is JvmContentRoot) continue
            val root = contentRootToVirtualFile(contentRoot) ?: continue

            when (contentRoot) {
                is JavaSourceRoot -> {
                    val modularRoot = modularSourceRoot(root)
                    if (modularRoot != null) {
                        modules += modularRoot
                    }
                    else {
                        result += JavaRoot(root, JavaRoot.RootType.SOURCE, contentRoot.packagePrefix?.let { prefix ->
                            if (isValidJavaFqName(prefix)) FqName(prefix)
                            else null.also {
                                report(STRONG_WARNING, "Invalid package prefix name is ignored: $prefix")
                            }
                        })
                    }
                }
                is JvmClasspathRoot -> {
                    result += JavaRoot(root, JavaRoot.RootType.BINARY)
                }
                is JvmModulePathRoot -> {
                    val module = modularBinaryRoot(root, contentRoot.file)
                    if (module != null) {
                        modules += module
                    }
                }
                else -> error("Unknown root type: $contentRoot")
            }
        }

        addModularRoots(modules, result)

        return RootsAndModules(result, modules)
    }

    private fun modularSourceRoot(root: VirtualFile): JavaModule.Explicit? {
        return null
        /*
        val moduleInfoFile =
                when {
                    root.isDirectory -> root.findChild(PsiJavaModule.MODULE_INFO_FILE)
                    root.name == PsiJavaModule.MODULE_INFO_FILE -> root
                    else -> null
                } ?: return null

        val psiFile = psiManager.findFile(moduleInfoFile) ?: return null
        val psiJavaModule = psiFile.children.singleOrNull { it is PsiJavaModule } as? PsiJavaModule ?: return null
        return JavaModule.Explicit(JavaModuleInfo.create(psiJavaModule), root, moduleInfoFile, isBinary = false)
        */
    }

    private fun modularBinaryRoot(root: VirtualFile, originalFile: File): JavaModule? {
        return null
        /*
        val moduleInfoFile = root.findChild(PsiJavaModule.MODULE_INFO_CLS_FILE)
        return if (moduleInfoFile != null) {
            val moduleInfo = JavaModuleInfo.read(moduleInfoFile) ?: return null
            return JavaModule.Explicit(moduleInfo, root, moduleInfoFile, isBinary = true)
        }

        // Only .jar files can be automatic modules
        if (isJar) {
            val automaticModuleName = manifest?.getValue(AUTOMATIC_MODULE_NAME)
            if (automaticModuleName != null) {
                return JavaModule.Automatic(automaticModuleName, root)
            }

            val moduleName = LightJavaModule.moduleName(originalFile.nameWithoutExtension)
            if (moduleName.isEmpty()) {
                report(ERROR, "Cannot infer automatic module name for the file", VfsUtilCore.getVirtualFileForJar(root) ?: root)
                return null
            }
            return JavaModule.Automatic(moduleName, root)
        }
    }

    private fun readManifestAttributes(jarRoot: VirtualFile): Attributes? {
        val manifestFile = jarRoot.findChild("META-INF")?.findChild("MANIFEST.MF")
        return try {
            manifestFile?.inputStream?.let(::Manifest)?.mainAttributes
        }
        catch (e: IOException) {
            null
        }
        */
    }

    private fun addModularRoots(modules: List<JavaModule>, result: MutableList<JavaRoot>) {
        val sourceModules = modules.filterIsInstance<JavaModule.Explicit>().filterNot(JavaModule::isBinary)
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
            else if (module.moduleRoot != existing.moduleRoot) {
                val jar = VfsUtilCore.getVirtualFileForJar(module.moduleRoot) ?: module.moduleRoot
                val existingPath = (VfsUtilCore.getVirtualFileForJar(existing.moduleRoot) ?: existing.moduleRoot).path
                report(STRONG_WARNING, "The root is ignored because a module with the same name '${module.name}' " +
                                       "has been found earlier on the module path at: $existingPath", jar)
            }
        }

        if (javaModuleFinder.allObservableModules.none()) return

        val addAllModulePathToRoots = "ALL-MODULE-PATH" in additionalModules
        if (addAllModulePathToRoots && sourceModules.isNotEmpty()) {
            report(ERROR, "-Xadd-modules=ALL-MODULE-PATH can only be used when compiling the unnamed module")
            return
        }

        val rootModules = when {
            sourceModules.isNotEmpty() -> listOf(sourceModules.single().name) + additionalModules
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
                result.add(JavaRoot(
                        module.moduleRoot,
                        if (module.isBinary) JavaRoot.RootType.BINARY else JavaRoot.RootType.SOURCE
                ))
            }
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
        const val AUTOMATIC_MODULE_NAME = "Automatic-Module-Name"
        const val IS_MULTI_RELEASE = "Multi-Release"
    }
}

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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiJavaModule
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
import org.jetbrains.kotlin.config.ContentRoot
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isValidJavaFqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleGraph
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo

internal class ClasspathRootsResolver(
        private val psiManager: PsiManager,
        private val messageCollector: MessageCollector?,
        private val additionalModules: List<String>,
        private val contentRootToVirtualFile: (JvmContentRoot) -> VirtualFile?
) {
    private val javaModuleFinder = CliJavaModuleFinder(VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JRT_PROTOCOL))
    val javaModuleGraph = JavaModuleGraph(javaModuleFinder)

    data class RootsAndModules(val roots: List<JavaRoot>, val modules: List<JavaModule>)

    fun convertClasspathRoots(contentRoots: Iterable<ContentRoot>): RootsAndModules {
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
                    // TODO: sanitize the automatic module name exactly as in javac
                    // TODO: read Automatic-Module-Name manifest entry
                    val module = modularBinaryRoot(root, automaticModuleName = { contentRoot.file.name })
                    if (module != null) {
                        // TODO: report something in case of several modules with the same name?
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
        val moduleInfoFile =
                when {
                    root.isDirectory -> root.findChild(PsiJavaModule.MODULE_INFO_FILE)
                    root.name == PsiJavaModule.MODULE_INFO_FILE -> root
                    else -> null
                } ?: return null

        val psiFile = psiManager.findFile(moduleInfoFile) ?: return null
        val psiJavaModule = psiFile.children.singleOrNull { it is PsiJavaModule } as? PsiJavaModule ?: return null
        return JavaModule.Explicit(JavaModuleInfo.create(psiJavaModule), root, moduleInfoFile, isBinary = false)
    }

    private fun modularBinaryRoot(root: VirtualFile, automaticModuleName: () -> String): JavaModule? {
        val moduleInfoFile = root.findChild(PsiJavaModule.MODULE_INFO_CLS_FILE)
        return if (moduleInfoFile != null) {
            val moduleInfo = JavaModuleInfo.read(moduleInfoFile) ?: return null
            JavaModule.Explicit(moduleInfo, root, moduleInfoFile, isBinary = true)
        }
        else {
            JavaModule.Automatic(automaticModuleName(), root)
        }
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
            // TODO: report a diagnostic if a module with this name was already added
            javaModuleFinder.addUserModule(module)
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

        // TODO: if at least one automatic module is added, add all automatic modules as per java.lang.module javadoc
        val allDependencies = javaModuleGraph.getAllDependencies(rootModules).also { loadedModules ->
            report(LOGGING, "Loading modules: $loadedModules")
        }

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
}

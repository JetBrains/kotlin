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
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRoot
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.config.ContentRoot
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.isValidJavaFqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleGraph
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo

internal class ClasspathRootsResolver(
        private val messageCollector: MessageCollector?,
        private val contentRootToVirtualFile: (JvmContentRoot) -> VirtualFile?
) {
    private val javaModuleFinder = CliJavaModuleFinder(VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JRT_PROTOCOL))
    private val javaModuleGraph = JavaModuleGraph(javaModuleFinder)

    fun convertClasspathRoots(contentRoots: Iterable<ContentRoot>): List<JavaRoot> {
        val result = mutableListOf<JavaRoot>()

        for (contentRoot in contentRoots) {
            if (contentRoot !is JvmContentRoot) continue
            val root = contentRootToVirtualFile(contentRoot) ?: continue

            when (contentRoot) {
                is JavaSourceRoot -> {
                    result += JavaRoot(root, JavaRoot.RootType.SOURCE, contentRoot.packagePrefix?.let { prefix ->
                        if (isValidJavaFqName(prefix)) FqName(prefix)
                        else null.also {
                            report(STRONG_WARNING, "Invalid package prefix name is ignored: $prefix")
                        }
                    })
                }
                is JvmClasspathRoot -> {
                    result += JavaRoot(root, JavaRoot.RootType.BINARY)
                }
                else -> error("Unknown root type: $contentRoot")
            }
        }

        addModularRoots(result)

        return result
    }

    private fun addModularRoots(result: MutableList<JavaRoot>) {
        val jrtFileSystem = javaModuleFinder.jrtFileSystem ?: return

        val rootModules = computeRootModules()
        val allDependencies = javaModuleGraph.getAllDependencies(rootModules).also { modules ->
            report(LOGGING, "Loading modules: $modules")
        }

        for (moduleName in allDependencies) {
            // TODO: support modules not only from Java runtime image, but from a separate module path
            val root = jrtFileSystem.findFileByPath("/modules/$moduleName")
            if (root == null) {
                report(ERROR, "Module $moduleName cannot be found in the module graph")
            }
            else {
                result.add(JavaRoot(root, JavaRoot.RootType.BINARY))
            }
        }
    }

    // See http://openjdk.java.net/jeps/261
    private fun computeRootModules(): List<String> {
        val result = arrayListOf<String>()

        val systemModules = javaModuleFinder.computeAllSystemModules()
        val javaSeExists = "java.se" in systemModules
        if (javaSeExists) {
            // The java.se module is a root, if it exists.
            result.add("java.se")
        }

        fun JavaModuleInfo.exportsAtLeastOnePackageUnqualified(): Boolean = exports.any { it.toModules.isEmpty() }

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

    private fun report(severity: CompilerMessageSeverity, message: String) {
        if (messageCollector == null) {
            throw IllegalStateException("$severity: $message (no MessageCollector configured)")
        }
        messageCollector.report(severity, message)
    }
}

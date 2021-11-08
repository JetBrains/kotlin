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

package org.jetbrains.kotlin.cli.jvm.modules

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.resolve.jvm.KotlinCliJavaFileManager
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleFinder
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo

class CliJavaModuleFinder(
    jdkRootFile: VirtualFile?,
    jrtFileSystemRoot: VirtualFile?,
    private val javaFileManager: KotlinCliJavaFileManager,
    project: Project,
    private val releaseTarget: Int
) : JavaModuleFinder {
    private val modulesRoot = jrtFileSystemRoot?.findChild("modules")
    private val ctSymFile = jdkRootFile?.findChild("lib")?.findChild("ct.sym")
    private val userModules = linkedMapOf<String, JavaModule>()

    private val allScope = GlobalSearchScope.allScope(project)

    public val compilationJdkVersion by lazy {
        //TODO: add test with -jdk-home
        // Observe all JDK codes from folder name chars in ct.sym file,
        //  there should be maximal one corresponding to used compilation JDK
        ctSymRootFolder()?.children?.maxOf {
            if (it.name == "META-INF") -1
            else it.name.substringBeforeLast("-modules").maxOf { char ->
                char.toString().toIntOrNull(36) ?: -1
            }
        } ?: -1
    }

    private val isJDK12OrLater
        get() = compilationJdkVersion >= 12

    private val useLastJdkApi: Boolean
        get() = releaseTarget == 0 || releaseTarget == compilationJdkVersion

    fun addUserModule(module: JavaModule) {
        userModules.putIfAbsent(module.name, module)
    }

    val allObservableModules: Sequence<JavaModule>
        get() = systemModules + userModules.values

    val ctSymModules by lazy {
         collectModuleRoots(releaseTarget)
    }

    val systemModules: Sequence<JavaModule.Explicit>
        get() = if (useLastJdkApi) modulesRoot?.children.orEmpty().asSequence().mapNotNull(this::findSystemModule) else
            ctSymModules.values.asSequence().mapNotNull { findSystemModule(it, true) }

    override fun findModule(name: String): JavaModule? =
        when {
            useLastJdkApi -> modulesRoot?.findChild(name)?.let(this::findSystemModule)
            else -> ctSymModules[name]?.let { findSystemModule(it, true) }
        } ?: userModules[name]

    private fun findSystemModule(moduleRoot: VirtualFile, useSig: Boolean = false): JavaModule.Explicit? {
        val file = moduleRoot.findChild(if (useSig) PsiJavaModule.MODULE_INFO_CLASS + ".sig" else PsiJavaModule.MODULE_INFO_CLS_FILE)
            ?: return null
        val moduleInfo = JavaModuleInfo.read(file, javaFileManager, allScope) ?: return null
        return JavaModule.Explicit(
            moduleInfo,
            when {
                useLastJdkApi -> listOf(JavaModule.Root(moduleRoot, isBinary = true, isBinarySignature = useSig))
                //TODO: distinguish roots from different modules under JDK 10-11
                useSig -> listFoldersForRelease().map { JavaModule.Root(it, isBinary = true, isBinarySignature = true) }
                else -> error("Can't find ${moduleRoot.path} module")
            },
            file, true
        )
    }

    private fun codeFor(release: Int): String = release.toString(36).toUpperCase()

    private fun matchesRelease(fileName: String, release: Int) =
        !fileName.contains("-") && fileName.contains(codeFor(release)) // skip `*-modules`

    private fun hasCtSymFile() = ctSymFile != null && ctSymFile.isValid

    fun listFoldersForRelease(): List<VirtualFile> {
        if (!hasCtSymFile()) return emptyList()
        val root = ctSymRootFolder() ?: return emptyList()
        return root.children.filter { matchesRelease(it.name, releaseTarget) }.flatMap {
            if (isJDK12OrLater)
                it.children.toList()
            else {
                listOf(it)
            }
        }
    }

    private fun collectModuleRoots(release: Int): Map<String, VirtualFile> {
        if (!hasCtSymFile()) return emptyMap()
        val result = mutableMapOf<String, VirtualFile>()
        val root = ctSymRootFolder() ?: return emptyMap()


        if (isJDK12OrLater) {
            listFoldersForRelease().forEach { modulesRoot ->
                modulesRoot.findChild("module-info.sig")?.let {
                    result[modulesRoot.name] = modulesRoot
                }
            }
        } else {
            if (releaseTarget > 8) {
                val moduleSigs = root.findChild(codeFor(release) + if (!isJDK12OrLater) "-modules" else "")
                    ?: error("Can't find modules signatures in `ct.sym` file for `-release $release` in ${ctSymFile?.path}")
                moduleSigs.children.forEach {
                    result[it.name] = it
                }
            }
        }
        return result
    }

    private fun ctSymRootFolder() = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.JAR_PROTOCOL)
        ?.findFileByPath(ctSymFile!!.path + URLUtil.JAR_SEPARATOR)
}

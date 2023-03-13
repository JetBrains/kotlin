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

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import java.util.concurrent.ConcurrentHashMap

class CliJavaModuleResolver(
    private val moduleGraph: JavaModuleGraph,
    private val userModules: List<JavaModule>,
    private val systemModules: List<JavaModule.Explicit>,
    private val project: Project
) : JavaModuleResolver {
    init {
        assert(userModules.count(JavaModule::isSourceModule) <= 1) {
            "Modules computed by ClasspathRootsResolver cannot have more than one source module: $userModules"
        }
    }

    private val virtualFileFinder by lazy { VirtualFileFinder.getInstance(project) }

    override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? {
        val virtualFile = virtualFileFinder.findSourceOrBinaryVirtualFile(classId) ?: return null

        return (findJavaModule(virtualFile) as? JavaModule.Explicit)?.moduleInfo?.annotations
    }

    private val sourceModule: JavaModule? = userModules.firstOrNull(JavaModule::isSourceModule)

    private fun findJavaModule(file: VirtualFile): JavaModule? {
        if (file.fileSystem.protocol == StandardFileSystems.JRT_PROTOCOL || file.extension == "sig") {
            return systemModules.firstOrNull { module -> file in module }
        }

        return when (file.fileType) {
            KotlinFileType.INSTANCE, JavaFileType.INSTANCE -> sourceModule
            JavaClassFileType.INSTANCE -> userModules.firstOrNull { module -> file in module }
            else -> null
        }
    }

    private operator fun JavaModule.contains(file: VirtualFile): Boolean =
        moduleRoots.any { (root, isBinary) -> isBinary && VfsUtilCore.isAncestor(root, file, false) }

    override fun checkAccessibility(
        fileFromOurModule: VirtualFile?, referencedFile: VirtualFile, referencedPackage: FqName?
    ): JavaModuleResolver.AccessError? {
        val ourModule = fileFromOurModule?.let(this::findJavaModule)
        val theirModule = this.findJavaModule(referencedFile)

        if (ourModule?.name == theirModule?.name) return null

        if (theirModule == null) {
            return JavaModuleResolver.AccessError.ModuleDoesNotReadUnnamedModule
        }

        if (ourModule != null && !moduleGraph.reads(ourModule.name, theirModule.name)) {
            return JavaModuleResolver.AccessError.ModuleDoesNotReadModule(theirModule.name)
        }

        val fqName = referencedPackage ?: return null
        if (!theirModule.exports(fqName) && (ourModule == null || !theirModule.exportsTo(fqName, ourModule.name))) {
            return JavaModuleResolver.AccessError.ModuleDoesNotExportPackage(theirModule.name)
        }

        return null
    }

    companion object {
        private const val MODULE_ANNOTATIONS_CACHE_SIZE = 10000
    }
}

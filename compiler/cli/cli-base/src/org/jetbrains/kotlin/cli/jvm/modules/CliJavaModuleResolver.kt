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

class CliJavaModuleResolver(
    private val moduleGraph: JavaModuleGraph,
    private val userModules: List<JavaModule>,
    private val systemModules: List<JavaModule.Explicit>,
    private val project: Project,
    private val access: JavaModuleAccess = JavaModuleAccess.EMPTY,
) : JavaModuleResolver {

    init {
        assert(userModules.count(JavaModule::isSourceModule) <= 1) {
            "Modules computed by ClasspathRootsResolver cannot have more than one source module: $userModules"
        }
    }

    private val virtualFileFinder by lazy { VirtualFileFinder.getInstance(project, module = null) }

    override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? {
        val virtualFile = virtualFileFinder.findSourceOrBinaryVirtualFile(classId) ?: return null

        return (findJavaModule(virtualFile) as? JavaModule.Explicit)?.moduleInfo?.annotations
    }

    private val sourceModule: JavaModule? = userModules.firstOrNull(JavaModule::isSourceModule)

    // JDK modules with roots given with -Xpatch-module, which are the roots outside the runtime image and ct.sym.
    private val patchedSystemModules: List<JavaModule> = systemModules.filter { module ->
        module.moduleRoots.any { root -> !root.isBinarySignature && root.file.fileSystem.protocol != StandardFileSystems.JRT_PROTOCOL }
    }

    private fun findJavaModule(file: VirtualFile): JavaModule? {
        if (file.fileSystem.protocol == StandardFileSystems.JRT_PROTOCOL || file.extension == "sig") {
            return systemModules.firstOrNull { module -> file in module }
        }

        return when (file.fileType) {
            KotlinFileType.INSTANCE, JavaFileType.INSTANCE -> sourceModule
            JavaClassFileType.INSTANCE ->
                userModules.firstOrNull { module -> file in module } ?: patchedSystemModules.firstOrNull { module -> file in module }
            else -> null
        }
    }

    private operator fun JavaModule.contains(file: VirtualFile): Boolean =
        moduleRoots.any { (val root = file, val isBinary) -> isBinary && VfsUtilCore.isAncestor(root, file, false) }

    override fun checkAccessibility(
        fileFromOurModule: VirtualFile?, referencedFile: VirtualFile, referencedPackage: FqName?
    ): JavaModuleResolver.AccessError? {
        val ourModule = fileFromOurModule?.let(this::findJavaModule)
        val theirModule = this.findJavaModule(referencedFile)

        if (ourModule?.name == theirModule?.name) return null

        if (theirModule == null) {
            // An automatic module reads the unnamed module, which matters for an automatic module patched with sources.
            if (ourModule is JavaModule.Automatic) return null
            if (ourModule != null && moduleGraph.readsAdditionally(access, ourModule.name, ALL_UNNAMED)) return null
            return JavaModuleResolver.AccessError.ModuleDoesNotReadUnnamedModule
        }

        // For javac, a module read from the classpath is in the unnamed module, which exports all its packages.
        if (ourModule != null && theirModule.name in access.modulesFromClasspath &&
            moduleGraph.readsAdditionally(access, ourModule.name, ALL_UNNAMED)
        ) {
            return null
        }

        if (ourModule != null && !moduleGraph.reads(ourModule.name, theirModule.name) &&
            !moduleGraph.readsAdditionally(access, ourModule.name, theirModule.name)
        ) {
            return JavaModuleResolver.AccessError.ModuleDoesNotReadModule(theirModule.name)
        }

        val fqName = referencedPackage ?: return null
        // Without a file from our module, it's unknown whether our module is named, so ALL-UNNAMED is not applied.
        val ourModuleName = ourModule?.name ?: ALL_UNNAMED.takeIf { fileFromOurModule != null }
        if (!theirModule.exports(fqName) && (ourModule == null || !theirModule.exportsTo(fqName, ourModule.name)) &&
            (ourModuleName == null || ourModuleName !in access.additionalExports["${theirModule.name}/${fqName.asString()}"].orEmpty())
        ) {
            return JavaModuleResolver.AccessError.ModuleDoesNotExportPackage(theirModule.name)
        }

        return null
    }
}

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
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver


/**
 * @param findSourceOrBinaryClassFile Finds the `.class`/`.sig` or `.java` file declaring a class anywhere on the classpath, to tell
 *  which module owns the class (see `CliVirtualFileFinder.findSourceOrBinaryClassFile`).
 */
class CliJavaModuleResolver(
    private val moduleGraph: JavaModuleGraph,
    private val userModules: List<JavaModule>,
    private val systemModules: List<JavaModule.Explicit>,
    private val findSourceOrBinaryClassFile: (ClassId) -> BinaryClassFileHandle?,
) : JavaModuleResolver {
    init {
        assert(userModules.count(JavaModule::isSourceModule) <= 1) {
            "Modules computed by ClasspathRootsResolver cannot have more than one source module: $userModules"
        }
    }

    override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? {
        val classFile = findSourceOrBinaryClassFile(classId) ?: return null
        // Every Java source root being compiled belongs to the source module, whichever of them holds `module-info.java`.
        val module = if (classFile.extension == JavaFileType.DEFAULT_EXTENSION) sourceModule else findJavaModule(classFile.rootId)

        return (module as? JavaModule.Explicit)?.moduleInfo?.annotations
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
        moduleRoots.any { (val root = file, val isBinary) -> isBinary && VfsUtilCore.isAncestor(root, file, false) }

    /** The module owning the binary classpath root [rootId]; a system module and a user module never share a root. */
    private fun findJavaModule(rootId: JvmClasspathRootId): JavaModule? =
        systemModules.firstOrNull { module -> rootId in module } ?: userModules.firstOrNull { module -> rootId in module }

    private operator fun JavaModule.contains(rootId: JvmClasspathRootId): Boolean =
        moduleRoots.any { root -> root.isBinary && root.id == rootId }

    override fun checkAccessibility(
        fileFromOurModule: VirtualFile?, referencedFile: VirtualFile, referencedPackage: FqName?
    ): JavaModuleResolver.AccessError? =
        checkAccessibility(fileFromOurModule?.let(this::findJavaModule), findJavaModule(referencedFile), referencedPackage)

    override fun checkAccessibility(
        fileFromOurModule: VirtualFile?, referencedClassFile: BinaryClassFileHandle, referencedPackage: FqName?,
    ): JavaModuleResolver.AccessError? =
        checkAccessibility(fileFromOurModule?.let(this::findJavaModule), findJavaModule(referencedClassFile.rootId), referencedPackage)

    private fun checkAccessibility(
        ourModule: JavaModule?, theirModule: JavaModule?, referencedPackage: FqName?,
    ): JavaModuleResolver.AccessError? {
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
}

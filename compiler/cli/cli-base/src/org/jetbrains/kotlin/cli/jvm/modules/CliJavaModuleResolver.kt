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

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinder
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver


/**
 * @param classpathFinder Finds the file declaring a class anywhere on the classpath, to tell which module owns the class.
 */
class CliJavaModuleResolver(
    moduleGraph: JavaModuleGraph,
    private val userModules: List<JavaModule>,
    systemModules: List<JavaModule.Explicit>,
    private val classpathFinder: CliVirtualFileFinder,
) : AbstractJavaModuleResolver(moduleGraph, systemModules) {
    init {
        assert(userModules.count(JavaModule::isSourceModule) <= 1) {
            "Modules computed by ClasspathRootsResolver cannot have more than one source module: $userModules"
        }
        // `findJavaModule(rootId)` takes the first module owning a root, which is only correct if the modules sharing a root have
        // the same name: one module which is both a system and a user module.
        val systemModuleNames = systemModules.flatMap { module -> module.moduleRoots.map { root -> root.id to module.name } }.toMap()
        for (module in userModules) {
            for (root in module.moduleRoots) {
                val systemModuleName = systemModuleNames[root.id] ?: continue
                check(systemModuleName == module.name) {
                    "The classpath root ${root.file} belongs to the system module $systemModuleName and the user module ${module.name}"
                }
            }
        }
    }

    override fun getAnnotationsForModuleOwnerOfClass(classId: ClassId): List<JavaAnnotation>? {
        val classFile = classpathFinder.findSourceOrBinaryClassFile(classId) ?: return null
        // Every Java source root being compiled belongs to the source module, whichever of them holds `module-info.java`.
        val module = if (classFile.extension == JavaFileType.DEFAULT_EXTENSION) sourceModule else findJavaModule(classFile.rootId)

        return (module as? JavaModule.Explicit)?.moduleInfo?.annotations
    }

    private val sourceModule: JavaModule? = userModules.firstOrNull(JavaModule::isSourceModule)

    override fun findSourceModule(file: VirtualFile): JavaModule? = sourceModule

    override fun findBinaryModule(file: VirtualFile): JavaModule? = userModules.firstOrNull { module -> file in module }

    /** The module owning the binary classpath root [rootId], looked up in the user modules first. */
    private fun findJavaModule(rootId: JvmClasspathRootId): JavaModule? =
        userModules.firstOrNull { module -> rootId in module } ?: findSystemModule(rootId)

    private operator fun JavaModule.contains(rootId: JvmClasspathRootId): Boolean =
        moduleRoots.any { root -> root.isBinary && root.id == rootId }

    override fun checkAccessibility(
        fileFromOurModule: VirtualFile?, referencedClassFile: BinaryClassFileHandle, referencedPackage: FqName?,
    ): JavaModuleResolver.AccessError? =
        checkAccessibility(fileFromOurModule?.let(this::findJavaModule), findJavaModule(referencedClassFile.rootId), referencedPackage)
}

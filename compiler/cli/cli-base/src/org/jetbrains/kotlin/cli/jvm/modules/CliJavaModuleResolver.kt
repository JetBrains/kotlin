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
import javax.lang.model.SourceVersion


internal const val ALL_UNNAMED = "ALL-UNNAMED"

/**
 * A value of `-Xadd-reads` (`<module>=<other-module>(,<other-module>)*`) or `-Xadd-exports`
 * (`<module>/<package>=<other-module>(,<other-module>)*`). Names that are not valid Java names are in [badNames] and left out of
 * [otherModules]. If the left-hand side contains such a name, [source] is `null` and [otherModules] is empty.
 */
class JavaModuleAccessOption(val source: String?, val otherModules: List<String>, val badNames: List<String>)

private val ADD_READS_VALUE = Regex("([^=]+)=(,*[^,].*)")
private val ADD_EXPORTS_VALUE = Regex("([^/]+)/([^=]+)=(,*[^,].*)")
private val TARGET_SEPARATORS = Regex("[ ,]+")

/**
 * Parses a value of `-Xadd-reads` or `-Xadd-exports` the way javac parses `--add-reads` and `--add-exports`, or returns `null` if the
 * value is malformed.
 */
fun parseJavaModuleAccessOption(value: String, isExports: Boolean): JavaModuleAccessOption? {
    val groups = (if (isExports) ADD_EXPORTS_VALUE else ADD_READS_VALUE).matchEntire(value)?.groupValues?.drop(1) ?: return null
    val sourceParts = groups.dropLast(1)
    val targets = groups.last()
    val badSourceParts = sourceParts.filterNot(SourceVersion::isName)
    // Like javac, names are checked split on commas, but applied split on spaces and commas.
    val badNames = badSourceParts + targets.split(',').filterNot { it.isEmpty() || isValidTarget(it) }
    if (badSourceParts.isNotEmpty()) return JavaModuleAccessOption(null, emptyList(), badNames)
    return JavaModuleAccessOption(sourceParts.joinToString("/"), targets.split(TARGET_SEPARATORS).filter(::isValidTarget), badNames)
}

private fun isValidTarget(name: String): Boolean = name == ALL_UNNAMED || SourceVersion.isName(name)

class CliJavaModuleResolver(
    private val moduleGraph: JavaModuleGraph,
    private val userModules: List<JavaModule>,
    private val systemModules: List<JavaModule.Explicit>,
    private val project: Project,
    // Values of -Xadd-reads and -Xadd-exports, see ClasspathRootsResolver.additionalJavaModuleReads.
    private val additionalReads: Map<String, Set<String>> = emptyMap(),
    private val additionalExports: Map<String, Set<String>> = emptyMap(),
    // See ClasspathRootsResolver.javaModulesFromClasspath.
    private val javaModulesFromClasspath: Set<String> = emptySet(),
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

    override fun checkAccessibility(
        fileFromOurModule: VirtualFile?, referencedFile: VirtualFile, referencedPackage: FqName?
    ): JavaModuleResolver.AccessError? {
        val ourModule = fileFromOurModule?.let(this::findJavaModule)
        val theirModule = this.findJavaModule(referencedFile)

        if (ourModule?.name == theirModule?.name) return null

        if (theirModule == null) {
            if (ourModule != null && moduleGraph.readsAdditionally(additionalReads, ourModule.name, ALL_UNNAMED)) return null
            return JavaModuleResolver.AccessError.ModuleDoesNotReadUnnamedModule
        }

        // For javac, a module read from the classpath is in the unnamed module, which exports all its packages.
        if (ourModule != null && theirModule.name in javaModulesFromClasspath &&
            moduleGraph.readsAdditionally(additionalReads, ourModule.name, ALL_UNNAMED)
        ) {
            return null
        }

        if (ourModule != null && !moduleGraph.reads(ourModule.name, theirModule.name) &&
            !moduleGraph.readsAdditionally(additionalReads, ourModule.name, theirModule.name, javaModulesFromClasspath)
        ) {
            return JavaModuleResolver.AccessError.ModuleDoesNotReadModule(theirModule.name)
        }

        val fqName = referencedPackage ?: return null
        // Without a file from our module, it's unknown whether our module is named, so ALL-UNNAMED is not applied.
        val ourModuleName = ourModule?.name ?: ALL_UNNAMED.takeIf { fileFromOurModule != null }
        if (!theirModule.exports(fqName) && (ourModule == null || !theirModule.exportsTo(fqName, ourModule.name)) &&
            (ourModuleName == null || ourModuleName !in additionalExports["${theirModule.name}/${fqName.asString()}"].orEmpty())
        ) {
            return JavaModuleResolver.AccessError.ModuleDoesNotExportPackage(theirModule.name)
        }

        return null
    }
}

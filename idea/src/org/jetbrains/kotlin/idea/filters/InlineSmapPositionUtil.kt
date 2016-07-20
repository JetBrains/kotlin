/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.filters

import com.intellij.openapi.compiler.CompilerPaths
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.codegen.inline.FileMapping
import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil
import org.jetbrains.kotlin.codegen.inline.SMAP
import org.jetbrains.kotlin.codegen.inline.SMAPParser
import org.jetbrains.kotlin.idea.refactoring.getLineCount
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.idea.util.DebuggerUtils
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.tail
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import java.io.File

fun isInlineFunctionLineNumber(file: VirtualFile, lineNumber: Int, project: Project): Boolean {
    val linesInFile = file.toPsiFile(project)?.getLineCount() ?: return false
    return lineNumber > linesInFile
}

fun readClassFile(jvmName: JvmClassName, file: VirtualFile, project: Project, sourceCondition: (VirtualFile) -> Boolean = { true }): ByteArray? {
    val fqNameWithInners = jvmName.fqNameForClassNameWithoutDollars.tail(jvmName.packageFqName)

    when {
        ProjectRootsUtil.isLibrarySourceFile(project, file) -> {
            val classId = ClassId(jvmName.packageFqName, Name.identifier(fqNameWithInners.asString()))

            val fileFinder = JvmVirtualFileFinder.SERVICE.getInstance(project)
            val classFile = fileFinder.findVirtualFileWithHeader(classId) ?: return null
            return classFile.contentsToByteArray()
        }

        ProjectRootsUtil.isProjectSourceFile(project, file) && sourceCondition(file) -> {
            val module = ProjectFileIndex.SERVICE.getInstance(project).getModuleForFile(file)
            val outputDir = CompilerPaths.getModuleOutputDirectory(module, /*forTests = */ false) ?: return null

            val className = fqNameWithInners.asString().replace('.', '$')
            val classByByDirectory = findClassFileByPath(jvmName.packageFqName.asString(), className, outputDir) ?: return null

            return classByByDirectory.readBytes()
        }

        else -> return null
    }
}

private fun findClassFileByPath(packageName: String, className: String, outputDir: VirtualFile): File? {
    val outDirFile = File(outputDir.path).check { it.exists() } ?: return null

    val parentDirectory = File(outDirFile, packageName.replace(".", File.separator))
    if (!parentDirectory.exists()) return null

    val classFile = File(parentDirectory, className + ".class")
    if (classFile.exists()) {
        return classFile
    }

    return null
}

fun parseStrata(strata: String?, line: Int, project: Project, isKotlin2: Boolean, searchScope: GlobalSearchScope): Pair<KtFile, Int>? {
    if (strata == null) return null

    val smap = SMAPParser.parse(strata)

    val mappingInfo = smap.fileMappings.firstOrNull {
        it.getIntervalIfContains(line) != null
    } ?: return null

    val newJvmName = JvmClassName.byInternalName(mappingInfo.path)
    val newSourceFile = DebuggerUtils.findSourceFileForClassIncludeLibrarySources(project, searchScope, newJvmName, mappingInfo.name) ?: return null

    val interval = mappingInfo.getIntervalIfContains(line)!!
    return newSourceFile to (if (isKotlin2) interval.source else interval.mapDestToSource(line)) - 1
}

fun readDebugInfo(bytes: ByteArray): SmapData? {
    val cr = ClassReader(bytes)
    var debugInfo: String? = null
    cr.accept(object : ClassVisitor(InlineCodegenUtil.API) {
        override fun visitSource(source: String?, debug: String?) {
            debugInfo = debug
        }
    }, ClassReader.SKIP_FRAMES and ClassReader.SKIP_CODE)
    return debugInfo?.let { SmapData(it) }
}

class SmapData(debugInfo: String) {
    var kotlin1: String? = null
        private set
    var kotlin2: String? = null
        private set

    init {
        val intervals = debugInfo.split(SMAP.END).filter { it.isNotBlank() }
        when(intervals.count()) {
            1 -> {
                kotlin1 = intervals[0] + SMAP.END
            }
            else -> {
                kotlin1 = intervals[0] + SMAP.END
                kotlin2 = intervals[1] + SMAP.END
            }
        }
    }
}

private fun FileMapping.getIntervalIfContains(destLine: Int) = lineMappings.firstOrNull { it.contains(destLine) }

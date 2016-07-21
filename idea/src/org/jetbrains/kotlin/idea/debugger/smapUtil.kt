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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.openapi.application.ApplicationManager
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
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.tail
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import java.io.File

fun inlineLineAndFileByPosition(lineNumber: Int, fqName: FqName, fileName: String, project: Project, searchScope: GlobalSearchScope): Pair<KtFile, Int>? {
    val internalName = fqName.asString().replace('.', '/')
    val jvmClassName = JvmClassName.byInternalName(internalName)

    val file = DebuggerUtils.findSourceFileForClassIncludeLibrarySources(project, searchScope, jvmClassName, fileName) ?: return null

    val virtualFile = file.virtualFile ?: return null

    val bytes = readClassFile(project, jvmClassName, virtualFile, { isInlineFunctionLineNumber(it, lineNumber, project) }) ?: return null
    val smapData = readDebugInfo(bytes) ?: return null
    return mapStacktraceLineToSource(smapData, lineNumber, project, SourceLineKind.EXECUTED_LINE, searchScope)
}

internal fun inlinedLinesNumbers(
        inlineLineNumber: Int, inlineFileName: String,
        destinationTypeFqName: FqName, destinationFileName: String,
        project: Project, sourceSearchScope: GlobalSearchScope): List<Int> {
    val internalName = destinationTypeFqName.asString().replace('.', '/')
    val jvmClassName = JvmClassName.byInternalName(internalName)

    val file = DebuggerUtils.findSourceFileForClassIncludeLibrarySources(project, sourceSearchScope, jvmClassName, destinationFileName) ?:
               return listOf()

    val virtualFile = file.virtualFile ?: return listOf()

    val bytes = readClassFile(project, jvmClassName, virtualFile) ?: return listOf()
    val smapData = readDebugInfo(bytes) ?: return listOf()

    val smap = smapData.kotlinStrata ?: return listOf()

    val mappingToInlinedFile = smap.fileMappings.firstOrNull() { it.name == inlineFileName } ?: return listOf()
    return mappingToInlinedFile.lineMappings.map { it.mapSourceToDest(inlineLineNumber) }
}

fun isInlineFunctionLineNumber(file: VirtualFile, lineNumber: Int, project: Project): Boolean {
    val linesInFile = file.toPsiFile(project)?.getLineCount() ?: return false
    return lineNumber > linesInFile
}

fun readClassFile(project: Project,
                  jvmName: JvmClassName,
                  file: VirtualFile,
                  sourceFileFilter: (VirtualFile) -> Boolean = { true },
                  libFileFilter: (VirtualFile) -> Boolean = { true }): ByteArray? {
    val fqNameWithInners = jvmName.fqNameForClassNameWithoutDollars.tail(jvmName.packageFqName)

    when {
        ProjectRootsUtil.isLibrarySourceFile(project, file) && libFileFilter(file) -> {
            val classId = ClassId(jvmName.packageFqName, Name.identifier(fqNameWithInners.asString()))

            val fileFinder = JvmVirtualFileFinder.SERVICE.getInstance(project)
            val classFile = fileFinder.findVirtualFileWithHeader(classId) ?: return null
            return classFile.contentsToByteArray()
        }

        ProjectRootsUtil.isProjectSourceFile(project, file) && sourceFileFilter(file) -> {
            val module = ProjectFileIndex.SERVICE.getInstance(project).getModuleForFile(file)
            val outputDir = CompilerPaths.getModuleOutputDirectory(module, /*forTests = */ false) ?: return null

            val className = fqNameWithInners.asString().replace('.', '$')
            val classByDirectory = findClassFileByPath(jvmName.packageFqName.asString(), className, outputDir) ?: return null

            return classByDirectory.readBytes()
        }

        else -> return null
    }
}

private fun findClassFileByPath(packageName: String, className: String, outputDir: VirtualFile): File? {
    val outDirFile = File(outputDir.path).check(File::exists) ?: return null

    val parentDirectory = File(outDirFile, packageName.replace(".", File.separator))
    if (!parentDirectory.exists()) return null

    if (ApplicationManager.getApplication().isUnitTestMode) {
        val beforeDexFileClassFile = File(parentDirectory, className + ".class.before_dex")
        if (beforeDexFileClassFile.exists()) {
            return beforeDexFileClassFile
        }
    }

    val classFile = File(parentDirectory, className + ".class")
    if (classFile.exists()) {
        return classFile
    }

    return null
}

enum class SourceLineKind {
    CALL_LINE,
    EXECUTED_LINE
}

fun mapStacktraceLineToSource(smapData: SmapData,
                              line: Int,
                              project: Project,
                              lineKind: SourceLineKind,
                              searchScope: GlobalSearchScope): Pair<KtFile, Int>? {
    val smap = when (lineKind) {
        SourceLineKind.CALL_LINE -> smapData.kotlinDebugStrata
        SourceLineKind.EXECUTED_LINE -> smapData.kotlinStrata
    } ?: return null

    val mappingInfo = smap.fileMappings.firstOrNull {
        it.getIntervalIfContains(line) != null
    } ?: return null

    val jvmName = JvmClassName.byInternalName(mappingInfo.path)
    val sourceFile = DebuggerUtils.findSourceFileForClassIncludeLibrarySources(
            project, searchScope, jvmName, mappingInfo.name) ?: return null

    val interval = mappingInfo.getIntervalIfContains(line)!!
    val sourceLine = when (lineKind) {
        SourceLineKind.CALL_LINE -> interval.source - 1
        SourceLineKind.EXECUTED_LINE -> interval.mapDestToSource(line) - 1
    }

    return sourceFile to sourceLine
}

fun readDebugInfo(bytes: ByteArray): SmapData? {
    val cr = ClassReader(bytes)
    var debugInfo: String? = null
    cr.accept(object : ClassVisitor(InlineCodegenUtil.API) {
        override fun visitSource(source: String?, debug: String?) {
            debugInfo = debug
        }
    }, ClassReader.SKIP_FRAMES and ClassReader.SKIP_CODE)
    return debugInfo?.let(::SmapData)
}

class SmapData(debugInfo: String) {
    var kotlinStrata: SMAP?
    var kotlinDebugStrata: SMAP?

    init {
        val intervals = debugInfo.split(SMAP.END).filter(String::isNotBlank)
        when(intervals.count()) {
            1 -> {
                kotlinStrata = SMAPParser.parse(intervals[0] + SMAP.END)
                kotlinDebugStrata = null
            }
            2 -> {
                kotlinStrata = SMAPParser.parse(intervals[0] + SMAP.END)
                kotlinDebugStrata = SMAPParser.parse(intervals[1] + SMAP.END)
            }
            else -> {
                kotlinStrata = null
                kotlinDebugStrata = null
            }
        }
    }
}

private fun FileMapping.getIntervalIfContains(destLine: Int) = lineMappings.firstOrNull { it.contains(destLine) }

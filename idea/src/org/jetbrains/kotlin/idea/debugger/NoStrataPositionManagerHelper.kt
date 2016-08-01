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

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.jdi.VirtualMachineProxyImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import org.jetbrains.kotlin.idea.refactoring.getLineStartOffset
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

internal fun getOriginalPositionOfInlinedLine(
        lineNumber: Int, fqName: FqName, fileName: String, project: Project, searchScope: GlobalSearchScope): Pair<KtFile, Int>? {
    val internalName = fqName.asString().replace('.', '/')
    val jvmClassName = JvmClassName.byInternalName(internalName)

    val file = DebuggerUtils.findSourceFileForClassIncludeLibrarySources(project, searchScope, jvmClassName, fileName) ?: return null

    val virtualFile = file.virtualFile ?: return null

    val bytes = readClassFile(project, jvmClassName, virtualFile, { isInlineFunctionLineNumber(it, lineNumber, project) }) ?: return null
    val smapData = readDebugInfo(bytes) ?: return null
    return mapStacktraceLineToSource(smapData, lineNumber, project, SourceLineKind.EXECUTED_LINE, searchScope)
}

internal fun getLocationsOfInlinedLine(type: ReferenceType, position: SourcePosition, sourceSearchScope: GlobalSearchScope): List<Location> {
    val line = position.line
    val file = position.file
    val project = position.file.project

    val lineStartOffset = file.getLineStartOffset(line) ?: return listOf()
    val element = file.findElementAt(lineStartOffset) ?: return listOf()

    val isInInline = runReadAction { element.parents.any { it is KtFunction && it.hasModifier(KtTokens.INLINE_KEYWORD) } }
    if (!isInInline) return listOf()

    val lines = inlinedLinesNumbers(line + 1, position.file.name, FqName(type.name()), type.sourceName(), project, sourceSearchScope)
    val inlineLocations = lines.flatMap { type.locationsOfLine(it) }

    return inlineLocations
}

private fun inlinedLinesNumbers(
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

@Volatile var emulateDexDebugInTests: Boolean = false

fun DebugProcess.isDexDebug() =
        (emulateDexDebugInTests && ApplicationManager.getApplication ().isUnitTestMode) ||
        (this.virtualMachineProxy as? VirtualMachineProxyImpl)?.virtualMachine?.name() == "Dalvik" // TODO: check other machine names
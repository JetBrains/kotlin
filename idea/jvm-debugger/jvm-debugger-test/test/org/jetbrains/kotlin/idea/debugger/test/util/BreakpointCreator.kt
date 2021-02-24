/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.util

import org.jetbrains.kotlin.test.InTextDirectivesUtils.findLinesWithPrefixesRemoved
import com.intellij.debugger.DebuggerInvocationUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.xdebugger.XDebuggerManager
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.jetbrains.kotlin.idea.util.application.runReadAction
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.ui.breakpoints.Breakpoint
import com.intellij.debugger.ui.breakpoints.BreakpointManager
import com.intellij.debugger.ui.breakpoints.LineBreakpoint
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.XBreakpointManager
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XBreakpointType
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import org.jetbrains.java.debugger.breakpoints.properties.JavaBreakpointProperties
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties
import org.jetbrains.kotlin.idea.debugger.breakpoints.*
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferenceKeys
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferences
import javax.swing.SwingUtilities

internal class BreakpointCreator(
    private val project: Project,
    private val logger: (String) -> Unit,
    private val preferences: DebuggerPreferences
) {
    fun createBreakpoints(file: PsiFile) {
        val document = runReadAction { PsiDocumentManager.getInstance(project).getDocument(file) } ?: return
        val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
        val kotlinFieldBreakpointType = findBreakpointType(KotlinFieldBreakpointType::class.java)
        val virtualFile = file.virtualFile

        val runnable = {
            var offset = -1
            while (true) {
                val fileText = document.text
                offset = fileText.indexOf("point!", offset + 1)
                if (offset == -1) break

                val commentLine = document.getLineNumber(offset)
                val lineIndex = commentLine + 1

                val comment = fileText
                    .substring(document.getLineStartOffset(commentLine), document.getLineEndOffset(commentLine))
                    .trim()

                when {
                    @Suppress("SpellCheckingInspection") comment.startsWith("//FieldWatchpoint!") -> {
                        val javaBreakpoint = createBreakpointOfType(
                            breakpointManager,
                            kotlinFieldBreakpointType,
                            lineIndex,
                            virtualFile
                        )

                        (javaBreakpoint as? KotlinFieldBreakpoint)?.apply {
                            @Suppress("SpellCheckingInspection")
                            val fieldName = comment.substringAfter("//FieldWatchpoint! (").substringBefore(")")

                            setFieldName(fieldName)
                            setWatchAccess(preferences[DebuggerPreferenceKeys.WATCH_FIELD_ACCESS])
                            setWatchModification(preferences[DebuggerPreferenceKeys.WATCH_FIELD_MODIFICATION])
                            setWatchInitialization(preferences[DebuggerPreferenceKeys.WATCH_FIELD_INITIALISATION])

                            BreakpointManager.addBreakpoint(javaBreakpoint)
                            logger("KotlinFieldBreakpoint created at ${file.virtualFile.name}:${lineIndex + 1}")
                        }
                    }
                    comment.startsWith("//Breakpoint!") -> {
                        val ordinal = getPropertyFromComment(comment, "lambdaOrdinal")?.toInt()
                        val condition = getPropertyFromComment(comment, "condition")
                        createLineBreakpoint(breakpointManager, file, lineIndex, ordinal, condition)
                    }
                    comment.startsWith("//FunctionBreakpoint!") -> {
                        createFunctionBreakpoint(breakpointManager, file, lineIndex, false)
                    }
                    else -> throw AssertionError("Cannot create breakpoint at line ${lineIndex + 1}")
                }
            }
        }

        if (!SwingUtilities.isEventDispatchThread()) {
            DebuggerInvocationUtil.invokeAndWait(project, runnable, ModalityState.defaultModalityState())
        } else {
            runnable.invoke()
        }
    }

    fun createAdditionalBreakpoints(fileContents: String) {
        val breakpoints = findLinesWithPrefixesRemoved(fileContents, "// ADDITIONAL_BREAKPOINT: ")
        for (breakpoint in breakpoints) {
            val chunks = breakpoint.split('/').map { it.trim() }
            val fileName = chunks.getOrNull(0) ?: continue
            val lineMarker = chunks.getOrNull(1) ?: continue
            val kind = chunks.getOrElse(2) { "line" }
            val ordinal = chunks.getOrNull(3)?.toInt()

            val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager

            when (kind) {
                "line" -> createBreakpoint(fileName, lineMarker) { psiFile, lineNumber ->
                    createLineBreakpoint(breakpointManager, psiFile, lineNumber + 1, ordinal, null)
                }
                "fun" -> createBreakpoint(fileName, lineMarker) { psiFile, lineNumber ->
                    createFunctionBreakpoint(breakpointManager, psiFile, lineNumber, true)
                }
                else -> error("Unknown breakpoint kind: $kind")
            }
        }
    }

    private fun getPropertyFromComment(comment: String, propertyName: String): String? {
        if (comment.contains("$propertyName = ")) {
            val result = comment.substringAfter("$propertyName = ")
            if (result.contains(", ")) {
                return result.substringBefore(", ")
            }
            if (result.contains(")")) {
                return result.substringBefore(")")
            }
        }
        return null
    }

    private fun createBreakpoint(fileName: String, lineMarker: String, action: (PsiFile, Int) -> Unit) {
        val sourceFiles = runReadAction {
            FilenameIndex.getAllFilesByExt(project, "kt")
                .filter { it.name == fileName && it.contentsToByteArray().toString(Charsets.UTF_8).contains(lineMarker) }
        }

        val sourceFile = sourceFiles.singleOrNull()
            ?: error("Single source file should be found: name = $fileName, sourceFiles = $sourceFiles")

        val runnable = Runnable {
            val psiSourceFile = PsiManager.getInstance(project).findFile(sourceFile)
                ?: error("Psi file not found for $sourceFile")

            val document = PsiDocumentManager.getInstance(project).getDocument(psiSourceFile)!!

            val index = psiSourceFile.text!!.indexOf(lineMarker)
            val lineNumber = document.getLineNumber(index)
            action(psiSourceFile, lineNumber)
        }

        DebuggerInvocationUtil.invokeAndWait(project, runnable, ModalityState.defaultModalityState())
    }

    private fun createFunctionBreakpoint(breakpointManager: XBreakpointManager, file: PsiFile, lineIndex: Int, fromLibrary: Boolean) {
        val breakpointType = findBreakpointType(KotlinFunctionBreakpointType::class.java)
        val breakpoint = createBreakpointOfType(breakpointManager, breakpointType, lineIndex, file.virtualFile)
        if (breakpoint is KotlinFunctionBreakpoint) {
            val lineNumberSuffix = if (fromLibrary) "" else ":${lineIndex + 1}"
            logger("FunctionBreakpoint created at ${file.virtualFile.name}$lineNumberSuffix")
        }
    }

    private fun createLineBreakpoint(
        breakpointManager: XBreakpointManager,
        file: PsiFile,
        lineIndex: Int,
        lambdaOrdinal: Int?,
        condition: String?
    ) {
        val kotlinLineBreakpointType = findBreakpointType(KotlinLineBreakpointType::class.java)
        val javaBreakpoint = createBreakpointOfType(
            breakpointManager,
            kotlinLineBreakpointType,
            lineIndex,
            file.virtualFile
        )

        if (javaBreakpoint is LineBreakpoint<*>) {
            val properties = javaBreakpoint.xBreakpoint.properties as? JavaLineBreakpointProperties ?: return
            var suffix = ""
            if (lambdaOrdinal != null) {
                if (lambdaOrdinal != -1) {
                    properties.lambdaOrdinal = lambdaOrdinal - 1
                } else {
                    properties.lambdaOrdinal = lambdaOrdinal
                }
                suffix += " lambdaOrdinal = $lambdaOrdinal"
            }
            if (condition != null) {
                javaBreakpoint.setCondition(TextWithImportsImpl(CodeFragmentKind.EXPRESSION, condition))
                suffix += " condition = $condition"
            }

            BreakpointManager.addBreakpoint(javaBreakpoint)
            logger("LineBreakpoint created at ${file.virtualFile.name}:${lineIndex + 1}$suffix")
        }
    }

    private fun createBreakpointOfType(
        breakpointManager: XBreakpointManager,
        breakpointType: XLineBreakpointType<XBreakpointProperties<*>>,
        lineIndex: Int,
        virtualFile: VirtualFile
    ): Breakpoint<out JavaBreakpointProperties<*>>? {
        if (!breakpointType.canPutAt(virtualFile, lineIndex, project)) return null
        val xBreakpoint = runWriteAction {
            breakpointManager.addLineBreakpoint(
                breakpointType,
                virtualFile.url,
                lineIndex,
                breakpointType.createBreakpointProperties(virtualFile, lineIndex)
            )
        }
        return BreakpointManager.getJavaBreakpoint(xBreakpoint)
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : XBreakpointType<*, *>> findBreakpointType(javaClass: Class<T>): XLineBreakpointType<XBreakpointProperties<*>> {
        return XDebuggerUtil.getInstance().findBreakpointType(javaClass) as XLineBreakpointType<XBreakpointProperties<*>>
    }
}
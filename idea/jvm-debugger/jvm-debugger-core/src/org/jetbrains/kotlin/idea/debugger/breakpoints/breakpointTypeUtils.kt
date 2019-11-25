/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.debugger.breakpoints

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.impl.XSourcePositionImpl
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.util.attachmentByPsiFile
import org.jetbrains.kotlin.idea.core.util.getLineNumber
import org.jetbrains.kotlin.idea.debugger.findElementAtLine
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.inline.INLINE_ONLY_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import java.util.*

interface KotlinBreakpointType

private val LOG = Logger.getInstance("BreakpointTypeUtilsKt")

class ApplicabilityResult(val isApplicable: Boolean, val shouldStop: Boolean) {
    companion object {
        @JvmStatic
        fun definitely(result: Boolean) = ApplicabilityResult(result, shouldStop = true)

        @JvmStatic
        fun maybe(result: Boolean) = ApplicabilityResult(result, shouldStop = false)

        @JvmField
        val UNKNOWN = ApplicabilityResult(isApplicable = false, shouldStop = false)

        @JvmField
        val DEFINITELY_YES = ApplicabilityResult(isApplicable = true, shouldStop = true)

        @JvmField
        val DEFINITELY_NO = ApplicabilityResult(isApplicable = false, shouldStop = true)

        @JvmField
        val MAYBE_YES = ApplicabilityResult(isApplicable = true, shouldStop = false)
    }
}

fun isBreakpointApplicable(file: VirtualFile, line: Int, project: Project, checker: (PsiElement) -> ApplicabilityResult): Boolean {
    val psiFile = PsiManager.getInstance(project).findFile(file)

    if (psiFile == null || psiFile.virtualFile?.fileType != KotlinFileType.INSTANCE) {
        return false
    }

    val document = FileDocumentManager.getInstance().getDocument(file) ?: return false

    return runReadAction {
        var isApplicable = false
        val checked = HashSet<PsiElement>()

        XDebuggerUtil.getInstance().iterateLine(project, document, line, fun(element: PsiElement): Boolean {
            if (element is PsiWhiteSpace || element.getParentOfType<PsiComment>(false) != null || !element.isValid) {
                return true
            }

            val parent = getTopmostParentOnLineOrSelf(element, document, line)
            if (!checked.add(parent)) {
                return true
            }

            val result = checker(parent)

            if (result.shouldStop && !result.isApplicable) {
                isApplicable = false
                return false
            }

            isApplicable = isApplicable or result.isApplicable
            return !result.shouldStop
        })

        return@runReadAction isApplicable
    }
}

private fun getTopmostParentOnLineOrSelf(element: PsiElement, document: Document, line: Int): PsiElement {
    var current = element
    var parent = current.parent
    while (parent != null && parent !is PsiFile) {
        val offset = parent.textOffset
        if (offset > document.textLength) break
        if (offset >= 0 && document.getLineNumber(offset) != line) break

        current = parent
        parent = current.parent
    }

    return current
}

fun computeLineBreakpointVariants(
    project: Project,
    position: XSourcePosition,
    kotlinBreakpointType: KotlinLineBreakpointType
): List<JavaLineBreakpointType.JavaBreakpointVariant> {
    val file = PsiManager.getInstance(project).findFile(position.file) as? KtFile ?: return emptyList()

    val pos = SourcePosition.createFromLine(file, position.line)
    val lambdas = getLambdasAtLineIfAny(pos)
    if (lambdas.isEmpty()) return emptyList()

    val result = LinkedList<JavaLineBreakpointType.JavaBreakpointVariant>()

    val elementAt = pos.elementAt.parentsWithSelf.firstIsInstance<KtElement>()
    val mainMethod = PsiTreeUtil.getParentOfType(elementAt, KtFunction::class.java, false)

    var mainMethodAdded = false

    if (mainMethod != null) {
        val bodyExpression = mainMethod.bodyExpression
        val isLambdaResult = bodyExpression is KtLambdaExpression && bodyExpression.functionLiteral in lambdas

        if (!isLambdaResult) {
            val variantElement = CodeInsightUtils.getTopmostElementAtOffset(elementAt, pos.offset)
            result.add(kotlinBreakpointType.LineKotlinBreakpointVariant(position, variantElement, -1))
            mainMethodAdded = true
        }
    }

    lambdas.forEachIndexed { ordinal, lambda ->
        val positionImpl = XSourcePositionImpl.createByElement(lambda.bodyExpression)

        if (positionImpl != null) {
            result.add(kotlinBreakpointType.LambdaJavaBreakpointVariant(positionImpl, lambda, ordinal))
        }
    }

    if (mainMethodAdded && result.size > 1) {
        result.add(kotlinBreakpointType.KotlinBreakpointVariant(position, lambdas.size))
    }

    return result
}

fun getLambdasAtLineIfAny(sourcePosition: SourcePosition): List<KtFunction> {
    val file = sourcePosition.file as? KtFile ?: return emptyList()
    val lineNumber = sourcePosition.line
    return getLambdasAtLineIfAny(file, lineNumber)
}

fun getLambdasAtLineIfAny(file: KtFile, line: Int): List<KtFunction> {
    val lineElement = findElementAtLine(file, line) as? KtElement ?: return emptyList()

    val start = lineElement.startOffset
    val end = lineElement.endOffset

    val allLiterals = CodeInsightUtils.
            findElementsOfClassInRange(file, start, end, KtFunction::class.java)
            .filterIsInstance<KtFunction>()
            // filter function literals and functional expressions
            .filter { it is KtFunctionLiteral || it.name == null }
            .toSet()

    return allLiterals.filter {
        val statement = it.bodyBlockExpression?.statements?.firstOrNull() ?: it
        statement.getLineNumber() == line && statement.getLineNumber(false) == line
    }
}

internal fun KtCallableDeclaration.isInlineOnly(): Boolean {
    if (!hasModifier(KtTokens.INLINE_KEYWORD)) {
        return false
    }

    val inlineOnlyAnnotation = annotationEntries
        .firstOrNull { it.shortName == INLINE_ONLY_ANNOTATION_FQ_NAME.shortName() }
        ?: return false

    return runReadAction f@{
        val bindingContext = inlineOnlyAnnotation.analyze(BodyResolveMode.PARTIAL)
        val annotationDescriptor = bindingContext[BindingContext.ANNOTATION, inlineOnlyAnnotation] ?: return@f false
        return@f annotationDescriptor.fqName == INLINE_ONLY_ANNOTATION_FQ_NAME
    }
}
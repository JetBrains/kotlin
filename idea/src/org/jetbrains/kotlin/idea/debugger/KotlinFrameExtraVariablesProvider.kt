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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.FrameExtraVariablesProvider
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.debugger.engine.evaluation.TextWithImports
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.text.CharArrayUtil
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.refactoring.getLineEndOffset
import org.jetbrains.kotlin.idea.refactoring.getLineStartOffset
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.*

class KotlinFrameExtraVariablesProvider : FrameExtraVariablesProvider {
    override fun isAvailable(sourcePosition: SourcePosition, evalContext: EvaluationContext): Boolean {
        if (sourcePosition.line < 0) return false
        return sourcePosition.file.fileType == KotlinFileType.INSTANCE && DebuggerSettings.getInstance().AUTO_VARIABLES_MODE
    }

    override fun collectVariables(
            sourcePosition: SourcePosition, evalContext: EvaluationContext, alreadyCollected: MutableSet<String>): Set<TextWithImports> {
        return runReadAction { findAdditionalExpressions(sourcePosition) }
    }
}

private fun findAdditionalExpressions(position: SourcePosition): Set<TextWithImports> {
    val line = position.line
    val file = position.file

    val vFile = file.virtualFile
    val doc = if (vFile != null) FileDocumentManager.getInstance().getDocument(vFile) else null
    if (doc == null || doc.lineCount == 0 || line > (doc.lineCount - 1)) {
        return emptySet()
    }

    val offset = file.getLineStartOffset(line)?.takeIf { it > 0 } ?: return emptySet()

    val elem = file.findElementAt(offset)
    val containingElement = getContainingElement(elem!!) ?: elem ?: return emptySet()

    val limit = getLineRangeForElement(containingElement, doc)

    var startLine = Math.max(limit.startOffset, line)
    while (startLine - 1 > limit.startOffset && shouldSkipLine(file, doc, startLine - 1)) {
        startLine--
    }

    var endLine = Math.min(limit.endOffset, line)
    while (endLine + 1 < limit.endOffset && shouldSkipLine(file, doc, endLine + 1)) {
        endLine++
    }

    val startOffset = file.getLineStartOffset(startLine) ?: return emptySet()
    val endOffset = file.getLineEndOffset(endLine) ?: return emptySet()

    if (startOffset >= endOffset) return emptySet()

    val lineRange = TextRange(startOffset, endOffset)
    if (lineRange.isEmpty) return emptySet()

    val expressions = LinkedHashSet<TextWithImports>()

    val variablesCollector = VariablesCollector(lineRange, expressions)
    containingElement.accept(variablesCollector)

    return expressions
}

private fun getContainingElement(element: PsiElement): KtElement? {
    val contElement = PsiTreeUtil.getParentOfType(element, KtDeclaration::class.java) ?: PsiTreeUtil.getParentOfType(element, KtElement::class.java)
    if (contElement is KtProperty && contElement.isLocal) {
        val parent = contElement.parent
        return getContainingElement(parent)
    }

    if (contElement is KtDeclarationWithBody) {
        return contElement.bodyExpression
    }
    return contElement
}

private fun getLineRangeForElement(containingElement: PsiElement, doc: Document): TextRange {
    val elemRange = containingElement.textRange
    return TextRange(doc.getLineNumber(elemRange.startOffset), doc.getLineNumber(elemRange.endOffset))
}

private fun shouldSkipLine(file: PsiFile, doc: Document, line: Int): Boolean {
    val start = CharArrayUtil.shiftForward(doc.charsSequence, doc.getLineStartOffset(line), " \n\t")
    val end = doc.getLineEndOffset(line)
    if (start >= end) {
        return true
    }

    val elemAtOffset = file.findElementAt(start)
    val topmostElementAtOffset = CodeInsightUtils.getTopmostElementAtOffset(elemAtOffset!!, start)
    return topmostElementAtOffset !is KtDeclaration
}

private class VariablesCollector(
        private val myLineRange: TextRange,
        private val myExpressions: MutableSet<TextWithImports>
) : KtTreeVisitorVoid() {

    override fun visitKtElement(element: KtElement) {
        if (element.isInRange()) {
            super.visitKtElement(element)
        }
    }

    override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
        if (expression.isInRange()) {
            val selector = expression.selectorExpression
            if (selector is KtReferenceExpression) {
                if (isRefToProperty(selector)) {
                    myExpressions.add(expression.createText())
                    return
                }
            }
        }
        super.visitQualifiedExpression(expression)
    }

    private fun isRefToProperty(expression: KtReferenceExpression): Boolean {
        val context = expression.analyzeFully()
        val descriptor = context[BindingContext.REFERENCE_TARGET, expression]
        if (descriptor is PropertyDescriptor) {
            val getter = descriptor.getter
            return (getter == null || context[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, getter] == null) &&
                   descriptor.compileTimeInitializer == null
        }
        return false
    }

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        if (expression.isInRange()) {
            if (isRefToProperty(expression)) {
                myExpressions.add(expression.createText())
            }
        }
        super.visitReferenceExpression(expression)
    }

    private fun KtElement.isInRange(): Boolean = myLineRange.intersects(this.textRange)
    private fun KtElement.createText(): TextWithImports = TextWithImportsImpl(CodeFragmentKind.EXPRESSION, this.text)

    override fun visitClass(klass: KtClass) {
        // Do not show expressions used in local classes
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        // Do not show expressions used in local functions
    }

    override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
        // Do not show expressions used in anonymous objects
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        // Do not show expressions used in lambdas
    }
}
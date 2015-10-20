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

import com.intellij.debugger.engine.FrameExtraVariablesProvider
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.evaluation.EvaluationContext
import com.intellij.debugger.engine.evaluation.TextWithImports
import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import com.intellij.util.text.CharArrayUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import java.util.LinkedHashSet
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully

public class KotlinFrameExtraVariablesProvider : FrameExtraVariablesProvider {
    override fun isAvailable(sourcePosition: SourcePosition?, evalContext: EvaluationContext?): Boolean {
        if (sourcePosition == null) return false
        if (sourcePosition.getLine() < 0) return false
        return sourcePosition.getFile().getFileType() == KotlinFileType.INSTANCE && DebuggerSettings.getInstance().AUTO_VARIABLES_MODE
    }

    override fun collectVariables(sourcePosition: SourcePosition?, evalContext: EvaluationContext?, alreadyCollected: Set<String>?
    ): Set<TextWithImports>? {
        if (sourcePosition != null) {
            return runReadAction { findAdditionalExpressions(sourcePosition) }
        }
        return setOf()
    }
}

private fun findAdditionalExpressions(position: SourcePosition): Set<TextWithImports> {
    val line = position.getLine()
    val file = position.getFile()

    val vFile = file.getVirtualFile()
    val doc = if (vFile != null) FileDocumentManager.getInstance().getDocument(vFile) else null
    if (doc == null || doc.getLineCount() == 0 || line > (doc.getLineCount() - 1)) {
        return emptySet()
    }

    val offset = doc.getLineStartOffset(line)
    if (offset < 0) return emptySet()

    val elem = file.findElementAt(offset)
    val containingElement = getContainingElement(elem!!) ?: elem

    if (containingElement == null) return emptySet()

    val limit = getLineRangeForElement(containingElement, doc)

    var startLine = Math.max(limit.getStartOffset(), line)
    while (startLine - 1 > limit.getStartOffset() && shouldSkipLine(file, doc, startLine - 1)) {
        startLine--
    }

    var endLine = Math.min(limit.getEndOffset(), line)
    while (endLine + 1 < limit.getEndOffset() && shouldSkipLine(file, doc, endLine + 1)) {
        endLine++
    }

    val startOffset = doc.getLineStartOffset(startLine)
    val endOffset = doc.getLineEndOffset(endLine)

    if (startOffset >= endOffset) return emptySet()

    val lineRange = TextRange(startOffset, endOffset)
    if (lineRange.isEmpty()) return emptySet()

    val expressions = LinkedHashSet<TextWithImports>()

    val variablesCollector = VariablesCollector(lineRange, expressions)
    containingElement.accept(variablesCollector)

    return expressions
}

private fun getContainingElement(element: PsiElement): KtElement? {
    val contElement = PsiTreeUtil.getParentOfType(element, javaClass<KtDeclaration>()) ?: PsiTreeUtil.getParentOfType(element, javaClass<KtElement>())
    if (contElement is KtProperty && contElement.isLocal()) {
        val parent = contElement.getParent()
        if (parent != null) {
            return getContainingElement(parent)
        }
    }

    if (contElement is KtDeclarationWithBody) {
        return contElement.getBodyExpression()
    }
    return contElement
}

private fun getLineRangeForElement(containingElement: PsiElement, doc: Document): TextRange {
    val elemRange = containingElement.getTextRange()
    return TextRange(doc.getLineNumber(elemRange.getStartOffset()), doc.getLineNumber(elemRange.getEndOffset()))
}

private fun shouldSkipLine(file: PsiFile, doc: Document, line: Int): Boolean {
    val start = CharArrayUtil.shiftForward(doc.getCharsSequence(), doc.getLineStartOffset(line), " \n\t")
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

    override fun visitJetElement(element: KtElement) {
        if (element.isInRange()) {
            super.visitJetElement(element)
        }
    }

    override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
        if (expression.isInRange()) {
            val selector = expression.getSelectorExpression()
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
            val getter = descriptor.getGetter()
            return (getter == null || context[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, getter] == null) &&
                   descriptor.getCompileTimeInitializer() == null
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

    private fun KtElement.isInRange(): Boolean = myLineRange.intersects(this.getTextRange())
    private fun KtElement.createText(): TextWithImports = TextWithImportsImpl(CodeFragmentKind.EXPRESSION, this.getText())

    override fun visitClass(klass: KtClass) {
        // Do not show expressions used in local classes
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        // Do not show expressions used in local functions
    }

    override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
        // Do not show expressions used in anonymous objects
    }

    override fun visitFunctionLiteralExpression(expression: KtFunctionLiteralExpression) {
        // Do not show expressions used in lambdas
    }
}
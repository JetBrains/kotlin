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

package org.jetbrains.kotlin.idea

import com.intellij.codeInsight.folding.JavaCodeFoldingSettings
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.CustomFoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor

class KotlinFoldingBuilder : CustomFoldingBuilder(), DumbAware {

    private val collectionFactoryFunctions: Set<FqName> =
        setOf(
            FqName("kotlin.arrayOf"),
            FqName("kotlin.booleanArrayOf"),
            FqName("kotlin.byteArrayOf"),
            FqName("kotlin.charArrayOf"),
            FqName("kotlin.doubleArrayOf"),
            FqName("kotlin.floatArrayOf"),
            FqName("kotlin.intArrayOf"),
            FqName("kotlin.longArrayOf"),
            FqName("kotlin.shortArrayOf"),
            FqName("kotlin.kotlin_builtins.arrayOf"),
            FqName("kotlin.kotlin_builtins.booleanArrayOf"),
            FqName("kotlin.kotlin_builtins.byteArrayOf"),
            FqName("kotlin.kotlin_builtins.charArrayOf"),
            FqName("kotlin.kotlin_builtins.doubleArrayOf"),
            FqName("kotlin.kotlin_builtins.floatArrayOf"),
            FqName("kotlin.kotlin_builtins.intArrayOf"),
            FqName("kotlin.kotlin_builtins.longArrayOf"),
            FqName("kotlin.kotlin_builtins.shortArrayOf"),
            FqName("kotlin.collections.arrayListOf"),
            FqName("kotlin.collections.hashMapOf"),
            FqName("kotlin.collections.hashSetOf"),
            FqName("kotlin.collections.linkedMapOf"),
            FqName("kotlin.collections.linkedSetOf"),
            FqName("kotlin.collections.linkedStringMapOf"),
            FqName("kotlin.collections.linkedStringSetOf"),
            FqName("kotlin.collections.listOf"),
            FqName("kotlin.collections.listOfNotNull"),
            FqName("kotlin.collections.mapOf"),
            FqName("kotlin.collections.mutableListOf"),
            FqName("kotlin.collections.mutableMapOf"),
            FqName("kotlin.collections.mutableSetOf"),
            FqName("kotlin.collections.setOf"),
            FqName("kotlin.collections.sortedMapOf"),
            FqName("kotlin.collections.sortedSetOf"),
            FqName("kotlin.collections.stringMapOf"),
            FqName("kotlin.collections.stringSetOf")
        )

    override fun buildLanguageFoldRegions(
        descriptors: MutableList<FoldingDescriptor>,
        root: PsiElement, document: Document, quick: Boolean
    ) {
        if (root !is KtFile) {
            return
        }
        val imports = root.importDirectives
        if (imports.size > 1) {
            val importKeyword = imports[0].firstChild
            val startOffset = importKeyword.endOffset + 1

            val importList = root.importList
            if (importList != null) {
                val endOffset = importList.endOffset

                val range = TextRange(startOffset, endOffset)
                descriptors.add(FoldingDescriptor(importList, range).apply { setCanBeRemovedWhenCollapsed(true) })
            }
        }

        appendDescriptors(root.node, document, descriptors)
    }

    private fun appendDescriptors(node: ASTNode, document: Document, descriptors: MutableList<FoldingDescriptor>) {
        if (needFolding(node)) {
            val textRange = getRangeToFold(node)
            val relativeRange = textRange.shiftRight(-node.textRange.startOffset)
            val foldRegionText = node.chars.subSequence(relativeRange.startOffset, relativeRange.endOffset)
            if (StringUtil.countNewLines(foldRegionText) > 0) {
                descriptors.add(FoldingDescriptor(node, textRange))
            }
        }

        var child = node.firstChildNode
        while (child != null) {
            appendDescriptors(child, document, descriptors)
            child = child.treeNext
        }
    }

    private fun needFolding(node: ASTNode): Boolean {
        val type = node.elementType
        val parentType = node.treeParent?.elementType

        return type == KtNodeTypes.FUNCTION_LITERAL ||
                (type == KtNodeTypes.BLOCK && parentType != KtNodeTypes.FUNCTION_LITERAL) ||
                type == KtNodeTypes.CLASS_BODY || type == KtTokens.BLOCK_COMMENT || type == KDocTokens.KDOC ||
                type == KtNodeTypes.STRING_TEMPLATE || type == KtNodeTypes.PRIMARY_CONSTRUCTOR ||
                node.shouldFoldCollection()
    }

    private fun ASTNode.shouldFoldCollection(): Boolean = with((psi as? KtCallExpression)?.referenceExpression()) {
        if (this == null || DumbService.isDumb(project)) {
            return false
        }

        fun KtReference.targets(bindingContext: BindingContext): Collection<DeclarationDescriptor> =
            bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, element as? KtReferenceExpression]?.let { listOf(it) }
                    ?: resolveToDescriptors(bindingContext)

        for (reference in references) {
            if (reference !is KtReference) continue

            val names = reference.resolvesByNames
            val bindingContext = analyze()
            val targets = reference.targets(bindingContext)

            for (target in targets) {
                val importableDescriptor = target.getImportableDescriptor()
                if (importableDescriptor.name !in names) continue // resolved via alias

                val importableFqName = target.importableFqName ?: continue
                if (target !is PackageViewDescriptor && importableFqName in collectionFactoryFunctions) {
                    return true
                }
            }
        }

        return false
    }

    private fun getRangeToFold(node: ASTNode): TextRange {
        if (node.elementType == KtNodeTypes.FUNCTION_LITERAL) {
            val psi = node.psi as? KtFunctionLiteral
            val lbrace = psi?.lBrace
            val rbrace = psi?.rBrace
            if (lbrace != null && rbrace != null) {
                return TextRange(lbrace.startOffset, rbrace.endOffset)
            }
        }

        if (node.elementType == KtNodeTypes.CALL_EXPRESSION) {
            val valueArgumentList = (node.psi as? KtCallExpression)?.valueArgumentList
            val leftParenthesis = valueArgumentList?.leftParenthesis
            val rightParenthesis = valueArgumentList?.rightParenthesis
            if (leftParenthesis != null && rightParenthesis != null) {
                return TextRange(leftParenthesis.startOffset, rightParenthesis.endOffset)
            }
        }

        return node.textRange
    }

    override fun getLanguagePlaceholderText(node: ASTNode, range: TextRange): String = when {
        node.elementType == KtTokens.BLOCK_COMMENT -> "/${getFirstLineOfComment(node)}.../"
        node.elementType == KDocTokens.KDOC -> "/**${getFirstLineOfComment(node)}...*/"
        node.elementType == KtNodeTypes.STRING_TEMPLATE -> "\"\"\"${getTrimmedFirstLineOfString(node).addSpaceIfNeeded()}...\"\"\""
        node.elementType == KtNodeTypes.PRIMARY_CONSTRUCTOR || node.elementType == KtNodeTypes.CALL_EXPRESSION -> "(...)"
        node.psi is KtImportList -> "..."
        else -> "{...}"
    }

    private fun getTrimmedFirstLineOfString(node: ASTNode): String {
        val lines = node.text.split("\n")
        val firstLine = lines.asSequence().map { it.replace("\"\"\"", "").trim() }.firstOrNull(String::isNotEmpty)
        return firstLine ?: ""
    }

    private fun String.addSpaceIfNeeded(): String {
        if (isEmpty() || endsWith(" ")) return this
        return this + " "
    }

    private fun getFirstLineOfComment(node: ASTNode): String {
        val targetCommentLine = node.text.split("\n").firstOrNull {
            getCommentContents(it).isNotEmpty()
        } ?: return ""
        return " ${getCommentContents(targetCommentLine)} "
    }

    private fun getCommentContents(line: String): String {
        return line.trim()
            .removePrefix("/**")
            .removePrefix("/*")
            .removePrefix("*/")
            .removePrefix("*")
            .trim()
    }

    override fun isRegionCollapsedByDefault(node: ASTNode): Boolean {
        val settings = JavaCodeFoldingSettings.getInstance()

        if (node.psi is KtImportList) {
            return settings.isCollapseImports
        }

        val type = node.elementType
        if (type == KtTokens.BLOCK_COMMENT || type == KDocTokens.KDOC) {
            if (isFirstElementInFile(node.psi)) {
                return settings.isCollapseFileHeader
            }
        }

        return false
    }

    override fun isCustomFoldingRoot(node: ASTNode) = node.elementType == KtNodeTypes.BLOCK || node.elementType == KtNodeTypes.CLASS_BODY

    private fun isFirstElementInFile(element: PsiElement): Boolean {
        val parent = element.parent
        if (parent is KtFile) {
            val firstNonWhiteSpace = parent.allChildren.firstOrNull {
                it.textLength != 0 && it !is PsiWhiteSpace
            }

            return element == firstNonWhiteSpace
        }

        return false
    }
}

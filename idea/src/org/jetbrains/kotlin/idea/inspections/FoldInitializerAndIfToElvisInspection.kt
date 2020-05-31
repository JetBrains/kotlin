/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.application.options.CodeStyle
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.core.util.isMultiLine
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.elvisPattern
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.expressionComparedToNull
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.fromIfKeywordToRightParenthesisTextRangeInThis
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.shouldBeTransformed
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.hasComments
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FoldInitializerAndIfToElvisInspection : AbstractApplicabilityBasedInspection<KtIfExpression>(KtIfExpression::class.java) {
    override fun inspectionText(element: KtIfExpression): String = KotlinBundle.message("if.null.return.break.foldable.to")

    override val defaultFixText: String get() = KotlinBundle.message("replace.if.with.elvis.operator")

    override fun inspectionHighlightRangeInElement(element: KtIfExpression) = element.fromIfKeywordToRightParenthesisTextRangeInThis()

    override fun inspectionHighlightType(element: KtIfExpression): ProblemHighlightType =
        if (element.shouldBeTransformed()) ProblemHighlightType.GENERIC_ERROR_OR_WARNING else ProblemHighlightType.INFORMATION

    override fun isApplicable(element: KtIfExpression): Boolean = Companion.isApplicable(element)

    override fun applyTo(element: KtIfExpression, project: Project, editor: Editor?) {
        Companion.applyTo(element).right?.textOffset?.let { editor?.caretModel?.moveToOffset(it) }
    }

    companion object {
        private fun applicabilityRange(element: KtIfExpression): TextRange? {
            val data = calcData(element) ?: return null

            if (data.initializer !is KtParenthesizedExpression
                && data.initializer.isMultiLine()
                && createElvisExpression(element, data, KtPsiFactory(element)).left is KtParenthesizedExpression
            ) return null

            val type = data.ifNullExpression.analyze().getType(data.ifNullExpression) ?: return null
            if (!type.isNothing()) return null

            return element.fromIfKeywordToRightParenthesisTextRangeInThis()
        }

        fun isApplicable(element: KtIfExpression): Boolean = applicabilityRange(element) != null

        fun applyTo(element: KtIfExpression): KtBinaryExpression {
            val data = calcData(element)!!
            val (initializer, declaration, _, typeReference) = data
            val factory = KtPsiFactory(element)

            val explicitTypeToSet = when {
                // for var with no explicit type, add it so that the actual change won't change
                declaration.isVar && declaration.typeReference == null -> {
                    if (element.condition is KtBinaryExpression) {
                        val ifEndOffset = element.endOffset
                        val context = element.analyze()
                        val isUsedAsNotNullable = ReferencesSearch.search(declaration, LocalSearchScope(declaration.parent)).any {
                            if (it.element.startOffset <= ifEndOffset) return@any false
                            val type = it.element.safeAs<KtExpression>()?.getType(context) ?: return@any false
                            !type.isNullable()
                        }
                        if (isUsedAsNotNullable) null else context.getType(initializer)
                    } else {
                        initializer.analyze(BodyResolveMode.PARTIAL).getType(initializer)
                    }
                }

                // for val with explicit type, change it to non-nullable
                !declaration.isVar && declaration.typeReference != null -> initializer.analyze(BodyResolveMode.PARTIAL).getType(initializer)
                    ?.makeNotNullable()

                else -> null
            }

            val childRangeBefore = PsiChildRange(declaration, element)
            val commentSaver = CommentSaver(childRangeBefore)
            val childRangeAfter = childRangeBefore.withoutLastStatement()

            val elvis = createElvisExpression(element, data, factory)

            return runWriteAction {
                if (typeReference != null) {
                    elvis.left!!.replace(factory.createExpressionByPattern("$0 as? $1", initializer, typeReference))
                }
                val newElvis = initializer.replaced(elvis)
                element.delete()

                if (explicitTypeToSet != null && !explicitTypeToSet.isError) {
                    declaration.setType(explicitTypeToSet)
                }

                commentSaver.restore(childRangeAfter)
                newElvis
            }
        }

        private fun createElvisExpression(element: KtIfExpression, data: Data, factory: KtPsiFactory): KtBinaryExpression {
            val (initializer, declaration, ifNullExpr, _) = data
            val margin = CodeStyle.getSettings(element.containingKtFile).defaultRightMargin
            val declarationTextLength = declaration.text.split("\n").lastOrNull()?.trim()?.length ?: 0
            val pattern = elvisPattern(declarationTextLength + ifNullExpr.textLength + 5 >= margin || element.then?.hasComments() == true)
            return factory.createExpressionByPattern(pattern, initializer, ifNullExpr) as KtBinaryExpression
        }

        private fun calcData(ifExpression: KtIfExpression): Data? {
            if (ifExpression.`else` != null) return null

            val operationExpression = ifExpression.condition as? KtOperationExpression ?: return null
            val value = when (operationExpression) {
                is KtBinaryExpression -> {
                    if (operationExpression.operationToken != KtTokens.EQEQ) return null
                    operationExpression.expressionComparedToNull()
                }
                is KtIsExpression -> {
                    if (!operationExpression.isNegated) return null
                    if (operationExpression.typeReference?.typeElement is KtNullableType) return null
                    operationExpression.leftHandSide
                }
                else -> return null
            } as? KtNameReferenceExpression ?: return null

            if (ifExpression.parent !is KtBlockExpression) return null
            val prevStatement = (ifExpression.siblings(forward = false, withItself = false)
                .firstIsInstanceOrNull<KtExpression>() ?: return null) as? KtVariableDeclaration
            prevStatement ?: return null
            if (prevStatement.nameAsName != value.getReferencedNameAsName()) return null
            val initializer = prevStatement.initializer ?: return null
            val then = ifExpression.then ?: return null
            val typeReference = (operationExpression as? KtIsExpression)?.typeReference

            if (typeReference != null) {
                val checkedType = ifExpression.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, typeReference]
                val variableType = (prevStatement.resolveToDescriptorIfAny() as? VariableDescriptor)?.type
                if (checkedType != null && variableType != null && !checkedType.isSubtypeOf(variableType)) return null
            }

            val statement = if (then is KtBlockExpression) then.statements.singleOrNull() else then
            statement ?: return null

            if (ReferencesSearch.search(prevStatement, LocalSearchScope(statement)).findFirst() != null) {
                return null
            }

            return Data(
                initializer,
                prevStatement,
                statement,
                typeReference
            )
        }

        private fun PsiChildRange.withoutLastStatement(): PsiChildRange {
            val newLast = last!!.siblings(forward = false, withItself = false).first { it !is PsiWhiteSpace }
            return PsiChildRange(first, newLast)
        }
    }

    private data class Data(
        val initializer: KtExpression,
        val declaration: KtVariableDeclaration,
        val ifNullExpression: KtExpression,
        val typeChecked: KtTypeReference? = null
    )
}
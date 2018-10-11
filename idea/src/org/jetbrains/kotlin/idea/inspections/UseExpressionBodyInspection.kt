/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING
import com.intellij.codeInspection.ProblemHighlightType.INFORMATION
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.canOmitDeclaredType
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isOneLiner
import org.jetbrains.kotlin.idea.intentions.hasResultingIfWithoutElse
import org.jetbrains.kotlin.idea.intentions.resultingWhens
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext

class UseExpressionBodyInspection(private val convertEmptyToUnit: Boolean) : AbstractKotlinInspection() {

    constructor() : this(convertEmptyToUnit = true)

    private data class Status(val toHighlight: PsiElement?, val subject: String, val highlightType: ProblemHighlightType)

    fun isActiveFor(declaration: KtDeclarationWithBody) = statusFor(declaration) != null

    private fun statusFor(declaration: KtDeclarationWithBody): Status? {
        if (declaration is KtConstructor<*>) return null

        val valueStatement = declaration.findValueStatement() ?: return null
        val value = valueStatement.getValue()
        if (value.anyDescendantOfType<KtReturnExpression>(
                canGoInside = { it !is KtFunctionLiteral && it !is KtNamedFunction && it !is KtPropertyAccessor }
            )
        ) return null

        val toHighlight = valueStatement.toHighlight()
        return when {
            valueStatement !is KtReturnExpression -> Status(toHighlight, "block body", INFORMATION)
            valueStatement.returnedExpression is KtWhenExpression -> Status(toHighlight, "'return when'", INFORMATION)
            valueStatement.isOneLiner() -> Status(toHighlight, "one-line return", GENERIC_ERROR_OR_WARNING)
            else -> Status(toHighlight, "return", INFORMATION)
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        declarationVisitor(fun(declaration) {
            if (declaration !is KtDeclarationWithBody) return
            val (toHighlightElement, suffix, highlightType) = statusFor(declaration) ?: return
            // Change range to start with left brace
            val hasHighlighting = highlightType != INFORMATION

            fun defaultLevel(): HighlightDisplayLevel {
                val project = declaration.project
                val inspectionProfileManager = ProjectInspectionProfileManager.getInstance(project)
                val inspectionProfile = inspectionProfileManager.currentProfile
                val state = inspectionProfile.getToolDefaultState("UseExpressionBody", project)
                return state.level
            }

            val toHighlightRange = toHighlightElement?.textRange?.let {
                if (hasHighlighting && defaultLevel() != HighlightDisplayLevel.DO_NOT_SHOW) {
                    it
                } else {
                    // Extend range to [left brace..end of highlight element]
                    val offset = (declaration.blockExpression()?.lBrace?.startOffset ?: it.startOffset) - it.startOffset
                    it.shiftRight(offset).grown(-offset)
                }
            }

            holder.registerProblemWithoutOfflineInformation(
                declaration,
                "Use expression body instead of $suffix",
                isOnTheFly,
                highlightType,
                toHighlightRange?.shiftRight(-declaration.startOffset),
                ConvertToExpressionBodyFix()
            )
        })

    private fun KtDeclarationWithBody.findValueStatement(): KtExpression? {
        val body = blockExpression() ?: return null
        return body.findValueStatement()
    }

    private fun KtDeclarationWithBody.blockExpression() = when (this) {
        is KtFunctionLiteral -> null
        else -> if (!hasBlockBody()) null else bodyExpression as? KtBlockExpression
    }

    private fun KtBlockExpression.findValueStatement(): KtExpression? {
        val bodyStatements = statements
        if (bodyStatements.isEmpty()) {
            return if (convertEmptyToUnit) KtPsiFactory(this).createExpression("Unit") else null
        }
        val statement = bodyStatements.singleOrNull() ?: return null
        when (statement) {
            is KtReturnExpression -> {
                return statement
            }

        //TODO: IMO this is not good code, there should be a way to detect that KtExpression does not have value
            is KtDeclaration, is KtLoopExpression -> return null

            else -> {
                // assignment does not have value
                if (statement is KtBinaryExpression && statement.operationToken in KtTokens.ALL_ASSIGNMENTS) return null

                val context = statement.analyze()
                val expressionType = context.getType(statement) ?: return null
                val isUnit = KotlinBuiltIns.isUnit(expressionType)
                if (!isUnit && !KotlinBuiltIns.isNothing(expressionType)) return null
                if (isUnit) {
                    if (statement.hasResultingIfWithoutElse()) {
                        return null
                    }
                    val resultingWhens = statement.resultingWhens()
                    if (resultingWhens.any { it.elseExpression == null && context.get(BindingContext.EXHAUSTIVE_WHEN, it) != true }) {
                        return null
                    }
                }
                return statement
            }
        }
    }

    private fun KtExpression.getValue() = when (this) {
        is KtReturnExpression -> returnedExpression
        else -> null
    } ?: this

    private fun KtExpression.toHighlight(): PsiElement? = when (this) {
        is KtReturnExpression -> returnKeyword
        is KtCallExpression -> calleeExpression
        is KtQualifiedExpression -> selectorExpression?.toHighlight()
        is KtObjectLiteralExpression -> objectDeclaration.getObjectKeyword()
        else -> null
    }

    fun simplify(declaration: KtDeclarationWithBody, canDeleteTypeRef: Boolean) {
        val deleteTypeHandler: (KtCallableDeclaration) -> Unit = {
            it.deleteChildRange(it.colon!!, it.typeReference!!)
        }
        simplify(declaration, deleteTypeHandler.takeIf { canDeleteTypeRef })
    }

    private fun simplify(declaration: KtDeclarationWithBody, deleteTypeHandler: ((KtCallableDeclaration) -> Unit)?) {
        val block = declaration.blockExpression() ?: return
        val valueStatement = block.findValueStatement() ?: return
        val value = valueStatement.getValue()

        if (!declaration.hasDeclaredReturnType() && declaration is KtNamedFunction && block.statements.isNotEmpty()) {
            val valueType = value.analyze().getType(value)
            if (valueType == null || !KotlinBuiltIns.isUnit(valueType)) {
                declaration.setType(KotlinBuiltIns.FQ_NAMES.unit.asString(), shortenReferences = true)
            }
        }

        val body = declaration.bodyExpression!!

        val commentSaver = CommentSaver(body)

        val factory = KtPsiFactory(declaration)
        declaration.addBefore(factory.createEQ(), body)
        val newBody = body.replaced(value)

        commentSaver.restore(newBody)

        if (deleteTypeHandler != null && declaration is KtCallableDeclaration) {
            if (declaration.hasDeclaredReturnType() && declaration.canOmitDeclaredType(newBody, canChangeTypeToSubtype = true)) {
                deleteTypeHandler(declaration)
            }
        }

        val editor = declaration.findExistingEditor()
        if (editor != null) {
            val startOffset = newBody.startOffset
            val document = editor.document
            val startLine = document.getLineNumber(startOffset)
            val rightMargin = editor.settings.getRightMargin(editor.project)
            if (document.getLineEndOffset(startLine) - document.getLineStartOffset(startLine) >= rightMargin) {
                declaration.addBefore(factory.createNewLine(), newBody)
            }
        }
    }

    inner class ConvertToExpressionBodyFix : LocalQuickFix {
        override fun getFamilyName() = name

        override fun getName() = "Convert to expression body"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val declaration = descriptor.psiElement as? KtDeclarationWithBody ?: return
            simplify(declaration) {
                val typeRef = it.typeReference!!
                val colon = it.colon!!
                it.findExistingEditor()?.apply {
                    selectionModel.setSelection(colon.startOffset, typeRef.endOffset)
                    caretModel.moveToOffset(typeRef.endOffset)
                }
            }
        }
    }

}
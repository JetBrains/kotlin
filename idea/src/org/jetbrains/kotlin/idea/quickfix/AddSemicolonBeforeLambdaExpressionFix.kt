/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.util.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class AddSemicolonBeforeLambdaExpressionFix(element: KtLambdaExpression) : KotlinQuickFixAction<KtLambdaExpression>(element) {
    override fun getText(): String = "Terminate preceding call with semicolon"
    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val lambdaExpressionArgument = element?.parent?.safeAs<KtLambdaArgument>() ?: return
        val callExpression = lambdaExpressionArgument.parent.safeAs<KtCallExpression>() ?: return

        val desiredEndOfCallExpression = lambdaExpressionArgument.findCorrectEndOfCall()

        desiredEndOfCallExpression?.let { endOfCall ->
            val psiFactory = KtPsiFactory(project)

            val addedSemicolon = when (val parent = callExpression.parent) {
                // Parent call is the call to the right, we want to give it this call's last lambda argument as a new receiver
                is KtCallExpression -> liftTrailingNodesAndRelocateLastLambda(
                    psiFactory, callExpression, endOfCall,
                    lastLambdaAcceptor = parent,
                    nodeBeforeSemicolon = callExpression
                )
                // Incorrect call is a part of the dot-qualified expression before or after it
                is KtDotQualifiedExpression -> {
                    val grandparent = parent.parent
                    when {
                        // Call expression is a receiver of the dot expression. Give last lambda as a new receiver to that dot expression
                        parent.receiverExpression === callExpression -> {
                            liftTrailingNodesAndRelocateLastLambda(
                                psiFactory, callExpression, endOfCall,
                                lastLambdaAcceptor = parent,
                                nodeBeforeSemicolon = callExpression
                            )
                        }
                        // Call expression is the right node of parent dot expression, possible call / dot to the right is a grandparent
                        // If grandparent is call or dot expression, last lambda becomes its new receiver
                        grandparent.isCallOrDotExpression -> {
                            liftTrailingNodesAndRelocateLastLambda(
                                psiFactory, callExpression, endOfCall,
                                lastLambdaAcceptor = grandparent,
                                nodeBeforeSemicolon = parent
                            )
                        }
                        // Parent is dot expression, but there is no call or dot after it, so just lift everything up
                        else -> {
                            liftTrailingNodes(
                                psiFactory, callExpression, endOfCall,
                                addNodesAfter = parent
                            )
                        }
                    }
                }
                // Simple case: extract all trailing nodes right after call - it is a standalone call expression
                else -> liftTrailingNodes(
                    psiFactory, callExpression, endOfCall,
                    addNodesAfter = callExpression
                )
            }
            editor?.caretModel?.moveToOffset(addedSemicolon.startOffset)
        }
    }

    private fun KtLambdaArgument.findCorrectEndOfCall() =
        PsiTreeUtil.findSiblingBackward(this, KtNodeTypes.LAMBDA_ARGUMENT, null)
            ?: PsiTreeUtil.findSiblingBackward(this, KtNodeTypes.VALUE_ARGUMENT_LIST, null)

    private fun liftTrailingNodesAndRelocateLastLambda(
        psiFactory: KtPsiFactory,
        callExpression: KtCallExpression,
        endOfCall: PsiElement,
        lastLambdaAcceptor: PsiElement,
        nodeBeforeSemicolon: PsiElement
    ): PsiElement {
        val (topCall, callHolder) = topLevelHolder(callExpression)
        val semicolon = callHolder.addBefore(psiFactory.createSemicolon(), topCall)

        makeNewExpressionsFromTrailingLambdas(callExpression, endOfCall, addNodesAfter = semicolon) { lastLambdaExpression ->
            lastLambdaAcceptor.addAfter(lastLambdaExpression, nodeBeforeSemicolon)
        }

        callHolder.addBefore(nodeBeforeSemicolon, semicolon)
        nodeBeforeSemicolon.delete()

        return semicolon
    }

    private fun liftTrailingNodes(
        psiFactory: KtPsiFactory,
        callExpression: KtCallExpression,
        endOfCall: PsiElement,
        addNodesAfter: PsiElement
    ): PsiElement {
        makeNewExpressionsFromTrailingLambdas(callExpression, endOfCall, addNodesAfter)
        return addNodesAfter.parent.addAfter(
            psiFactory.createSemicolon(),
            addNodesAfter
        )
    }

    private val PsiElement.isCallOrDotExpression
        get() = this is KtCallExpression || this is KtDotQualifiedExpression

    data class TopExpressionAndHolder(val top: PsiElement, val holder: PsiElement)

    private fun topLevelHolder(callExpression: KtCallExpression): TopExpressionAndHolder {
        var me: PsiElement = callExpression
        var parent: PsiElement = callExpression.parent
        while (parent.isCallOrDotExpression) {
            me = parent
            parent = parent.parent
        }
        return TopExpressionAndHolder(me, parent)
    }

    private fun makeNewExpressionsFromTrailingLambdas(
        oldCallExpression: KtCallExpression,
        endOfArguments: PsiElement,
        addNodesAfter: PsiElement,
        lastLambdaHandler: ((PsiElement) -> Unit)? = null
    ) {
        var lastSibling = oldCallExpression.lastChild
        var lastLambdaWasProcessed = false

        while (lastSibling != endOfArguments) {
            when (lastSibling) {
                is KtLambdaArgument -> {
                    val lambdaExpression: PsiElement = lastSibling.getLambdaExpression() ?: lastSibling

                    if (lastLambdaHandler != null && !lastLambdaWasProcessed) {
                        lastLambdaWasProcessed = true
                        lastLambdaHandler(lambdaExpression)
                    } else {
                        addNodesAfter.parent.addAfter(lambdaExpression, addNodesAfter)
                    }
                }
                else -> addNodesAfter.parent.addAfter(
                    lastSibling,
                    addNodesAfter
                )
            }
            lastSibling = lastSibling.prevSibling
        }

        oldCallExpression.deleteChildRange(endOfArguments.nextSibling, oldCallExpression.lastChild)
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
            diagnostic.createIntentionForFirstParentOfType(::AddSemicolonBeforeLambdaExpressionFix)
    }
}
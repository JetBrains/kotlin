/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.codegen.kotlinType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.intentions.calleeName
import org.jetbrains.kotlin.idea.intentions.loopToCallChain.previousStatement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.addRemoveModifier.addModifier
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.supertypes

class WrapResourceWithUseCallInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)

                // Differentiate between resource of variable or non variable reference expression
                // TODO: check variable definition expression anywhere, not just the previous statement expression.
                // TODO: And highlight all references or just definition dependend on definition.

                var propertyStatement: KtProperty? = null
                var variableAssigned = false
                // Check if variable assigned or not
                expression.parent.accept(object : KtTreeVisitorVoid() {
                    override fun visitProperty(property: KtProperty) {
                        super.visitProperty(property)
                        val referenceExpression = expression.receiverExpression as? KtNameReferenceExpression ?: return
                        if (property.text.contains(referenceExpression.text)) {
                            variableAssigned = true
                            propertyStatement = property
                        }
                    }
                })

                if (variableAssigned) {
                    // Check if reference and receiver are there.
                    val referenceExpression = expression.receiverExpression as? KtNameReferenceExpression ?: return
                    val callExpression = expression.selectorExpression as? KtCallExpression ?: return

                    // Check if variable referenced class in expression (KtNameReferenceExpression) contains inner supertype Closeable
                    val bindingContext = referenceExpression.analyze(BodyResolveMode.FULL)
                    val closeableSuperType = referenceExpression.kotlinType(bindingContext)?.supertypes()?.first()
                    val constructorType = closeableSuperType?.constructor
                    val descriptor = constructorType?.declarationDescriptor
                    if (descriptor?.name.toString() != "Closeable") return

                    // Check for lambda expression
                    val callLambdaArgument = callExpression.lambdaArguments
                    if (callLambdaArgument.size > 0) return

                    val problemDescriptor = holder.manager.createProblemDescriptor(
                        expression.psiOrParent,
                        TextRange(
                            propertyStatement?.startOffset!! - callExpression.startOffset,
                            callExpression.endOffset - expression.startOffset
                        ),
                        "Convert resource to use call with prop", ProblemHighlightType.LIKE_UNUSED_SYMBOL, isOnTheFly,
                        // variable assigned resource
                        ChangeResourceVariableWithUseCall(expression)
                    )
                    holder.registerProblem(problemDescriptor)
                } else if (expression.receiverExpression is KtCallExpression && expression.selectorExpression is KtCallExpression) {

                    // Check if reference and receiver are there and both are KtCallExpression.
                    val referenceExpression = expression.receiverExpression as? KtCallExpression ?: return
                    val callExpression = expression.selectorExpression as? KtCallExpression ?: return

                    // Check if class in expression (KtCallExpression) contains inner supertype Closeable
                    val bindingContext = referenceExpression.analyze(BodyResolveMode.FULL)
                    val closeableSuperType = referenceExpression.kotlinType(bindingContext)?.supertypes()?.first()
                    val constructorType = closeableSuperType?.constructor
                    val descriptor = constructorType?.declarationDescriptor
                    if (descriptor?.name.toString() != "Closeable") return

                    // Check for lambda expression
                    val callLambdaArgument = callExpression.lambdaArguments
                    if (callLambdaArgument.size > 0) return

                    val problemDescriptor = holder.manager.createProblemDescriptor(
                        expression.psiOrParent,
                        TextRange(
                            referenceExpression.endOffset - callExpression.startOffset,
                            callExpression.endOffset - expression.startOffset
                        ),
                        "Convert resource to use call with prop", ProblemHighlightType.LIKE_UNUSED_SYMBOL, isOnTheFly,
                        // non variable assigned resource
                        ChangeResourceWithUseCall(expression)
                    )
                    holder.registerProblem(problemDescriptor)
                }
            }
        }
    }

    // QuickFix for non variable assigned method call.
    class ChangeResourceWithUseCall(val element: KtDotQualifiedExpression) : LocalQuickFix {
        override fun getName() = "Convert '${element.calleeName}' to use{} call with prop"

        override fun getFamilyName(): String = "Convert resource to use{} call with prop"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {

            // Get the resource object method
            val callExpression = element.receiverExpression as KtCallExpression

            // Get the call expression used by the resource variable
            val methodResourceCallExpression = element.selectorExpression as KtCallExpression

            // Get random variable/property name
            val validator = CollectingNameValidator()
            val variableName = KotlinNameSuggester.suggestNameByName("res", validator)


            // Create the actual use {} call with property
            val factory = KtPsiFactory(element)

            val useCallExpression = factory.buildExpression {
                if (callExpression != null) {
                    appendFixedText(callExpression.text)
                    appendFixedText(".")
                }
                appendFixedText("use {")

                if (variableName != null) {
                    appendFixedText(variableName)
                    appendFixedText(" ->")
                }

                appendFixedText("\n")
                if (methodResourceCallExpression != null) {
                    appendFixedText(variableName)
                    appendFixedText(".")
                    appendFixedText(methodResourceCallExpression.text)
                }
                appendFixedText("\n}")
            }

            val result = element.replace(useCallExpression) as KtExpression

            if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
            val typeParameter = descriptor.psiElement as? KtTypeParameter
                ?: throw AssertionError("Convert resource to use{} call with prop ${descriptor.psiElement.text}")
            addModifier(typeParameter, if (result == Variance.IN_VARIANCE) KtTokens.IN_KEYWORD else KtTokens.OUT_KEYWORD)

        }
    }

    // QuickFix for variable assigned method call.
    class ChangeResourceVariableWithUseCall(val element: KtDotQualifiedExpression) : LocalQuickFix {
        override fun getName() = "Convert '${element.calleeName}' to use{} call with prop"

        override fun getFamilyName(): String = "Convert resource to use{} call with prop"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {

            // Get the previous statement that contains the variable assigned resource call method.
            val resourceStatementExpression = element.previousStatement() as KtProperty
            val callResourceStatementExpression = resourceStatementExpression.lastChild as KtCallExpression
            val resourceName = callResourceStatementExpression.text

            // Get the reference and therefore the resource variable name
            val referenceExpression = element.receiverExpression as KtNameReferenceExpression
            val variableName = referenceExpression.getReferencedNameAsName()

            // Get the (or multiple) call expression(s) used by the resource variable
            val callExpression = element.selectorExpression as KtCallExpression
            val callReferenceExpression = callExpression.referenceExpression()
            val callExpressionTextList = mutableListOf<KtDotQualifiedExpression>()

            // Save single/multiple variable call expression(s)
            // Iterate through multiple referenced expressions and save them in a mutable list
            element.parent.accept(object : KtTreeVisitorVoid() {
                override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                    super.visitDotQualifiedExpression(expression)
                    if ((expression.receiverExpression as KtNameReferenceExpression).getReferencedNameAsName() == variableName) {
                        callExpressionTextList.add(expression)
                    }
                }
            })

            // Create the actual use {} call with property
            val factory = KtPsiFactory(element)

            // Take last item to add without "\n"
            val lastItem = callExpressionTextList.takeLast(1).single()
            // Drop last item to only add "\n" to all items except last
            val useExpressionList = callExpressionTextList.dropLast(1)
            // Drop first item to delete all other references except the first one for replace
            val deleteExpressionList = callExpressionTextList.drop(1)

            val useCallExpression = factory.buildExpression {
                if (resourceName != null) {
                    appendFixedText(resourceName)
                    appendFixedText(".")
                }
                appendFixedText("use {")

                if (variableName != null) {
                    appendName(variableName)
                    appendFixedText(" ->")
                }

                appendFixedText("\n")
                if (callReferenceExpression != null) {
                    useExpressionList.forEach {
                        appendName(variableName)
                        appendFixedText(".")
                        appendFixedText(it.selectorExpression?.text!!)
                        appendFixedText("\n")
                    }
                    appendName(variableName)
                    appendFixedText(".")
                    appendFixedText(lastItem.selectorExpression?.text!!)
                }
                appendFixedText("\n}")
            }

            // Delete the variable assigned statement
            resourceStatementExpression.delete()

            // Delete multiple referenced expressions of same variable name if any
            deleteExpressionList.forEach {
                it.delete()
            }

            val result = element.replace(useCallExpression) as KtExpression

            if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
            val typeParameter = descriptor.psiElement as? KtTypeParameter
                ?: throw AssertionError("Convert resource to use{} call with prop ${descriptor.psiElement.text}")
            addModifier(typeParameter, if (result == Variance.IN_VARIANCE) KtTokens.IN_KEYWORD else KtTokens.OUT_KEYWORD)

        }
    }

    private fun KtDotQualifiedExpression.multipleExpressions(): Boolean {
        var result = false
        accept(object : KtTreeVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                result = true
            }
        })
        return result
    }
}
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
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
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
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.supertypes

class WrapResourceWithUseCallInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)

                var propertyStatement: KtProperty? = null
                var variableAssigned = false
                // Differentiate between resource of variable or non variable reference expression
                expression.parent.accept(object : KtTreeVisitorVoid() {
                    override fun visitProperty(property: KtProperty) {
                        super.visitProperty(property)
                        val refExpression = expression.receiverExpression as? KtNameReferenceExpression ?: return
                        if (property.text.contains(refExpression.text)) {
                            variableAssigned = true
                            propertyStatement = property
                        }
                    }
                })

                val referenceExpression: KtReferenceExpression
                val callExpression: KtReferenceExpression

                var dotParent: Boolean = false

                // Check if variable assigned, non-variable with nested DotQualifiedExpression or just non-variable
                when {
                    variableAssigned -> {
                        // Check if reference and receiver are there.
                        referenceExpression = expression.receiverExpression as? KtNameReferenceExpression ?: return
                        callExpression = expression.selectorExpression as? KtCallExpression ?: return
                    }
                    expression.parent is KtDotQualifiedExpression &&
                            expression.receiverExpression is KtCallExpression && expression.selectorExpression is KtCallExpression &&
                            (expression.parent as KtDotQualifiedExpression).selectorExpression is KtCallExpression
                    -> {
                        dotParent = true
                        referenceExpression = expression.selectorExpression as? KtCallExpression ?: return
                        callExpression = (expression.parent as KtDotQualifiedExpression).selectorExpression as? KtCallExpression ?: return
                    }
                    expression.receiverExpression is KtCallExpression && expression.selectorExpression is KtCallExpression &&
                            expression.parent !is KtCallExpression -> {
                        // Check if reference and receiver are there and both are KtCallExpression.
                        referenceExpression = expression.receiverExpression as? KtCallExpression ?: return
                        callExpression = expression.selectorExpression as? KtCallExpression ?: return
                    }
                    else -> return
                }

                // Check if variable referenced class in expression (KtNameReferenceExpression) contains inner supertype Closeable
                val bindingContext = referenceExpression.analyze(BodyResolveMode.FULL)
                val type = referenceExpression.kotlinType(bindingContext) ?: return
                if (type.supertypes().all {
                        it.constructor.declarationDescriptor?.fqNameSafe?.asString().let {
                            it != "java.io.Closeable" && it != "java.lang.AutoCloseable"
                        }
                    }) return

                // Check for lambda expression
                val callLambdaArgument = (callExpression as KtCallExpression).lambdaArguments
                if (callLambdaArgument.size > 0) return

                val smartPointer = SmartPointerManager.getInstance(expression.project).createSmartPsiElementPointer(expression)
                val problemDescriptor = holder.manager.createProblemDescriptor(
                    expression.psiOrParent,
                    TextRange(
                        callExpression.startOffset.let {
                            when {
                                variableAssigned -> propertyStatement?.startOffset!!
                                dotParent -> callExpression.endOffset
                                else -> referenceExpression.endOffset
                            }.minus(it)
                        },
                        expression.startOffset.let {
                            when {
                                dotParent -> referenceExpression.endOffset
                                else -> callExpression.endOffset
                            }.minus(it)
                        }
//                                referenceExpression.endOffset - callExpression.startOffset,
//                        callExpression.endOffset - expression.startOffset
                    ),
                    "Convert to .use()", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly,
                    when {
                        variableAssigned -> ChangeResourceVariableWithUseCall(smartPointer)
                        dotParent -> ChangeResourceWithUseCall(dotParent, smartPointer)
                        else -> ChangeResourceWithUseCall(dotParent, smartPointer)
                    }
                )
                holder.registerProblem(problemDescriptor)
            }
        }
    }

    // QuickFix for non variable assigned method call.
    class ChangeResourceWithUseCall(private val dotParent: Boolean, private val pointer: SmartPsiElementPointer<KtDotQualifiedExpression>) :
        LocalQuickFix {
        override fun getName() = "Convert '${pointer.element?.calleeName}' to use{} call with prop"

        override fun getFamilyName(): String = "Convert resource to use{} call with prop"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {

            var callExpression: KtCallExpression? = null
            var dotCallExpression: KtDotQualifiedExpression? = null
            val methodResourceCallExpression: KtCallExpression

            when {
                dotParent -> {
                    dotCallExpression = pointer.element as KtDotQualifiedExpression
                    methodResourceCallExpression =
                            (pointer.element!!.parent as KtDotQualifiedExpression).selectorExpression as KtCallExpression
                }
                else -> {
                    callExpression = pointer.element?.receiverExpression as KtCallExpression
                    methodResourceCallExpression = pointer.element!!.selectorExpression as KtCallExpression
                }
            }

            // Get random variable/property name
            val validator = CollectingNameValidator()
            val variableName = KotlinNameSuggester.suggestNameByName("res", validator)

            // Create the actual use {} call with property
            val factory = KtPsiFactory(pointer.element!!)

            val useCallExpression = factory.buildExpression {
                appendFixedText(
                    when {
                        dotParent -> dotCallExpression?.text!!
                        else -> callExpression?.text!!
                    }
                )
                appendFixedText(".")
                appendFixedText("use {")
                appendFixedText(variableName)
                appendFixedText(" ->")
                appendFixedText("\n")
                appendFixedText(variableName)
                appendFixedText(".")
                appendFixedText(methodResourceCallExpression.text)
                appendFixedText("\n}")
            }

            val result = when {
                dotParent -> pointer.element!!.parent.replace(useCallExpression) as KtExpression
                else -> pointer.element!!.replace(useCallExpression) as KtExpression
            }

            if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
            val typeParameter = descriptor.psiElement as? KtTypeParameter
                ?: throw AssertionError("Convert resource to use{} call with prop ${descriptor.psiElement.text}")
            addModifier(typeParameter, if (result == Variance.IN_VARIANCE) KtTokens.IN_KEYWORD else KtTokens.OUT_KEYWORD)
        }
    }

    // QuickFix for variable assigned method call.
    class ChangeResourceVariableWithUseCall(private val pointer: SmartPsiElementPointer<KtDotQualifiedExpression>) : LocalQuickFix {
        override fun getName() = "Convert '${pointer.element?.calleeName}' to use{} call with prop"

        override fun getFamilyName(): String = "Convert resource to use{} call with prop"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {

            // Get the previous statement that contains the variable assigned resource call method.
            val resourceStatementExpression = pointer.element?.previousStatement() as KtProperty
            val callResourceStatementExpression =
                resourceStatementExpression.lastChild as? KtCallExpression
                    ?: resourceStatementExpression.lastChild as KtDotQualifiedExpression
            val resourceName = callResourceStatementExpression.text

            // Get the reference and therefore the resource variable name
            val referenceExpression = pointer.element!!.receiverExpression as KtNameReferenceExpression
            val variableName = referenceExpression.getReferencedNameAsName()

            // Get the (or multiple) call expression(s) used by the resource variable
            val callExpression = pointer.element!!.selectorExpression as KtCallExpression
            val callReferenceExpression = callExpression.referenceExpression()
            val callExpressionTextList = mutableListOf<KtDotQualifiedExpression>()

            // Save single/multiple variable call expression(s)
            // Iterate through multiple referenced expressions and save them in a mutable list
            pointer.element!!.parent.accept(object : KtTreeVisitorVoid() {
                override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                    super.visitDotQualifiedExpression(expression)
                    when (variableName) {
                        (expression.receiverExpression as? KtNameReferenceExpression)?.getReferencedNameAsName() -> callExpressionTextList.add(
                            expression
                        )
                        (expression.receiverExpression as? KtCallExpression)?.getCallNameExpression()?.getReferencedNameAsName() -> callExpressionTextList.add(
                            expression
                        )
                    }
                }
            })

            // Create the actual use {} call with property
            val factory = KtPsiFactory(pointer.element!!)

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
                appendName(variableName)
                appendFixedText(" ->")

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

            val result = pointer.element!!.replace(useCallExpression) as KtExpression

            if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
            val typeParameter = descriptor.psiElement as? KtTypeParameter
                ?: throw AssertionError("Convert resource to use{} call with prop ${descriptor.psiElement.text}")
            addModifier(typeParameter, if (result == Variance.IN_VARIANCE) KtTokens.IN_KEYWORD else KtTokens.OUT_KEYWORD)
        }
    }
}
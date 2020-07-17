/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.addIfNotNull

class SyntheticPropertyAccessorReferenceDescriptorImpl(
    expression: KtNameReferenceExpression,
    getter: Boolean
) : SyntheticPropertyAccessorReference(expression, getter), KtDescriptorsBasedReference {
    override fun isReferenceTo(element: PsiElement): Boolean =
        super<SyntheticPropertyAccessorReference>.isReferenceTo(element)

    override fun additionalIsReferenceToChecker(element: PsiElement): Boolean = matchesTarget(element)

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val descriptors = expression.getReferenceTargets(context)
        if (descriptors.none { it is SyntheticJavaPropertyDescriptor }) return emptyList()

        val result = SmartList<FunctionDescriptor>()
        for (descriptor in descriptors) {
            if (descriptor is SyntheticJavaPropertyDescriptor) {
                if (getter) {
                    result.add(descriptor.getMethod)
                } else {
                    result.addIfNotNull(descriptor.setMethod)
                }
            }
        }
        return result
    }

    private fun renameByPropertyName(newName: String): PsiElement? {
        val nameIdentifier = KtPsiFactory(expression).createNameIdentifier(newName)
        expression.getReferencedNameElement().replace(nameIdentifier)
        return expression
    }

    private fun KtExpression.createCall(
        psiFactory: KtPsiFactory,
        newName: String? = null,
        argument: KtExpression? = null
    ): KtExpression {
        return if (this is KtQualifiedExpression) {
            copied().also {
                val selector = it.getQualifiedElementSelector() as? KtExpression
                selector?.replace(selector.createCall(psiFactory, newName, argument))
            }
        } else {
            psiFactory.buildExpression {
                if (newName != null) {
                    appendFixedText(newName)
                } else {
                    appendExpression(this@createCall)
                }
                appendFixedText("(")
                if (argument != null) {
                    appendExpression(argument)
                }
                appendFixedText(")")
            }
        }
    }

    override fun handleElementRename(newElementName: String): PsiElement? {
        if (!Name.isValidIdentifier(newElementName)) return expression

        val newNameAsName = Name.identifier(newElementName)
        val newName = if (getter) {
            SyntheticJavaPropertyDescriptor.propertyNameByGetMethodName(newNameAsName)
        } else {
            //TODO: it's not correct
            //TODO: setIsY -> setIsIsY bug
            SyntheticJavaPropertyDescriptor.propertyNameBySetMethodName(
                newNameAsName,
                withIsPrefix = expression.getReferencedNameAsName().asString().startsWith(
                    "is"
                )
            )
        }
        // get/set becomes ordinary method
        if (newName == null) {
            val psiFactory = KtPsiFactory(expression)

            val newGetterName = if (getter) newElementName else JvmAbi.getterName(expression.getReferencedName())

            if (expression.readWriteAccess(false) == ReferenceAccess.READ) {
                return expression.replaced(expression.createCall(psiFactory, newGetterName))
            }

            val newSetterName = if (getter) JvmAbi.setterName(expression.getReferencedName()) else newElementName

            val fullExpression = expression.getQualifiedExpressionForSelectorOrThis()
            fullExpression.getAssignmentByLHS()?.let { assignment ->
                val rhs = assignment.right ?: return expression
                val operationToken = assignment.operationToken as? KtSingleValueToken ?: return expression
                val counterpartOp = OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS[operationToken]
                val setterArgument = if (counterpartOp != null) {
                    val getterCall = if (getter) fullExpression.createCall(psiFactory, newGetterName) else fullExpression
                    psiFactory.createExpressionByPattern("$0 ${counterpartOp.value} $1", getterCall, rhs)
                } else {
                    rhs
                }
                val newSetterCall = fullExpression.createCall(psiFactory, newSetterName, setterArgument)
                return assignment.replaced(newSetterCall).getQualifiedElementSelector()
            }

            fullExpression.getStrictParentOfType<KtUnaryExpression>()?.let { unaryExpr ->
                val operationToken = unaryExpr.operationToken as? KtSingleValueToken ?: return expression
                if (operationToken !in OperatorConventions.INCREMENT_OPERATIONS) return expression
                val operationName = OperatorConventions.getNameForOperationSymbol(operationToken)
                val originalValue = if (getter) fullExpression.createCall(psiFactory, newGetterName) else fullExpression
                val incDecValue = psiFactory.createExpressionByPattern("$0.$operationName()", originalValue)
                val parent = unaryExpr.parent
                val context = parent.parentsWithSelf.firstOrNull { it is KtBlockExpression || it is KtDeclarationContainer }
                if (context == parent || context == null) {
                    val newSetterCall = fullExpression.createCall(psiFactory, newSetterName, incDecValue)
                    return unaryExpr.replaced(newSetterCall).getQualifiedElementSelector()
                } else {
                    val anchor = parent.parentsWithSelf.firstOrNull { it.parent == context }
                    val validator = NewDeclarationNameValidator(
                        context,
                        anchor,
                        NewDeclarationNameValidator.Target.VARIABLES
                    )
                    val varName = KotlinNameSuggester.suggestNamesByExpressionAndType(
                        unaryExpr,
                        null,
                        unaryExpr.analyze(),
                        validator,
                        "p"
                    ).first()
                    val isPrefix = unaryExpr is KtPrefixExpression
                    val varInitializer = if (isPrefix) incDecValue else originalValue
                    val newVar = psiFactory.createDeclarationByPattern<KtProperty>("val $varName = $0", varInitializer)
                    val setterArgument = psiFactory.createExpression(if (isPrefix) varName else "$varName.$operationName()")
                    val newSetterCall = fullExpression.createCall(psiFactory, newSetterName, setterArgument)
                    val newLine = psiFactory.createNewLine()
                    context.addBefore(newVar, anchor)
                    context.addBefore(newLine, anchor)
                    context.addBefore(newSetterCall, anchor)
                    return unaryExpr.replaced(psiFactory.createExpression(varName))
                }
            }

            return expression
        }

        return renameByPropertyName(newName.identifier)
    }

    override val resolvesByNames: Collection<Name>
        get() = listOf(element.getReferencedNameAsName())
}

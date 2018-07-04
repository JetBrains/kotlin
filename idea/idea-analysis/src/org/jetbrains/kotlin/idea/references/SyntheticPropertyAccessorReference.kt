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

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
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
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.addIfNotNull

sealed class SyntheticPropertyAccessorReference(expression: KtNameReferenceExpression, private val getter: Boolean) :
        KtSimpleReference<KtNameReferenceExpression>(expression) {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val descriptors = super.getTargetDescriptors(context)
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

    override fun isReferenceTo(element: PsiElement?): Boolean {
        if (element !is PsiMethod || !isAccessorName(element.name)) return false
        if (!getter && expression.readWriteAccess(true) == ReferenceAccess.READ) return false
        return super.isReferenceTo(element)
    }

    private fun isAccessorName(name: String): Boolean {
        if (getter) {
            return name.startsWith("get") || name.startsWith("is")
        }
        return name.startsWith("set")
    }

    override fun getRangeInElement() = TextRange(0, expression.textLength)

    override fun canRename() = true

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

    override fun handleElementRename(newElementName: String?): PsiElement? {
        if (!Name.isValidIdentifier(newElementName!!)) return expression

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
                val operationToken = assignment.operationToken as? KtSingleValueToken
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

    class Getter(expression: KtNameReferenceExpression) : SyntheticPropertyAccessorReference(expression, true)
    class Setter(expression: KtNameReferenceExpression) : SyntheticPropertyAccessorReference(expression, false)
}

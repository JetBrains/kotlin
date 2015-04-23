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

package org.jetbrains.kotlin.resolve.validation

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorUtils

public class DeprecatedSymbolValidator : SymbolUsageValidator {
    private val JAVA_DEPRECATED = FqName(javaClass<Deprecated>().getName())

    override fun validateCall(targetDescriptor: CallableDescriptor, trace: BindingTrace, element: PsiElement) {
        val deprecated = targetDescriptor.getDeprecatedAnnotation()
        if (deprecated != null) {
            val (annotation, target) = deprecated
            trace.report(createDeprecationDiagnostic(element, target, annotation))
        }
        else if (targetDescriptor is PropertyDescriptor) {
            propertyGetterWorkaround(targetDescriptor, trace, element)
        }
    }

    override fun validateTypeUsage(targetDescriptor: ClassifierDescriptor, trace: BindingTrace, element: PsiElement) {
        // Do not check types in annotation entries to prevent cycles in resolve, rely on call message
        val annotationEntry = JetStubbedPsiUtil.getPsiOrStubParent(element, javaClass<JetAnnotationEntry>(), true)
        if (annotationEntry != null && annotationEntry.getCalleeExpression().getConstructorReferenceExpression() == element)
            return

        // Do not check types in calls to super constructor in extends list, rely on call message
        val superExpression = JetStubbedPsiUtil.getPsiOrStubParent(element, javaClass<JetDelegatorToSuperCall>(), true)
        if (superExpression != null && superExpression.getCalleeExpression().getConstructorReferenceExpression() == element)
            return

        val deprecated = targetDescriptor.getDeprecatedAnnotation()
        if (deprecated != null) {
            val (annotation, target) = deprecated
            trace.report(createDeprecationDiagnostic(element, target, annotation))
        }
    }

    private fun DeclarationDescriptor.getDeprecatedAnnotation(): Pair<AnnotationDescriptor, DeclarationDescriptor>? {
        val ownAnnotation = getDeclaredDeprecatedAnnotation()
        if (ownAnnotation != null)
            return ownAnnotation to this
        when (this) {
            is ConstructorDescriptor -> {
                val classDescriptor = getContainingDeclaration()
                val classAnnotation = classDescriptor.getDeclaredDeprecatedAnnotation()
                if (classAnnotation != null)
                    return classAnnotation to classDescriptor
            }
            is PropertyAccessorDescriptor -> {
                val propertyDescriptor = getContainingDeclaration()
                val propertyAnnotation = propertyDescriptor.getDeclaredDeprecatedAnnotation()
                if (propertyAnnotation != null)
                    return propertyAnnotation to propertyDescriptor
            }
        }
        return null
    }

    private fun DeclarationDescriptor.getDeclaredDeprecatedAnnotation(): AnnotationDescriptor? {
        return getAnnotations().findAnnotation(KotlinBuiltIns.FQ_NAMES.deprecated) ?: getAnnotations().findAnnotation(JAVA_DEPRECATED)
    }

    private fun createDeprecationDiagnostic(element: PsiElement, descriptor: DeclarationDescriptor, deprecated: AnnotationDescriptor): Diagnostic {
        val message = getMessageFromAnnotationDescriptor(deprecated)
        return if (message == null)
            Errors.DEPRECATED_SYMBOL.on(element, descriptor)
        else
            Errors.DEPRECATED_SYMBOL_WITH_MESSAGE.on(element, descriptor, message)
    }

    private fun getMessageFromAnnotationDescriptor(descriptor: AnnotationDescriptor): String? {
        val parameterName = Name.identifier("value")
        for ((parameterDescriptor, argument) in descriptor.getAllValueArguments()) {
            if (parameterDescriptor.getName() == parameterName) {
                val parameterValue = argument.getValue()
                if (parameterValue is String) {
                    return parameterValue
                }
                else
                    return null
            }
        }
        return null
    }

    private val PROPERTY_SET_OPERATIONS = TokenSet.create(JetTokens.EQ, JetTokens.PLUSEQ, JetTokens.MINUSEQ, JetTokens.MULTEQ, JetTokens.DIVEQ, JetTokens.PERCEQ, JetTokens.PLUSPLUS, JetTokens.MINUSMINUS)
    fun propertyGetterWorkaround(propertyDescriptor: PropertyDescriptor, trace: BindingTrace, expression: PsiElement) {
        // property getters do not come as callable yet, so we analyse surroundings to check for deprecation annotation on getter
        val binaryExpression = PsiTreeUtil.getParentOfType<JetBinaryExpression>(expression, javaClass<JetBinaryExpression>())
        if (binaryExpression != null) {
            val left = binaryExpression.getLeft()
            if (left == expression) {
                val operation = binaryExpression.getOperationToken()
                if (operation != null && operation in PROPERTY_SET_OPERATIONS)
                    return
            }

            val jetReferenceExpressions = PsiTreeUtil.getChildrenOfType<JetReferenceExpression>(left, javaClass<JetReferenceExpression>())
            if (jetReferenceExpressions != null) {
                for (expr in jetReferenceExpressions) {
                    if (expr == expression) {
                        val operation = binaryExpression.getOperationToken()
                        if (operation != null && operation in PROPERTY_SET_OPERATIONS)
                            return // skip binary set operations
                    }
                }
            }
        }

        val unaryExpression = PsiTreeUtil.getParentOfType(expression, javaClass<JetUnaryExpression>())
        if (unaryExpression != null) {
            val operation = unaryExpression.getOperationReference().getReferencedNameElementType()
            if (operation != null && operation in PROPERTY_SET_OPERATIONS)
                return // skip unary set operations

        }

        val callableExpression = PsiTreeUtil.getParentOfType(expression, javaClass<JetCallableReferenceExpression>())
        if (callableExpression != null && callableExpression.getCallableReference() == expression) {
            return // skip Type::property
        }

        propertyDescriptor.getGetter()?.let { validateCall(it, trace, expression) }
    }
}
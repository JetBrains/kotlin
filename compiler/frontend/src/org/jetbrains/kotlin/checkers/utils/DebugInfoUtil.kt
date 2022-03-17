/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.checkers.utils

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.util.slicedMap.WritableSlice
import java.util.HashMap

object DebugInfoUtil {
    private val MAY_BE_UNRESOLVED = TokenSet.create(KtTokens.IN_KEYWORD, KtTokens.NOT_IN)
    private val EXCLUDED = TokenSet.create(
        KtTokens.COLON,
        KtTokens.AS_KEYWORD,
        KtTokens.`AS_SAFE`,
        KtTokens.IS_KEYWORD,
        KtTokens.NOT_IS,
        KtTokens.OROR,
        KtTokens.ANDAND,
        KtTokens.EQ,
        KtTokens.EQEQEQ,
        KtTokens.EXCLEQEQEQ,
        KtTokens.ELVIS,
        KtTokens.EXCLEXCL
    )

    fun markDebugAnnotations(
        root: PsiElement,
        bindingContext: BindingContext,
        debugInfoReporter: DebugInfoReporter
    ) {
        val markedWithErrorElements: MutableMap<KtReferenceExpression, DiagnosticFactory<*>?> = HashMap()
        for (diagnostic in bindingContext.diagnostics) {
            val factory = diagnostic.factory
            if (Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS.contains(diagnostic.factory)) {
                markedWithErrorElements[diagnostic.psiElement as KtReferenceExpression] = factory
            } else if (factory === Errors.SUPER_IS_NOT_AN_EXPRESSION
                || factory === Errors.SUPER_NOT_AVAILABLE
            ) {
                val superExpression = diagnostic.psiElement as KtSuperExpression
                markedWithErrorElements[superExpression.instanceReference] = factory
            } else if (factory === Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND) {
                markedWithErrorElements[diagnostic.psiElement as KtSimpleNameExpression] = factory
            } else if (factory === Errors.UNSUPPORTED || factory === Errors.UNSUPPORTED_FEATURE) {
                for (reference in PsiTreeUtil.findChildrenOfType(
                    diagnostic.psiElement,
                    KtReferenceExpression::class.java
                )) {
                    markedWithErrorElements[reference] = factory
                }
            }
        }
        root.acceptChildren(object : KtTreeVisitorVoid() {
            override fun visitForExpression(expression: KtForExpression) {
                val range = expression.loopRange
                if (range != null) {
                    reportIfDynamicCall(range, range, BindingContext.LOOP_RANGE_ITERATOR_RESOLVED_CALL)
                    reportIfDynamicCall(range, range, BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL)
                    reportIfDynamicCall(range, range, BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL)
                }
                super.visitForExpression(expression)
            }

            override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
                for (entry in destructuringDeclaration.entries) {
                    reportIfDynamicCall(entry, entry, BindingContext.COMPONENT_RESOLVED_CALL)
                }
                super.visitDestructuringDeclaration(destructuringDeclaration)
            }

            override fun visitProperty(property: KtProperty) {
                val descriptor = bindingContext.get(BindingContext.VARIABLE, property)
                val delegate = property.delegate
                if (descriptor is PropertyDescriptor && delegate != null) {
                    reportIfDynamicCall(delegate, descriptor, BindingContext.PROVIDE_DELEGATE_RESOLVED_CALL)
                    reportIfDynamicCall(delegate, descriptor.getter, BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL)
                    reportIfDynamicCall(delegate, descriptor.setter, BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL)
                }
                super.visitProperty(property)
            }

            override fun visitThisExpression(expression: KtThisExpression) {
                val resolvedCall = expression.getResolvedCall(bindingContext)
                if (resolvedCall != null) {
                    reportIfDynamic(expression, resolvedCall.resultingDescriptor, debugInfoReporter)
                }
                super.visitThisExpression(expression)
            }

            override fun visitReferenceExpression(expression: KtReferenceExpression) {
                super.visitReferenceExpression(expression)
                if (!BindingContextUtils.isExpressionWithValidReference(expression, bindingContext)) {
                    return
                }
                var referencedNameElementType: IElementType? = null
                if (expression is KtSimpleNameExpression) {
                    val elementType = expression.getNode().elementType
                    if (elementType === KtNodeTypes.OPERATION_REFERENCE) {
                        referencedNameElementType = expression.getReferencedNameElementType()
                        if (EXCLUDED.contains(referencedNameElementType)) {
                            return
                        }
                    }
                    if (elementType === KtNodeTypes.LABEL ||
                        expression.getReferencedNameElementType() === KtTokens.THIS_KEYWORD
                    ) {
                        return
                    }
                }
                debugInfoReporter.preProcessReference(expression)
                var target: String? = null
                val declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, expression)
                if (declarationDescriptor != null) {
                    target = declarationDescriptor.toString()
                    reportIfDynamic(expression, declarationDescriptor, debugInfoReporter)
                }
                if (target == null) {
                    val labelTarget = bindingContext.get(BindingContext.LABEL_TARGET, expression)
                    if (labelTarget != null) {
                        target = labelTarget.text
                    }
                }
                if (target == null) {
                    val declarationDescriptors = bindingContext.get(BindingContext.AMBIGUOUS_REFERENCE_TARGET, expression)
                    if (declarationDescriptors != null) {
                        target = "[" + declarationDescriptors.size + " descriptors]"
                    }
                }
                if (target == null) {
                    val labelTargets = bindingContext.get(BindingContext.AMBIGUOUS_LABEL_TARGET, expression)
                    if (labelTargets != null) {
                        target = "[" + labelTargets.size + " elements]"
                    }
                }
                if (MAY_BE_UNRESOLVED.contains(referencedNameElementType) || (expression is KtNameReferenceExpression && expression.isPlaceholder)) {
                    return
                }
                val resolved = target != null
                var markedWithError = markedWithErrorElements.containsKey(expression)
                if (expression is KtArrayAccessExpression &&
                    markedWithErrorElements.containsKey(expression.arrayExpression)
                ) {
                    // if 'foo' in 'foo[i]' is unresolved it means 'foo[i]' is unresolved (otherwise 'foo[i]' is marked as 'missing unresolved')
                    markedWithError = true
                }
                val expressionType = bindingContext.getType(expression)
                val factory = markedWithErrorElements[expression]
                if (declarationDescriptor != null &&
                    (ErrorUtils.isError(declarationDescriptor) || ErrorUtils.containsErrorType(expressionType))
                ) {
                    if (factory !== Errors.EXPRESSION_EXPECTED_PACKAGE_FOUND) {
                        debugInfoReporter.reportElementWithErrorType(expression)
                    }
                }
                if (resolved && markedWithError) {
                    if (Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS.contains(factory)) {
                        debugInfoReporter.reportUnresolvedWithTarget(expression, target!!)
                    }
                } else if (!resolved && !markedWithError) {
                    debugInfoReporter.reportMissingUnresolved(expression)
                }
            }

            private fun <E : KtElement, K, D : CallableDescriptor?> reportIfDynamicCall(
                element: E,
                key: K,
                slice: WritableSlice<K, ResolvedCall<D>>
            ): Boolean {
                val resolvedCall = bindingContext[slice, key]
                return if (resolvedCall != null) {
                    reportIfDynamic(element, resolvedCall.resultingDescriptor, debugInfoReporter)
                } else false
            }
        })
    }

    private fun reportIfDynamic(
        element: KtElement,
        declarationDescriptor: DeclarationDescriptor?,
        debugInfoReporter: DebugInfoReporter
    ): Boolean {
        if (declarationDescriptor != null && declarationDescriptor.isDynamic()) {
            debugInfoReporter.reportDynamicCall(element, declarationDescriptor)
            return true
        }
        return false
    }

    abstract class DebugInfoReporter {
        fun preProcessReference(@Suppress("UNUSED_PARAMETER") expression: KtReferenceExpression) {
            // do nothing
        }

        abstract fun reportElementWithErrorType(expression: KtReferenceExpression)
        abstract fun reportMissingUnresolved(expression: KtReferenceExpression)
        abstract fun reportUnresolvedWithTarget(expression: KtReferenceExpression, target: String)
        open fun reportDynamicCall(element: KtElement, declarationDescriptor: DeclarationDescriptor) {}
    }
}
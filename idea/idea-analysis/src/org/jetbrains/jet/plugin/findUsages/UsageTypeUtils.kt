/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.findUsages

import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetForExpression
import org.jetbrains.jet.lang.psi.JetMultiDeclaration
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetReferenceExpression
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.psi.JetImportDirective
import org.jetbrains.jet.lang.psi.psiUtil.getParentByTypeAndBranch
import org.jetbrains.jet.lang.psi.JetCallableReferenceExpression
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.psiUtil.isAncestor
import org.jetbrains.jet.lang.psi.JetFunction
import org.jetbrains.jet.lang.psi.JetTypeParameter
import org.jetbrains.jet.lang.psi.JetTypeConstraint
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier
import org.jetbrains.jet.lang.psi.JetTypedef
import org.jetbrains.jet.lang.psi.JetTypeProjection
import org.jetbrains.jet.lang.psi.JetParameter
import org.jetbrains.jet.lang.psi.JetIsExpression
import org.jetbrains.jet.lang.psi.JetBinaryExpressionWithTypeRHS
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetSuperExpression
import org.jetbrains.jet.lang.psi.JetDelegatorByExpressionSpecifier
import org.jetbrains.jet.lang.psi.psiUtil.getParentByTypesAndPredicate
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.plugin.references.JetArrayAccessReference
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.references.JetInvokeFunctionReference
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor
import org.jetbrains.jet.lang.psi.JetAnnotationEntry
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetUnaryExpression
import org.jetbrains.jet.lang.psi.JetWhenConditionInRange
import org.jetbrains.jet.lang.psi.JetPackageDirective
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor
import com.intellij.psi.PsiPackage
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.plugin.findUsages.UsageTypeEnum.*

public object UsageTypeUtils {
    public fun getUsageType(element: PsiElement?): UsageTypeEnum? {
        when (element) {
            is JetForExpression -> return IMPLICIT_ITERATION
            is JetMultiDeclaration -> return READ
        }

        val refExpr = element?.getParentByType(javaClass<JetReferenceExpression>())
        if (refExpr == null) return null

        val context = AnalyzerFacadeWithCache.getContextForElement(refExpr)

        fun getCommonUsageType(): UsageTypeEnum? {
            return when {
                refExpr.getParentByType(javaClass<JetImportDirective>()) != null ->
                    CLASS_IMPORT
                refExpr.getParentByTypeAndBranch(javaClass<JetCallableReferenceExpression>()) { getCallableReference() } != null ->
                    CALLABLE_REFERENCE
                else -> null
            }
        }

        fun getClassUsageType(): UsageTypeEnum? {
            val property = refExpr.getParentByType(javaClass<JetProperty>())
            if (property != null) {
                when {
                    property.getTypeReference().isAncestor(refExpr) ->
                        return if (property.isLocal()) CLASS_LOCAL_VAR_DECLARATION else NON_LOCAL_PROPERTY_TYPE

                    property.getReceiverTypeReference().isAncestor(refExpr) ->
                        return EXTENSION_RECEIVER_TYPE
                }
            }

            val function = refExpr.getParentByType(javaClass<JetFunction>())
            if (function != null) {
                when {
                    function.getTypeReference().isAncestor(refExpr) ->
                        return FUNCTION_RETURN_TYPE
                    function.getReceiverTypeReference().isAncestor(refExpr) ->
                        return EXTENSION_RECEIVER_TYPE
                }
            }

            return when {
                refExpr.getParentByTypeAndBranch(javaClass<JetTypeParameter>()) { getExtendsBound() } != null
                || refExpr.getParentByTypeAndBranch(javaClass<JetTypeConstraint>()) { getBoundTypeReference() } != null ->
                    TYPE_CONSTRAINT

                refExpr is JetDelegationSpecifier
                || refExpr.getParentByTypeAndBranch(javaClass<JetDelegationSpecifier>()) { getTypeReference() } != null ->
                    SUPER_TYPE

                refExpr.getParentByTypeAndBranch(javaClass<JetTypedef>()) { getTypeReference() } != null ->
                    TYPE_DEFINITION

                refExpr.getParentByType(javaClass<JetTypeProjection>()) != null ->
                    TYPE_PARAMETER

                refExpr.getParentByTypeAndBranch(javaClass<JetParameter>()) { getTypeReference() } != null ->
                    VALUE_PARAMETER_TYPE

                refExpr.getParentByTypeAndBranch(javaClass<JetIsExpression>()) { getTypeReference() } != null ->
                    IS

                with(refExpr.getParentByTypeAndBranch(javaClass<JetBinaryExpressionWithTypeRHS>()) { getRight() }) {
                    val opType = this?.getOperationReference()?.getReferencedNameElementType()
                    opType == JetTokens.AS_KEYWORD || opType == JetTokens.AS_SAFE
                } ->
                    CLASS_CAST_TO

                with(refExpr.getParentByType(javaClass<JetDotQualifiedExpression>())) {
                    if (this == null) false
                    else if (getReceiverExpression() == refExpr) true
                    else
                        getSelectorExpression() == refExpr
                        && getParentByTypeAndBranch(javaClass<JetDotQualifiedExpression>(), true) { getReceiverExpression() } != null
                } ->
                    CLASS_OBJECT_ACCESS

                refExpr.getParentByTypeAndBranch(javaClass<JetSuperExpression>()) { getSuperTypeQualifier() } != null ->
                    SUPER_TYPE_QUALIFIER

                else -> null
            }
        }

        fun getVariableUsageType(): UsageTypeEnum? {
            if (refExpr.getParentByTypeAndBranch(javaClass<JetDelegatorByExpressionSpecifier>()) { getDelegateExpression() } != null) {
                return DELEGATE
            }

            val dotQualifiedExpression = refExpr.getParentByType(javaClass<JetDotQualifiedExpression>())

            if (dotQualifiedExpression != null) {
                val parent = dotQualifiedExpression.getParent()
                when {
                    dotQualifiedExpression.getReceiverExpression().isAncestor(refExpr) ->
                        return RECEIVER

                    parent is JetDotQualifiedExpression && parent.getReceiverExpression().isAncestor(refExpr) ->
                        return RECEIVER
                }
            }

            return when {
                (refExpr.getParentByTypesAndPredicate(false, javaClass<JetBinaryExpression>()) { JetPsiUtil.isAssignment(it) })
                        ?.getLeft().isAncestor(refExpr) ->
                    WRITE

                refExpr.getParentByType(javaClass<JetSimpleNameExpression>()) != null ->
                    READ

                else -> null
            }
        }

        fun getFunctionUsageType(descriptor: FunctionDescriptor): UsageTypeEnum? {
            val ref = refExpr.getReference()
            when (ref) {
                is JetArrayAccessReference -> {
                    return when {
                        context[BindingContext.INDEXED_LVALUE_GET, refExpr] != null -> IMPLICIT_GET
                        context[BindingContext.INDEXED_LVALUE_SET, refExpr] != null -> IMPLICIT_SET
                        else -> null
                    }
                }
                is JetInvokeFunctionReference -> return IMPLICIT_INVOKE
            }

            return when {
                refExpr.getParentByTypeAndBranch(javaClass<JetDelegationSpecifier>()) { getTypeReference() } != null ->
                    SUPER_TYPE

                descriptor is ConstructorDescriptor
                && refExpr.getParentByTypeAndBranch(javaClass<JetAnnotationEntry>()) { getTypeReference() } != null ->
                    ANNOTATION

                with(refExpr.getParentByTypeAndBranch(javaClass<JetCallExpression>()) { getCalleeExpression() }) {
                    this?.getCalleeExpression() is JetSimpleNameExpression
                } ->
                    if (descriptor is ConstructorDescriptor) CLASS_NEW_OPERATOR else FUNCTION_CALL

                refExpr.getParentByTypeAndBranch(javaClass<JetBinaryExpression>()) { getOperationReference() } != null,
                refExpr.getParentByTypeAndBranch(javaClass<JetUnaryExpression>()) { getOperationReference() } != null,
                refExpr.getParentByTypeAndBranch(javaClass<JetWhenConditionInRange>()) { getOperationReference() } != null ->
                    FUNCTION_CALL

                else -> null
            }
        }

        fun getPackageUsageType(): UsageTypeEnum? {
            return when {
                refExpr.getParentByType(javaClass<JetPackageDirective>()) != null -> PACKAGE_DIRECTIVE
                refExpr.getParentByType(javaClass<JetQualifiedExpression>()) != null -> PACKAGE_MEMBER_ACCESS
                else -> getClassUsageType()
            }
        }

        val usageType = getCommonUsageType()
        if (usageType != null) return usageType

        val descriptor = context[BindingContext.REFERENCE_TARGET, refExpr]

        return when (descriptor) {
            is ClassifierDescriptor -> when ((descriptor as? ClassDescriptor)?.getKind()) {
            // Treat object accesses as variables to simulate the old behaviour (when variables were created for objects)
                ClassKind.OBJECT, ClassKind.ENUM_ENTRY -> getVariableUsageType()
                else -> getClassUsageType()
            }
            is PackageViewDescriptor -> {
                if (refExpr.getReference()?.resolve() is PsiPackage) getPackageUsageType() else getClassUsageType()
            }
            is VariableDescriptor -> getVariableUsageType()
            is FunctionDescriptor -> getFunctionUsageType(descriptor)
            else -> null
        }
    }
}

enum class UsageTypeEnum {
    TYPE_CONSTRAINT
    VALUE_PARAMETER_TYPE
    NON_LOCAL_PROPERTY_TYPE
    FUNCTION_RETURN_TYPE
    SUPER_TYPE
    TYPE_DEFINITION
    IS
    CLASS_OBJECT_ACCESS
    EXTENSION_RECEIVER_TYPE
    SUPER_TYPE_QUALIFIER

    FUNCTION_CALL
    IMPLICIT_GET
    IMPLICIT_SET
    IMPLICIT_INVOKE
    IMPLICIT_ITERATION

    RECEIVER
    DELEGATE

    PACKAGE_DIRECTIVE
    PACKAGE_MEMBER_ACCESS

    CALLABLE_REFERENCE

    READ
    WRITE
    CLASS_IMPORT
    CLASS_LOCAL_VAR_DECLARATION
    TYPE_PARAMETER
    CLASS_CAST_TO
    ANNOTATION
    CLASS_NEW_OPERATOR
}
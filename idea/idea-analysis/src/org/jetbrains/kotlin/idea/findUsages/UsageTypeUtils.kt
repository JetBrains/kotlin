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

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.findUsages.UsageTypeEnum.*
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils

public object UsageTypeUtils {
    public fun getUsageType(element: PsiElement?): UsageTypeEnum? {
        when (element) {
            is KtForExpression -> return IMPLICIT_ITERATION
            is KtMultiDeclaration -> return READ
            is KtPropertyDelegate -> return PROPERTY_DELEGATION
            is KtStringTemplateExpression -> return USAGE_IN_STRING_LITERAL
        }

        val refExpr = element?.getNonStrictParentOfType<KtReferenceExpression>()
        if (refExpr == null) return null

        val context = refExpr.analyze()

        fun getCommonUsageType(): UsageTypeEnum? {
            return when {
                refExpr.getNonStrictParentOfType<KtImportDirective>() != null ->
                    CLASS_IMPORT
                refExpr.getParentOfTypeAndBranch<KtCallableReferenceExpression>(){ getCallableReference() } != null ->
                    CALLABLE_REFERENCE
                else -> null
            }
        }

        fun getClassUsageType(): UsageTypeEnum? {
            if (refExpr.getNonStrictParentOfType<KtTypeProjection>() != null) return TYPE_PARAMETER

            val property = refExpr.getNonStrictParentOfType<KtProperty>()
            if (property != null) {
                when {
                    property.getTypeReference().isAncestor(refExpr) ->
                        return if (property.isLocal()) CLASS_LOCAL_VAR_DECLARATION else NON_LOCAL_PROPERTY_TYPE

                    property.getReceiverTypeReference().isAncestor(refExpr) ->
                        return EXTENSION_RECEIVER_TYPE
                }
            }

            val function = refExpr.getNonStrictParentOfType<KtFunction>()
            if (function != null) {
                when {
                    function.getTypeReference().isAncestor(refExpr) ->
                        return FUNCTION_RETURN_TYPE
                    function.getReceiverTypeReference().isAncestor(refExpr) ->
                        return EXTENSION_RECEIVER_TYPE
                }
            }

            return when {
                refExpr.getParentOfTypeAndBranch<KtTypeParameter>(){ getExtendsBound() } != null
                || refExpr.getParentOfTypeAndBranch<KtTypeConstraint>(){ getBoundTypeReference() } != null ->
                    TYPE_CONSTRAINT

                refExpr is KtDelegationSpecifier
                || refExpr.getParentOfTypeAndBranch<KtDelegationSpecifier>(){ getTypeReference() } != null ->
                    SUPER_TYPE

                refExpr.getParentOfTypeAndBranch<KtTypedef>(){ getTypeReference() } != null ->
                    TYPE_DEFINITION

                refExpr.getParentOfTypeAndBranch<KtParameter>(){ getTypeReference() } != null ->
                    VALUE_PARAMETER_TYPE

                refExpr.getParentOfTypeAndBranch<KtIsExpression>(){ getTypeReference() } != null
                || refExpr.getParentOfTypeAndBranch<KtWhenConditionIsPattern>(){ getTypeReference() } != null ->
                    IS

                with(refExpr.getParentOfTypeAndBranch<KtBinaryExpressionWithTypeRHS>(){ getRight() }) {
                    val opType = this?.getOperationReference()?.getReferencedNameElementType()
                    opType == KtTokens.AS_KEYWORD || opType == KtTokens.AS_SAFE
                } ->
                    CLASS_CAST_TO

                with(refExpr.getNonStrictParentOfType<KtDotQualifiedExpression>()) {
                    if (this == null) false
                    else if (getReceiverExpression() == refExpr) true
                    else
                        getSelectorExpression() == refExpr
                        && getParentOfTypeAndBranch<KtDotQualifiedExpression>(strict = true) { getReceiverExpression() } != null
                } ->
                    CLASS_OBJECT_ACCESS

                refExpr.getParentOfTypeAndBranch<KtSuperExpression>(){ getSuperTypeQualifier() } != null ->
                    SUPER_TYPE_QUALIFIER

                else -> null
            }
        }

        fun getVariableUsageType(): UsageTypeEnum? {
            if (refExpr.getParentOfTypeAndBranch<KtDelegatorByExpressionSpecifier>(){ getDelegateExpression() } != null) {
                return DELEGATE
            }

            if (refExpr.getParent() is KtValueArgumentName) return NAMED_ARGUMENT

            val dotQualifiedExpression = refExpr.getNonStrictParentOfType<KtDotQualifiedExpression>()

            if (dotQualifiedExpression != null) {
                val parent = dotQualifiedExpression.getParent()
                when {
                    dotQualifiedExpression.getReceiverExpression().isAncestor(refExpr) ->
                        return RECEIVER

                    parent is KtDotQualifiedExpression && parent.getReceiverExpression().isAncestor(refExpr) ->
                        return RECEIVER
                }
            }

            return when (refExpr.readWriteAccess(useResolveForReadWrite = true)) {
                ReferenceAccess.READ -> READ
                ReferenceAccess.WRITE, ReferenceAccess.READ_WRITE -> WRITE
            }
        }

        fun getFunctionUsageType(descriptor: FunctionDescriptor): UsageTypeEnum? {
            when (refExpr.mainReference) {
                is KtArrayAccessReference -> {
                    return when {
                        context[BindingContext.INDEXED_LVALUE_GET, refExpr] != null -> IMPLICIT_GET
                        context[BindingContext.INDEXED_LVALUE_SET, refExpr] != null -> IMPLICIT_SET
                        else -> null
                    }
                }
                is KtInvokeFunctionReference -> return IMPLICIT_INVOKE
            }

            return when {
                refExpr.getParentOfTypeAndBranch<KtDelegationSpecifier>(){ getTypeReference() } != null ->
                    SUPER_TYPE

                descriptor is ConstructorDescriptor
                && refExpr.getParentOfTypeAndBranch<KtAnnotationEntry>(){ getTypeReference() } != null ->
                    ANNOTATION

                with(refExpr.getParentOfTypeAndBranch<KtCallExpression>(){ getCalleeExpression() }) {
                    this?.getCalleeExpression() is KtSimpleNameExpression
                } ->
                    if (descriptor is ConstructorDescriptor) CLASS_NEW_OPERATOR else FUNCTION_CALL

                refExpr.getParentOfTypeAndBranch<KtBinaryExpression>(){ getOperationReference() } != null,
                refExpr.getParentOfTypeAndBranch<KtUnaryExpression>(){ getOperationReference() } != null,
                refExpr.getParentOfTypeAndBranch<KtWhenConditionInRange>(){ getOperationReference() } != null ->
                    FUNCTION_CALL

                else -> null
            }
        }

        fun getPackageUsageType(): UsageTypeEnum? {
            return when {
                refExpr.getNonStrictParentOfType<KtPackageDirective>() != null -> PACKAGE_DIRECTIVE
                refExpr.getNonStrictParentOfType<KtQualifiedExpression>() != null -> PACKAGE_MEMBER_ACCESS
                else -> getClassUsageType()
            }
        }

        val usageType = getCommonUsageType()
        if (usageType != null) return usageType

        val descriptor = context[BindingContext.REFERENCE_TARGET, refExpr]

        return when (descriptor) {
            is ClassifierDescriptor -> when {
            // Treat object accesses as variables to simulate the old behaviour (when variables were created for objects)
                DescriptorUtils.isNonCompanionObject(descriptor), DescriptorUtils.isEnumEntry(descriptor) -> getVariableUsageType()
                DescriptorUtils.isCompanionObject(descriptor) -> COMPANION_OBJECT_ACCESS
                else -> getClassUsageType()
            }
            is PackageViewDescriptor -> {
                if (refExpr.mainReference.resolve() is PsiPackage) getPackageUsageType() else getClassUsageType()
            }
            is VariableDescriptor -> getVariableUsageType()
            is FunctionDescriptor -> getFunctionUsageType(descriptor)
            else -> null
        }
    }
}

enum class UsageTypeEnum {
    TYPE_CONSTRAINT,
    VALUE_PARAMETER_TYPE,
    NON_LOCAL_PROPERTY_TYPE,
    FUNCTION_RETURN_TYPE,
    SUPER_TYPE,
    TYPE_DEFINITION,
    IS,
    CLASS_OBJECT_ACCESS,
    COMPANION_OBJECT_ACCESS,
    EXTENSION_RECEIVER_TYPE,
    SUPER_TYPE_QUALIFIER,

    FUNCTION_CALL,
    IMPLICIT_GET,
    IMPLICIT_SET,
    IMPLICIT_INVOKE,
    IMPLICIT_ITERATION,
    PROPERTY_DELEGATION,

    RECEIVER,
    DELEGATE,

    PACKAGE_DIRECTIVE,
    PACKAGE_MEMBER_ACCESS,

    CALLABLE_REFERENCE,

    READ,
    WRITE,
    CLASS_IMPORT,
    CLASS_LOCAL_VAR_DECLARATION,
    TYPE_PARAMETER,
    CLASS_CAST_TO,
    ANNOTATION,
    CLASS_NEW_OPERATOR,
    NAMED_ARGUMENT,

    USAGE_IN_STRING_LITERAL
}

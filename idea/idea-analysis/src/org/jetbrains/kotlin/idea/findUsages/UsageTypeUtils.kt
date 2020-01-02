/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

object UsageTypeUtils {
    fun getUsageType(element: PsiElement?): UsageTypeEnum? {
        when (element) {
            is KtForExpression -> return IMPLICIT_ITERATION
            is KtDestructuringDeclarationEntry -> return READ
            is KtPropertyDelegate -> return PROPERTY_DELEGATION
            is KtStringTemplateExpression -> return USAGE_IN_STRING_LITERAL
        }

        val refExpr = element?.getNonStrictParentOfType<KtReferenceExpression>() ?: return null

        val context = refExpr.analyze(BodyResolveMode.PARTIAL)

        fun getCommonUsageType(): UsageTypeEnum? = when {
            refExpr.getNonStrictParentOfType<KtImportDirective>() != null -> CLASS_IMPORT
            refExpr.getParentOfTypeAndBranch<KtCallableReferenceExpression>() { callableReference } != null -> CALLABLE_REFERENCE
            else -> null
        }

        fun getClassUsageType(): UsageTypeEnum? {
            if (refExpr.getNonStrictParentOfType<KtTypeProjection>() != null) return TYPE_PARAMETER

            val property = refExpr.getNonStrictParentOfType<KtProperty>()
            if (property != null) {
                when {
                    property.typeReference.isAncestor(refExpr) ->
                        return if (property.isLocal) CLASS_LOCAL_VAR_DECLARATION else NON_LOCAL_PROPERTY_TYPE

                    property.receiverTypeReference.isAncestor(refExpr) ->
                        return EXTENSION_RECEIVER_TYPE
                }
            }

            val function = refExpr.getNonStrictParentOfType<KtFunction>()
            if (function != null) {
                when {
                    function.typeReference.isAncestor(refExpr) ->
                        return FUNCTION_RETURN_TYPE
                    function.receiverTypeReference.isAncestor(refExpr) ->
                        return EXTENSION_RECEIVER_TYPE
                }
            }

            return when {
                refExpr.getParentOfTypeAndBranch<KtTypeParameter>() { extendsBound } != null || refExpr.getParentOfTypeAndBranch<KtTypeConstraint>() { boundTypeReference } != null -> TYPE_CONSTRAINT

                refExpr is KtSuperTypeListEntry || refExpr.getParentOfTypeAndBranch<KtSuperTypeListEntry>() { typeReference } != null -> SUPER_TYPE

                refExpr.getParentOfTypeAndBranch<KtParameter>() { typeReference } != null -> VALUE_PARAMETER_TYPE

                refExpr.getParentOfTypeAndBranch<KtIsExpression>() { typeReference } != null || refExpr.getParentOfTypeAndBranch<KtWhenConditionIsPattern>() { typeReference } != null -> IS

                with(refExpr.getParentOfTypeAndBranch<KtBinaryExpressionWithTypeRHS>() { right }) {
                    val opType = this?.operationReference?.getReferencedNameElementType()
                    opType == KtTokens.AS_KEYWORD || opType == KtTokens.AS_SAFE
                } -> CLASS_CAST_TO

                with(refExpr.getNonStrictParentOfType<KtDotQualifiedExpression>()) {
                    when {
                        this == null -> {
                            false
                        }
                        receiverExpression == refExpr -> {
                            true
                        }
                        else -> {
                            selectorExpression == refExpr
                                    && getParentOfTypeAndBranch<KtDotQualifiedExpression>(strict = true) { receiverExpression } != null
                        }
                    }
                } -> CLASS_OBJECT_ACCESS

                refExpr.getParentOfTypeAndBranch<KtSuperExpression>() { superTypeQualifier } != null -> SUPER_TYPE_QUALIFIER

                refExpr.getParentOfTypeAndBranch<KtTypeAlias> { getTypeReference() } != null -> TYPE_ALIAS

                else -> null
            }
        }

        fun getVariableUsageType(): UsageTypeEnum? {
            if (refExpr.getParentOfTypeAndBranch<KtDelegatedSuperTypeEntry>() { delegateExpression } != null) return DELEGATE

            if (refExpr.parent is KtValueArgumentName) return NAMED_ARGUMENT

            val dotQualifiedExpression = refExpr.getNonStrictParentOfType<KtDotQualifiedExpression>()

            if (dotQualifiedExpression != null) {
                val parent = dotQualifiedExpression.parent
                when {
                    dotQualifiedExpression.receiverExpression.isAncestor(refExpr) ->
                        return RECEIVER

                    parent is KtDotQualifiedExpression && parent.receiverExpression.isAncestor(refExpr) ->
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
                refExpr.getParentOfTypeAndBranch<KtSuperTypeListEntry>() { typeReference } != null -> SUPER_TYPE

                descriptor is ConstructorDescriptor && refExpr.getParentOfTypeAndBranch<KtAnnotationEntry>() { typeReference } != null -> ANNOTATION

                with(refExpr.getParentOfTypeAndBranch<KtCallExpression>() { calleeExpression }) {
                    this?.calleeExpression is KtSimpleNameExpression
                } -> if (descriptor is ConstructorDescriptor) CLASS_NEW_OPERATOR else FUNCTION_CALL

                refExpr.getParentOfTypeAndBranch<KtBinaryExpression>() { operationReference } != null || refExpr.getParentOfTypeAndBranch<KtUnaryExpression>() { operationReference } != null || refExpr.getParentOfTypeAndBranch<KtWhenConditionInRange>() { operationReference } != null -> FUNCTION_CALL

                else -> null
            }
        }

        fun getPackageUsageType(): UsageTypeEnum? = when {
            refExpr.getNonStrictParentOfType<KtPackageDirective>() != null -> PACKAGE_DIRECTIVE
            refExpr.getNonStrictParentOfType<KtQualifiedExpression>() != null -> PACKAGE_MEMBER_ACCESS
            else -> getClassUsageType()
        }

        val usageType = getCommonUsageType()
        if (usageType != null) return usageType

        return when (val descriptor = context[BindingContext.REFERENCE_TARGET, refExpr]) {
            is ClassifierDescriptor -> when {
                // Treat object accesses as variables to simulate the old behaviour (when variables were created for objects)
                DescriptorUtils.isNonCompanionObject(descriptor) || DescriptorUtils.isEnumEntry(descriptor) -> getVariableUsageType()
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
    IS,
    CLASS_OBJECT_ACCESS,
    COMPANION_OBJECT_ACCESS,
    EXTENSION_RECEIVER_TYPE,
    SUPER_TYPE_QUALIFIER,
    TYPE_ALIAS,

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

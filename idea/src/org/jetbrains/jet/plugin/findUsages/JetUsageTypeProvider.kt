/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.intellij.usages.UsageTarget
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProviderEx
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.psi.psiUtil.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import com.intellij.psi.PsiPackage
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.references.JetArrayAccessReference
import org.jetbrains.jet.plugin.references.JetInvokeFunctionReference

public object JetUsageTypeProvider : UsageTypeProviderEx {
    public override fun getUsageType(element: PsiElement?): UsageType? {
        return getUsageType(element, UsageTarget.EMPTY_ARRAY)
    }

    public override fun getUsageType(element: PsiElement?, targets: Array<out UsageTarget>): UsageType? {
        when (element) {
            is JetForExpression -> return JetUsageTypes.IMPLICIT_ITERATION
            is JetMultiDeclaration -> return UsageType.READ
        }

        val refExpr = element?.getParentByType(javaClass<JetReferenceExpression>())
        if (refExpr == null) return null

        val context = AnalyzerFacadeWithCache.getContextForElement(refExpr)

        fun getCommonUsageType(): UsageType? {
            return when {
                refExpr.getParentByType(javaClass<JetImportDirective>()) != null ->
                    UsageType.CLASS_IMPORT
                refExpr.getParentByTypeAndBranch(javaClass<JetCallableReferenceExpression>()) { getCallableReference() } != null ->
                    JetUsageTypes.CALLABLE_REFERENCE
                else -> null
            }
        }

        fun getClassUsageType(): UsageType? {
            val property = refExpr.getParentByType(javaClass<JetProperty>())
            if (property != null) {
                when {
                    property.getTypeRef().isAncestor(refExpr) ->
                        return if (property.isLocal()) UsageType.CLASS_LOCAL_VAR_DECLARATION else JetUsageTypes.NON_LOCAL_PROPERTY_TYPE

                    property.getReceiverTypeRef().isAncestor(refExpr) ->
                        return JetUsageTypes.EXTENSION_RECEIVER_TYPE
                }
            }

            val function = refExpr.getParentByType(javaClass<JetFunction>())
            if (function != null) {
                when {
                    function.getReturnTypeRef().isAncestor(refExpr) ->
                        return JetUsageTypes.FUNCTION_RETURN_TYPE
                    function.getReceiverTypeRef().isAncestor(refExpr) ->
                        return JetUsageTypes.EXTENSION_RECEIVER_TYPE
                }
            }

            return when {
                refExpr.getParentByTypeAndBranch(javaClass<JetTypeParameter>()) { getExtendsBound() } != null
                || refExpr.getParentByTypeAndBranch(javaClass<JetTypeConstraint>()) { getBoundTypeReference() } != null ->
                    JetUsageTypes.TYPE_CONSTRAINT

                refExpr is JetDelegationSpecifier
                || refExpr.getParentByTypeAndBranch(javaClass<JetDelegationSpecifier>()) { getTypeReference() } != null ->
                    JetUsageTypes.SUPER_TYPE

                refExpr.getParentByTypeAndBranch(javaClass<JetTypedef>()) { getTypeReference() } != null ->
                    JetUsageTypes.TYPE_DEFINITION

                refExpr.getParentByType(javaClass<JetTypeProjection>()) != null ->
                    UsageType.TYPE_PARAMETER

                refExpr.getParentByTypeAndBranch(javaClass<JetParameter>()) { getTypeReference() } != null ->
                    JetUsageTypes.VALUE_PARAMETER_TYPE

                refExpr.getParentByTypeAndBranch(javaClass<JetIsExpression>()) { getTypeRef() } != null ->
                    JetUsageTypes.IS

                with(refExpr.getParentByTypeAndBranch(javaClass<JetBinaryExpressionWithTypeRHS>()) { getRight() }) {
                    val opType = this?.getOperationReference()?.getReferencedNameElementType()
                    opType == JetTokens.AS_KEYWORD || opType == JetTokens.AS_SAFE
                } ->
                    UsageType.CLASS_CAST_TO

                with(refExpr.getParentByType(javaClass<JetDotQualifiedExpression>())) {
                    if (this == null) false
                    else if (getReceiverExpression() == refExpr) true
                    else
                        getSelectorExpression() == refExpr
                            && getParentByTypeAndBranch(javaClass<JetDotQualifiedExpression>(), true) { getReceiverExpression() } != null
                } ->
                    JetUsageTypes.CLASS_OBJECT_ACCESS

                refExpr.getParentByTypeAndBranch(javaClass<JetSuperExpression>()) { getSuperTypeQualifier() } != null ->
                    JetUsageTypes.SUPER_TYPE_QUALIFIER

                else -> null
            }
        }

        fun getVariableUsageType(): UsageType? {
            if (refExpr.getParentByTypeAndBranch(javaClass<JetDelegatorByExpressionSpecifier>()) { getDelegateExpression() } != null) {
                return JetUsageTypes.DELEGATE
            }

            val dotQualifiedExpression = refExpr.getParentByType(javaClass<JetDotQualifiedExpression>())

            if (dotQualifiedExpression != null) {
                val parent = dotQualifiedExpression.getParent()
                when {
                    dotQualifiedExpression.getReceiverExpression().isAncestor(refExpr) ->
                        return JetUsageTypes.RECEIVER

                    parent is JetDotQualifiedExpression && parent.getReceiverExpression().isAncestor(refExpr) ->
                        return JetUsageTypes.RECEIVER
                }
            }

            return when {
                (refExpr.getParentByTypesAndPredicate(false, javaClass<JetBinaryExpression>()) { JetPsiUtil.isAssignment(it) })
                        ?.getLeft().isAncestor(refExpr) ->
                    UsageType.WRITE

                refExpr.getParentByType(javaClass<JetSimpleNameExpression>()) != null ->
                    UsageType.READ

                else -> null
            }
        }

        fun getFunctionUsageType(descriptor: FunctionDescriptor): UsageType? {
            val ref = refExpr.getReference()
            when (ref) {
                is JetArrayAccessReference -> {
                    return when {
                        context[BindingContext.INDEXED_LVALUE_GET, refExpr] != null -> JetUsageTypes.IMPLICIT_GET
                        context[BindingContext.INDEXED_LVALUE_SET, refExpr] != null -> JetUsageTypes.IMPLICIT_SET
                        else -> null
                    }
                }
                is JetInvokeFunctionReference -> return JetUsageTypes.IMPLICIT_INVOKE
            }

            return when {
                refExpr.getParentByTypeAndBranch(javaClass<JetDelegationSpecifier>()) { getTypeReference() } != null ->
                    JetUsageTypes.SUPER_TYPE

                descriptor is ConstructorDescriptor
                && refExpr.getParentByTypeAndBranch(javaClass<JetAnnotationEntry>()) { getTypeReference() } != null ->
                    UsageType.ANNOTATION

                with(refExpr.getParentByTypeAndBranch(javaClass<JetCallExpression>()) { getCalleeExpression() }) {
                    this?.getCalleeExpression() is JetSimpleNameExpression
                } ->
                    if (descriptor is ConstructorDescriptor) UsageType.CLASS_NEW_OPERATOR else JetUsageTypes.FUNCTION_CALL

                refExpr.getParentByTypeAndBranch(javaClass<JetBinaryExpression>()) { getOperationReference() } != null,
                refExpr.getParentByTypeAndBranch(javaClass<JetUnaryExpression>()) { getOperationReference() } != null,
                refExpr.getParentByTypeAndBranch(javaClass<JetWhenConditionInRange>()) { getOperationReference() } != null ->
                    JetUsageTypes.FUNCTION_CALL

                else -> null
            }
        }

        fun getPackageUsageType(): UsageType? {
            return when {
                refExpr.getParentByType(javaClass<JetPackageDirective>()) != null -> JetUsageTypes.PACKAGE_DIRECTIVE
                refExpr.getParentByType(javaClass<JetQualifiedExpression>()) != null -> JetUsageTypes.PACKAGE_MEMBER_ACCESS
                else -> getClassUsageType()
            }
        }

        val usageType = getCommonUsageType()
        if (usageType != null) return usageType

        val descriptor = context[BindingContext.REFERENCE_TARGET, refExpr]

        return when (descriptor) {
            is ClassifierDescriptor -> if (DescriptorUtils.isSingleton(descriptor)) {
                // Treat object accesses as variables to simulate the old behaviour (when variables were created for objects)
                getVariableUsageType()
            } else {
                getClassUsageType()
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

object JetUsageTypes {
    // types
    val TYPE_CONSTRAINT = UsageType(JetBundle.message("usageType.type.constraint"))
    val VALUE_PARAMETER_TYPE = UsageType(JetBundle.message("usageType.value.parameter.type"))
    val NON_LOCAL_PROPERTY_TYPE = UsageType(JetBundle.message("usageType.nonLocal.property.type"))
    val FUNCTION_RETURN_TYPE = UsageType(JetBundle.message("usageType.function.return.type"))
    val SUPER_TYPE = UsageType(JetBundle.message("usageType.superType"))
    val TYPE_DEFINITION = UsageType(JetBundle.message("usageType.type.definition"))
    val IS = UsageType(JetBundle.message("usageType.is"))
    val CLASS_OBJECT_ACCESS = UsageType(JetBundle.message("usageType.class.object"))
    val EXTENSION_RECEIVER_TYPE = UsageType(JetBundle.message("usageType.extension.receiver.type"))
    val SUPER_TYPE_QUALIFIER = UsageType(JetBundle.message("usageType.super.type.qualifier"))

    // functions
    val FUNCTION_CALL = UsageType(JetBundle.message("usageType.function.call"))
    val IMPLICIT_GET = UsageType(JetBundle.message("usageType.implicit.get"))
    val IMPLICIT_SET = UsageType(JetBundle.message("usageType.implicit.set"))
    val IMPLICIT_INVOKE = UsageType(JetBundle.message("usageType.implicit.invoke"))
    val IMPLICIT_ITERATION = UsageType(JetBundle.message("usageType.implicit.iteration"))

    // values
    val RECEIVER = UsageType(JetBundle.message("usageType.receiver"))
    val DELEGATE = UsageType(JetBundle.message("usageType.delegate"))

    // packages
    val PACKAGE_DIRECTIVE = UsageType(JetBundle.message("usageType.packageDirective"))
    val PACKAGE_MEMBER_ACCESS = UsageType(JetBundle.message("usageType.packageMemberAccess"))

    // common usage types
    val CALLABLE_REFERENCE = UsageType(JetBundle.message("usageType.callable.reference"))
}
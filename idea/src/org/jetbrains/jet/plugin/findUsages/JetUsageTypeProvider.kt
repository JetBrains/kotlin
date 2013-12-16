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
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usages.UsageTarget
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProviderEx
import jet.Function1
import org.jetbrains.annotations.Nullable
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.psi.psiUtil.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaNamespaceDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorUtils

public object JetUsageTypeProvider : UsageTypeProviderEx {
    public override fun getUsageType(element: PsiElement?): UsageType? {
        return getUsageType(element, UsageTarget.EMPTY_ARRAY)
    }

    public override fun getUsageType(element: PsiElement?, targets: Array<out UsageTarget>): UsageType? {
        val simpleName = element?.getParentByType(javaClass<JetSimpleNameExpression>())
        if (simpleName == null) return null

        fun getCommonUsageType(): UsageType? {
            return when {
                simpleName.getParentByType(javaClass<JetImportDirective>()) != null ->
                    UsageType.CLASS_IMPORT
                simpleName.getParentByTypeAndBranch(javaClass<JetCallableReferenceExpression>()) { getCallableReference() } != null ->
                    JetUsageTypes.CALLABLE_REFERENCE
                else -> null
            }
        }

        fun getClassUsageType(): UsageType? {
            val property = simpleName.getParentByType(javaClass<JetProperty>())
            if (property != null) {
                when {
                    property.getTypeRef().isAncestor(simpleName) ->
                        return if (property.isLocal()) UsageType.CLASS_LOCAL_VAR_DECLARATION else JetUsageTypes.NON_LOCAL_PROPERTY_TYPE

                    property.getReceiverTypeRef().isAncestor(simpleName) ->
                        return JetUsageTypes.EXTENSION_RECEIVER_TYPE
                }
            }

            val function = simpleName.getParentByType(javaClass<JetFunction>())
            if (function != null) {
                when {
                    function.getReturnTypeRef().isAncestor(simpleName) ->
                        return JetUsageTypes.FUNCTION_RETURN_TYPE
                    function.getReceiverTypeRef().isAncestor(simpleName) ->
                        return JetUsageTypes.EXTENSION_RECEIVER_TYPE
                }
            }

            return when {
                simpleName.getParentByTypeAndBranch(javaClass<JetTypeParameter>()) { getExtendsBound() } != null
                || simpleName.getParentByTypeAndBranch(javaClass<JetTypeConstraint>()) { getBoundTypeReference() } != null ->
                    JetUsageTypes.TYPE_CONSTRAINT

                simpleName is JetDelegationSpecifier
                || simpleName.getParentByTypeAndBranch(javaClass<JetDelegationSpecifier>()) { getTypeReference() } != null ->
                    JetUsageTypes.SUPER_TYPE

                simpleName.getParentByTypeAndBranch(javaClass<JetTypedef>()) { getTypeReference() } != null ->
                    JetUsageTypes.TYPE_DEFINITION

                simpleName.getParentByType(javaClass<JetTypeProjection>()) != null ->
                    UsageType.TYPE_PARAMETER

                simpleName.getParentByTypeAndBranch(javaClass<JetParameter>()) { getTypeReference() } != null ->
                    JetUsageTypes.VALUE_PARAMETER_TYPE

                simpleName.getParentByTypeAndBranch(javaClass<JetIsExpression>()) { getTypeRef() } != null ->
                    JetUsageTypes.IS

                with(simpleName.getParentByTypeAndBranch(javaClass<JetBinaryExpressionWithTypeRHS>()) { getRight() }) {
                    val opType = this?.getOperationReference()?.getReferencedNameElementType()
                    opType == JetTokens.AS_KEYWORD || opType == JetTokens.AS_SAFE
                } ->
                    UsageType.CLASS_CAST_TO

                with(simpleName.getParentByType(javaClass<JetDotQualifiedExpression>())) {
                    if (this == null) false
                    else if (getReceiverExpression() == simpleName) true
                    else
                        getSelectorExpression() == simpleName
                            && getParentByTypeAndBranch(javaClass<JetDotQualifiedExpression>(), true) { getReceiverExpression() } != null
                } ->
                    JetUsageTypes.CLASS_OBJECT_ACCESS

                simpleName.getParentByTypeAndBranch(javaClass<JetSuperExpression>()) { getSuperTypeQualifier() } != null ->
                    JetUsageTypes.SUPER_TYPE_QUALIFIER

                else -> null
            }
        }

        fun getVariableUsageType(): UsageType? {
            if (simpleName.getParentByTypeAndBranch(javaClass<JetDelegatorByExpressionSpecifier>()) { getDelegateExpression() } != null) {
                return JetUsageTypes.DELEGATE
            }

            val dotQualifiedExpression = simpleName.getParentByType(javaClass<JetDotQualifiedExpression>())

            if (dotQualifiedExpression != null) {
                val parent = dotQualifiedExpression.getParent()
                when {
                    dotQualifiedExpression.getReceiverExpression().isAncestor(simpleName) ->
                        return JetUsageTypes.RECEIVER

                    parent is JetDotQualifiedExpression && parent.getReceiverExpression().isAncestor(simpleName) ->
                        return JetUsageTypes.RECEIVER
                }
            }

            return when {
                (simpleName.getParentByTypesAndPredicate(false, javaClass<JetBinaryExpression>()) { JetPsiUtil.isAssignment(it) })
                        ?.getLeft().isAncestor(simpleName) ->
                    UsageType.WRITE

                simpleName.getParentByType(javaClass<JetSimpleNameExpression>()) != null ->
                    UsageType.READ

                else -> null
            }
        }

        fun getFunctionUsageType(descriptor: FunctionDescriptor): UsageType? {
            return when {
                simpleName.getParentByTypeAndBranch(javaClass<JetDelegationSpecifier>()) { getTypeReference() } != null ->
                    JetUsageTypes.SUPER_TYPE

                descriptor is ConstructorDescriptor
                && simpleName.getParentByTypeAndBranch(javaClass<JetAnnotationEntry>()) { getTypeReference() } != null ->
                    UsageType.ANNOTATION

                with(simpleName.getParentByTypeAndBranch(javaClass<JetCallExpression>()) { getCalleeExpression() }) {
                    this?.getCalleeExpression() is JetSimpleNameExpression
                } ->
                    if (descriptor is ConstructorDescriptor) UsageType.CLASS_NEW_OPERATOR else JetUsageTypes.FUNCTION_CALL

                else -> null
            }
        }

        val usageType = getCommonUsageType()
        if (usageType != null) return usageType

        val reference = simpleName.getParentByType(javaClass<JetSimpleNameExpression>())
        if (reference == null) return null

        val file = simpleName.getContainingFile()
        if (file == null) return null

        val bindingContext = AnalyzerFacadeWithCache.analyzeFileWithCache(file as JetFile).getBindingContext()
        val descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, reference)

        return when (descriptor) {
            is ClassifierDescriptor -> if (DescriptorUtils.isSingleton(descriptor)) {
                // Treat object accesses as variables to simulate the old behaviour (when variables were created for objects)
                getVariableUsageType()
            } else {
                getClassUsageType()
            }
            is JavaNamespaceDescriptor -> getClassUsageType()
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

    // values
    val RECEIVER = UsageType(JetBundle.message("usageType.receiver"))
    val DELEGATE = UsageType(JetBundle.message("usageType.delegate"))

    // common usage types
    val CALLABLE_REFERENCE = UsageType(JetBundle.message("usageType.callable.reference"))
}
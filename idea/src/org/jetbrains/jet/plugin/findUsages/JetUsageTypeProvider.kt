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

public class JetUsageTypeProvider() : UsageTypeProviderEx {
    public override fun getUsageType(element: PsiElement?): UsageType? {
        return getUsageType(element, UsageTarget.EMPTY_ARRAY)
    }

    public override fun getUsageType(element: PsiElement?, targets: Array<out UsageTarget>): UsageType? {
        if (element == null) return null

        fun getCommonUsageType(): UsageType? {
            return when {
                element.getParentByType(javaClass<JetImportDirective>()) != null ->
                    JetUsageTypes.IMPORT_DIRECTIVE
                element.getParentByTypeAndBranch(javaClass<JetCallableReferenceExpression>()) { getCallableReference() } != null ->
                    JetUsageTypes.CALLABLE_REFERENCE
                else -> null
            }
        }

        fun getClassUsageType(): UsageType? {
            val property = element.getParentByType(javaClass<JetProperty>())
            if (property != null) {
                when {
                    property.getTypeRef().isAncestor(element) ->
                        return if (property.isLocal()) JetUsageTypes.LOCAL_VARIABLE_TYPE else JetUsageTypes.NON_LOCAL_PROPERTY_TYPE

                    property.getReceiverTypeRef().isAncestor(element) ->
                        return JetUsageTypes.EXTENSION_RECEIVER_TYPE
                }
            }

            val function = element.getParentByType(javaClass<JetFunction>())
            if (function != null) {
                when {
                    function.getReturnTypeRef().isAncestor(element) ->
                        return JetUsageTypes.FUNCTION_RETURN_TYPE
                    function.getReceiverTypeRef().isAncestor(element) ->
                        return JetUsageTypes.EXTENSION_RECEIVER_TYPE
                }
            }

            return when {
                element.getParentByTypeAndBranch(javaClass<JetTypeParameter>()) { getExtendsBound() } != null
                || element.getParentByTypeAndBranch(javaClass<JetTypeConstraint>()) { getBoundTypeReference() } != null ->
                    JetUsageTypes.TYPE_CONSTRAINT

                element is JetDelegationSpecifier
                || element.getParentByTypeAndBranch(javaClass<JetDelegationSpecifier>()) { getTypeReference() } != null ->
                    JetUsageTypes.SUPER_TYPE

                element.getParentByTypeAndBranch(javaClass<JetTypedef>()) { getTypeReference() } != null ->
                    JetUsageTypes.TYPE_DEFINITION

                element.getParentByType(javaClass<JetTypeProjection>()) != null ->
                    JetUsageTypes.TYPE_ARGUMENT

                element.getParentByTypeAndBranch(javaClass<JetParameter>()) { getTypeReference() } != null ->
                    JetUsageTypes.VALUE_PARAMETER_TYPE

                element.getParentByTypeAndBranch(javaClass<JetIsExpression>()) { getTypeRef() } != null ->
                    JetUsageTypes.IS

                with(element.getParentByTypeAndBranch(javaClass<JetBinaryExpressionWithTypeRHS>()) { getRight() }) {
                    val opType = this?.getOperationReference()?.getReferencedNameElementType()
                    opType == JetTokens.AS_KEYWORD || opType == JetTokens.AS_SAFE
                } ->
                    JetUsageTypes.AS

                with(element.getParentByTypeAndBranch(javaClass<JetDotQualifiedExpression>()) { getReceiverExpression() }) {
                    this?.getReceiverExpression() is JetSimpleNameExpression
                } ->
                    JetUsageTypes.CLASS_OBJECT_ACCESS

                element.getParentByTypeAndBranch(javaClass<JetSuperExpression>()) { getSuperTypeQualifier() } != null ->
                    JetUsageTypes.SUPER_TYPE_QUALIFIER

                else -> null
            }
        }

        fun getVariableUsageType(): UsageType? {
            if (element.getParentByTypeAndBranch(javaClass<JetDelegatorByExpressionSpecifier>()) { getDelegateExpression() } != null) {
                return JetUsageTypes.DELEGATE
            }

            val dotQualifiedExpression = element.getParentByType(javaClass<JetDotQualifiedExpression>())

            return if (dotQualifiedExpression != null) {
                val parent = dotQualifiedExpression.getParent()
                when {
                    dotQualifiedExpression.getReceiverExpression().isAncestor(element) ->
                        JetUsageTypes.RECEIVER

                    parent is JetDotQualifiedExpression && parent.getReceiverExpression().isAncestor(element) ->
                        JetUsageTypes.RECEIVER

                    else -> JetUsageTypes.SELECTOR
                }
            }
            else {
                when {
                    element.getParentByTypeAndPredicate(
                            javaClass<JetBinaryExpression>(), false, { JetPsiUtil.isAssignment(it) }
                    )?.getLeft().isAncestor(element) ->
                        UsageType.WRITE

                    element.getParentByType(javaClass<JetSimpleNameExpression>()) != null ->
                        UsageType.READ

                    else -> null
                }
            }
        }

        fun getFunctionUsageType(descriptor: FunctionDescriptor): UsageType? {
            return when {
                element.getParentByTypeAndBranch(javaClass<JetDelegationSpecifier>()) { getTypeReference() } != null ->
                    JetUsageTypes.SUPER_TYPE

                descriptor is ConstructorDescriptor
                && element.getParentByTypeAndBranch(javaClass<JetAnnotationEntry>()) { getTypeReference() } != null ->
                    JetUsageTypes.ANNOTATION_TYPE

                with(element.getParentByTypeAndBranch(javaClass<JetCallExpression>()) { getCalleeExpression() }) {
                    this?.getCalleeExpression() is JetSimpleNameExpression
                } ->
                    if (descriptor is ConstructorDescriptor) JetUsageTypes.INSTANTIATION else JetUsageTypes.FUNCTION_CALL

                else -> null
            }
        }

        val usageType = getCommonUsageType()
        if (usageType != null) return usageType

        val reference = element.getParentByType(javaClass<JetSimpleNameExpression>())
        if (reference == null) return null

        val file = element.getContainingFile()
        if (file == null) return null

        val bindingContext = AnalyzerFacadeWithCache.analyzeFileWithCache(file as JetFile).getBindingContext()
        val descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, reference)

        return when (descriptor) {
            is ClassifierDescriptor -> getClassUsageType()
            is VariableDescriptor -> getVariableUsageType()
            is FunctionDescriptor -> getFunctionUsageType(descriptor)
            else -> null
        }
    }
}

object JetUsageTypes {
    // types
    val ANNOTATION_TYPE = UsageType(JetBundle.message("usageType.annotation.type"))
    val TYPE_CONSTRAINT = UsageType(JetBundle.message("usageType.type.constraint"))
    val TYPE_ARGUMENT = UsageType(JetBundle.message("usageType.type.argument"))
    val VALUE_PARAMETER_TYPE = UsageType(JetBundle.message("usageType.value.parameter.type"))
    val NON_LOCAL_PROPERTY_TYPE = UsageType(JetBundle.message("usageType.nonLocal.property.type"))
    val LOCAL_VARIABLE_TYPE = UsageType(JetBundle.message("usageType.local.variable.type"))
    val FUNCTION_RETURN_TYPE = UsageType(JetBundle.message("usageType.function.return.type"))
    val SUPER_TYPE = UsageType(JetBundle.message("usageType.superType"))
    val TYPE_DEFINITION = UsageType(JetBundle.message("usageType.type.definition"))
    val AS = UsageType(JetBundle.message("usageType.as"))
    val IS = UsageType(JetBundle.message("usageType.is"))
    val CLASS_OBJECT_ACCESS = UsageType(JetBundle.message("usageType.class.object"))
    val EXTENSION_RECEIVER_TYPE = UsageType(JetBundle.message("usageType.extension.receiver.type"))
    val SUPER_TYPE_QUALIFIER = UsageType(JetBundle.message("usageType.super.type.qualifier"))

    // functions
    val INSTANTIATION = UsageType(JetBundle.message("usageType.instantiation"))
    val FUNCTION_CALL = UsageType(JetBundle.message("usageType.function.call"))

    // values
    val RECEIVER = UsageType(JetBundle.message("usageType.receiver"))
    val SELECTOR = UsageType(JetBundle.message("usageType.selector"))
    val DELEGATE = UsageType(JetBundle.message("usageType.delegate"))

    // common usage types
    val IMPORT_DIRECTIVE = UsageType(JetBundle.message("usageType.import"))
    val CALLABLE_REFERENCE = UsageType(JetBundle.message("usageType.callable.reference"))
}
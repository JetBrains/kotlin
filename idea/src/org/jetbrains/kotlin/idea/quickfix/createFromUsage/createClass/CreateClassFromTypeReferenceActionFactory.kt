/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeIntersector
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.ifEmpty
import java.util.*

object CreateClassFromTypeReferenceActionFactory : CreateClassFromUsageFactory<KtUserType>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtUserType? {
        return QuickFixUtil.getParentElementOfType(diagnostic, KtUserType::class.java)
    }

    override fun getPossibleClassKinds(element: KtUserType, diagnostic: Diagnostic): List<ClassKind> {
        val typeRefParent = element.parent.parent
        if (typeRefParent is KtConstructorCalleeExpression) return Collections.emptyList()

        val isQualifier = (element.parent as? KtUserType)?.let { it.qualifier == element } ?: false

        val typeReference = element.parent as? KtTypeReference
        val isUpperBound =
            typeReference?.getParentOfTypeAndBranch<KtTypeParameter> { extendsBound } != null || typeReference?.getParentOfTypeAndBranch<KtTypeConstraint> { boundTypeReference } != null

        return when (typeRefParent) {
            is KtSuperTypeEntry -> listOfNotNull(
                ClassKind.INTERFACE,
                if (typeRefParent.classExpected()) ClassKind.PLAIN_CLASS else null
            )
            else -> ClassKind.values().filter {
                val noTypeArguments = element.typeArgumentsAsTypes.isEmpty()
                when (it) {
                    ClassKind.OBJECT -> noTypeArguments && isQualifier
                    ClassKind.ANNOTATION_CLASS -> noTypeArguments && !isQualifier && !isUpperBound
                    ClassKind.ENUM_ENTRY -> false
                    ClassKind.ENUM_CLASS -> noTypeArguments && !isUpperBound
                    else -> true
                }
            }
        }
    }

    private fun KtSuperTypeEntry.classExpected(): Boolean {
        val containingClass = getStrictParentOfType<KtClass>() ?: return false
        return !containingClass.hasModifier(KtTokens.ANNOTATION_KEYWORD)
                && !containingClass.hasModifier(KtTokens.ENUM_KEYWORD)
                && !containingClass.hasModifier(KtTokens.INLINE_KEYWORD)
    }

    private fun getExpectedUpperBound(element: KtUserType, context: BindingContext): KotlinType? {
        val projection = (element.parent as? KtTypeReference)?.parent as? KtTypeProjection ?: return null
        val argumentList = projection.parent as? KtTypeArgumentList ?: return null
        val index = argumentList.arguments.indexOf(projection)
        val callElement = argumentList.parent as? KtCallElement ?: return null
        val resolvedCall = callElement.getResolvedCall(context) ?: return null
        val typeParameterDescriptor = resolvedCall.candidateDescriptor.typeParameters.getOrNull(index) ?: return null
        if (typeParameterDescriptor.upperBounds.isEmpty()) return null
        return TypeIntersector.getUpperBoundsAsType(typeParameterDescriptor)
    }

    override fun extractFixData(element: KtUserType, diagnostic: Diagnostic): ClassInfo? {
        val name = element.referenceExpression?.getReferencedName() ?: return null
        val typeRefParent = element.parent.parent
        if (typeRefParent is KtConstructorCalleeExpression) return null

        val (context, module) = element.analyzeAndGetResult()
        val qualifier = element.qualifier?.referenceExpression
        val qualifierDescriptor = qualifier?.let { context[BindingContext.REFERENCE_TARGET, it] }

        val targetParents = getTargetParentsByQualifier(element, qualifier != null, qualifierDescriptor).ifEmpty { return null }
        val expectedUpperBound = getExpectedUpperBound(element, context)

        val anyType = module.builtIns.anyType

        return ClassInfo(
            name = name,
            targetParents = targetParents,
            expectedTypeInfo = expectedUpperBound?.let { TypeInfo.ByType(it, Variance.INVARIANT) } ?: TypeInfo.Empty,
            open = typeRefParent is KtSuperTypeEntry && typeRefParent.classExpected(),
            typeArguments = element.typeArgumentsAsTypes.map {
                if (it != null) TypeInfo(it, Variance.INVARIANT) else TypeInfo(anyType, Variance.INVARIANT)
            }
        )
    }
}

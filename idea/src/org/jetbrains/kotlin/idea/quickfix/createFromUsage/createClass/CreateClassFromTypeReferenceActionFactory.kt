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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyzeAndGetResult
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
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

        val interfaceExpected = typeRefParent is KtSuperTypeEntry

        val isQualifier = (element.parent as? KtUserType)?.let { it.qualifier == element } ?: false

        val typeReference = element.parent as? KtTypeReference
        val isUpperBound = typeReference?.getParentOfTypeAndBranch<KtTypeParameter> { extendsBound } != null
                           || typeReference?.getParentOfTypeAndBranch<KtTypeConstraint> { boundTypeReference } != null

        return when {
            interfaceExpected -> Collections.singletonList(ClassKind.INTERFACE)
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
        if (element.parent.parent is KtConstructorCalleeExpression) return null

        val file = element.containingFile as? KtFile ?: return null

        val (context, module) = element.analyzeAndGetResult()
        val qualifier = element.qualifier?.referenceExpression
        val qualifierDescriptor = qualifier?.let { context[BindingContext.REFERENCE_TARGET, it] }

        val targetParents = getTargetParentsByQualifier(file, qualifier != null, qualifierDescriptor).ifEmpty { return null }
        val expectedUpperBound = getExpectedUpperBound(element, context)

        val anyType = module.builtIns.anyType

        return ClassInfo(
                name = name,
                targetParents = targetParents,
                expectedTypeInfo = expectedUpperBound?.let { TypeInfo.ByType(it, Variance.INVARIANT) } ?: TypeInfo.Empty,
                typeArguments = element.typeArgumentsAsTypes.map {
                    if (it != null) TypeInfo(it, Variance.INVARIANT) else TypeInfo(anyType, Variance.INVARIANT)
                }
        )
    }
}

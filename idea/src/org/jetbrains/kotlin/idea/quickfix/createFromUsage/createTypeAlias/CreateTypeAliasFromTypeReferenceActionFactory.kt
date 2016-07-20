/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createTypeAlias

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator
import org.jetbrains.kotlin.idea.quickfix.IntentionActionPriority
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactoryWithDelegate
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromTypeReferenceActionFactory
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.resolve.bindingContextUtil.getAbbreviatedTypeOrType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitution
import org.jetbrains.kotlin.types.typeUtil.containsError

object CreateTypeAliasFromTypeReferenceActionFactory : KotlinSingleIntentionActionFactoryWithDelegate<KtUserType, TypeAliasInfo>(IntentionActionPriority.LOW) {
    override fun getElementOfInterest(diagnostic: Diagnostic) = CreateClassFromTypeReferenceActionFactory.getElementOfInterest(diagnostic)

    data class TypeConstraintInfo(val typeParameter: TypeParameterDescriptor, val upperBound: KotlinType)

    private fun getTypeConstraintInfo(element: KtUserType): TypeConstraintInfo? {
        val context = element.analyze(BodyResolveMode.PARTIAL)
        val containingTypeArg = (element.parent as? KtTypeReference)?.parent as? KtTypeProjection ?: return null
        val argumentList = containingTypeArg.parent as? KtTypeArgumentList ?: return null
        val containingTypeRef = (argumentList.parent as? KtTypeElement)?.parent as? KtTypeReference ?: return null
        val containingType = containingTypeRef.getAbbreviatedTypeOrType(context) ?: return null
        val baseType = containingType.constructor.declarationDescriptor?.defaultType ?: return null
        val typeParameter = containingType.constructor.parameters.getOrNull(argumentList.arguments.indexOf(containingTypeArg))
        val upperBound = typeParameter?.upperBounds?.singleOrNull() ?: return null
        val substitution = getTypeSubstitution(baseType, containingType) ?: return null
        val substitutedUpperBound = TypeSubstitutor.create(substitution).substitute(upperBound, Variance.INVARIANT) ?: return null
        if (substitutedUpperBound.containsError()) return null
        return TypeConstraintInfo(typeParameter, substitutedUpperBound)
    }

    override fun extractFixData(element: KtUserType, diagnostic: Diagnostic): TypeAliasInfo? {
        if (element.getParentOfTypeAndBranch<KtUserType>(true) { qualifier } != null) return null

        val classInfo = CreateClassFromTypeReferenceActionFactory.extractFixData(element, diagnostic) ?: return null

        val expectedType = getTypeConstraintInfo(element)?.upperBound
        if (expectedType != null && expectedType.containsError()) return null

        val validator = CollectingNameValidator(
                filter = NewDeclarationNameValidator(classInfo.targetParent, null, NewDeclarationNameValidator.Target.FUNCTIONS_AND_CLASSES)
        )
        val typeParameterNames = KotlinNameSuggester.suggestNamesForTypeParameters(classInfo.typeArguments.size, validator)
        return TypeAliasInfo(classInfo.name, classInfo.targetParent, typeParameterNames, expectedType)
    }

    override fun createFix(originalElement: KtUserType, data: TypeAliasInfo) = CreateTypeAliasFromUsageFix(originalElement, data)
}
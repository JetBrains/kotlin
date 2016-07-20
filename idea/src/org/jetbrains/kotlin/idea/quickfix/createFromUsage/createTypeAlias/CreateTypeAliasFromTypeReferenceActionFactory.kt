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
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.contains
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.CreateClassFromTypeReferenceActionFactory
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.resolve.bindingContextUtil.getAbbreviatedTypeOrType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

object CreateTypeAliasFromTypeReferenceActionFactory : CreateTypeAliasFromUsageFactory<KtUserType>() {
    override fun getElementOfInterest(diagnostic: Diagnostic) = CreateClassFromTypeReferenceActionFactory.getElementOfInterest(diagnostic)

    private fun getBoundingTypeParameter(element: KtUserType): TypeParameterDescriptor? {
        val context = element.analyze(BodyResolveMode.PARTIAL)
        val containingTypeArg = (element.parent as? KtTypeReference)?.parent as? KtTypeProjection ?: return null
        val argumentList = containingTypeArg.parent as? KtTypeArgumentList ?: return null
        val containingTypeRef = (argumentList.parent as? KtTypeElement)?.parent as? KtTypeReference ?: return null
        val containingType = containingTypeRef.getAbbreviatedTypeOrType(context) ?: return null
        return containingType.constructor.parameters.getOrNull(argumentList.arguments.indexOf(containingTypeArg))
    }

    override fun extractFixData(element: KtUserType, diagnostic: Diagnostic): TypeAliasInfo? {
        if (element.getParentOfTypeAndBranch<KtUserType>(true) { qualifier } != null) return null

        val classInfo = CreateClassFromTypeReferenceActionFactory.extractFixData(element, diagnostic) ?: return null
        val boundingTypeParameter = getBoundingTypeParameter(element)
        val expectedType = if (boundingTypeParameter != null) {
            val upperBound = boundingTypeParameter.upperBounds.singleOrNull() ?: return null
            if (boundingTypeParameter in upperBound) return null
            upperBound
        }
        else null
        val validator = CollectingNameValidator(
                filter = NewDeclarationNameValidator(classInfo.targetParent, null, NewDeclarationNameValidator.Target.FUNCTIONS_AND_CLASSES)
        )
        val typeParameterNames = KotlinNameSuggester.suggestNamesForTypeParameters(classInfo.typeArguments.size, validator)
        return TypeAliasInfo(classInfo.name, classInfo.targetParent, typeParameterNames, expectedType)
    }
}
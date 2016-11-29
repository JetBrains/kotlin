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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createTypeParameter

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.quickfix.KotlinIntentionActionFactoryWithDelegate
import org.jetbrains.kotlin.idea.quickfix.QuickFixWithDelegateFactory
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.utils.addToStdlib.singletonList

object CreateTypeParameterUnmatchedTypeArgumentActionFactory : KotlinIntentionActionFactoryWithDelegate<KtTypeArgumentList, CreateTypeParameterData>() {
    override fun getElementOfInterest(diagnostic: Diagnostic) = diagnostic.psiElement as? KtTypeArgumentList

    override fun extractFixData(element: KtTypeArgumentList, diagnostic: Diagnostic): CreateTypeParameterData? {
        val project = element.project
        val typeArguments = element.arguments
        val context = element.analyze()
        val parent = element.parent
        val referencedDescriptor = when (parent) {
            is KtUserType -> context[BindingContext.REFERENCE_TARGET, parent.referenceExpression]
            is KtCallElement -> parent.getResolvedCall(context)?.resultingDescriptor
            else -> null
        } ?: return null
        val referencedDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, referencedDescriptor) as? KtTypeParameterListOwner
                                    ?: return null

        val missingParameterCount = typeArguments.size - referencedDeclaration.typeParameters.size
        if (missingParameterCount <= 0) return null

        val scope = referencedDeclaration.getResolutionScope()
        val suggestedNames = KotlinNameSuggester.suggestNamesForTypeParameters(
                missingParameterCount,
                CollectingNameValidator(referencedDeclaration.typeParameters.mapNotNull { it.name }) {
                    scope.findClassifier(Name.identifier(it), NoLookupLocation.FROM_IDE) == null
                }
        )
        val typeParameterInfos = suggestedNames.map { name ->
                    TypeParameterInfo(
                            name,
                            null,
                            createFakeTypeParameterDescriptor(referencedDescriptor, name)
                    )
                }
        return CreateTypeParameterData(referencedDeclaration, typeParameterInfos)
    }

    override fun createFixes(
            originalElementPointer: SmartPsiElementPointer<KtTypeArgumentList>,
            diagnostic: Diagnostic,
            quickFixDataFactory: () -> CreateTypeParameterData?
    ): List<QuickFixWithDelegateFactory> {
        return QuickFixWithDelegateFactory factory@ {
            val originalElement = originalElementPointer.element ?: return@factory null
            val data = quickFixDataFactory() ?: return@factory null
            CreateTypeParameterFromUsageFix(originalElement, data, false)
        }.singletonList()
    }
}
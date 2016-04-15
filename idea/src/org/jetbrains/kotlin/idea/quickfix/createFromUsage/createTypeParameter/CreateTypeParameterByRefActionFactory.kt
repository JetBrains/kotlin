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

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactoryWithDelegate
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass.getUpperBoundType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.types.KotlinType

data class CreateTypeParameterData(
        val name: String,
        val declaration: KtTypeParameterListOwner,
        val upperBoundType: KotlinType?
)

object CreateTypeParameterByRefActionFactory : KotlinSingleIntentionActionFactoryWithDelegate<KtUserType, CreateTypeParameterData>() {
    override fun getElementOfInterest(diagnostic: Diagnostic): KtUserType? {
        val ktUserType = diagnostic.psiElement.getParentOfTypeAndBranch<KtUserType> { referenceExpression } ?: return null
        if (ktUserType.qualifier != null) return null
        if (ktUserType.getParentOfTypeAndBranch<KtUserType>(true) { qualifier } != null) return null
        if (ktUserType.typeArgumentList != null) return null
        return ktUserType
    }

    override fun extractFixData(element: KtUserType, diagnostic: Diagnostic): CreateTypeParameterData? {
        val name = element.referencedName ?: return null
        val declaration = element.parents.firstOrNull {
            it is KtProperty || it is KtNamedFunction || it is KtClass
        } as? KtTypeParameterListOwner ?: return null
        val upperBoundType = getUpperBoundType(element)
        return CreateTypeParameterData(name, declaration, upperBoundType)
    }

    override fun createFix(originalElement: KtUserType, data: CreateTypeParameterData) = CreateTypeParameterFromUsageFix(originalElement, data)
}
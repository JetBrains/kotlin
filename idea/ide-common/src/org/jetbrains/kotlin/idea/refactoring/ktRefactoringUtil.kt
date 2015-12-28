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

package org.jetbrains.kotlin.idea.refactoring

import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.BindingContext

fun KtElement.getContextForContainingDeclarationBody(): BindingContext? {
    val enclosingDeclaration = getStrictParentOfType<KtDeclaration>()
    val bodyElement = when (enclosingDeclaration) {
        is KtDeclarationWithBody -> enclosingDeclaration.getBodyExpression()
        is KtWithExpressionInitializer -> enclosingDeclaration.getInitializer()
        is KtDestructuringDeclaration -> enclosingDeclaration.getInitializer()
        is KtParameter -> enclosingDeclaration.getDefaultValue()
        is KtAnonymousInitializer -> enclosingDeclaration.body
        is KtClass -> {
            val delegationSpecifierList = enclosingDeclaration.getSuperTypeList()
            if (delegationSpecifierList.isAncestor(this)) this else null
        }
        else -> null
    }
    return bodyElement?.let { it.analyze() }
}
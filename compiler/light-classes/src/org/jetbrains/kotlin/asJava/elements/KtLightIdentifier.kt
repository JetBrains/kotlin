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

package org.jetbrains.kotlin.asJava.elements

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.impl.light.LightIdentifier
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

abstract class KtLightIdentifierBase(
    private val lightOwner: PsiElement,
    text: String?
) : LightIdentifier(lightOwner.manager, text), PsiElementWithOrigin<PsiElement> {
    override fun isPhysical() = true
    override fun getParent() = lightOwner
    override fun getContainingFile() = lightOwner.containingFile
    override fun getTextRange() = origin?.textRange ?: TextRange.EMPTY_RANGE
    override fun getTextOffset(): Int = origin?.textOffset ?: -1
}

open class KtLightIdentifierWithOrigin(
    lightOwner: PsiElement,
    private val ktDeclaration: KtDeclaration?
) : KtLightIdentifierBase(lightOwner, ktDeclaration?.name) {
    override val origin: PsiElement?
        get() = when (ktDeclaration) {
            is KtSecondaryConstructor -> ktDeclaration.getConstructorKeyword()
            is KtPrimaryConstructor -> ktDeclaration.getConstructorKeyword()
                ?: ktDeclaration.valueParameterList
                ?: ktDeclaration.containingClassOrObject?.nameIdentifier
            is KtPropertyAccessor -> ktDeclaration.namePlaceholder
            is KtNamedDeclaration -> ktDeclaration.nameIdentifier
            else -> null
        }
}

class KtLightIdentifier(
    private val lightOwner: PsiElement,
    ktDeclaration: KtDeclaration?
) : KtLightIdentifierWithOrigin(lightOwner, ktDeclaration), PsiCompiledElement {

    override fun getMirror() = ((lightOwner as? KtLightElement<*, *>)?.clsDelegate as? PsiNameIdentifierOwner)?.nameIdentifier
}

/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsRepositoryPsiElement
import com.intellij.psi.impl.light.LightElement
import org.jetbrains.kotlin.asJava.builder.ClsWrapperStubPsiFactory.ORIGIN
import org.jetbrains.kotlin.asJava.builder.LightElementOrigin
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration

abstract class KtLightMemberImpl<out D : PsiMember>(
        computeRealDelegate: () -> D,
        override val lightMemberOrigin: LightMemberOrigin?,
        private val containingClass: KtLightClass,
        private val dummyDelegate: D?
) : LightElement(containingClass.manager, KotlinLanguage.INSTANCE), PsiMember, KtLightMember<D> {
    override val clsDelegate by lazyPub(computeRealDelegate)
    private val lightIdentifier by lazyPub { KtLightIdentifier(this, kotlinOrigin as? KtNamedDeclaration) }

    private val _modifierList by lazyPub {
        if (lightMemberOrigin is LightMemberOriginForDeclaration)
            KtLightModifierList(this, dummyDelegate?.modifierList)
        else clsDelegate.modifierList!!
    }

    override fun hasModifierProperty(name: String) = _modifierList.hasModifierProperty(name)

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun toString(): String = "${this::class.java.simpleName}:$name"

    override fun getContainingClass() = containingClass

    override fun getContainingFile() = containingClass.containingFile

    override fun getParent(): PsiElement = containingClass

    override fun isValid() = containingClass.isValid

    override fun getName(): String = dummyDelegate?.name ?: clsDelegate.name!!

    override fun getNameIdentifier(): PsiIdentifier = lightIdentifier

    override fun getUseScope() = kotlinOrigin?.useScope ?: super.getUseScope()

    override val kotlinOrigin: KtDeclaration? get() = lightMemberOrigin?.originalElement

    override fun getNavigationElement() = kotlinOrigin ?: super.getNavigationElement()

    override fun getPresentation(): ItemPresentation? = (kotlinOrigin ?: this).let { ItemPresentationProviders.getItemPresentation(it) }

    override fun getText() = kotlinOrigin?.text ?: ""

    override fun getTextRange() = kotlinOrigin?.textRange ?: TextRange.EMPTY_RANGE

    override fun isWritable() = kotlinOrigin?.isWritable ?: false

    override fun getDocComment() = (clsDelegate as PsiDocCommentOwner).docComment

    override fun isDeprecated() = (clsDelegate as PsiDocCommentOwner).isDeprecated
}

private val visibilityModifiers = arrayOf(PsiModifier.PRIVATE, PsiModifier.PACKAGE_LOCAL, PsiModifier.PROTECTED, PsiModifier.PUBLIC)

internal fun getMemberOrigin(member: PsiMember): LightMemberOriginForDeclaration? {
    if (member !is ClsRepositoryPsiElement<*>) return null

    val stubElement = member.stub as? UserDataHolder ?: return null

    return stubElement.getUserData<LightElementOrigin>(ORIGIN) as? LightMemberOriginForDeclaration ?: return null
}


class KtLightModifierList(
        private val owner: KtLightMember<*>,
        private val dummyDelegate: PsiModifierList?
) : LightElement(owner.manager, KotlinLanguage.INSTANCE), PsiModifierList {
    private val clsDelegate by lazyPub { owner.clsDelegate.modifierList!! }
    private val _annotations by lazyPub { computeAnnotations(this, clsDelegate) }

    override fun hasModifierProperty(name: String): Boolean {
        if (dummyDelegate != null) {
            if (name in visibilityModifiers) {
                if (owner.kotlinOrigin?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: false) {
                    return clsDelegate.hasModifierProperty(name)
                }
            }
            return dummyDelegate.hasModifierProperty(name)
        }
        return clsDelegate.hasModifierProperty(name)
    }

    override fun hasExplicitModifier(name: String) = hasModifierProperty(name)

    override fun setModifierProperty(name: String, value: Boolean) = clsDelegate.setModifierProperty(name, value)
    override fun checkSetModifierProperty(name: String, value: Boolean) = clsDelegate.checkSetModifierProperty(name, value)
    override fun addAnnotation(qualifiedName: String) = clsDelegate.addAnnotation(qualifiedName)
    override fun getApplicableAnnotations(): Array<out PsiAnnotation> = annotations
    override fun getAnnotations(): Array<out PsiAnnotation> = _annotations.value
    override fun findAnnotation(qualifiedName: String) = annotations.firstOrNull { it.qualifiedName == qualifiedName }
    override fun getParent() = owner
    override fun getText(): String? = ""
    override fun getTextRange() = TextRange.EMPTY_RANGE
    override fun copy(): PsiElement = KtLightModifierList(owner, dummyDelegate)
    override fun getReferences() = PsiReference.EMPTY_ARRAY
    override fun isEquivalentTo(another: PsiElement?) =
            another is KtLightModifierList && owner == another.owner

    override fun toString() = "Light modifier list of $owner"
}

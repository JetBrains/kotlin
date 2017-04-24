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

import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsRepositoryPsiElement
import org.jetbrains.kotlin.asJava.builder.ClsWrapperStubPsiFactory.ORIGIN
import org.jetbrains.kotlin.asJava.builder.LightElementOrigin
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.hasBody

abstract class KtLightMemberImpl<out D : PsiMember>(
        computeRealDelegate: () -> D,
        override val lightMemberOrigin: LightMemberOrigin?,
        private val containingClass: KtLightClass,
        private val dummyDelegate: D?
) : KtLightElementBase(containingClass), PsiMember, KtLightMember<D> {
    override val clsDelegate by lazyPub(computeRealDelegate)
    private val lightIdentifier by lazyPub { KtLightIdentifier(this, kotlinOrigin as? KtNamedDeclaration) }

    private val _modifierList by lazyPub {
        if (lightMemberOrigin is LightMemberOriginForDeclaration)
            KtLightMemberModifierList(this, dummyDelegate?.modifierList)
        else clsDelegate.modifierList!!
    }

    override fun hasModifierProperty(name: String) = _modifierList.hasModifierProperty(name)

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun toString(): String = "${this::class.java.simpleName}:$name"

    override fun getContainingClass() = containingClass

    override fun getName(): String = dummyDelegate?.name ?: clsDelegate.name!!

    override fun getNameIdentifier(): PsiIdentifier = lightIdentifier

    override val kotlinOrigin: KtDeclaration? get() = lightMemberOrigin?.originalElement

    override fun getDocComment() = (clsDelegate as PsiDocCommentOwner).docComment

    override fun isDeprecated() = (clsDelegate as PsiDocCommentOwner).isDeprecated
}

internal fun getMemberOrigin(member: PsiMember): LightMemberOriginForDeclaration? {
    if (member !is ClsRepositoryPsiElement<*>) return null

    val stubElement = member.stub as? UserDataHolder ?: return null

    return stubElement.getUserData<LightElementOrigin>(ORIGIN) as? LightMemberOriginForDeclaration ?: return null
}

private val visibilityModifiers = arrayOf(PsiModifier.PRIVATE, PsiModifier.PACKAGE_LOCAL, PsiModifier.PROTECTED, PsiModifier.PUBLIC)

private class KtLightMemberModifierList(
        owner: KtLightMember<*>, private val dummyDelegate: PsiModifierList?
) : KtLightModifierList<KtLightMember<*>>(owner) {
    override fun hasModifierProperty(name: String) = when {
        name == PsiModifier.ABSTRACT && isImplementationInInterface() -> false
        name == PsiModifier.DEFAULT && isImplementationInInterface() -> true
        dummyDelegate != null -> {
            when {
                name in visibilityModifiers && isMethodOverride() ->
                    clsDelegate.hasModifierProperty(name)
                else -> dummyDelegate.hasModifierProperty(name)
            }
        }
        else -> clsDelegate.hasModifierProperty(name)
    }

    private fun isMethodOverride() = owner is KtLightMethod && owner.kotlinOrigin?.hasModifier(KtTokens.OVERRIDE_KEYWORD) ?: false

    private fun isImplementationInInterface()
            = owner.containingClass.isInterface && owner is KtLightMethod && owner.kotlinOrigin?.hasBody() ?: false

    override fun copy() = KtLightMemberModifierList(owner, dummyDelegate)
}


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

import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.PsiVariableEx
import com.intellij.psi.impl.light.LightElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForEnumEntry
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.lang.UnsupportedOperationException

// Copied from com.intellij.psi.impl.light.LightField
sealed class KtLightFieldImpl(
        override val lightMemberOrigin: LightMemberOrigin?,
        override val clsDelegate: PsiField,
        private val containingClass: KtLightClass
) : LightElement(clsDelegate.manager, KotlinLanguage.INSTANCE), KtLightField {
    private val lightIdentifier by lazy(LazyThreadSafetyMode.PUBLICATION) { KtLightIdentifier(this, kotlinOrigin as? KtNamedDeclaration) }

    @Throws(IncorrectOperationException::class)
    override fun setInitializer(initializer: PsiExpression?) = throw IncorrectOperationException("Not supported")

    override fun getUseScope() = kotlinOrigin?.useScope ?: super.getUseScope()

    override fun getPresentation(): ItemPresentation? = (kotlinOrigin ?: this).let { ItemPresentationProviders.getItemPresentation(it) }

    override fun getName() = clsDelegate.name

    override fun getNameIdentifier() = lightIdentifier

    override fun getDocComment() = clsDelegate.docComment

    override fun isDeprecated() = clsDelegate.isDeprecated

    override fun getContainingClass() = containingClass

    override fun getContainingFile() = containingClass.containingFile

    override fun getType() = clsDelegate.type

    override fun getTypeElement() = clsDelegate.typeElement

    override fun getInitializer() = clsDelegate.initializer

    override fun hasInitializer() = clsDelegate.hasInitializer()

    @Throws(IncorrectOperationException::class)
    override fun normalizeDeclaration() = throw IncorrectOperationException("Not supported")

    override fun computeConstantValue() = clsDelegate.computeConstantValue()

    override fun setName(@NonNls name: String): PsiElement {
        (kotlinOrigin as? KtNamedDeclaration)?.setName(name)
        return this
    }

    private val _modifierList by lazy(LazyThreadSafetyMode.PUBLICATION) {
        if (lightMemberOrigin is LightMemberOriginForDeclaration)
            clsDelegate.modifierList?.let { KtLightModifierList(it, this) }
        else clsDelegate.modifierList
    }

    override fun getModifierList() = _modifierList

    override fun hasModifierProperty(@NonNls name: String) = clsDelegate.hasModifierProperty(name)

    override fun getText() = kotlinOrigin?.text ?: ""

    override fun getTextRange() = kotlinOrigin?.textRange ?: TextRange.EMPTY_RANGE

    override fun isValid() = containingClass.isValid

    override fun toString(): String = "${this.javaClass.simpleName}:$name"

    override fun equals(other: Any?): Boolean =
            other is KtLightField &&
            name == other.name &&
            lightMemberOrigin == other.lightMemberOrigin &&
            containingClass == other.containingClass &&
            clsDelegate == other.clsDelegate

    override fun hashCode(): Int {
        var result = lightMemberOrigin?.hashCode() ?: 0
        result = 31 * result + clsDelegate.hashCode()
        result = 31 * result + containingClass.hashCode()
        return result
    }

    override val kotlinOrigin: KtDeclaration? get() = lightMemberOrigin?.originalElement

    override fun getNavigationElement() = kotlinOrigin ?: super.getNavigationElement()

    override fun computeConstantValue(visitedVars: MutableSet<PsiVariable>?): Any? {
        return (clsDelegate as PsiVariableEx).computeConstantValue(visitedVars)
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (another is KtLightField && kotlinOrigin == another.kotlinOrigin && clsDelegate == another.clsDelegate) {
            return true
        }
        return super.isEquivalentTo(another)
    }

    override fun isWritable() = kotlinOrigin?.isWritable ?: false

    override fun copy() = Factory.create(lightMemberOrigin?.copy(), clsDelegate, containingClass)


    class KtLightEnumConstant(
            origin: LightMemberOrigin?,
            override val clsDelegate: PsiEnumConstant,
            containingClass: KtLightClass,
            private val initializingClass: PsiEnumConstantInitializer?
    ) : KtLightFieldImpl(origin, clsDelegate, containingClass), PsiEnumConstant {
        // NOTE: we don't use "delegation by" because the compiler would generate method calls to ALL of PsiEnumConstant members,
        // but we need only members whose implementations are not present in KotlinLightField
        override fun getArgumentList() = clsDelegate.argumentList

        override fun getInitializingClass(): PsiEnumConstantInitializer? = initializingClass
        override fun getOrCreateInitializingClass(): PsiEnumConstantInitializer =
                initializingClass ?: throw UnsupportedOperationException("Can't create enum constant body: ${clsDelegate.name}")

        override fun resolveConstructor() = clsDelegate.resolveConstructor()
        override fun resolveMethod() = clsDelegate.resolveMethod()
        override fun resolveMethodGenerics() = clsDelegate.resolveMethodGenerics()
    }

    class KtLightFieldForDeclaration(origin: LightMemberOrigin?, delegate: PsiField, containingClass: KtLightClass) :
            KtLightFieldImpl(origin, delegate, containingClass)

    companion object Factory {
        fun create(origin: LightMemberOrigin?, delegate: PsiField, containingClass: KtLightClass): KtLightField {
            when (delegate) {
                is PsiEnumConstant -> {
                    val kotlinEnumEntry = (origin as? LightMemberOriginForDeclaration)?.originalElement as? KtEnumEntry
                    val initializingClass = if (kotlinEnumEntry != null && kotlinEnumEntry.declarations.isNotEmpty()) {
                        KtLightClassForEnumEntry(kotlinEnumEntry, delegate)
                    }
                    else null
                    return KtLightEnumConstant(origin, delegate, containingClass, initializingClass)
                }
                else -> return KtLightFieldForDeclaration(origin, delegate, containingClass)
            }
        }
    }
}

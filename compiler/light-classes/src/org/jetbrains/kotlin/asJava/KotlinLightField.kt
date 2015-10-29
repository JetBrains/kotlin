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

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext

public interface KotlinLightField : PsiField, KotlinLightElement<KtDeclaration, PsiField>

// Copied from com.intellij.psi.impl.light.LightField
sealed class KotlinLightFieldImpl(
        private val origin: KtDeclaration?,
        private val delegate: PsiField,
        private val containingClass: KotlinLightClass
) : LightElement(delegate.manager, KotlinLanguage.INSTANCE), KotlinLightField {
    @Throws(IncorrectOperationException::class)
    override fun setInitializer(initializer: PsiExpression?) = throw IncorrectOperationException("Not supported")

    override fun getUseScope() = origin?.useScope ?: super.getUseScope()

    override fun getName() = delegate.name

    override fun getNameIdentifier() = delegate.nameIdentifier

    override fun getDocComment() = delegate.docComment

    override fun isDeprecated() = delegate.isDeprecated

    override fun getContainingClass() = containingClass

    override fun getType() = delegate.type

    override fun getTypeElement() = delegate.typeElement

    override fun getInitializer() = delegate.initializer

    override fun hasInitializer() = delegate.hasInitializer()

    @Throws(IncorrectOperationException::class)
    override fun normalizeDeclaration() = throw IncorrectOperationException("Not supported")

    override fun computeConstantValue() = delegate.computeConstantValue()

    @Throws(IncorrectOperationException::class)
    override fun setName(@NonNls name: String) = throw IncorrectOperationException("Not supported")

    override fun getModifierList() = delegate.modifierList

    override fun hasModifierProperty(@NonNls name: String) = delegate.hasModifierProperty(name)

    override fun getText() = delegate.text

    override fun getTextRange() = TextRange(-1, -1)

    override fun isValid() = containingClass.isValid

    override fun toString(): String = "${this.javaClass.simpleName}:$name"

    override fun getOrigin() = origin

    override fun getDelegate() = delegate

    override fun getNavigationElement() = origin ?: super.getNavigationElement()

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (another is KotlinLightField && origin == another.getOrigin() && delegate == another.getDelegate()) {
            return true
        }
        return super.isEquivalentTo(another)
    }

    override fun isWritable() = getOrigin()?.isWritable ?: false

    override fun copy() = Factory.create(origin?.copy() as? KtDeclaration, delegate, containingClass)

    class KotlinLightEnumConstant(
            origin: KtEnumEntry,
            enumConstant: PsiEnumConstant,
            containingClass: KotlinLightClass,
            private val initializingClass: PsiEnumConstantInitializer?
    ) : KotlinLightFieldImpl(origin, enumConstant, containingClass), PsiEnumConstant {
        override fun getDelegate() = super.getDelegate() as PsiEnumConstant

        // NOTE: we don't use "delegation by" because the compiler would generate method calls to ALL of PsiEnumConstant members,
        // but we need only members whose implementations are not present in KotlinLightField
        override fun getArgumentList() = getDelegate().argumentList

        override fun getInitializingClass(): PsiEnumConstantInitializer? = initializingClass
        override fun getOrCreateInitializingClass(): PsiEnumConstantInitializer =
                initializingClass ?: throw UnsupportedOperationException("Can't create enum constant body: ${getDelegate().getName()}")

        override fun resolveConstructor() = getDelegate().resolveConstructor()
        override fun resolveMethod() = getDelegate().resolveMethod()
        override fun resolveMethodGenerics() = getDelegate().resolveMethodGenerics()
    }

    public class KotlinLightFieldForDeclaration(origin: KtDeclaration?, delegate: PsiField, containingClass: KotlinLightClass)
    : KotlinLightFieldImpl(origin, delegate, containingClass)

    companion object Factory {
        fun create(origin: KtDeclaration?, delegate: PsiField, containingClass: KotlinLightClass): KotlinLightField {
            if (origin is KtEnumEntry) {
                assert(delegate is PsiEnumConstant) { "Field delegate should be an enum constant (${delegate.name}):\n${origin.getElementTextWithContext()}" }
                val enumConstant = delegate as PsiEnumConstant
                val enumConstantFqName = FqName(containingClass.getFqName().asString() + "." + origin.name)
                val initializingClass = if (origin.declarations.isEmpty())
                    null
                else
                    KotlinLightClassForEnumEntry(delegate.manager, enumConstantFqName, origin, enumConstant)
                return KotlinLightEnumConstant(origin, enumConstant, containingClass, initializingClass)
            }
            return KotlinLightFieldForDeclaration(origin, delegate, containingClass)
        }
    }
}

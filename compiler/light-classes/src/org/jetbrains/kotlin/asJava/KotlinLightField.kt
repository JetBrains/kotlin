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

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.search.SearchScope
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtDeclaration

// Copied from com.intellij.psi.impl.light.LightField
abstract class KotlinLightField<T : KtDeclaration, D : PsiField>(
        manager: PsiManager,
        private val origin: T,
        private val delegate: D,
        private val containingClass: PsiClass
) : LightElement(manager, JavaLanguage.INSTANCE), PsiField, KotlinLightElement<T, D> {

    abstract override fun copy(): KotlinLightField<T, D>

    @Throws(IncorrectOperationException::class)
    override fun setInitializer(initializer: PsiExpression?) {
        throw IncorrectOperationException("Not supported")
    }

    override fun getUseScope(): SearchScope {
        return origin.useScope
    }

    override fun getName(): String? {
        return delegate.name
    }

    override fun getNameIdentifier(): PsiIdentifier {
        return delegate.nameIdentifier
    }

    override fun getDocComment(): PsiDocComment? {
        return delegate.docComment
    }

    override fun isDeprecated(): Boolean {
        return delegate.isDeprecated
    }

    override fun getContainingClass(): PsiClass? {
        return containingClass
    }

    override fun getType(): PsiType {
        return delegate.type
    }

    override fun getTypeElement(): PsiTypeElement? {
        return delegate.typeElement
    }

    override fun getInitializer(): PsiExpression? {
        return delegate.initializer
    }

    override fun hasInitializer(): Boolean {
        return delegate.hasInitializer()
    }

    @Throws(IncorrectOperationException::class)
    override fun normalizeDeclaration() {
        throw IncorrectOperationException("Not supported")
    }

    override fun computeConstantValue(): Any? {
        return delegate.computeConstantValue()
    }

    @Throws(IncorrectOperationException::class)
    override fun setName(@NonNls name: String): PsiElement {
        throw IncorrectOperationException("Not supported")
    }

    override fun getModifierList(): PsiModifierList? {
        return delegate.modifierList
    }

    override fun hasModifierProperty(@NonNls name: String): Boolean {
        return delegate.hasModifierProperty(name)
    }

    override fun getText(): String {
        return delegate.text
    }

    override fun getTextRange(): TextRange {
        return TextRange(-1, -1)
    }

    override fun isValid(): Boolean {
        return containingClass.isValid
    }

    override fun toString(): String {
        return "KotlinLightField:" + name!!
    }

    override fun getOrigin(): T {
        return origin
    }

    override fun getDelegate(): D {
        return delegate
    }

    override fun getNavigationElement(): PsiElement {
        return getOrigin()
    }

    override fun getLanguage(): Language {
        return KotlinLanguage.INSTANCE
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (another is KotlinLightField<*, *> && origin.isEquivalentTo(another.getOrigin())) {
            return true
        }
        return super.isEquivalentTo(another)
    }

    override fun isWritable(): Boolean {
        return getOrigin().isWritable
    }
}

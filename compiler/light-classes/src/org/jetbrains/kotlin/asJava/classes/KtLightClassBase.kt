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

package org.jetbrains.kotlin.asJava.classes

import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.light.AbstractLightClass
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.asJava.elements.KtLightFieldImpl
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.idea.KotlinLanguage

abstract class KtLightClassBase protected constructor(
    manager: PsiManager
) : AbstractLightClass(manager, KotlinLanguage.INSTANCE), KtExtensibleLightClass {
    protected open val myInnersCache = KotlinClassInnerStuffCache(
        myClass = this,
        dependencies = listOf(KotlinModificationTrackerService.getInstance(manager.project).outOfBlockModificationTracker),
        lazyCreator = LightClassesLazyCreator(project)
    )

    override fun getDelegate() = clsDelegate

    override fun getFields() = myInnersCache.fields

    override fun getMethods() = myInnersCache.methods

    override fun getConstructors() = myInnersCache.constructors

    override fun getInnerClasses() = myInnersCache.innerClasses

    override fun getAllFields() = PsiClassImplUtil.getAllFields(this)

    override fun getAllMethods() = PsiClassImplUtil.getAllMethods(this)

    override fun getAllInnerClasses() = PsiClassImplUtil.getAllInnerClasses(this)

    override fun findFieldByName(name: String, checkBases: Boolean) = myInnersCache.findFieldByName(name, checkBases)

    override fun findMethodsByName(name: String, checkBases: Boolean) = myInnersCache.findMethodsByName(name, checkBases)

    override fun findInnerClassByName(name: String, checkBases: Boolean) = myInnersCache.findInnerClassByName(name, checkBases)

    override fun getOwnFields(): List<PsiField> = KtLightFieldImpl.fromClsFields(delegate, this)

    override fun getOwnMethods(): List<PsiMethod> = KtLightMethodImpl.fromClsMethods(delegate, this)

    override fun getText(): String {
        val origin = kotlinOrigin
        return if (origin == null) "" else origin.text
    }

    override fun getLanguage() = KotlinLanguage.INSTANCE

    override fun getPresentation() = ItemPresentationProviders.getItemPresentation(this)

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    override fun getContext() = parent

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return PsiClassImplUtil.isClassEquivalentTo(this, another)
    }
}

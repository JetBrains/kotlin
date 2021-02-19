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

package org.jetbrains.kotlin.idea.asJava

import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.psi.*
import com.intellij.psi.impl.PsiCachedValueImpl
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.PsiUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.classes.KotlinClassInnerStuffCache
import org.jetbrains.kotlin.asJava.classes.KotlinClassInnerStuffCache.Companion.processDeclarationsInEnum
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.frontend.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.frontend.api.tokens.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.trackers.createProjectWideOutOfBlockModificationTracker
import javax.swing.Icon

abstract class FirLightClassBase protected constructor(manager: PsiManager) : LightElement(manager, KotlinLanguage.INSTANCE), PsiClass,
    KtLightClass, PsiExtensibleClass {

    override val clsDelegate: PsiClass
        get() = invalidAccess()

    private class FirLightClassesLazyCreator(private val project: Project) : KotlinClassInnerStuffCache.LazyCreator() {
        @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
        override fun <T : Any> get(initializer: () -> T, dependencies: List<Any>): Lazy<T> = lazyPub {
            PsiCachedValueImpl(PsiManager.getInstance(project)) {
                CachedValueProvider.Result.create(hackyAllowRunningOnEdt(initializer), dependencies)
            }.value ?: error("initializer cannot return null")
        }
    }

    private val myInnersCache = KotlinClassInnerStuffCache(
        myClass = this@FirLightClassBase,
        externalDependencies = listOf(manager.project.createProjectWideOutOfBlockModificationTracker()),
        lazyCreator = FirLightClassesLazyCreator(project)
    )

    override fun getFields(): Array<PsiField> = myInnersCache.fields

    override fun getMethods(): Array<PsiMethod> = myInnersCache.methods

    override fun getConstructors(): Array<PsiMethod> = myInnersCache.constructors

    override fun getInnerClasses(): Array<out PsiClass> = myInnersCache.innerClasses

    override fun getAllFields(): Array<PsiField> = PsiClassImplUtil.getAllFields(this)

    override fun getAllMethods(): Array<PsiMethod> = PsiClassImplUtil.getAllMethods(this)

    override fun getAllInnerClasses(): Array<PsiClass> = PsiClassImplUtil.getAllInnerClasses(this)

    override fun findFieldByName(name: String, checkBases: Boolean) =
        myInnersCache.findFieldByName(name, checkBases)

    override fun findMethodsByName(name: String, checkBases: Boolean): Array<PsiMethod> =
        myInnersCache.findMethodsByName(name, checkBases)

    override fun findInnerClassByName(name: String, checkBases: Boolean): PsiClass? =
        myInnersCache.findInnerClassByName(name, checkBases)

    override fun processDeclarations(
        processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement?, place: PsiElement
    ): Boolean {

        if (isEnum && !processDeclarationsInEnum(processor, state, myInnersCache)) return false

        return PsiClassImplUtil.processDeclarationsInClass(
            this,
            processor,
            state,
            null,
            lastParent,
            place,
            PsiUtil.getLanguageLevel(place),
            false
        )
    }

    override fun getText(): String = kotlinOrigin?.text ?: ""

    override fun getLanguage(): KotlinLanguage = KotlinLanguage.INSTANCE

    override fun getPresentation(): ItemPresentation? =
        ItemPresentationProviders.getItemPresentation(this)

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    override fun getContext(): PsiElement = parent

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        PsiClassImplUtil.isClassEquivalentTo(this, another)

    override fun getDocComment(): PsiDocComment? = null

    override fun hasTypeParameters(): Boolean = PsiImplUtil.hasTypeParameters(this)

    override fun getExtendsListTypes(): Array<PsiClassType?> =
        PsiClassImplUtil.getExtendsListTypes(this)

    override fun getImplementsListTypes(): Array<PsiClassType?> =
        PsiClassImplUtil.getImplementsListTypes(this)

    override fun findMethodBySignature(patternMethod: PsiMethod?, checkBases: Boolean): PsiMethod? =
        patternMethod?.let { PsiClassImplUtil.findMethodBySignature(this, it, checkBases) }

    override fun findMethodsBySignature(patternMethod: PsiMethod?, checkBases: Boolean): Array<PsiMethod?> =
        patternMethod?.let { PsiClassImplUtil.findMethodsBySignature(this, it, checkBases) } ?: emptyArray()

    override fun findMethodsAndTheirSubstitutorsByName(
        @NonNls name: String?,
        checkBases: Boolean
    ): List<Pair<PsiMethod?, PsiSubstitutor?>?> =
        PsiClassImplUtil.findMethodsAndTheirSubstitutorsByName(this, name, checkBases)

    override fun getAllMethodsAndTheirSubstitutors(): List<Pair<PsiMethod?, PsiSubstitutor?>?> {
        return PsiClassImplUtil.getAllWithSubstitutorsByMap(this, PsiClassImplUtil.MemberType.METHOD)
    }

    override fun getRBrace(): PsiElement? = null

    override fun getLBrace(): PsiElement? = null

    override fun getInitializers(): Array<PsiClassInitializer> = PsiClassInitializer.EMPTY_ARRAY

    override fun getElementIcon(flags: Int): Icon? =
        throw UnsupportedOperationException("This should be done by KotlinFirIconProvider")

    override fun getVisibleSignatures(): MutableCollection<HierarchicalMethodSignature> = PsiSuperMethodImplUtil.getVisibleSignatures(this)

    override fun setName(name: String): PsiElement? = cannotModify()

    abstract override fun copy(): PsiElement

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitClass(this)
        } else {
            visitor.visitElement(this)
        }
    }
}
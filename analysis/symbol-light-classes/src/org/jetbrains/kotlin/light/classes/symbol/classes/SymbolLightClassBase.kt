/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.InheritanceImplUtil
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.PsiUtil
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.light.classes.symbol.SymbolFakeFile
import org.jetbrains.kotlin.light.classes.symbol.analyzeForLightClasses
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import javax.swing.Icon


abstract class SymbolLightClassBase protected constructor(val ktModule: KtModule, manager: PsiManager) :
    LightElement(manager, KotlinLanguage.INSTANCE), PsiClass, KtExtensibleLightClass {

    private val myInnersCache by lazyPub {
        ClassInnerStuffCache(
            /* aClass = */ this,
            /* generateEnumMethods = */ false,
            /* modificationTrackers = */ modificationTrackerForClassInnerStuff(),
        )
    }

    protected open fun modificationTrackerForClassInnerStuff(): List<ModificationTracker> {
        return listOf(project.createProjectWideOutOfBlockModificationTracker())
    }

    override fun getFields(): Array<PsiField> = myInnersCache.fields

    override fun getMethods(): Array<PsiMethod> = myInnersCache.methods

    override fun getConstructors(): Array<PsiMethod> = myInnersCache.constructors

    override fun getInnerClasses(): Array<out PsiClass> = myInnersCache.innerClasses

    override fun getAllFields(): Array<PsiField> = PsiClassImplUtil.getAllFields(this)

    override fun getAllMethods(): Array<PsiMethod> = PsiClassImplUtil.getAllMethods(this)

    override fun getAllInnerClasses(): Array<PsiClass> = PsiClassImplUtil.getAllInnerClasses(this)

    override fun findFieldByName(name: String, checkBases: Boolean) = myInnersCache.findFieldByName(name, checkBases)

    override fun findMethodsByName(name: String, checkBases: Boolean): Array<PsiMethod> = myInnersCache.findMethodsByName(name, checkBases)

    override fun findInnerClassByName(name: String, checkBases: Boolean): PsiClass? = myInnersCache.findInnerClassByName(name, checkBases)

    override fun processDeclarations(
        processor: PsiScopeProcessor, state: ResolveState, lastParent: PsiElement?, place: PsiElement
    ): Boolean = PsiClassImplUtil.processDeclarationsInClass(
        /* aClass = */ this,
        /* processor = */ processor,
        /* state = */ state,
        /* visited = */ null,
        /* last = */ lastParent,
        /* place = */ place,
        /* languageLevel = */ PsiUtil.getLanguageLevel(place),
        /* isRaw = */ false,
    )

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        if (manager.areElementsEquivalent(baseClass, this)) return false
        LightClassInheritanceHelper.getService(project).isInheritor(this, baseClass, checkDeep).ifSure { return it }

        val thisClassOrigin = kotlinOrigin
        val baseClassOrigin = (baseClass as? KtLightClass)?.kotlinOrigin

        return if (baseClassOrigin != null && thisClassOrigin != null) {
            analyzeForLightClasses(ktModule) {
                thisClassOrigin.checkIsInheritor(baseClassOrigin, checkDeep)
            }
        } else {
            hasSuper(baseClass, checkDeep) ||
                    InheritanceImplUtil.isInheritor(this, baseClass, checkDeep)
        }
    }

    internal open val isTopLevel: Boolean get() = false

    private val _containingFile: PsiFile? by lazyPub {
        val kotlinOrigin = kotlinOrigin ?: return@lazyPub null
        val containingClass = isTopLevel.ifFalse { getOutermostClassOrObject(kotlinOrigin).toLightClass() } ?: this
        SymbolFakeFile(kotlinOrigin, containingClass)
    }

    override fun getContainingFile(): PsiFile? = _containingFile

    private fun PsiClass.hasSuper(
        baseClass: PsiClass,
        checkDeep: Boolean,
        visitedSupers: MutableSet<PsiClass> = mutableSetOf()
    ): Boolean {
        visitedSupers.add(this)
        val notVisitedSupers = supers.filterNot { visitedSupers.contains(it) }
        if (notVisitedSupers.any { it == baseClass }) return true
        if (!checkDeep) return false
        return notVisitedSupers.any { it.hasSuper(baseClass, true, visitedSupers) }
    }

    override fun getText(): String = kotlinOrigin?.text ?: ""

    override fun getLanguage(): KotlinLanguage = KotlinLanguage.INSTANCE

    override fun getPresentation(): ItemPresentation? = ItemPresentationProviders.getItemPresentation(this)

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    override fun getContext(): PsiElement? = parent

    override fun isEquivalentTo(another: PsiElement?): Boolean = PsiClassImplUtil.isClassEquivalentTo(this, another)

    override fun getDocComment(): PsiDocComment? = null

    override fun hasTypeParameters(): Boolean = PsiImplUtil.hasTypeParameters(this)

    override fun getExtendsListTypes(): Array<PsiClassType> = PsiClassImplUtil.getExtendsListTypes(this)

    override fun getImplementsListTypes(): Array<PsiClassType> = PsiClassImplUtil.getImplementsListTypes(this)

    override fun findMethodBySignature(patternMethod: PsiMethod?, checkBases: Boolean): PsiMethod? =
        patternMethod?.let { PsiClassImplUtil.findMethodBySignature(this, it, checkBases) }

    override fun findMethodsBySignature(patternMethod: PsiMethod?, checkBases: Boolean): Array<PsiMethod> =
        patternMethod?.let { PsiClassImplUtil.findMethodsBySignature(this, it, checkBases) } ?: PsiMethod.EMPTY_ARRAY

    override fun findMethodsAndTheirSubstitutorsByName(
        @NonNls name: String?,
        checkBases: Boolean,
    ): List<Pair<PsiMethod?, PsiSubstitutor?>?> = PsiClassImplUtil.findMethodsAndTheirSubstitutorsByName(this, name, checkBases)

    override fun getAllMethodsAndTheirSubstitutors(): List<Pair<PsiMethod?, PsiSubstitutor?>?> {
        return PsiClassImplUtil.getAllWithSubstitutorsByMap(this, PsiClassImplUtil.MemberType.METHOD)
    }

    override fun getRBrace(): PsiElement? = null

    override fun getLBrace(): PsiElement? = null

    override fun getInitializers(): Array<PsiClassInitializer> = PsiClassInitializer.EMPTY_ARRAY

    override fun getElementIcon(flags: Int): Icon? = throw UnsupportedOperationException("This should be done by KotlinIconProvider")

    override fun getVisibleSignatures(): MutableCollection<HierarchicalMethodSignature> = PsiSuperMethodImplUtil.getVisibleSignatures(this)

    override fun setName(name: String): PsiElement? = cannotModify()

    override fun getTextRange(): TextRange? = kotlinOrigin?.textRange ?: TextRange.EMPTY_RANGE

    abstract override fun copy(): PsiElement

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitClass(this)
        } else {
            visitor.visitElement(this)
        }
    }
}

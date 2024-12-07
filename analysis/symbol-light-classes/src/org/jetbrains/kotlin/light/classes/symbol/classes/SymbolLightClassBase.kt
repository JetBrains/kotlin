/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupportBase
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.light.classes.symbol.SymbolFakeFile
import org.jetbrains.kotlin.light.classes.symbol.analyzeForLightClasses
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.toArrayIfNotEmptyOrDefault
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import javax.swing.Icon


internal abstract class SymbolLightClassBase protected constructor(val ktModule: KaModule, manager: PsiManager) :
    LightElement(manager, KotlinLanguage.INSTANCE), PsiClass, KtExtensibleLightClass {

    private val contentFinderCache by lazyPub {
        ClassContentFinderCache(
            extensibleClass = this,
            modificationTrackers = contentModificationTrackers(),
        )
    }

    open fun contentModificationTrackers(): List<ModificationTracker> = listOf(
        KotlinAsJavaSupportBase.getInstance(project).outOfBlockModificationTracker(this)
    )

    override fun getFields(): Array<PsiField> = ownFields.toArrayIfNotEmptyOrDefault(PsiField.EMPTY_ARRAY)

    override fun getMethods(): Array<PsiMethod> = ownMethods.toArrayIfNotEmptyOrDefault(PsiMethod.EMPTY_ARRAY)

    override fun getConstructors(): Array<PsiMethod> = ownConstructors.let { if (it.isEmpty()) it else it.clone() }

    override fun getInnerClasses(): Array<out PsiClass> = ownInnerClasses.toArrayIfNotEmptyOrDefault(PsiClass.EMPTY_ARRAY)

    override fun getAllFields(): Array<PsiField> = PsiClassImplUtil.getAllFields(this)

    override fun getAllMethods(): Array<PsiMethod> = PsiClassImplUtil.getAllMethods(this)

    override fun getAllInnerClasses(): Array<PsiClass> = PsiClassImplUtil.getAllInnerClasses(this)

    override fun findFieldByName(
        name: String,
        checkBases: Boolean,
    ) = contentFinderCache.findFieldByName(name, checkBases)

    override fun findMethodsByName(
        name: String,
        checkBases: Boolean,
    ): Array<PsiMethod> = contentFinderCache.findMethodsByName(name, checkBases)

    override fun findInnerClassByName(
        name: String,
        checkBases: Boolean,
    ): PsiClass? = contentFinderCache.findInnerClassByName(name, checkBases)

    abstract override fun getOwnFields(): List<PsiField>
    abstract override fun getOwnMethods(): List<PsiMethod>
    abstract override fun getOwnInnerClasses(): List<PsiClass>

    open val ownConstructors: Array<PsiMethod> get() = cachedValue { PsiImplUtil.getConstructors(this) }

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
                checkIsInheritor(thisClassOrigin, baseClassOrigin, checkDeep)
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

    override fun findMethodBySignature(patternMethod: PsiMethod, checkBases: Boolean): PsiMethod? =
        PsiClassImplUtil.findMethodBySignature(this, patternMethod, checkBases)

    override fun findMethodsBySignature(patternMethod: PsiMethod, checkBases: Boolean): Array<PsiMethod> =
        PsiClassImplUtil.findMethodsBySignature(this, patternMethod, checkBases)

    override fun findMethodsAndTheirSubstitutorsByName(
        @NonNls name: String,
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

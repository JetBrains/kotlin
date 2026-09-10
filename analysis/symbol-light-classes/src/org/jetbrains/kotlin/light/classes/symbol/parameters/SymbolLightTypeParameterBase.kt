/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.lang.Language
import com.intellij.openapi.util.Pair
import com.intellij.psi.*
import com.intellij.psi.impl.InheritanceImplUtil
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightDeclaration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.light.classes.symbol.basicIsEquivalentTo
import org.jetbrains.kotlin.light.classes.symbol.invalidAccess
import org.jetbrains.kotlin.light.classes.symbol.isOriginEquivalentTo
import org.jetbrains.kotlin.psi.KtTypeParameter
import javax.swing.Icon

/**
 * A base class for type parameters of symbol light declarations.
 *
 * It provides the trivial part of [PsiTypeParameter] as a [PsiClass] and the parent chain to [parent],
 * so subclasses only have to provide the name, the index, the bounds and the annotations.
 */
internal abstract class SymbolLightTypeParameterBase<L : SymbolLightTypeParameterListBase<*>>(
    protected val parent: L,
) : LightElement(parent.manager, KotlinLanguage.INSTANCE), PsiTypeParameter,
    KtLightDeclaration<KtTypeParameter, PsiTypeParameter> {

    override val givenAnnotations: List<KtLightAbstractAnnotation> get() = invalidAccess()

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitTypeParameter(this)
        } else {
            super<LightElement>.accept(visitor)
        }
    }

    abstract override fun getExtendsList(): PsiReferenceList
    override fun getExtendsListTypes(): Array<PsiClassType> = PsiClassImplUtil.getExtendsListTypes(this)

    //PsiClass simple implementation
    override fun getImplementsList(): PsiReferenceList? = null
    override fun getImplementsListTypes(): Array<PsiClassType> = PsiClassImplUtil.getImplementsListTypes(this)
    override fun getSuperClass(): PsiClass? = PsiClassImplUtil.getSuperClass(this)
    override fun getInterfaces(): Array<PsiClass> = PsiClassImplUtil.getInterfaces(this)
    override fun getSupers(): Array<PsiClass> = PsiClassImplUtil.getSupers(this)
    override fun getSuperTypes(): Array<PsiClassType> = PsiClassImplUtil.getSuperTypes(this)
    override fun getConstructors(): Array<PsiMethod> = PsiMethod.EMPTY_ARRAY
    override fun getInitializers(): Array<PsiClassInitializer> = PsiClassInitializer.EMPTY_ARRAY
    override fun getAllFields(): Array<PsiField> = PsiField.EMPTY_ARRAY
    override fun getAllMethods(): Array<PsiMethod> = PsiMethod.EMPTY_ARRAY
    override fun getAllInnerClasses(): Array<PsiClass> = PsiClass.EMPTY_ARRAY
    override fun findFieldByName(name: String?, checkBases: Boolean): PsiField? = null
    override fun findMethodBySignature(patternMethod: PsiMethod, checkBases: Boolean): PsiMethod? = null
    override fun findMethodsBySignature(patternMethod: PsiMethod, checkBases: Boolean): Array<PsiMethod> = PsiMethod.EMPTY_ARRAY
    override fun findMethodsAndTheirSubstitutorsByName(name: String, checkBases: Boolean): List<Pair<PsiMethod, PsiSubstitutor>> =
        emptyList()

    override fun getAllMethodsAndTheirSubstitutors(): List<Pair<PsiMethod, PsiSubstitutor>> = emptyList()
    override fun findInnerClassByName(name: String?, checkBases: Boolean): PsiClass? = null
    override fun getLBrace(): PsiElement? = null
    override fun getRBrace(): PsiElement? = null
    override fun getScope(): PsiElement = parent

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        return InheritanceImplUtil.isInheritor(this, baseClass, checkDeep)
    }

    override fun isInheritorDeep(baseClass: PsiClass, classToByPass: PsiClass?): Boolean {
        return InheritanceImplUtil.isInheritorDeep(this, baseClass, classToByPass)
    }

    override fun getVisibleSignatures(): MutableCollection<HierarchicalMethodSignature> = mutableListOf()
    override fun setName(name: String): PsiElement = cannotModify()
    override fun getNameIdentifier(): PsiIdentifier? = null
    override fun getModifierList(): PsiModifierList? = null
    override fun hasModifierProperty(name: String): Boolean = false
    override fun getOwner(): PsiTypeParameterListOwner = parent.owner
    override fun getParent(): PsiElement = parent
    override fun getContainingClass(): PsiClass? = null
    override fun getDocComment(): PsiDocComment? = null
    override fun isDeprecated(): Boolean = false
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY
    override fun hasTypeParameters(): Boolean = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getQualifiedName(): String? = null
    override fun getMethods(): Array<PsiMethod> = PsiMethod.EMPTY_ARRAY
    override fun findMethodsByName(name: String?, checkBases: Boolean): Array<PsiMethod> = PsiMethod.EMPTY_ARRAY
    override fun getFields(): Array<PsiField> = PsiField.EMPTY_ARRAY
    override fun getInnerClasses(): Array<PsiClass> = PsiClass.EMPTY_ARRAY
    override fun isInterface(): Boolean = false
    override fun isAnnotationType(): Boolean = false
    override fun isEnum(): Boolean = false
    override fun addAnnotation(qualifiedName: String): PsiAnnotation = cannotModify()
    //End of PsiClass simple implementation

    abstract override fun getName(): String
    abstract override fun getIndex(): Int

    override fun toString(): String = this::class.simpleName.orEmpty()
    override fun getLanguage(): Language = KotlinLanguage.INSTANCE

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int

    override fun isEquivalentTo(another: PsiElement): Boolean {
        return basicIsEquivalentTo(this, another) || isOriginEquivalentTo(another)
    }

    override fun getElementIcon(flags: Int): Icon? = null
    override fun getContainingFile(): PsiFile = parent.containingFile
}

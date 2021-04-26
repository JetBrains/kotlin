/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava.elements

import com.intellij.lang.Language
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.asJava.classes.KotlinSuperTypeListBuilder
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightDeclaration
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.asJava.basicIsEquivalentTo
import org.jetbrains.kotlin.idea.asJava.invalidAccess
import org.jetbrains.kotlin.idea.asJava.mapSupertype
import org.jetbrains.kotlin.idea.frontend.api.isValid
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtClassType
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.psiUtil.startOffset

internal class FirLightTypeParameter(
    private val parent: FirLightTypeParameterListForSymbol,
    private val index: Int,
    private val typeParameterSymbol: KtTypeParameterSymbol
) : LightElement(parent.manager, KotlinLanguage.INSTANCE), PsiTypeParameter,
    KtLightDeclaration<KtTypeParameter, PsiTypeParameter> {

    override val clsDelegate: PsiTypeParameter get() = invalidAccess()

    override val givenAnnotations: List<KtLightAbstractAnnotation>? get() = invalidAccess()

    override val kotlinOrigin: KtTypeParameter? = typeParameterSymbol.psi as? KtTypeParameter

    override fun copy(): PsiElement =
        FirLightTypeParameter(parent, index, typeParameterSymbol)

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitTypeParameter(this)
        } else {
            super<LightElement>.accept(visitor)
        }
    }

    private val _extendsList: PsiReferenceList by lazyPub {

        val listBuilder = KotlinSuperTypeListBuilder(
            kotlinOrigin = null,
            manager = manager,
            language = language,
            role = PsiReferenceList.Role.EXTENDS_LIST
        )

        typeParameterSymbol.upperBounds
            .filterIsInstance<KtClassType>()
            .filter { it.classId != StandardClassIds.Any }
            .mapNotNull {
                it.mapSupertype(
                    psiContext = this,
                    kotlinCollectionAsIs = true,
                    annotations = emptyList()
                )
            }
            .forEach { listBuilder.addReference(it) }

        listBuilder
    }

    override fun getExtendsList(): PsiReferenceList = _extendsList

    override fun getExtendsListTypes(): Array<PsiClassType> =
        PsiClassImplUtil.getExtendsListTypes(this)

    //PsiClass simple implementation
    override fun getImplementsList(): PsiReferenceList? = null
    override fun getImplementsListTypes(): Array<PsiClassType> = PsiClassType.EMPTY_ARRAY
    override fun getSuperClass(): PsiClass? = null
    override fun getInterfaces(): Array<PsiClass> = PsiClass.EMPTY_ARRAY
    override fun getSupers(): Array<PsiClass> = PsiClass.EMPTY_ARRAY
    override fun getSuperTypes(): Array<PsiClassType> = PsiClassType.EMPTY_ARRAY
    override fun getConstructors(): Array<PsiMethod> = PsiMethod.EMPTY_ARRAY
    override fun getInitializers(): Array<PsiClassInitializer> = PsiClassInitializer.EMPTY_ARRAY
    override fun getAllFields(): Array<PsiField> = PsiField.EMPTY_ARRAY
    override fun getAllMethods(): Array<PsiMethod> = PsiMethod.EMPTY_ARRAY
    override fun getAllInnerClasses(): Array<PsiClass> = PsiClass.EMPTY_ARRAY
    override fun findFieldByName(name: String?, checkBases: Boolean): PsiField? = null
    override fun findMethodBySignature(patternMethod: PsiMethod?, checkBases: Boolean): PsiMethod? = null
    override fun findMethodsBySignature(patternMethod: PsiMethod?, checkBases: Boolean): Array<PsiMethod> = PsiMethod.EMPTY_ARRAY
    override fun findMethodsAndTheirSubstitutorsByName(name: String?, checkBases: Boolean)
            : MutableList<Pair<PsiMethod, PsiSubstitutor>> = mutableListOf()

    override fun getAllMethodsAndTheirSubstitutors()
            : MutableList<Pair<PsiMethod, PsiSubstitutor>> = mutableListOf()

    override fun findInnerClassByName(name: String?, checkBases: Boolean): PsiClass? = null
    override fun getLBrace(): PsiElement? = null
    override fun getRBrace(): PsiElement? = null
    override fun getScope(): PsiElement = parent
    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean = false
    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean = false
    override fun getVisibleSignatures(): MutableCollection<HierarchicalMethodSignature> = mutableListOf()
    override fun setName(name: String): PsiElement = cannotModify()
    override fun getNameIdentifier(): PsiIdentifier? = null
    override fun getModifierList(): PsiModifierList? = null
    override fun hasModifierProperty(name: String): Boolean = false
    override fun getOwner(): PsiTypeParameterListOwner? = parent.owner
    override fun getParent(): PsiElement = parent
    override fun getAnnotations(): Array<PsiAnnotation> = PsiAnnotation.EMPTY_ARRAY
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
    override fun findAnnotation(qualifiedName: String): PsiAnnotation? = null
    override fun addAnnotation(qualifiedName: String): PsiAnnotation = cannotModify()
    //End of PsiClass simple implementation

    override fun getText(): String = kotlinOrigin?.text ?: ""
    override fun getName(): String? = typeParameterSymbol.name.asString()
    override fun getIndex(): Int = index
    override fun getApplicableAnnotations(): Array<PsiAnnotation> = PsiAnnotation.EMPTY_ARRAY //TODO

    override fun toString(): String = "FirLightTypeParameter:$name"

    override fun getNavigationElement(): PsiElement =
        kotlinOrigin ?: parent.navigationElement

    override fun getLanguage(): Language = KotlinLanguage.INSTANCE

    override fun getUseScope(): SearchScope =
        kotlinOrigin?.useScope ?: parent.useScope

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightTypeParameter && index == other.index && typeParameterSymbol == other.typeParameterSymbol)

    override fun hashCode(): Int = typeParameterSymbol.hashCode() + index

    override fun isEquivalentTo(another: PsiElement): Boolean =
        basicIsEquivalentTo(this, another)

    override fun getTextRange(): TextRange? = kotlinOrigin?.textRange
    override fun getContainingFile(): PsiFile = parent.containingFile
    override fun getTextOffset(): Int = kotlinOrigin?.startOffset ?: super.getTextOffset()
    override fun getStartOffsetInParent(): Int = kotlinOrigin?.startOffsetInParent ?: super.getStartOffsetInParent()

    override fun isValid(): Boolean = super.isValid() && typeParameterSymbol.isValid()
}

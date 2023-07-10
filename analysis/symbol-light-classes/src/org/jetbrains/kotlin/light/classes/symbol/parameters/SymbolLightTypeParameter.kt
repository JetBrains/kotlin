/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.lang.Language
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.search.SearchScope
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.psiSafe
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.analysis.api.types.KtErrorType
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.KotlinSuperTypeListBuilder
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightDeclaration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.psiUtil.startOffset

internal class SymbolLightTypeParameter private constructor(
    private val parent: SymbolLightTypeParameterList,
    private val index: Int,
    private val typeParameterSymbolPointer: KtSymbolPointer<KtTypeParameterSymbol>,
    private val typeParameterDeclaration: KtTypeParameter?,
    override val kotlinOrigin: KtTypeParameter?,
) : LightElement(parent.manager, KotlinLanguage.INSTANCE), PsiTypeParameter,
    KtLightDeclaration<KtTypeParameter, PsiTypeParameter> {

    constructor(
        ktAnalysisSession: KtAnalysisSession,
        parent: SymbolLightTypeParameterList,
        index: Int,
        typeParameterSymbol: KtTypeParameterSymbol,
    ) : this(
        parent = parent,
        index = index,
        typeParameterSymbolPointer = with(ktAnalysisSession) { typeParameterSymbol.createPointer() },
        typeParameterDeclaration = typeParameterSymbol.sourcePsiSafe(),
        kotlinOrigin = typeParameterSymbol.psiSafe(),
    )

    private val ktModule: KtModule get() = parent.ktModule

    private inline fun <T> withTypeParameterSymbol(crossinline action: KtAnalysisSession.(KtTypeParameterSymbol) -> T): T =
        typeParameterSymbolPointer.withSymbol(ktModule, action)

    override val givenAnnotations: List<KtLightAbstractAnnotation> get() = invalidAccess()

    override fun copy(): PsiElement = copyTo(parent)

    internal fun copyTo(parent: SymbolLightTypeParameterList): SymbolLightTypeParameter = SymbolLightTypeParameter(
        parent,
        index,
        typeParameterSymbolPointer,
        typeParameterDeclaration,
        kotlinOrigin,
    )

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitTypeParameter(this)
        } else {
            super<LightElement>.accept(visitor)
        }
    }

    private val _extendsList: PsiReferenceList by lazyPub {
        val listBuilder = KotlinSuperTypeListBuilder(
            this,
            kotlinOrigin = null,
            manager = manager,
            language = language,
            role = PsiReferenceList.Role.EXTENDS_LIST
        )

        withTypeParameterSymbol { typeParameterSymbol ->
            typeParameterSymbol.upperBounds
                .filter { type ->
                    when (type) {
                        is KtNonErrorClassType -> type.classId != StandardClassIds.Any
                        is KtErrorType -> false
                        else -> true
                    }
                }
                .mapNotNull {
                    mapType(it, this@SymbolLightTypeParameter, KtTypeMappingMode.GENERIC_ARGUMENT)
                }
                .forEach { listBuilder.addReference(it) }
        }

        listBuilder
    }

    override fun getExtendsList(): PsiReferenceList = _extendsList
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
    override fun findMethodBySignature(patternMethod: PsiMethod?, checkBases: Boolean): PsiMethod? = null
    override fun findMethodsBySignature(patternMethod: PsiMethod?, checkBases: Boolean): Array<PsiMethod> = PsiMethod.EMPTY_ARRAY
    override fun findMethodsAndTheirSubstitutorsByName(name: String?, checkBases: Boolean): List<Pair<PsiMethod, PsiSubstitutor>> =
        emptyList()

    override fun getAllMethodsAndTheirSubstitutors(): List<Pair<PsiMethod, PsiSubstitutor>> = emptyList()
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
    override fun getOwner(): PsiTypeParameterListOwner = parent.owner
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

    private val _name: String by lazyPub {
        typeParameterDeclaration?.name ?: withTypeParameterSymbol { it.name.asString() }
    }

    override fun getName(): String = _name

    override fun getIndex(): Int = index
    override fun getApplicableAnnotations(): Array<PsiAnnotation> = PsiAnnotation.EMPTY_ARRAY //TODO
    override fun toString(): String = "SymbolLightTypeParameter:$name"

    override fun getNavigationElement(): PsiElement = kotlinOrigin ?: parent.navigationElement
    override fun getLanguage(): Language = KotlinLanguage.INSTANCE

    override fun getUseScope(): SearchScope = kotlinOrigin?.useScope ?: parent.useScope

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightTypeParameter || other.ktModule != ktModule || other.index != index) return false
        if (typeParameterDeclaration != null || other.typeParameterDeclaration != null) {
            return other.typeParameterDeclaration == typeParameterDeclaration
        }

        return other.kotlinOrigin == kotlinOrigin &&
                compareSymbolPointers(typeParameterSymbolPointer, other.typeParameterSymbolPointer) &&
                other.parent == parent
    }

    override fun hashCode(): Int = typeParameterDeclaration?.hashCode() ?: name.hashCode()
    override fun isEquivalentTo(another: PsiElement): Boolean {
        return basicIsEquivalentTo(this, another) || isOriginEquivalentTo(another)
    }

    override fun getText(): String? = typeParameterDeclaration?.text
    override fun getTextRange(): TextRange? = typeParameterDeclaration?.textRange
    override fun getContainingFile(): PsiFile = parent.containingFile
    override fun getTextOffset(): Int = typeParameterDeclaration?.startOffset ?: -1
    override fun getStartOffsetInParent(): Int = typeParameterDeclaration?.startOffsetInParent ?: -1

    override fun isValid(): Boolean = super.isValid() && typeParameterDeclaration?.isValid ?: typeParameterSymbolPointer.isValid(ktModule)
}

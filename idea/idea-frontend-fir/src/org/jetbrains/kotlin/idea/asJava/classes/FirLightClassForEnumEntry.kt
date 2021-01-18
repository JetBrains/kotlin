/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.asJava.*
import org.jetbrains.kotlin.idea.asJava.FirLightClassModifierList
import org.jetbrains.kotlin.idea.asJava.FirLightPsiJavaCodeReferenceElementWithNoReference
import org.jetbrains.kotlin.idea.asJava.classes.createMethods
import org.jetbrains.kotlin.idea.frontend.api.fir.analyzeWithSymbolAsContext
import org.jetbrains.kotlin.idea.frontend.api.isValid
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.KtClassOrObject

internal class FirLightClassForEnumEntry(
    private val enumEntrySymbol: KtEnumEntrySymbol,
    private val enumConstant: FirLightFieldForEnumEntry,
    private val enumClass: FirLightClassForSymbol,
    manager: PsiManager
) : FirLightClassBase(manager), PsiEnumConstantInitializer {

    override fun getName(): String? = enumEntrySymbol.name.asString()

    override fun getBaseClassType(): PsiClassType = enumConstant.type as PsiClassType //???TODO

    override fun getBaseClassReference(): PsiJavaCodeReferenceElement =
        FirLightPsiJavaCodeReferenceElementWithNoReference(enumConstant) //???TODO

    override fun getArgumentList(): PsiExpressionList? = null

    override fun getEnumConstant(): PsiEnumConstant = enumConstant

    override fun isInQualifiedNew(): Boolean = false

    override fun equals(other: Any?): Boolean =
        other is FirLightClassForEnumEntry &&
                this.enumEntrySymbol == other.enumEntrySymbol

    override fun hashCode(): Int =
        enumEntrySymbol.hashCode()

    override fun copy(): PsiElement =
        FirLightClassForEnumEntry(enumEntrySymbol, enumConstant, enumClass, manager)

    override fun toString(): String = "FirLightClassForEnumEntry for $name"

    override fun getNameIdentifier(): PsiIdentifier? = null //TODO

    private val _modifierList: PsiModifierList by lazyPub {
        FirLightClassModifierList(
            containingDeclaration = this,
            modifiers = setOf(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL),
            annotations = emptyList()
        )
    }

    override fun getModifierList(): PsiModifierList? = _modifierList

    override fun hasModifierProperty(name: String): Boolean =
        name == PsiModifier.PUBLIC || name == PsiModifier.STATIC || name == PsiModifier.FINAL

    override fun getContainingClass(): PsiClass? = enumClass

    override fun isDeprecated(): Boolean = false

    override fun getTypeParameters(): Array<PsiTypeParameter> = emptyArray()

    override fun getTypeParameterList(): PsiTypeParameterList? = null

    override fun getQualifiedName(): String? = "${enumConstant.containingClass.qualifiedName}.${enumConstant.name}"

    override fun isInterface(): Boolean = false

    override fun isAnnotationType(): Boolean = false

    override fun isEnum(): Boolean = false

    private val _extendsList: PsiReferenceList? by lazyPub {

        val mappedType = enumEntrySymbol.annotatedType
            .mapSupertype(this@FirLightClassForEnumEntry)
            ?: return@lazyPub null

        KotlinSuperTypeListBuilder(
            kotlinOrigin = enumClass.kotlinOrigin?.getSuperTypeList(),
            manager = manager,
            language = language,
            role = PsiReferenceList.Role.EXTENDS_LIST
        ).also {
            it.addReference(mappedType)
        }
    }

    override fun getExtendsList(): PsiReferenceList? = _extendsList

    override fun getImplementsList(): PsiReferenceList? = null

    override fun getSuperClass(): PsiClass? = enumClass

    override fun getInterfaces(): Array<PsiClass> = PsiClass.EMPTY_ARRAY

    override fun getSupers(): Array<PsiClass> = arrayOf(enumClass)

    override fun getSuperTypes(): Array<PsiClassType> = PsiClassImplUtil.getSuperTypes(this)

    override fun getParent(): PsiElement? = containingClass ?: containingFile

    override fun getScope(): PsiElement? = parent

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean = false //TODO

    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean = false //TODO

    override val kotlinOrigin: KtClassOrObject? = enumConstant.kotlinOrigin

    override val originKind: LightClassOriginKind = LightClassOriginKind.SOURCE

    override fun getOwnFields(): MutableList<PsiField> = mutableListOf()

    override fun getOwnMethods(): MutableList<KtLightMethod> {
        val result = mutableListOf<KtLightMethod>()

        analyzeWithSymbolAsContext(enumEntrySymbol) {
            val callableSymbols = enumEntrySymbol.getDeclaredMemberScope().getCallableSymbols()
            createMethods(callableSymbols, result)
        }

        return result
    }

    override fun getOwnInnerClasses(): MutableList<PsiClass> = mutableListOf()

    override fun isValid(): Boolean = super.isValid() && enumEntrySymbol.isValid()
}

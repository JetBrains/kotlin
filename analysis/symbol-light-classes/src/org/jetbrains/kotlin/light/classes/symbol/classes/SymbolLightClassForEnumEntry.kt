/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.isValid
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.asJava.classes.KotlinSuperTypeListBuilder
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.light.classes.symbol.codeReferences.SymbolLightPsiJavaCodeReferenceElementWithNoReference
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.KtClassOrObject

context(KtAnalysisSession)
internal class SymbolLightClassForEnumEntry(
    private val enumEntrySymbol: KtEnumEntrySymbol,
    private val enumConstant: SymbolLightFieldForEnumEntry,
    private val enumClass: SymbolLightClass,
    manager: PsiManager
) : SymbolLightClassBase(manager), PsiEnumConstantInitializer {

    override fun getName(): String? = enumEntrySymbol.name.asString()

    override fun getBaseClassType(): PsiClassType = enumConstant.type as PsiClassType //???TODO

    override fun getBaseClassReference(): PsiJavaCodeReferenceElement =
        SymbolLightPsiJavaCodeReferenceElementWithNoReference(enumConstant) //???TODO

    override fun getArgumentList(): PsiExpressionList? = null

    override fun getEnumConstant(): PsiEnumConstant = enumConstant

    override fun isInQualifiedNew(): Boolean = false

    override fun equals(other: Any?): Boolean =
        other is SymbolLightClassForEnumEntry &&
                this.enumEntrySymbol == other.enumEntrySymbol

    override fun hashCode(): Int =
        enumEntrySymbol.hashCode()

    override fun copy(): PsiElement =
        SymbolLightClassForEnumEntry(enumEntrySymbol, enumConstant, enumClass, manager)

    override fun toString(): String = "SymbolLightClassForEnumEntry:$name"

    override fun getNameIdentifier(): PsiIdentifier? = null //TODO

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightClassModifierList(
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
        val mappedType =
            enumEntrySymbol.returnType.asPsiType(this@SymbolLightClassForEnumEntry, KtTypeMappingMode.SUPER_TYPE) as? PsiClassType
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

        val declaredMemberScope = enumEntrySymbol.getDeclaredMemberScope()

        createMethods(declaredMemberScope.getCallableSymbols(), result)
        createConstructors(declaredMemberScope.getConstructors(), result)

        return result
    }

    override fun getOwnInnerClasses(): MutableList<PsiClass> = mutableListOf()

    override fun isValid(): Boolean = super.isValid() && enumEntrySymbol.isValid()
}

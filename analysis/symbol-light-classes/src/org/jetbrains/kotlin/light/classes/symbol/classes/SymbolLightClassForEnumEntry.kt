/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.KotlinSuperTypeListBuilder
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.light.classes.symbol.codeReferences.SymbolLightPsiJavaCodeReferenceElementWithNoReference
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList

internal class SymbolLightClassForEnumEntry(
    private val enumConstant: SymbolLightFieldForEnumEntry,
    private val enumClass: SymbolLightClassForClassOrObject,
    ktModule: KtModule,
) : SymbolLightClassForClassOrObject(
    enumConstant.kotlinOrigin,
    ktModule,
), PsiEnumConstantInitializer {
    override fun getBaseClassType(): PsiClassType = enumConstant.type as PsiClassType //???TODO

    override fun getBaseClassReference(): PsiJavaCodeReferenceElement =
        SymbolLightPsiJavaCodeReferenceElementWithNoReference(enumConstant) //???TODO

    override fun getArgumentList(): PsiExpressionList? = null

    override fun getEnumConstant(): PsiEnumConstant = enumConstant

    override fun isInQualifiedNew(): Boolean = false

    override fun copy() = SymbolLightClassForEnumEntry(enumConstant, enumClass, ktModule)

    override fun equals(other: Any?): Boolean = this === other ||
            other is SymbolLightClassForEnumEntry && other.enumConstant == enumConstant

    override fun hashCode(): Int = enumConstant.hashCode()

    override fun toString(): String = "SymbolLightClassForEnumEntry:$name"

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightClassModifierList(
            containingDeclaration = this,
            lazyModifiers = lazyOf(setOf(PsiModifier.STATIC, PsiModifier.FINAL)),
            lazyAnnotations = lazyOf(emptyList()),
        )
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun hasModifierProperty(name: String): Boolean = name == PsiModifier.STATIC || name == PsiModifier.FINAL

    override fun getContainingClass(): PsiClass = enumClass

    override fun getTypeParameters(): Array<PsiTypeParameter> = emptyArray()
    override fun getTypeParameterList(): PsiTypeParameterList? = null

    override fun getQualifiedName(): String = "${enumConstant.containingClass.qualifiedName}.${enumConstant.name}"

    override fun isEnum(): Boolean = false

    private val _extendsList: PsiReferenceList? by lazyPub {
        val mappedType = enumConstant.withEnumEntrySymbol { symbol ->
            symbol.returnType.asPsiType(this@SymbolLightClassForEnumEntry, KtTypeMappingMode.SUPER_TYPE) as? PsiClassType
        } ?: return@lazyPub null

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
    override fun getSuperClass(): PsiClass = enumClass
    override fun getInterfaces(): Array<PsiClass> = PsiClass.EMPTY_ARRAY
    override fun getSupers(): Array<PsiClass> = arrayOf(enumClass)

    override fun getParent(): PsiElement? = containingClass

    override fun getScope(): PsiElement? = parent

    private val _ownFields: List<KtLightField> by lazyPub {
        enumConstant.withEnumEntrySymbol { enumEntrySymbol ->
            val result = mutableListOf<KtLightField>()

            // Then, add instance fields: properties from parameters, and then member properties
            addPropertyBackingFields(result, enumEntrySymbol)

            result
        }
    }

    override fun getOwnFields(): List<KtLightField> = _ownFields

    private val _ownMethods: List<KtLightMethod> by lazyPub {
        enumConstant.withEnumEntrySymbol { enumEntrySymbol ->
            val result = mutableListOf<KtLightMethod>()

            val declaredMemberScope = enumEntrySymbol.getDeclaredMemberScope()
            val visibleDeclarations = declaredMemberScope.getCallableSymbols()

            val suppressStatic = isCompanionObject
            createMethods(visibleDeclarations, result, suppressStatic = suppressStatic)

            createConstructors(declaredMemberScope.getConstructors(), result)

            result
        }
    }

    override fun getOwnMethods(): List<KtLightMethod> = _ownMethods
    override fun getOwnInnerClasses(): MutableList<PsiClass> = mutableListOf()
}

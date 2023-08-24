/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.KotlinSuperTypeListBuilder
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.codeReferences.SymbolLightPsiJavaCodeReferenceElementWithNoReference
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightFieldForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.isOriginEquivalentTo
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.InitializedModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.psi.KtEnumEntry

internal class SymbolLightClassForEnumEntry(
    private val enumConstant: SymbolLightFieldForEnumEntry,
    private val enumClass: SymbolLightClassBase,
    ktModule: KtModule,
) : SymbolLightClassBase(ktModule, enumConstant.manager), PsiEnumConstantInitializer {
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

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return super.isEquivalentTo(another) || isOriginEquivalentTo(another)
    }

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightClassModifierList(
            containingDeclaration = this,
            modifiersBox = InitializedModifiersBox(PsiModifier.STATIC, PsiModifier.FINAL),
        )
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun hasModifierProperty(name: String): Boolean = name == PsiModifier.STATIC || name == PsiModifier.FINAL

    override fun getContainingClass(): PsiClass = enumClass

    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY
    override fun getTypeParameterList(): PsiTypeParameterList? = null

    override fun getQualifiedName(): String = "${enumConstant.containingClass.qualifiedName}.${enumConstant.name}"

    override fun isEnum(): Boolean = false

    private val _extendsList: PsiReferenceList? by lazyPub {
        val mappedType = enumConstant.withEnumEntrySymbol { symbol ->
            symbol.returnType.asPsiType(
                this@SymbolLightClassForEnumEntry,
                allowErrorTypes = true,
                KtTypeMappingMode.SUPER_TYPE
            ) as? PsiClassType
        } ?: return@lazyPub null

        KotlinSuperTypeListBuilder(
            this,
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
    override fun getSuperTypes(): Array<PsiClassType> = arrayOf(baseClassType)

    override fun getParent(): PsiElement = containingClass

    override fun getScope(): PsiElement = parent

    override fun getOwnFields(): List<KtLightField> = cachedValue {
        enumConstant.withEnumEntrySymbol { enumEntrySymbol ->
            val result = mutableListOf<KtLightField>()

            // Then, add instance fields: properties from parameters, and then member properties
            enumEntrySymbol.enumEntryInitializer?.let { initializer ->
                addPropertyBackingFields(
                    result,
                    initializer,

                    // `addPropertyBackingFields` detects that property fields should be static when the given symbol with members is an
                    // object. Unfortunately, the enum entry's initializer is an anonymous object, yet we want the enum entry's light class
                    // to have non-static properties.
                    forceIsStaticTo = false,
                )
            }

            result
        }
    }

    override fun getOwnMethods(): List<KtLightMethod> = cachedValue {
        enumConstant.withEnumEntrySymbol { enumEntrySymbol ->
            val result = mutableListOf<KtLightMethod>()

            enumEntrySymbol.enumEntryInitializer?.let { initializer ->
                val declaredMemberScope = initializer.getDeclaredMemberScope()
                val visibleDeclarations = declaredMemberScope.getCallableSymbols()

                createMethods(visibleDeclarations, result)
                createConstructors(declaredMemberScope.getConstructors(), result)
            }

            result
        }
    }

    override fun getOwnInnerClasses(): List<PsiClass> = emptyList()

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        if (!checkDeep) return baseClass == enumClass

        return super.isInheritor(baseClass, checkDeep = true)
    }

    // probably should be dropped after KT-54798
    override fun getNameIdentifier(): PsiIdentifier = KtLightIdentifier(this, kotlinOrigin)
    override fun getName(): String? = kotlinOrigin.name

    override fun isDeprecated(): Boolean = false
    override fun isInterface(): Boolean = false
    override fun isAnnotationType(): Boolean = false
    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean = false
    override val kotlinOrigin: KtEnumEntry get() = enumConstant.kotlinOrigin
    override val originKind: LightClassOriginKind = LightClassOriginKind.SOURCE
    override fun isValid(): Boolean = enumConstant.isValid
}

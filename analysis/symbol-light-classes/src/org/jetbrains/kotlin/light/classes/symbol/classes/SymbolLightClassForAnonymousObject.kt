/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.symbols.KtAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.symbolPointerOfType
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.getParentForLocalDeclaration
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.psi.KtClassOrObject

internal class SymbolLightClassForAnonymousObject : SymbolLightClassForClassLike<KtAnonymousObjectSymbol>, PsiAnonymousClass {
    constructor(
        anonymousObjectDeclaration: KtClassOrObject,
        ktModule: KtModule,
    ) : this(
        classOrObjectDeclaration = anonymousObjectDeclaration,
        classOrObjectSymbolPointer = anonymousObjectDeclaration.symbolPointerOfType(),
        ktModule = ktModule,
        manager = anonymousObjectDeclaration.manager,
    )

    private constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classOrObjectSymbolPointer: KtSymbolPointer<KtAnonymousObjectSymbol>,
        ktModule: KtModule,
        manager: PsiManager,
    ) : super(classOrObjectDeclaration, classOrObjectSymbolPointer, ktModule, manager)

    private val _baseClassType: PsiClassType by lazyPub {
        extendsListTypes.firstOrNull()
            ?: implementsListTypes.firstOrNull()
            ?: PsiType.getJavaLangObject(manager, resolveScope)
    }

    override fun getBaseClassReference(): PsiJavaCodeReferenceElement =
        JavaPsiFacade.getElementFactory(manager.project).createReferenceElementByType(baseClassType)

    override fun getBaseClassType(): PsiClassType = _baseClassType

    private val _extendsList by lazyPub {
        withClassOrObjectSymbol {
            createInheritanceList(forExtendsList = true, it.superTypes)
        }
    }

    private val _implementsList by lazyPub {
        withClassOrObjectSymbol {
            createInheritanceList(forExtendsList = false, it.superTypes)
        }
    }

    override fun getExtendsList(): PsiReferenceList? = _extendsList
    override fun getImplementsList(): PsiReferenceList? = _implementsList

    override fun getOwnMethods(): List<PsiMethod> = cachedValue {
        withClassOrObjectSymbol {
            val result = mutableListOf<KtLightMethod>()
            val declaredMemberScope = it.getDeclaredMemberScope()

            createMethods(declaredMemberScope.getCallableSymbols(), result)
            createConstructors(declaredMemberScope.getConstructors(), result)
            result
        }
    }

    override fun getOwnFields(): List<KtLightField> = cachedValue {
        val result = mutableListOf<KtLightField>()
        val nameGenerator = SymbolLightField.FieldNameGenerator()

        withClassOrObjectSymbol {
            it.getDeclaredMemberScope().getCallableSymbols()
                .filterIsInstance<KtPropertySymbol>()
                .forEach { propertySymbol ->
                    createField(
                        propertySymbol,
                        nameGenerator,
                        isStatic = false,
                        result,
                    )
                }

            result
        }
    }

    override fun getParent(): PsiElement? = kotlinOrigin?.let(::getParentForLocalDeclaration)
    override fun getArgumentList(): PsiExpressionList? = null
    override fun isInQualifiedNew(): Boolean = false
    override fun getName(): String? = null
    override fun getNameIdentifier(): KtLightIdentifier? = null
    override fun getModifierList(): PsiModifierList? = null
    override fun hasModifierProperty(name: String): Boolean = name == PsiModifier.FINAL

    override fun classKind(): KtClassKind = KtClassKind.ANONYMOUS_OBJECT

    override fun getContainingClass(): PsiClass? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getQualifiedName(): String? = null
    override fun copy() = SymbolLightClassForAnonymousObject(classOrObjectDeclaration, classOrObjectSymbolPointer, ktModule, manager)
}

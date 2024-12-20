/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiSymbolPointerCreator
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.asJava.classes.getParentForLocalDeclaration
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.psi.KtClassOrObject

internal class SymbolLightClassForAnonymousObject : SymbolLightClassForClassLike<KaAnonymousObjectSymbol>, PsiAnonymousClass {
    @OptIn(KaImplementationDetail::class)
    constructor(
        anonymousObjectDeclaration: KtClassOrObject,
        ktModule: KaModule,
    ) : this(
        classOrObjectDeclaration = anonymousObjectDeclaration,
        classSymbolPointer = KaPsiSymbolPointerCreator.symbolPointerOfType(anonymousObjectDeclaration),
        ktModule = ktModule,
        manager = anonymousObjectDeclaration.manager,
    )

    private constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classSymbolPointer: KaSymbolPointer<KaAnonymousObjectSymbol>,
        ktModule: KaModule,
        manager: PsiManager,
    ) : super(classOrObjectDeclaration, classSymbolPointer, ktModule, manager)

    private val _baseClassType: PsiClassType by lazyPub {
        extendsListTypes.firstOrNull()
            ?: implementsListTypes.firstOrNull()
            ?: PsiType.getJavaLangObject(manager, resolveScope)
    }

    override fun getBaseClassReference(): PsiJavaCodeReferenceElement =
        JavaPsiFacade.getElementFactory(manager.project).createReferenceElementByType(baseClassType)

    override fun getBaseClassType(): PsiClassType = _baseClassType

    private val _extendsList by lazyPub {
        withClassSymbol {
            createInheritanceList(this@SymbolLightClassForAnonymousObject, forExtendsList = true, it.superTypes)
        }
    }

    private val _implementsList by lazyPub {
        withClassSymbol {
            createInheritanceList(this@SymbolLightClassForAnonymousObject, forExtendsList = false, it.superTypes)
        }
    }

    override fun getExtendsList(): PsiReferenceList? = _extendsList
    override fun getImplementsList(): PsiReferenceList? = _implementsList

    override fun getOwnMethods(): List<PsiMethod> = cachedValue {
        withClassSymbol {
            val result = mutableListOf<PsiMethod>()
            val declaredMemberScope = it.declaredMemberScope

            createMethods(this@SymbolLightClassForAnonymousObject, declaredMemberScope.callables, result)
            createConstructors(this@SymbolLightClassForAnonymousObject, declaredMemberScope.constructors, result)
            result
        }
    }

    override fun getOwnFields(): List<PsiField> = cachedValue {
        val result = mutableListOf<PsiField>()
        val nameGenerator = SymbolLightField.FieldNameGenerator()

        withClassSymbol {
            it.declaredMemberScope.callables
                .filterIsInstance<KaPropertySymbol>()
                .forEach { propertySymbol ->
                    createAndAddField(
                        this@SymbolLightClassForAnonymousObject,
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

    override fun classKind(): KaClassKind = KaClassKind.ANONYMOUS_OBJECT

    override fun getContainingClass(): PsiClass? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getQualifiedName(): String? = null
    override fun copy() = SymbolLightClassForAnonymousObject(classOrObjectDeclaration, classSymbolPointer, ktModule, manager)
}

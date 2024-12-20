/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiField
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiSymbolPointerCreator
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.psi.KtClassOrObject

internal class SymbolLightClassForValueClass : SymbolLightClassForClassOrObject {
    @OptIn(KaImplementationDetail::class)
    constructor(
        classOrObject: KtClassOrObject,
        ktModule: KaModule,
    ) : this(
        classOrObjectDeclaration = classOrObject,
        classSymbolPointer = KaPsiSymbolPointerCreator.symbolPointerOfType(classOrObject),
        ktModule = ktModule,
        manager = classOrObject.manager,
    ) {
        require(classOrObject.hasModifier(KtTokens.INLINE_KEYWORD) || classOrObject.hasModifier(KtTokens.VALUE_KEYWORD))
    }

    constructor(
        ktAnalysisSession: KaSession,
        ktModule: KaModule,
        classSymbol: KaNamedClassSymbol,
        manager: PsiManager,
    ) : super(
        ktAnalysisSession = ktAnalysisSession,
        ktModule = ktModule,
        classSymbol = classSymbol,
        manager = manager,
    ) {
        require(classSymbol.isInline)
    }

    private constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classSymbolPointer: KaSymbolPointer<KaNamedClassSymbol>,
        ktModule: KaModule,
        manager: PsiManager,
    ) : super(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classSymbolPointer = classSymbolPointer,
        ktModule = ktModule,
        manager = manager,
    )

    override val ownConstructors: Array<PsiMethod> get() = PsiMethod.EMPTY_ARRAY

    override fun getOwnMethods(): List<PsiMethod> = cachedValue {
        withClassSymbol { classSymbol ->
            val result = mutableListOf<PsiMethod>()

            // Value classes have overridden methods
            val applicableDeclarations = classSymbol.declaredMemberScope
                .callables
                .filter {
                    (it as? KaPropertySymbol)?.isOverride == true || (it as? KaNamedFunctionSymbol)?.isOverride == true
                }
                .filterNot {
                    hasTypeForValueClassInSignature(it)
                }

            createMethods(this@SymbolLightClassForValueClass, applicableDeclarations, result)
            generateMethodsFromAny(classSymbol, result)

            val propertySymbol = propertySymbol(classSymbol)

            // Only public properties have accessors for value classes
            if (propertySymbol != null && propertySymbol.visibility == KaSymbolVisibility.PUBLIC) {
                // (inline or) value class primary constructor must have only final read-only (val) property parameter
                // Even though the property parameter is mutable (for some reasons, e.g., testing or not checked yet),
                // we can enforce immutability here.
                createPropertyAccessors(this@SymbolLightClassForValueClass, result, propertySymbol, isTopLevel = false, isMutable = false)
            }

            addDelegatesToInterfaceMethods(result, classSymbol)

            result
        }
    }

    override fun getOwnFields(): List<PsiField> = cachedValue {
        withClassSymbol { classSymbol ->
            buildList {
                val nameGenerator = SymbolLightField.FieldNameGenerator()
                addCompanionObjectFieldIfNeeded(this, classSymbol)

                addFieldsFromCompanionIfNeeded(this, classSymbol, nameGenerator)

                propertySymbol(classSymbol)?.let {
                    createAndAddField(this@SymbolLightClassForValueClass, it, nameGenerator, isStatic = false, result = this)
                }
            }
        }
    }

    override fun copy(): SymbolLightClassForValueClass =
        SymbolLightClassForValueClass(classOrObjectDeclaration, classSymbolPointer, ktModule, manager)
}

private fun KaSession.propertySymbol(classSymbol: KaNamedClassSymbol): KaKotlinPropertySymbol? {
    return classSymbol.declaredMemberScope
        .constructors
        .singleOrNull { it.isPrimary }
        ?.valueParameters
        ?.singleOrNull()
        ?.generatedPrimaryConstructorProperty
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.symbolPointerOfType
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

internal class SymbolLightClassForInlineClass : SymbolLightClassForClassOrObject {
    constructor(
        classOrObject: KtClassOrObject,
        ktModule: KtModule,
    ) : this(
        classOrObjectDeclaration = classOrObject,
        classOrObjectSymbolPointer = classOrObject.symbolPointerOfType(),
        ktModule = ktModule,
        manager = classOrObject.manager,
    ) {
        require(classOrObject.hasModifier(KtTokens.INLINE_KEYWORD))
    }

    private constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classOrObjectSymbolPointer: KtSymbolPointer<KtNamedClassOrObjectSymbol>,
        ktModule: KtModule,
        manager: PsiManager,
    ) : super(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classOrObjectSymbolPointer = classOrObjectSymbolPointer,
        ktModule = ktModule,
        manager = manager,
    )

    override fun getOwnMethods(): List<PsiMethod> = cachedValue {
        withClassOrObjectSymbol { classOrObjectSymbol ->
            val result = mutableListOf<KtLightMethod>()

            val declaredMemberScope = classOrObjectSymbol.getDeclaredMemberScope()
            val applicableDeclarations = declaredMemberScope.getCallableSymbols()
                .filter {
                    (it as? KtPropertySymbol)?.isOverride == true || (it as? KtFunctionSymbol)?.isOverride == true
                }
                .filterNot {
                    it.deprecationStatus?.deprecationLevel == DeprecationLevelValue.HIDDEN
                }

            createMethods(applicableDeclarations, result, suppressStatic = false)

            val inlineClassParameterSymbol = declaredMemberScope.getConstructors()
                .singleOrNull { it.isPrimary }
                ?.valueParameters
                ?.singleOrNull()

            if (inlineClassParameterSymbol != null) {
                val propertySymbol = declaredMemberScope.getCallableSymbols(inlineClassParameterSymbol.name)
                    .singleOrNull { it is KtPropertySymbol && it.isFromPrimaryConstructor } as? KtPropertySymbol

                if (propertySymbol != null) {
                    // (inline or) value class primary constructor must have only final read-only (val) property parameter
                    // Even though the property parameter is mutable (for some reasons, e.g., testing or not checked yet),
                    // we can enforce immutability here.
                    createPropertyAccessors(result, propertySymbol, isTopLevel = false, isMutable = false)
                }
            }


            result
        }
    }

    override fun getOwnFields(): List<KtLightField> = cachedValue {
        withClassOrObjectSymbol { classOrObjectSymbol ->
            mutableListOf<KtLightField>().apply {
                addPropertyBackingFields(this, classOrObjectSymbol)
            }
        }
    }

    override fun copy(): SymbolLightClassForInlineClass =
        SymbolLightClassForInlineClass(classOrObjectDeclaration, classOrObjectSymbolPointer, ktModule, manager)
}

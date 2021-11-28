/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.light.classes.symbol.classes.createMethods
import org.jetbrains.kotlin.light.classes.symbol.classes.createPropertyAccessors
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

internal class FirLightInlineClass(
    private val classOrObjectSymbol: KtNamedClassOrObjectSymbol,
    manager: PsiManager
) : FirLightClassForSymbol(classOrObjectSymbol, manager) {

    init {
        require(classOrObjectSymbol.isInline)
    }

    private val _ownMethods: List<KtLightMethod> by lazyPub {
        val result = mutableListOf<KtLightMethod>()

        analyzeWithSymbolAsContext(classOrObjectSymbol) {
            val declaredMemberScope = classOrObjectSymbol.getDeclaredMemberScope()
            val applicableDeclarations = declaredMemberScope.getCallableSymbols()
                .filter {
                    (it as? KtPropertySymbol)?.isOverride == true || (it as? KtFunctionSymbol)?.isOverride == true
                }
                .filterNot {
                    it.deprecationStatus?.deprecationLevel == DeprecationLevelValue.HIDDEN
                }

            createMethods(applicableDeclarations, result, suppressStaticForMethods = false)

            val inlineClassParameterSymbol =
                declaredMemberScope.getConstructors().singleOrNull { it.isPrimary }?.valueParameters?.singleOrNull()
            if (inlineClassParameterSymbol != null) {
                val propertySymbol = declaredMemberScope.getCallableSymbols { it == inlineClassParameterSymbol.name }
                    .singleOrNull { it is KtPropertySymbol && it.isFromPrimaryConstructor } as? KtPropertySymbol
                if (propertySymbol != null) {
                    // (inline or) value class primary constructor must have only final read-only (val) property parameter
                    // Even though the property parameter is mutable (for some reasons, e.g., testing or not checked yet),
                    // we can enforce immutability here.
                    createPropertyAccessors(result, propertySymbol, isTopLevel = false, isMutable = false)
                }
            }
        }

        result
    }

    override fun getOwnMethods(): List<PsiMethod> = _ownMethods

    override fun getOwnFields(): List<KtLightField> = emptyList()

    override fun copy(): FirLightInlineClass =
        FirLightInlineClass(classOrObjectSymbol, manager)
}

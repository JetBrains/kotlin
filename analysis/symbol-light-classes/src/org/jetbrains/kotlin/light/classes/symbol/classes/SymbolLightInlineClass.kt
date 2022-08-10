/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue

context(KtAnalysisSession)
internal class SymbolLightInlineClass(
    private val classOrObjectSymbol: KtNamedClassOrObjectSymbol,
    manager: PsiManager
) : SymbolLightClass(classOrObjectSymbol, manager) {

    init {
        require(classOrObjectSymbol.isInline)
    }

    private val _ownMethods: List<KtLightMethod> by lazyPub {
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


        result
    }

    private val _ownFields: List<KtLightField> by lazyPub {
        mutableListOf<KtLightField>().apply {
            addPropertyBackingFields(this)
        }
    }

    override fun getOwnMethods(): List<PsiMethod> = _ownMethods

    override fun getOwnFields(): List<KtLightField> = _ownFields

    override fun copy(): SymbolLightInlineClass = SymbolLightInlineClass(classOrObjectSymbol, manager)
}

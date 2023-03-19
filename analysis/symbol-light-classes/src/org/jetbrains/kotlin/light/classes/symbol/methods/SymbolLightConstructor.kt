/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.with
import java.util.*

internal class SymbolLightConstructor(
    ktAnalysisSession: KtAnalysisSession,
    constructorSymbol: KtConstructorSymbol,
    containingClass: SymbolLightClassBase,
    methodIndex: Int,
    argumentsSkipMask: BitSet? = null,
) : SymbolLightMethod<KtConstructorSymbol>(
    ktAnalysisSession = ktAnalysisSession,
    functionSymbol = constructorSymbol,
    lightMemberOrigin = null,
    containingClass = containingClass,
    methodIndex = methodIndex,
    argumentsSkipMask = argumentsSkipMask,
) {
    private val _name: String? = containingClass.name

    override fun getName(): String = _name ?: ""

    override fun isConstructor(): Boolean = true
    override fun isOverride(): Boolean = false

    override fun hasTypeParameters(): Boolean = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    private val _modifierList: PsiModifierList by lazyPub {
        val initialValue = if (this.containingClass is SymbolLightClassForEnumEntry) {
            GranularModifiersBox.VISIBILITY_MODIFIERS_MAP.with(PsiModifier.PACKAGE_LOCAL)
        } else {
            emptyMap()
        }

        SymbolLightMemberModifierList(
            containingDeclaration = this,
            modifiersBox = GranularModifiersBox(
                initialValue = initialValue,
                computer = ::computeModifiers,
            ),
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(
                    ktModule = ktModule,
                    annotatedSymbolPointer = functionSymbolPointer,
                )
            ),
        )
    }

    private fun computeModifiers(modifier: String): Map<String, Boolean>? {
        if (modifier !in GranularModifiersBox.VISIBILITY_MODIFIERS) return null
        return GranularModifiersBox.computeVisibilityForMember(ktModule, functionSymbolPointer)
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun getReturnType(): PsiType? = null
}

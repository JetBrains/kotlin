/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.NullabilityType
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeAnnotations
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForEnumEntry
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.toPsiVisibilityForMember
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

    override fun hasTypeParameters(): Boolean = false
    override fun getTypeParameterList(): PsiTypeParameterList? = null
    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    private val _modifierList: PsiModifierList by lazyPub {
        val (staticModifiers, lazyModifiers) = if (containingClass is SymbolLightClassForEnumEntry) {
            setOf(PsiModifier.PACKAGE_LOCAL) to null
        } else {
            emptySet<String>() to lazyPub {
                // FIR treats an enum entry as an anonymous object w/ its own ctor (not default one).
                // On the other hand, FE 1.0 doesn't add anything; then ULC adds default ctor w/ package local visibility.
                // Technically, an enum entry should not be instantiated anywhere else, and thus FIR's modeling makes sense.
                // But, to be backward compatible, we manually force the visibility of enum entry ctor to be package private.
                withFunctionSymbol { constructorSymbol ->
                    setOf(constructorSymbol.toPsiVisibilityForMember())
                }
            }
        }

        SymbolLightMemberModifierList(
            containingDeclaration = this,
            staticModifiers = staticModifiers,
            lazyModifiers = lazyModifiers,
        ) { modifierList ->
            withFunctionSymbol { constructorSymbol ->
                constructorSymbol.computeAnnotations(
                    modifierList = modifierList,
                    nullability = NullabilityType.Unknown,
                    annotationUseSiteTarget = null,
                )
            }
        }
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun getReturnType(): PsiType? = null
}

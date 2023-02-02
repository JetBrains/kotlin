/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.modifierLists

/**
 * This class is used as a proxy for [com.intellij.psi.PsiModifierList.hasModifierProperty].
 *
 * [GranularModifiersBox] provides an ability to compute each modifier separately to avoid heavy computation.
 * [InitializedModifiersBox] is a collection of possible modifiers.
 * [EmptyModifiersBox] just a box without modifiers.
 *
 * @see SymbolLightModifierList
 */
internal sealed interface ModifiersBox {
    fun hasModifier(modifier: String): Boolean
}

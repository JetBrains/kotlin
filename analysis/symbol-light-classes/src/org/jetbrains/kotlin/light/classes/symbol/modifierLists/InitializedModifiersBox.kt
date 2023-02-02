/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.modifierLists

internal class InitializedModifiersBox(private val modifiers: Set<String>) : ModifiersBox {
    constructor(vararg modifiers: String) : this(modifiers.toSet())

    override fun hasModifier(modifier: String): Boolean = modifier in modifiers
}

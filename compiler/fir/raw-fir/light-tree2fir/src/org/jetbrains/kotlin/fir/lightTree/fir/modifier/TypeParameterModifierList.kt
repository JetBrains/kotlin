/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir.modifier

class TypeParameterModifierList<Node : Any>(
    varianceOrReificationModifiers: Long = ModifierFlag.NONE.value
) : ModifierList<Node>(varianceOrReificationModifiers) {
    fun hasReified(): Boolean = hasFlag(ModifierFlag.REIFICATION_REIFIED)
}

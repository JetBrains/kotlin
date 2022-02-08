/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir.modifier

import org.jetbrains.kotlin.KtSourceElement

class TypeProjectionModifier(val source: KtSourceElement? = null, varianceModifiers: Long = ModifierFlag.NONE.value) :
    Modifier(varianceModifiers) {
}

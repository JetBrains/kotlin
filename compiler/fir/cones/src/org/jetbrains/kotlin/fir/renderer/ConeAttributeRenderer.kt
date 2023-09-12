/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.types.ConeAttribute

abstract class ConeAttributeRenderer {
    abstract fun render(attributes: Iterable<ConeAttribute<*>>): String

    object ToString : ConeAttributeRenderer() {
        override fun render(attributes: Iterable<ConeAttribute<*>>): String =
            attributes.joinToString(separator = " ", postfix = " ") { it.toString() }
    }

    object ForReadability : ConeAttributeRenderer() {
        override fun render(attributes: Iterable<ConeAttribute<*>>): String =
            attributes.joinToString(separator = " ", postfix = " ") { it.renderForReadability() }
    }
}

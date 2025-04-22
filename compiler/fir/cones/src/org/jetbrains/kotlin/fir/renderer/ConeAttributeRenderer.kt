/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.types.ConeAttribute
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

abstract class ConeAttributeRenderer {
    abstract fun render(attributes: Iterable<ConeAttribute<*>>): String

    object ToString : ConeAttributeRenderer() {
        override fun render(attributes: Iterable<ConeAttribute<*>>): String {
            return attributes.sortedBy { it.key.qualifiedName }.joinToString(separator = " ", postfix = " ")
        }
    }

    object ForReadability : ConeAttributeRenderer() {
        override fun render(attributes: Iterable<ConeAttribute<*>>): String {
            return attributes.mapNotNull { attribute -> attribute.renderForReadability()?.let { attribute to it } }
                .sortedBy { (attribute, _) -> attribute.key.qualifiedName }
                .ifNotEmpty {
                    joinToString(separator = " ", postfix = " ") { (_, output) ->
                        output
                    }
                } ?: ""
        }
    }
}

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.generators.tree.*

data class FieldSet(val fieldDefinitions: List<Field>) {
    operator fun invoke(config: Field.() -> Unit): FieldSet {
        val configured = fieldDefinitions.map { it.copy().apply(config) }
        return FieldSet(configured)
    }
}

fun fieldSet(vararg fields: Field): FieldSet {
    return FieldSet(fields.toList())
}
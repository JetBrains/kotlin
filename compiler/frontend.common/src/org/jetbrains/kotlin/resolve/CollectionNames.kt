/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.name.Name

object CollectionNames {
    object Factories {
        val LIST_OF: Name = Name.identifier("listOf")
        val MUTABLE_LIST_OF: Name = Name.identifier("mutableListOf")
        val SET_OF: Name = Name.identifier("setOf")
        val MUTABLE_SET_OF: Name = Name.identifier("mutableSetOf")
        val SEQUENCE_OF: Name = Name.identifier("sequenceOf")

        val NAMES: List<Name> = listOf(
            LIST_OF,
            MUTABLE_LIST_OF,
            SET_OF,
            MUTABLE_SET_OF,
            SEQUENCE_OF,
        )
    }
}

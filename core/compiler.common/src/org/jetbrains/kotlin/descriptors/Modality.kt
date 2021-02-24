/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

// For sealed classes, isOverridable is false but isOverridableByMembers is true
enum class Modality {
    // THE ORDER OF ENTRIES MATTERS HERE
    FINAL,
    // NB: class can be sealed but not function or property
    SEALED,
    OPEN,
    ABSTRACT;

    companion object {
        fun convertFromFlags(sealed: Boolean, abstract: Boolean, open: Boolean): Modality {
            return when {
                sealed -> SEALED
                abstract -> ABSTRACT
                open -> OPEN
                else -> FINAL
            }
        }
    }
}

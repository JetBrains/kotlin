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

        // NB: never returns SEALED
        fun convertFromFlags(abstract: Boolean, open: Boolean): Modality {
            if (abstract) return ABSTRACT
            if (open) return OPEN
            return FINAL
        }
    }
}

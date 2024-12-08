/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

/**
 * This class is intended to provide all necessary information via stubs to
 * create [org.jetbrains.kotlin.descriptors.ValueClassRepresentation] during stub -> FIR
 * conversion.
 */
enum class KotlinValueClassRepresentation {
    // The order of entries is important, as an entry's ordinal is used to serialize/deserialize it
    INLINE_CLASS,
    MULTI_FIELD_VALUE_CLASS,
    VALHALLA_VALUE_CLASS,
    ;
}

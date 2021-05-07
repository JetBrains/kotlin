/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

enum class SmartcastStability(private val str: String, val description: String = str) {
    // Local value, or parameter, or private / internal member value without open / custom getter,
    // or protected / public member value from the same module without open / custom getter
    // Smart casts are completely safe
    STABLE_VALUE("stable val"),

    // Member value with open / custom getter
    // Smart casts are not safe
    PROPERTY_WITH_GETTER("custom getter", "property that has open or custom getter"),

    // Protected / public member value from another module
    // Smart casts are not safe
    ALIEN_PUBLIC_PROPERTY("alien public", "public API property declared in different module"),

    // Local variable already captured by a changing closure
    // Smart casts are not safe
    CAPTURED_VARIABLE("captured var", "local variable that is captured by a changing closure"),

    // Member variable regardless of its visibility
    // Smart casts are not safe
    MUTABLE_PROPERTY("member", "mutable property that could have been changed by this time"),
}

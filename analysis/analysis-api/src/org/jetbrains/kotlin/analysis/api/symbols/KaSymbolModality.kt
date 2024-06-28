/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

/**
 * See [the official Kotlin documentation](https://kotlinlang.org/docs/inheritance.html) about inheritance.
 */
public enum class KaSymbolModality {
    /**
     * Default modality declarations in Kotlin.
     *
     * A declaration with an implementation which cannot be overridden.
     */
    FINAL,

    /**
     * Sealed classes and interfaces provide controlled inheritance of your class hierarchies.
     * All direct subclasses of a sealed class are known at compile time.
     *
     * See more details in [the official Kotlin documentation](https://kotlinlang.org/docs/sealed-classes.html).
     */
    SEALED,

    /**
     * A declaration with an implementation which can be overridden.
     */
    OPEN,

    /**
     * A declaration without an implementation.
     */
    ABSTRACT;
}

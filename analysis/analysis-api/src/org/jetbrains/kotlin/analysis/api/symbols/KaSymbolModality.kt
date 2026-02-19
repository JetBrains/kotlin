/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

/**
 * A symbol's modality defines its ability to be [inherited](https://kotlinlang.org/docs/inheritance.html).
 */
public enum class KaSymbolModality {
    /**
     * A declaration with an implementation that cannot be overridden. This is the default modality in Kotlin.
     */
    FINAL,

    /**
     * A declaration which can be overridden according to the rules of [sealed classes and interfaces](https://kotlinlang.org/docs/sealed-classes.html).
     *
     * Sealed classes and interfaces provide controlled inheritance of class hierarchies. All direct subclasses of a sealed class are known
     * at compile time.
     */
    SEALED,

    /**
     * A declaration with an implementation which can be overridden.
     */
    OPEN,

    /**
     * A declaration without an implementation. It can be overridden, and *must be* overridden in non-abstract classes.
     */
    ABSTRACT;
}

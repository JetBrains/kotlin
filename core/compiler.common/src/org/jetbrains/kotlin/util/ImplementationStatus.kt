/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

/** Implementation status of a member symbol that is available inside a class scope. */
enum class ImplementationStatus {
    /** This symbol is not implemented and should be implemented if the class is not abstract. */
    NOT_IMPLEMENTED,

    /** This symbol is a var property but only has a val property as implementation */
    VAR_IMPLEMENTED_BY_VAL,

    /** The symbol is inheriting multiple non-abstract symbols and hence must be explicitly implemented. */
    AMBIGUOUSLY_INHERITED,

    /**
     * This symbol has an inherited implementation, and it can be overridden if desired. For example, it's an open non-abstract member or
     * it's automatically synthesized by the Kotlin compiler.
     */
    INHERITED_OR_SYNTHESIZED,

    /** The symbol is already implemented in this class. */
    ALREADY_IMPLEMENTED,

    /**
     * The symbol is not implemented in the class, and it cannot be implemented. For example, it's final in super classes.
     */
    CANNOT_BE_IMPLEMENTED;

    val shouldBeImplemented: Boolean get() = this == NOT_IMPLEMENTED || this == AMBIGUOUSLY_INHERITED
    val isOverridable: Boolean get() = this != ALREADY_IMPLEMENTED && this != CANNOT_BE_IMPLEMENTED
}


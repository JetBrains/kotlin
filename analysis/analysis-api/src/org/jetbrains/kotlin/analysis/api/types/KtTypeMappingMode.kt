/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

public enum class KtTypeMappingMode {
    /**
     * kotlin.Int is mapped to I
     */
    DEFAULT,

    /**
     * kotlin.Int is mapped to I
     * Type aliases are mapped to their expanded form
     */
    DEFAULT_UAST,

    /**
     * kotlin.Int is mapped to Ljava/lang/Integer;
     */
    GENERIC_ARGUMENT,

    /**
     * kotlin.Int is mapped to Ljava/lang/Integer;
     * No projections allowed in immediate arguments
     */
    SUPER_TYPE,

    /**
     * Similar to [SUPER_TYPE], except for that Kotlin collections remain as-is.
     */
    SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS,

    /**
     * When method return type should be boxed (e.g., kotlin.Int to Ljava/lang/Integer;)
     */
    RETURN_TYPE_BOXED,

    /**
     * Optimal mode to convert the return type of declarations if it's part of signature.
     */
    RETURN_TYPE,

    /**
     * Optimal mode to convert the type of value parameter if it's part of signature.
     */
    VALUE_PARAMETER,
}

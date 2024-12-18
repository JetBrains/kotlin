/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

/**
 * [KaTypeMappingMode] determines how a Kotlin type is mapped to a Java type when calling [KaType.asPsiType][org.jetbrains.kotlin.analysis.api.components.KaJavaInteroperabilityComponent.asPsiType].
 */
public enum class KaTypeMappingMode {
    /**
     * Kotlin primitives are mapped to Java primitives in non-generic usages. For example, `kotlin.Int` is mapped to `int`.
     */
    DEFAULT,

    /**
     * Similar to [DEFAULT], but type aliases are additionally mapped to their expanded form.
     */
    DEFAULT_UAST,

    /**
     * Kotlin primitives are mapped to Java boxed types in non-generic usages. For example, `kotlin.Int` is mapped to `java.lang.Integer`.
     */
    GENERIC_ARGUMENT,

    /**
     * Kotlin primitives are mapped to Java boxed types in non-generic usages. For example, `kotlin.Int` is mapped to `java.lang.Integer`.
     *
     * Additionally, the mode avoids converting declaration-site variance of type parameters to Java use-site wildcard types. For example,
     * a type parameter `out T` given a use-site `List<T>` would *not* be converted to `List<? extends T>`.
     */
    SUPER_TYPE,

    /**
     * Similar to [SUPER_TYPE], except that Kotlin collections are not converted to their Java equivalents.
     */
    SUPER_TYPE_KOTLIN_COLLECTIONS_AS_IS,

    /**
     * Kotlin primitives in method return types are mapped to Java boxed types. For example, `kotlin.Int` is mapped to `java.lang.Integer`.
     *
     * This type mapping mode should *only* be used when converting return types.
     */
    RETURN_TYPE_BOXED,

    /**
     * The optimal mode to convert a declaration's return type if it's part of the signature.
     */
    RETURN_TYPE,

    /**
     * The optimal mode to convert a value parameter's type if it's part of the signature.
     */
    VALUE_PARAMETER,
}

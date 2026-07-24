/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner

/**
 * [KaType] instances for built-in types.
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaBuiltinTypes : KaLifetimeOwner {
    /** The [Int] class type. */
    public val int: KaType

    /** The [Long] class type. */
    public val long: KaType

    /** The [Short] class type. */
    public val short: KaType

    /** The [Byte] class type. */
    public val byte: KaType

    /** The [Float] class type. */
    public val float: KaType

    /** The [Double] class type. */
    public val double: KaType

    /** The [Boolean] class type. */
    public val boolean: KaType

    /** The [Char] class type. */
    public val char: KaType

    /** The [String] class type. */
    public val string: KaType

    /** The [Unit] class type. */
    public val unit: KaType

    /** The [Nothing] class type. */
    public val nothing: KaType

    /** The [Any] class type. */
    public val any: KaType

    /** The [Throwable] class type. */
    public val throwable: KaType

    /** The `Any?` type. */
    public val nullableAny: KaType

    /** The `Nothing?` type. */
    public val nullableNothing: KaType

    /** The [Annotation] type. */
    public val annotationType: KaType
}

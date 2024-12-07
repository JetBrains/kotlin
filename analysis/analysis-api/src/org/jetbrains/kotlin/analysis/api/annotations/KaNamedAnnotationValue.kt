/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.name.Name

/**
 * A name/value pair representing an annotation argument.
 *
 * #### Example
 *
 * ```kotlin
 * annotation class Foo(val bar: String)
 *
 * @Foo(bar = "abc")
 * fun foo() {}
 * ```
 *
 * The annotation application `@Foo(bar = "abc")` has a single [KaNamedAnnotationValue] `bar = "abc"`, with the name "bar" and a
 * [KaAnnotationValue.ConstantValue] representing the [String] constant `"abc"`.
 */
public interface KaNamedAnnotationValue : KaLifetimeOwner {
    /**
     * The name of the annotation argument.
     */
    public val name: Name

    /**
     * The value of the annotation argument.
     */
    public val expression: KaAnnotationValue
}

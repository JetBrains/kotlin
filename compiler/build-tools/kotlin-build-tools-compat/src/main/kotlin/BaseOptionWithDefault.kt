/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.compat

import org.jetbrains.kotlin.buildtools.api.internal.BaseOption

internal abstract class BaseOptionWithDefault<V> private constructor(
    id: String,
    private val hasDefault: Boolean = false,
    private val default: V? = null,
) : BaseOption<V>(id) {
    constructor(id: String) : this(id, false, null)
    constructor(id: String, default: V) : this(id, true, default)

    @Suppress("UNCHECKED_CAST")
    val defaultValue: V
        get() = if (hasDefault) {
            default as V
        } else {
            error("Value is not set for $id and it has no default value")
        }
}
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.v2.internal


public sealed class Option(public val id: String) {
    override fun toString(): String = id

    public open class Mandatory internal constructor(id: String) : Option(id)
    public open class WithDefault<V> internal constructor(id: String, public val defaultValue: V) : Option(id)
}


/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.reflect.KParameter

internal abstract class ReflectKParameter : KParameter {
    abstract val callable: ReflectKCallable<*>

    final override fun equals(other: Any?): Boolean =
        other is ReflectKParameter && callable == other.callable && index == other.index

    final override fun hashCode(): Int =
        (callable.hashCode() * 31) + index.hashCode()

    final override fun toString(): String =
        ReflectionObjectRenderer.renderParameter(this)
}

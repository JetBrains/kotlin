/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts.description

enum class InvocationKind {
    ZERO,
    AT_MOST_ONCE,
    EXACTLY_ONCE,
    AT_LEAST_ONCE,
    UNKNOWN
}

fun InvocationKind.isDefinitelyVisited(): Boolean = this == InvocationKind.EXACTLY_ONCE || this == InvocationKind.AT_LEAST_ONCE
fun InvocationKind.canBeRevisited(): Boolean = this == InvocationKind.UNKNOWN || this == InvocationKind.AT_LEAST_ONCE


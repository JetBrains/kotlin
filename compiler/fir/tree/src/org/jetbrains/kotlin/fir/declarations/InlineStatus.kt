/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

enum class InlineStatus(val returnAllowed: Boolean) {
    Inline(returnAllowed = true), // lambda will be inlined
    NoInline(returnAllowed = false), // lambda won't be inlined (is part of not inline function call or marked with `noinline` modifier)
    CrossInline(returnAllowed = false), // lambda will be inlined but marked with `crossinline` modifier
    Unknown(returnAllowed = false), //  inlinability of lambda is unknown
}

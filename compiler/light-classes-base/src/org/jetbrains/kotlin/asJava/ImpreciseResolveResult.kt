/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

enum class ImpreciseResolveResult {
    MATCH,
    NO_MATCH,
    UNSURE;

    inline fun ifSure(body: (Boolean) -> Unit) = when (this) {
        MATCH -> body(true)
        NO_MATCH -> body(false)
        UNSURE -> Unit
    }
}
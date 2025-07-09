/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle

enum class MangleMode(val signature: Boolean, val fqn: Boolean) {
    SIGNATURE(true, false),
    FQNAME(false, true),
    FULL(true, true)
}
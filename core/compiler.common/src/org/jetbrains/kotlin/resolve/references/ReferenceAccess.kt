/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.references

enum class ReferenceAccess(
    val isRead: Boolean,
    val isWrite: Boolean
) {
    READ(true, false),
    WRITE(false, true),
    READ_WRITE(true, true)
}

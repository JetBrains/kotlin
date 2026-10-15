/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

data class JvmClassFileVersion(val major: Int, val isPreview: Boolean) {
    companion object {
        @JvmStatic
        fun fromAsmVersion(version: Int): JvmClassFileVersion =
            JvmClassFileVersion(major = version and 0xFFFF, isPreview = version ushr 16 == 0xFFFF)
    }
}

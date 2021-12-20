/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import java.io.File
import java.io.PrintWriter

// TODO md5 hash
data class CacheInfo(val path: String, val libPath: String, var flatHash: ULong, var transHash: ULong) {
    fun save() {
        PrintWriter(File(File(path), "info")).use {
            it.println(libPath)
            it.println(flatHash.toString(16))
            it.println(transHash.toString(16))
        }
    }

    companion object {
        fun load(path: String): CacheInfo? {
            val info = File(File(path), "info")

            if (!info.exists()) return null

            val (libPath, flatHash, transHash) = info.readLines()

            return CacheInfo(path, libPath, flatHash.toULong(16), transHash.toULong(16))
        }
    }
}

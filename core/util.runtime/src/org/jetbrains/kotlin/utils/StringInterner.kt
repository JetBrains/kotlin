package org.jetbrains.kotlin.utils

import java.util.concurrent.ConcurrentHashMap

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

object StringInterner {
    private val cache = ConcurrentHashMap<String, String>()

    @JvmStatic
    fun interned(string: String): String = cache.getOrPut(string) { string }
}
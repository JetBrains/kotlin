/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.example.api

class PublicService {
    @InternalApi
    fun version(): String = "1.0"
    fun compute(x: Int): Int = x * 2
}

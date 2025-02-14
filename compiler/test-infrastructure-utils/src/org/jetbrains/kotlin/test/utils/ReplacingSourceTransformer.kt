/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import java.util.function.Function

class ReplacingSourceTransformer(val from: String, val to: String) : Function<String, String>, (String) -> String {
    init {
        require(from.isNotEmpty()) { "Cannot replace empty string" }
    }

    private val randomComment: String = CharArray(6) { (('0'..'9') + ('a'..'z') + ('A'..'Z')).random() }
        .joinToString("", prefix = "/* ", postfix = " */")

    fun invokeForTestFile(source: String): String = source.replace(from, to + randomComment)

    fun revertForFile(actual: String): String =
        actual.replace(to + randomComment, from).replace(randomComment, "")

    override fun apply(source: String): String = invokeForTestFile(source)
    override fun invoke(source: String): String = invokeForTestFile(source)
}
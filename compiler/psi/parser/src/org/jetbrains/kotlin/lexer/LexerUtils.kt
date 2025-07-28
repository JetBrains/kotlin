/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lexer

fun KotlinLexer.assertCorrectState(fileName: String? = null) {
    val states = (flex as _JetLexer).states

    if (states.isNotEmpty()) {
        val message = "${fileName?.takeIf { it.isNotEmpty() }?.let { "file://$it " } ?: ""}Nonempty lexer states (${states.size}): ${states.joinToString("; ")}"
        println(message)
        error(message)
    }
}
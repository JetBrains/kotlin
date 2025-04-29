/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.lexer

enum class KDocKnownTag(val isReferenceRequired: Boolean, val isSectionStart: Boolean) {
    AUTHOR(false, false),
    THROWS(true, false),
    EXCEPTION(true, false),
    PARAM(true, false),
    RECEIVER(false, false),
    RETURN(false, false),
    SEE(true, false),
    SINCE(false, false),
    CONSTRUCTOR(false, true),
    PROPERTY(true, true),
    SAMPLE(true, false),
    SUPPRESS(false, false);


    companion object {
        fun findByTagName(tagName: CharSequence): KDocKnownTag? {
            val name = if (tagName.startsWith('@')) {
                tagName.subSequence(1, tagName.length)
            } else {
                tagName
            }
            try {
                val upperCaseAsciiOnly = buildString {
                    name.map {
                        append(if (it in 'a'..'z') it.uppercaseChar() else it)
                    }
                }
                return valueOf(upperCaseAsciiOnly)
            } catch (_: IllegalArgumentException) {
            }

            return null
        }
    }
}
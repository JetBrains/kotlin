/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser

import fleet.com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import org.jetbrains.kotlin.kmp.lexer.KDocKnownTag
import org.jetbrains.kotlin.kmp.lexer.KDocTokens
import org.jetbrains.kotlin.kmp.lexer.KtTokens

class KDocParser {
    companion object {
        private fun parseTag(builder: SyntaxTreeBuilder, currentSectionMarker: SyntaxTreeBuilder.Marker): SyntaxTreeBuilder.Marker {
            var currentSectionMarker = currentSectionMarker
            val tagName = builder.tokenText
            val knownTag = KDocKnownTag.findByTagName(tagName!!)
            if (knownTag != null && knownTag.isSectionStart) {
                currentSectionMarker.done(KDocParseNodes.KDOC_SECTION)
                currentSectionMarker = builder.mark()
            }
            val tagStart = builder.mark()
            builder.advanceLexer()

            while (!builder.eof() && !isAtEndOfTag(builder)) {
                builder.advanceLexer()
            }
            tagStart.done(KDocParseNodes.KDOC_TAG)
            return currentSectionMarker
        }

        private fun isAtEndOfTag(builder: SyntaxTreeBuilder): Boolean {
            if (builder.tokenType === KDocTokens.END) {
                return true
            }
            if (builder.tokenType === KDocTokens.LEADING_ASTERISK) {
                var lookAheadCount = 1
                if (builder.lookAhead(1) === KDocTokens.TEXT) {
                    lookAheadCount++
                }
                if (builder.lookAhead(lookAheadCount) === KDocTokens.TAG_NAME) {
                    return true
                }
            }
            return false
        }
    }

    fun parse(builder: SyntaxTreeBuilder) : SyntaxTreeBuilder.Marker {
        val rootMarker = builder.mark()

        if (builder.tokenType === KDocTokens.START) {
            builder.advanceLexer()
        }
        var currentSectionMarker: SyntaxTreeBuilder.Marker? = builder.mark()

        // todo: parse KDoc tags, markdown, etc...
        while (!builder.eof()) {
            when {
                builder.tokenType === KDocTokens.TAG_NAME -> {
                    currentSectionMarker = parseTag(builder, currentSectionMarker!!)
                }
                builder.tokenType === KDocTokens.END -> {
                    if (currentSectionMarker != null) {
                        currentSectionMarker.done(KDocParseNodes.KDOC_SECTION)
                        currentSectionMarker = null
                    }
                    builder.advanceLexer()
                }
                else -> {
                    builder.advanceLexer()
                }
            }
        }

        currentSectionMarker?.done(KDocParseNodes.KDOC_SECTION)
        rootMarker.done(KtTokens.DOC_COMMENT)

        return rootMarker
    }
}
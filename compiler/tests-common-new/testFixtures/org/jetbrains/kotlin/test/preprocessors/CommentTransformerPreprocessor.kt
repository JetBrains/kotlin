/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.preprocessors

import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.extension
import org.jetbrains.kotlin.test.services.ReversibleSourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestServices
import java.util.regex.Pattern
import java.util.regex.Pattern.MULTILINE
import java.util.regex.Pattern.compile

/**
 * A preprocessor that normalizes comments of different styles in source files.
 * Currently, it only supports mapping between standard single-line comment (`//`) and configuration comment (`#`).
 * This transformation is reversible and operates on files with the `.config` extension.
 */
class CommentTransformerPreprocessor(testServices: TestServices) : ReversibleSourceFilePreprocessor(testServices) {
    companion object {
        const val CONFIG_EXTENSION: String = "config"
        const val LEADING_SPACES_GROUP_NAME: String = "leadingspaces"
        const val COMMENT_CONTENT_GROUP_NAME: String = "commentcontent"

        private val standardCommentPattern: Pattern = compile(
            """^(?<${LEADING_SPACES_GROUP_NAME}>\s*)//(?<${COMMENT_CONTENT_GROUP_NAME}>.*)$""",
            MULTILINE
        )
    }

    override fun process(file: TestFile, content: String): String {
        if (file.extension != CONFIG_EXTENSION) return content

        var previousIndex = 0
        val matcher = standardCommentPattern.matcher(content)
        return buildString {
            while (matcher.find()) {
                append(content.subSequence(previousIndex, matcher.start()))
                append(matcher.group(LEADING_SPACES_GROUP_NAME))
                append('#')
                append(matcher.group(COMMENT_CONTENT_GROUP_NAME))

                previousIndex = matcher.end()
            }
            append(content.subSequence(previousIndex, content.length))
        }
    }

    override fun revert(file: TestFile, actualContent: String): String {
        if (file.extension != CONFIG_EXTENSION) return actualContent
        // Return just the original content because it's not supposed to be changed.
        return file.originalContent.trim()
    }
}
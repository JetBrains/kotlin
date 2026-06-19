/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.intellij.java.syntax.parser

import com.intellij.java.syntax.element.JavaDocSyntaxElementType
import com.intellij.java.syntax.element.JavaDocSyntaxTokenType
import com.intellij.java.syntax.element.JavaSyntaxElementType
import com.intellij.java.syntax.parser.JavaParserUtil.emptyElement
import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.SyntaxElementTypeSet
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import com.intellij.platform.syntax.parser.WhitespacesAndCommentsBinder
import com.intellij.platform.syntax.parser.WhitespacesBinders.greedyLeftBinder
import com.intellij.platform.syntax.parser.WhitespacesBinders.greedyRightBinder
import com.intellij.platform.syntax.syntaxElementTypeSetOf
import com.intellij.pom.java.LanguageLevel
import com.intellij.util.text.CharSequenceSubSequence
import org.jetbrains.annotations.Contract

@Suppress("UnstableApiUsage", "unused")
class JavaDocParser(
    val builder: SyntaxTreeBuilder,
    val languageLevel: LanguageLevel,
) {
    companion object {
        /** Binder taking the next space if it isn't an EOL */
        private val fakeCollapseRightBinder = object : WhitespacesAndCommentsBinder {
            override fun getEdgePosition(
                tokens: List<SyntaxElementType>,
                atStreamEdge: Boolean,
                getter: WhitespacesAndCommentsBinder.TokenTextGetter,
            ): Int {
                if (tokens.isNotEmpty() && !isEolToken(tokens.first(), getter.get(0))) {
                    return 1
                }
                return 0
            }
        }
    }

    private var braceScope: Int = 0
    private var closingStatusList: MutableList<Boolean> = mutableListOf() // true for all '{' opening an inline tag

    fun parseJavadocReference(parser: JavaParser) {
        parser.referenceParser.parseJavaCodeReference(builder, true, true, false, false)
        swallowTokens()
    }

    fun parseJavadocType(parser: JavaParser) {
        parser.referenceParser.parseType(builder, ReferenceParser.EAT_LAST_DOT or ReferenceParser.ELLIPSIS or ReferenceParser.WILDCARD)
        swallowTokens()
    }

    private fun swallowTokens() {
        while (!builder.eof()) builder.advanceLexer()
    }

    fun parseDocCommentText() {
        builder.enforceCommentTokens(SKIP_TOKENS)

        while (!builder.eof()) {
            val tokenType = getTokenType()
            if (tokenType === JavaDocSyntaxTokenType.DOC_TAG_NAME) {
                parseTag()
            } else {
                parseDataItem(null, false)
            }
        }
    }

    private fun parseTag() {
        val tagName = builder.tokenText
        val tag = builder.mark()
        builder.advanceLexer()
        while (true) {
            val tokenType = getTokenType()
            if (tokenType == null || tokenType === JavaDocSyntaxTokenType.DOC_TAG_NAME || tokenType === JavaDocSyntaxTokenType.DOC_COMMENT_END) break
            parseDataItem(tagName, false)
        }
        tag.done(JavaDocSyntaxElementType.DOC_TAG)
    }

    private fun parseDataItem(
        tagName: String?,
        isInline: Boolean,
    ) {
        var tokenType = getTokenType()
        if (tokenType === JavaDocSyntaxTokenType.DOC_INLINE_TAG_START) {
            val tag = builder.mark()
            builder.advanceLexer()

            tokenType = getTokenType()
            if (tokenType !== JavaDocSyntaxTokenType.DOC_TAG_NAME && tokenType !== JavaDocSyntaxTokenType.DOC_COMMENT_BAD_CHARACTER) {
                tag.rollbackTo()
                builder.remapCurrentToken(JavaDocSyntaxTokenType.DOC_COMMENT_DATA)
                builder.advanceLexer()
                closingStatusList.add(false)
                return
            }

            closingStatusList.add(true)

            var inlineTagName: String? = ""
            var isSnippet = false

            while (true) {
                tokenType = getTokenType()
                if (tokenType === JavaDocSyntaxTokenType.DOC_TAG_NAME) {
                    inlineTagName = builder.tokenText
                    isSnippet = inlineTagName == SNIPPET_TAG
                } else if (tokenType == null || tokenType === JavaDocSyntaxTokenType.DOC_COMMENT_END) {
                    break
                }

                if (tokenType === JavaDocSyntaxTokenType.DOC_INLINE_TAG_END) {
                    val shouldClose = closingStatusList.removeLast()
                    if (shouldClose) {
                        setBraceScope(getBraceScope() - 1)
                        builder.advanceLexer()
                        break
                    } else {
                        builder.remapCurrentToken(JavaDocSyntaxTokenType.DOC_COMMENT_DATA)
                        builder.advanceLexer()
                        continue
                    }
                }

                parseDataItem(inlineTagName, true)
            }

            if (isSnippet) {
                tag.done(JavaDocSyntaxElementType.DOC_SNIPPET_TAG)
            } else {
                tag.done(JavaDocSyntaxElementType.DOC_INLINE_TAG)
            }
        } else if (TAG_VALUES_SET.contains(tokenType)) {
            if (SEE_TAG == tagName && !isInline ||
                LINK_TAG == tagName && isInline ||
                languageLevel.isAtLeast(LanguageLevel.JDK_1_4) && LINK_PLAIN_TAG == tagName && isInline
            ) {
                parseSeeTagValue(false)
            } else if (!isInline && tagName != null && REFERENCE_TAGS.contains(tagName) || isInline && tagName == INHERIT_DOC_TAG) {
                val tagValue = builder.mark()
                builder.remapCurrentToken(JavaDocSyntaxElementType.DOC_REFERENCE_HOLDER)
                builder.advanceLexer()
                tagValue.done(JavaDocSyntaxElementType.DOC_TAG_VALUE_ELEMENT)
            } else if (!isInline && PARAM_TAG == tagName) {
                parseParameterRef()
            } else if (languageLevel.isAtLeast(LanguageLevel.JDK_1_5) && VALUE_TAG == tagName && isInline) {
                parseSeeTagValue(true)
            } else if (SNIPPET_TAG == tagName && isInline) {
                parseSnippetTagValue()
            } else {
                parseSimpleTagValue()
            }
        } else if (tokenType === JavaDocSyntaxTokenType.DOC_INLINE_CODE_FENCE) {
            parseInlineCodeBlock()
        } else if (tokenType === JavaDocSyntaxTokenType.DOC_CODE_FENCE) {
            parseCodeBlock()
        } else if (tokenType === JavaDocSyntaxTokenType.DOC_LBRACKET) {
            parseMarkdownReferenceChecked()
        } else if (tokenType === JavaDocSyntaxTokenType.DOC_COMMENT_DATA || (isWhiteSpace(tokenType) && !isEolToken(
                tokenType,
                builder.tokenText
            ))
        ) {
            parseCommentData()
        } else {
            remapAndAdvance()
        }
    }

    private fun parseCommentData() {
        val commentData = builder.mark()
        val offset = builder.currentOffset

        var aheadType: SyntaxElementType? = null
        while (builder.rawLookup(1).also { aheadType = it } != null) {
            val hasCommentTokenNext = COMMENT_DATA_TOKENS.contains(aheadType)
            val hasWhiteSpaceTokenNext = isWhiteSpace(aheadType) && !isEolToken(aheadType, builder.rawTokenText(1))

            if (hasCommentTokenNext || hasWhiteSpaceTokenNext) {
                builder.rawAdvanceLexer(1)
            } else {
                break
            }
        }

        if (builder.currentOffset != offset) {
            builder.advanceLexer()
            commentData.collapse(JavaDocSyntaxTokenType.DOC_COMMENT_DATA)
        } else {
            commentData.drop()
            builder.advanceLexer()
        }
    }

    private fun SyntaxTreeBuilder.rawTokenText(index: Int): CharSequence {
        return CharSequenceSubSequence(
            baseSequence = text,
            start = rawTokenTypeStart(index),
            end = rawTokenTypeStart(index + 1)
        )
    }

    private fun parseInlineCodeBlock() {
        val blockMarker = builder.mark()
        builder.rawAdvanceLexer(1)
        val contentMarker = builder.mark()
        val stopElementType = findInlineToken(JavaDocSyntaxTokenType.DOC_INLINE_CODE_FENCE)

        if (stopElementType !== JavaDocSyntaxTokenType.DOC_INLINE_CODE_FENCE) {
            // Bail out, no end
            contentMarker.drop()
            blockMarker.rollbackTo()
            builder.advanceLexer()
            return
        }

        fakeCollapse(contentMarker, JavaDocSyntaxTokenType.DOC_COMMENT_DATA)
        if (!builder.eof()) {
            builder.advanceLexer()
        }

        blockMarker.done(JavaDocSyntaxElementType.DOC_MARKDOWN_CODE_BLOCK)
    }

    private fun parseCodeBlock() {
        if (getBraceScope() > 0) {
            builder.remapCurrentToken(JavaDocSyntaxTokenType.DOC_COMMENT_DATA)
            builder.advanceLexer()
            return
        }
        // Store the fence type, a fenced code block can only be closed by the same fence type
        val fenceStart = builder.tokenText!![0]
        val tag = builder.mark()

        // Look for the nearest closing code fence, converting everything inside as comment data
        while (!builder.eof()) {
            builder.advanceLexer()
            if (getTokenType() === JavaDocSyntaxTokenType.DOC_CODE_FENCE && builder.tokenText!![0] == fenceStart) {
                break
            }
            builder.remapCurrentToken(JavaDocSyntaxTokenType.DOC_COMMENT_DATA)
        }

        if (!builder.eof()) {
            builder.advanceLexer()
        }

        tag.done(JavaDocSyntaxElementType.DOC_MARKDOWN_CODE_BLOCK)
    }

    /** Ensure a reference link is good before parsing it  */
    private fun parseMarkdownReferenceChecked() {
        var hasLabel = true
        var tag = builder.mark()

        // Step 1 ensure that we have a label
        var leftBracketCount = 1
        var rightBracketCount = 0
        val startLabelOffset = builder.currentOffset

        // Labels are allowed balanced brackets, count each side
        while (!builder.eof()) {
            val token = findInlineToken(JavaDocSyntaxTokenType.DOC_RBRACKET, JavaDocSyntaxTokenType.DOC_LBRACKET, true)
            if (token === JavaDocSyntaxTokenType.DOC_LBRACKET) {
                leftBracketCount++
                continue
            }
            if (token === JavaDocSyntaxTokenType.DOC_RBRACKET) {
                rightBracketCount++
                if (leftBracketCount == rightBracketCount) break
            }
        }

        val endLabelOffset = builder.currentOffset
        val isShortRefEmpty = endLabelOffset - startLabelOffset <= 1
        if (leftBracketCount != rightBracketCount || isShortRefEmpty) {
            // Stop if unbalanced brackets/empty reference
            tag.rollbackTo()
            builder.advanceLexer()
            return
        }

        val firstReferenceToken = findInlineToken(JavaDocSyntaxTokenType.DOC_LBRACKET, JavaDocSyntaxTokenType.DOC_SPACE, false)
        if (firstReferenceToken !== JavaDocSyntaxTokenType.DOC_LBRACKET) {
            hasLabel = false
            // The label is actually a reference, verify brackets balance or if we have a normal markdown link
            if (leftBracketCount > 1 || firstReferenceToken === JavaDocSyntaxTokenType.DOC_LPAREN) {
                tag.rollbackTo()
                builder.advanceLexer()
                return
            }
        }

        // Step 2 get the reference for full reference link
        if (hasLabel) {
            if (findInlineToken(
                    JavaDocSyntaxTokenType.DOC_RBRACKET, JavaDocSyntaxTokenType.DOC_LBRACKET,
                    true
                ) !== JavaDocSyntaxTokenType.DOC_RBRACKET
            ) {
                hasLabel = false
                // The label is actually a reference, verify brackets balance
                if (leftBracketCount > 1) {
                    tag.rollbackTo()
                    builder.advanceLexer()
                    return
                }
            }
        }

        // Step 3, validity ensured, parse the link
        tag.rollbackTo()
        tag = builder.mark()
        if (hasLabel) {
            builder.advanceLexer()
            val label = builder.mark()
            /* Collapses comment data within a label */
            var commentDataMarker: SyntaxTreeBuilder.Marker? = null
            // Label range already known, mark it as comment data
            while (!builder.eof()) {
                if (builder.tokenType === JavaDocSyntaxTokenType.DOC_INLINE_CODE_FENCE) {
                    commentDataMarker?.let {
                        fakeCollapse(it, JavaDocSyntaxTokenType.DOC_COMMENT_DATA)
                        commentDataMarker = null
                    }
                    parseInlineCodeBlock()
                    continue
                }

                if (builder.currentOffset < endLabelOffset) {
                    if (commentDataMarker == null) {
                        commentDataMarker = builder.mark()
                    }
                    builder.advanceLexer()
                    continue
                }
                break
            }

            commentDataMarker?.let {
                fakeCollapse(it, JavaDocSyntaxTokenType.DOC_COMMENT_DATA)
            }
            label.done(JavaDocSyntaxElementType.DOC_MARKDOWN_REFERENCE_LABEL)
            builder.advanceLexer()
        }

        // Parse the reference itself
        builder.advanceLexer()
        parseMarkdownReference()
        builder.advanceLexer()

        tag.done(JavaDocSyntaxElementType.DOC_MARKDOWN_REFERENCE_LINK)
    }

    private fun parseMarkdownReference() {
        val moduleMarker = parseModuleRef(builder.mark())
        var referenceParsed = false
        var referenceEnded = false
        var fragmentReference = false

        if (getTokenType() === JavaDocSyntaxTokenType.DOC_RBRACKET) {
            if (moduleMarker == null) {
                return
            }
            referenceEnded = true
        }
        moduleMarker?.done(JavaDocSyntaxElementType.DOC_TAG_VALUE_ELEMENT)
        val refStart = builder.mark()

        if (!referenceEnded && getTokenType() !== JavaDocSyntaxTokenType.DOC_SHARP && getTokenType() !== JavaDocSyntaxTokenType.DOC_DOUBLE_SHARP) {
            builder.remapCurrentToken(JavaDocSyntaxElementType.DOC_REFERENCE_HOLDER)
            builder.advanceLexer()
        }

        if (!referenceEnded && getTokenType() === JavaDocSyntaxTokenType.DOC_SHARP) {
            // Existing integration require this token for auto completion
            builder.remapCurrentToken(JavaDocSyntaxTokenType.DOC_TAG_VALUE_SHARP_TOKEN)

            // method/variable name
            builder.advanceLexer()
            builder.remapCurrentToken(JavaDocSyntaxTokenType.DOC_TAG_VALUE_TOKEN)

            // A method only has parenthesis, comment data which may be the type, the optional argument name and commas
            builder.advanceLexer()
            if (builder.tokenType === JavaDocSyntaxTokenType.DOC_LPAREN) {
                builder.advanceLexer()
                val subValue = builder.mark()

                // Only the first data element count as a type in links like [#foo(int arg1, int arg2)]
                var dataSinceComma = false

                while (!builder.eof()) {
                    val type = getTokenType()
                    if (type === JavaDocSyntaxTokenType.DOC_COMMENT_DATA) {
                        if (!dataSinceComma) {
                            dataSinceComma = true
                            builder.remapCurrentToken(JavaDocSyntaxElementType.DOC_TYPE_HOLDER)
                        }
                    } else if (type !== JavaDocSyntaxTokenType.DOC_COMMA) {
                        break
                    } else {
                        dataSinceComma = false
                    }
                    builder.advanceLexer()
                }

                if (getTokenType() === JavaDocSyntaxTokenType.DOC_RPAREN) {
                    subValue.done(JavaDocSyntaxElementType.DOC_TAG_VALUE_ELEMENT)
                    builder.advanceLexer()
                } else {
                    subValue.drop()
                }
            }

            referenceParsed = true
        } else if (!referenceEnded && getTokenType() === JavaDocSyntaxTokenType.DOC_DOUBLE_SHARP) {
            // auto-completion token
            builder.remapCurrentToken(JavaDocSyntaxTokenType.DOC_TAG_VALUE_DOUBLE_SHARP_TOKEN)
            builder.advanceLexer()

            // fragment name
            val nameMarker = builder.mark()

            if (getTokenType() !== JavaDocSyntaxTokenType.DOC_COMMENT_DATA) {
                nameMarker.rollbackTo()
            } else {
                builder.advanceLexer()
                nameMarker.done(JavaDocSyntaxElementType.DOC_FRAGMENT_NAME)
                referenceParsed = true
                fragmentReference = true
            }
        }

        if (referenceParsed) {
            when (fragmentReference) {
                true -> refStart.done(JavaDocSyntaxElementType.DOC_FRAGMENT_REF)
                false -> refStart.done(JavaDocSyntaxElementType.DOC_METHOD_OR_FIELD_REF)
            }
        } else {
            refStart.drop()
        }
        // This method is guaranteed the existence of an end bracket
        if (getTokenType() !== JavaDocSyntaxTokenType.DOC_RBRACKET) {
            findInlineToken(JavaDocSyntaxTokenType.DOC_RBRACKET)
        }
    }


    private fun findInlineToken(needle: SyntaxElementType?): SyntaxElementType? {
        return findInlineToken(needle, null, false)
    }

    /**
     * Look for the token provided by `needle`, taking into account markdown line break rules
     *
     * @param travelToken             The token that is either allowed or disallowed to encounter while looking for the `needle`
     * When `null`, no check is performed
     * @param isTravelTokenDisallowed When `true`, the `travelToken` will abort the search
     * When `false`, encountering something other than `travelToken` or `needle` will abort the search
     * @return The last token encountered during the search.
     */
    @Contract(mutates = "param1")
    private fun findInlineToken(
        needle: SyntaxElementType?,
        travelToken: SyntaxElementType?,
        isTravelTokenDisallowed: Boolean,
    ): SyntaxElementType? {
        var token: SyntaxElementType? = null
        var previousToken: SyntaxElementType?
        while (!builder.eof()) {
            builder.advanceLexer()
            previousToken = token
            token = getTokenType(false)
            if (token === needle) {
                return token
            }
            val travelTokenFound = travelToken === token
            if ((travelToken != null) && ((isTravelTokenDisallowed && travelTokenFound) || (!isTravelTokenDisallowed && !travelTokenFound))) {
                break
            }

            // Markdown specific, check for EOL
            if (token === SyntaxTokenTypes.WHITE_SPACE && previousToken === SyntaxTokenTypes.WHITE_SPACE) {
                break
            }
        }

        return if (builder.eof()) null else token
    }

    private fun parseSnippetTagValue() {
        // we are right after @snippet
        val snippetValue = builder.mark()
        snippetValue.setCustomEdgeTokenBinders(greedyLeftBinder(), greedyRightBinder())

        // recovery, when "foo" goes right after @snippet
        while (true) {
            val token = getTokenType()
            if (token !== JavaDocSyntaxTokenType.DOC_TAG_VALUE_QUOTE) {
                break
            }
            builder.advanceLexer()
        }

        val tokenType = getTokenType()
        if (tokenType === JavaDocSyntaxTokenType.DOC_TAG_VALUE_COLON) {
            emptyElement(builder, JavaDocSyntaxElementType.DOC_SNIPPET_ATTRIBUTE_LIST)
            parseSnippetTagBody()
        } else if (tokenType === JavaDocSyntaxTokenType.DOC_TAG_VALUE_TOKEN) {
            parseSnippetAttributeList()
            if (builder.tokenType === JavaDocSyntaxTokenType.DOC_TAG_VALUE_COLON) {
                parseSnippetTagBody()
            }
        } else {
            emptyElement(builder, JavaDocSyntaxElementType.DOC_SNIPPET_ATTRIBUTE_LIST)
            var current = getTokenType()
            while (current != null && current !== JavaDocSyntaxTokenType.DOC_INLINE_TAG_END) {
                builder.advanceLexer()
                current = getTokenType()
            }
        }
        snippetValue.done(JavaDocSyntaxElementType.DOC_SNIPPET_TAG_VALUE)
    }

    private fun parseSnippetTagBody() {
        val body = builder.mark()
        body.setCustomEdgeTokenBinders(greedyLeftBinder(), greedyRightBinder())
        require(getTokenType() === JavaDocSyntaxTokenType.DOC_TAG_VALUE_COLON)
        builder.advanceLexer()
        while (true) {
            val tokenType = getTokenType()
            if (tokenType == null || tokenType === JavaDocSyntaxTokenType.DOC_INLINE_TAG_END) {
                break
            }
            builder.advanceLexer()
        }
        body.done(JavaDocSyntaxElementType.DOC_SNIPPET_BODY)
    }

    private fun parseSnippetAttributeList() {
        val attributeList = builder.mark()
        outer@ while (true) {
            var type = getTokenType()
            while (type !== JavaDocSyntaxTokenType.DOC_TAG_VALUE_TOKEN) {
                // recovery
                if (type !== JavaDocSyntaxTokenType.DOC_TAG_VALUE_QUOTE) {
                    break@outer
                }
                builder.advanceLexer()
                type = getTokenType()
            }
            parseSnippetAttribute()
        }
        attributeList.done(JavaDocSyntaxElementType.DOC_SNIPPET_ATTRIBUTE_LIST)
    }

    private fun parseSnippetAttribute() {
        val attribute = builder.mark()
        require(builder.tokenType === JavaDocSyntaxTokenType.DOC_TAG_VALUE_TOKEN)
        builder.remapCurrentToken(JavaDocSyntaxTokenType.DOC_TAG_ATTRIBUTE_NAME)
        builder.advanceLexer()
        getTokenType() // skipping spaces
        if ("=" == builder.tokenText) {
            builder.advanceLexer()
            val afterEqToken = getTokenType()
            if (afterEqToken === JavaDocSyntaxTokenType.DOC_TAG_VALUE_QUOTE) {
                val quotedValue = builder.mark()
                builder.advanceLexer()
                if (getTokenType() === JavaDocSyntaxTokenType.DOC_TAG_VALUE_TOKEN) {
                    builder.advanceLexer()
                }
                if (getTokenType() === JavaDocSyntaxTokenType.DOC_TAG_VALUE_QUOTE) {
                    builder.advanceLexer()
                }
                quotedValue.collapse(JavaDocSyntaxElementType.DOC_SNIPPET_ATTRIBUTE_VALUE)
            } else if (afterEqToken === JavaDocSyntaxTokenType.DOC_TAG_VALUE_TOKEN) {
                builder.remapCurrentToken(JavaDocSyntaxElementType.DOC_SNIPPET_ATTRIBUTE_VALUE)
                builder.advanceLexer()
            }
        }
        attribute.done(JavaDocSyntaxElementType.DOC_SNIPPET_ATTRIBUTE)
    }

    private fun parseSeeTagValue(allowBareFieldReference: Boolean) {
        val moduleMarker = parseModuleRef(builder.mark())

        val tokenType = getTokenType()
        if (tokenType === JavaDocSyntaxTokenType.DOC_TAG_VALUE_SHARP_TOKEN) {
            parseMethodRef(builder.mark())
        } else if (tokenType === JavaDocSyntaxTokenType.DOC_TAG_VALUE_DOUBLE_SHARP_TOKEN) {
            parseFragmentRef(builder.mark())
        } else if (tokenType === JavaDocSyntaxTokenType.DOC_TAG_VALUE_TOKEN) {
            val refStart = builder.mark()
            builder.remapCurrentToken(JavaDocSyntaxElementType.DOC_REFERENCE_HOLDER)
            builder.advanceLexer()

            if (getTokenType() === JavaDocSyntaxTokenType.DOC_TAG_VALUE_SHARP_TOKEN) {
                parseMethodRef(refStart)
            } else if (getTokenType() === JavaDocSyntaxTokenType.DOC_TAG_VALUE_DOUBLE_SHARP_TOKEN) {
                parseFragmentRef(refStart)
            } else if (allowBareFieldReference) {
                refStart.rollbackTo()
                builder.remapCurrentToken(JavaDocSyntaxTokenType.DOC_TAG_VALUE_TOKEN)
                parseMethodRef(builder.mark())
            } else {
                refStart.drop()
            }
        } else if (moduleMarker == null) {
            val tagValue = builder.mark()
            builder.advanceLexer()
            tagValue.done(JavaDocSyntaxElementType.DOC_TAG_VALUE_ELEMENT)
        }

        moduleMarker?.done(JavaDocSyntaxElementType.DOC_TAG_VALUE_ELEMENT)
    }

    private fun parseModuleRef(
        refStart: SyntaxTreeBuilder.Marker,
    ): SyntaxTreeBuilder.Marker? {
        builder.advanceLexer()
        if (getTokenType() === JavaDocSyntaxTokenType.DOC_TAG_VALUE_SLASH) {
            refStart.rollbackTo()
            val containerRef = builder.mark()
            val moduleRef = builder.mark()
            builder.advanceLexer()
            moduleRef.done(JavaSyntaxElementType.MODULE_REFERENCE)
            builder.advanceLexer()
            return containerRef
        } else {
            refStart.rollbackTo()
            return null
        }
    }

    private fun parseFragmentRef(mark: SyntaxTreeBuilder.Marker) {
        builder.advanceLexer()
        val fragmentName = builder.mark()
        if (getTokenType() !== JavaDocSyntaxTokenType.DOC_TAG_VALUE_TOKEN) {
            fragmentName.drop()
            mark.done(JavaDocSyntaxElementType.DOC_FRAGMENT_REF)
        } else {
            builder.advanceLexer()
            fragmentName.done(JavaDocSyntaxElementType.DOC_FRAGMENT_NAME)
            mark.done(JavaDocSyntaxElementType.DOC_FRAGMENT_REF)
        }
    }

    private fun parseMethodRef(
        refStart: SyntaxTreeBuilder.Marker,
    ) {
        if (getTokenType() === JavaDocSyntaxTokenType.DOC_TAG_VALUE_SHARP_TOKEN) {
            builder.advanceLexer()
        }
        if (getTokenType() !== JavaDocSyntaxTokenType.DOC_TAG_VALUE_TOKEN) {
            refStart.done(JavaDocSyntaxElementType.DOC_METHOD_OR_FIELD_REF)
            return
        }
        builder.advanceLexer()

        if (getTokenType() === JavaDocSyntaxTokenType.DOC_TAG_VALUE_LPAREN) {
            builder.advanceLexer()

            val subValue = builder.mark()

            var tokenType: SyntaxElementType?
            while (TAG_VALUES_SET.contains(getTokenType().also { tokenType = it })) {
                when (tokenType) {
                    JavaDocSyntaxTokenType.DOC_TAG_VALUE_TOKEN -> {
                        builder.remapCurrentToken(JavaDocSyntaxElementType.DOC_TYPE_HOLDER)
                        builder.advanceLexer()

                        while (TAG_VALUES_SET.contains(getTokenType().also {
                                tokenType = it
                            }) && tokenType !== JavaDocSyntaxTokenType.DOC_TAG_VALUE_COMMA && tokenType !== JavaDocSyntaxTokenType.DOC_TAG_VALUE_RPAREN
                        ) {
                            builder.advanceLexer()
                        }
                    }
                    JavaDocSyntaxTokenType.DOC_TAG_VALUE_RPAREN -> {
                        subValue.done(JavaDocSyntaxElementType.DOC_TAG_VALUE_ELEMENT)
                        builder.advanceLexer()
                        refStart.done(JavaDocSyntaxElementType.DOC_METHOD_OR_FIELD_REF)
                        return
                    }
                    else -> {
                        builder.advanceLexer()
                    }
                }
            }

            subValue.done(JavaDocSyntaxElementType.DOC_TAG_VALUE_ELEMENT)
        }

        refStart.done(JavaDocSyntaxElementType.DOC_METHOD_OR_FIELD_REF)
    }

    private fun parseParameterRef() {
        val tagValue = builder.mark()
        while (TAG_VALUES_SET.contains(getTokenType())) builder.advanceLexer()
        tagValue.done(JavaDocSyntaxElementType.DOC_PARAMETER_REF)
    }

    private fun parseSimpleTagValue() {
        val tagData = builder.mark()
        while (true) {
            val tokenType = getTokenType()
            if (tokenType === JavaDocSyntaxTokenType.DOC_COMMENT_BAD_CHARACTER) {
                builder.remapCurrentToken(JavaDocSyntaxTokenType.DOC_TAG_VALUE_TOKEN)
            } else if (!TAG_VALUES_SET.contains(tokenType)) break
            builder.advanceLexer()
        }
        tagData.done(JavaDocSyntaxElementType.DOC_TAG_VALUE_ELEMENT)
    }

    /**
     * Behaves like-ish [SyntaxTreeBuilder.Marker.collapse] but skips over eol and leading asterisks.
     * This means it cannot merge everything in a single node.
     *
     * Note that [marker] will be dropped.
     */
    private fun fakeCollapse(marker: SyntaxTreeBuilder.Marker, tokenType: SyntaxElementType) {
        /** Whether a token should be ignored during the fake collapse */
        fun shouldIgnore(type: SyntaxElementType?) = (type != null &&
                (type === JavaDocSyntaxTokenType.DOC_COMMENT_LEADING_ASTERISKS || isEolToken(type, builder.tokenText)))

        val endOffset = builder.currentOffset
        marker.rollbackTo()
        val currentTokenType = getTokenType(false)
        var fuseMarker: SyntaxTreeBuilder.Marker? = if (shouldIgnore(currentTokenType)) null else builder.mark()

        while (!builder.eof() && builder.currentOffset < endOffset) {
            if (shouldIgnore(currentTokenType)) {
                fuseMarker?.collapse(tokenType)
                fuseMarker?.setCustomEdgeTokenBinders(null, fakeCollapseRightBinder)
                fuseMarker = null

                builder.advanceLexer()
                continue
            }
            if (fuseMarker == null) {
                fuseMarker = builder.mark()
            }
            if (isWhiteSpace(builder.rawLookup(1)) && builder.rawLookup(2) == JavaDocSyntaxTokenType.DOC_COMMENT_LEADING_ASTERISKS) {
                builder.advanceLexer()
                fuseMarker.collapse(tokenType)
                fuseMarker.setCustomEdgeTokenBinders(null, fakeCollapseRightBinder)
                fuseMarker = null
            } else {
                builder.advanceLexer()
            }
        }
        fuseMarker?.collapse(tokenType)
        fuseMarker?.setCustomEdgeTokenBinders(null, fakeCollapseRightBinder)
    }

    private fun getTokenType(skipWhitespace: Boolean = true): SyntaxElementType? {
        var tokenType: SyntaxElementType?
        while ((builder.tokenType.also { tokenType = it }) === JavaDocSyntaxTokenType.DOC_SPACE) {
            builder.remapCurrentToken(SyntaxTokenTypes.WHITE_SPACE)
            if (skipWhitespace) builder.advanceLexer()
        }
        return tokenType
    }

    private fun getBraceScope(): Int {
        return braceScope
    }

    private fun setBraceScope(braceScope: Int) {
        this.braceScope = braceScope
    }

    private fun remapAndAdvance() {
        if (INLINE_TAG_BORDERS_SET.contains(builder.tokenType) && getBraceScope() == 0) {
            builder.remapCurrentToken(JavaDocSyntaxTokenType.DOC_COMMENT_DATA)
        }
        builder.advanceLexer()
    }
}

/** @return Whether the token is eol */
@Suppress("UnstableApiUsage")
private fun isEolToken(type: SyntaxElementType?, text: CharSequence?): Boolean {
    return isWhiteSpace(type) && text != null && text.contains('\n')
}

/** @return Whether the token is a whitespace */
@Suppress("UnstableApiUsage")
private fun isWhiteSpace(type: SyntaxElementType?): Boolean {
    return type != null && (type == JavaDocSyntaxTokenType.DOC_SPACE || type == SyntaxTokenTypes.WHITE_SPACE)
}

@Suppress("UnstableApiUsage")
private val TAG_VALUES_SET: SyntaxElementTypeSet = syntaxElementTypeSetOf(
    JavaDocSyntaxTokenType.DOC_TAG_VALUE_TOKEN,
    JavaDocSyntaxTokenType.DOC_TAG_VALUE_COMMA,
    JavaDocSyntaxTokenType.DOC_TAG_VALUE_DOT,
    JavaDocSyntaxTokenType.DOC_TAG_VALUE_LPAREN,
    JavaDocSyntaxTokenType.DOC_TAG_VALUE_RPAREN,
    JavaDocSyntaxTokenType.DOC_TAG_VALUE_SLASH,
    JavaDocSyntaxTokenType.DOC_TAG_VALUE_SHARP_TOKEN,
    JavaDocSyntaxTokenType.DOC_TAG_VALUE_DOUBLE_SHARP_TOKEN,
    JavaDocSyntaxTokenType.DOC_TAG_VALUE_LT,
    JavaDocSyntaxTokenType.DOC_TAG_VALUE_GT,
    JavaDocSyntaxTokenType.DOC_TAG_VALUE_COLON,
    JavaDocSyntaxTokenType.DOC_TAG_VALUE_QUOTE
)

@Suppress("UnstableApiUsage")
private val INLINE_TAG_BORDERS_SET: SyntaxElementTypeSet = syntaxElementTypeSetOf(
    JavaDocSyntaxTokenType.DOC_INLINE_TAG_START, JavaDocSyntaxTokenType.DOC_INLINE_TAG_END
)

@Suppress("UnstableApiUsage")
private val SKIP_TOKENS: SyntaxElementTypeSet = syntaxElementTypeSetOf(JavaDocSyntaxTokenType.DOC_COMMENT_LEADING_ASTERISKS)

@Suppress("UnstableApiUsage")
private val COMMENT_DATA_TOKENS: SyntaxElementTypeSet = syntaxElementTypeSetOf(
    JavaDocSyntaxTokenType.DOC_COMMENT_DATA,
    JavaDocSyntaxTokenType.DOC_TAG_VALUE_SLASH,
    JavaDocSyntaxTokenType.DOC_COMMA,
    JavaDocSyntaxTokenType.DOC_SHARP, JavaDocSyntaxTokenType.DOC_DOUBLE_SHARP,
)

private const val SEE_TAG = "@see"
private const val LINK_TAG = "@link"
private const val LINK_PLAIN_TAG = "@linkplain"
private const val PARAM_TAG = "@param"
private const val VALUE_TAG = "@value"
private const val SNIPPET_TAG = "@snippet"
private const val INHERIT_DOC_TAG = "@inheritDoc"

private val REFERENCE_TAGS: Set<String> = setOf("@throws", "@exception", "@provides", "@uses")

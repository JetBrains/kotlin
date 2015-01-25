/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kdoc.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens;

public class KDocParser implements PsiParser {
    @Override
    @NotNull
    public ASTNode parse(IElementType root, PsiBuilder builder) {
        PsiBuilder.Marker rootMarker = builder.mark();
        if (builder.getTokenType() == KDocTokens.START) {
            builder.advanceLexer();
        }
        PsiBuilder.Marker currentSectionMarker = builder.mark();

        // todo: parse KDoc tags, markdown, etc...
        while (!builder.eof()) {
            if (builder.getTokenType() == KDocTokens.TAG_NAME) {
                currentSectionMarker = parseTag(builder, currentSectionMarker);
            }
            else if (builder.getTokenType() == KDocTokens.MARKDOWN_LINK) {
                PsiBuilder.Marker linkStart = builder.mark();
                builder.advanceLexer();
                linkStart.done(KDocElementTypes.KDOC_LINK);
            }
            else if (builder.getTokenType() == KDocTokens.END) {
                if (currentSectionMarker != null) {
                    currentSectionMarker.done(KDocElementTypes.KDOC_SECTION);
                    currentSectionMarker = null;
                }
                builder.advanceLexer();
            }
            else {
                builder.advanceLexer();
            }
        }

        if (currentSectionMarker != null) {
            currentSectionMarker.done(KDocElementTypes.KDOC_SECTION);
        }
        rootMarker.done(root);
        return builder.getTreeBuilt();
    }

    private static PsiBuilder.Marker parseTag(PsiBuilder builder, PsiBuilder.Marker currentSectionMarker) {
        String tagName = builder.getTokenText();
        KDocKnownTag knownTag = KDocKnownTag.findByTagName(tagName);
        if (knownTag != null && knownTag.isSectionStart()) {
            currentSectionMarker.done(KDocElementTypes.KDOC_SECTION);
            currentSectionMarker = builder.mark();
        }
        PsiBuilder.Marker tagStart = builder.mark();
        builder.advanceLexer();

        if (knownTag != null && knownTag.isReferenceRequired() && builder.getTokenType() == KDocTokens.TEXT_OR_LINK) {
            PsiBuilder.Marker referenceMarker = builder.mark();
            builder.advanceLexer();
            referenceMarker.done(KDocElementTypes.KDOC_LINK);
        }

        while (!builder.eof() && !isAtEndOfTag(builder)) {
            if (builder.getTokenType() == KDocTokens.MARKDOWN_LINK) {
                PsiBuilder.Marker linkStart = builder.mark();
                builder.advanceLexer();
                linkStart.done(KDocElementTypes.KDOC_LINK);
            }
            else {
                builder.advanceLexer();
            }
        }
        tagStart.done(KDocElementTypes.KDOC_TAG);
        return currentSectionMarker;
    }

    private static boolean isAtEndOfTag(PsiBuilder builder) {
        if (builder.getTokenType() == KDocTokens.END) {
            return true;
        }
        if (builder.getTokenType() == KDocTokens.LEADING_ASTERISK) {
            int lookAheadCount = 1;
            if (builder.lookAhead(1) == KDocTokens.TEXT) {
                lookAheadCount++;
            }
            if (builder.lookAhead(lookAheadCount) == KDocTokens.TAG_NAME) {
                return true;
            }
        }
        return false;
    }
}

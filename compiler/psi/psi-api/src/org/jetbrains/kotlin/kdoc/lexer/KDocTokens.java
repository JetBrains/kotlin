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

package org.jetbrains.kotlin.kdoc.lexer;

import com.intellij.psi.tree.ILazyParseableElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KDocTokenId;
import org.jetbrains.kotlin.KotlinElementTypeProvider;


public interface KDocTokens {
    @NotNull ILazyParseableElementType KDOC = KotlinElementTypeProvider.getInstance().getKdocType();

    int START_Id = KDocTokenId.START;
    int END_Id = KDocTokenId.END;
    int LEADING_ASTERISK_Id = KDocTokenId.LEADING_ASTERISK;
    int TEXT_Id = KDocTokenId.TEXT;
    int CODE_BLOCK_TEXT_Id = KDocTokenId.CODE_BLOCK_TEXT;
    int TAG_NAME_Id = KDocTokenId.TAG_NAME;
    int MARKDOWN_ESCAPED_CHAR_Id = KDocTokenId.MARKDOWN_ESCAPED_CHAR;
    @Deprecated
    int MARKDOWN_INLINE_LINK_Id = KDocTokenId.MARKDOWN_LINK;
    int KDOC_LPAR_Id = KDocTokenId.KDOC_LPAR;
    int KDOC_RPAR_Id = KDocTokenId.KDOC_RPAR;
    int CODE_SPAN_TEXT_Id = KDocTokenId.CODE_SPAN_TEXT;

    @NotNull KDocToken START                 = new KDocToken("KDOC_START", KDocTokenId.START);
    @NotNull KDocToken END                   = new KDocToken("KDOC_END", KDocTokenId.END);
    @NotNull KDocToken LEADING_ASTERISK      = new KDocToken("KDOC_LEADING_ASTERISK", KDocTokenId.LEADING_ASTERISK);

    @NotNull KDocToken TEXT                  = new KDocToken("KDOC_TEXT", KDocTokenId.TEXT);
    @NotNull KDocToken CODE_BLOCK_TEXT       = new KDocToken("KDOC_CODE_BLOCK_TEXT", KDocTokenId.CODE_BLOCK_TEXT);
    @NotNull KDocToken CODE_SPAN_TEXT       = new KDocToken("KDOC_CODE_SPAN_TEXT", KDocTokenId.CODE_SPAN_TEXT);

    @NotNull KDocToken TAG_NAME              = new KDocToken("KDOC_TAG_NAME", KDocTokenId.TAG_NAME);

    @NotNull ILazyParseableElementType MARKDOWN_LINK = KotlinElementTypeProvider.getInstance().getKdocMarkdownLinkType();

    @NotNull KDocToken KDOC_LPAR = new KDocToken("KDOC_LPAR", KDocTokenId.KDOC_LPAR);
    @NotNull KDocToken KDOC_RPAR = new KDocToken("KDOC_RPAR", KDocTokenId.KDOC_RPAR);

    @NotNull KDocToken MARKDOWN_ESCAPED_CHAR = new KDocToken("KDOC_MARKDOWN_ESCAPED_CHAR", KDocTokenId.MARKDOWN_ESCAPED_CHAR);
    @Deprecated
    @NotNull KDocToken MARKDOWN_INLINE_LINK = new KDocToken("KDOC_MARKDOWN_INLINE_LINK", KDocTokenId.MARKDOWN_LINK);
    @SuppressWarnings("unused")
    @NotNull TokenSet KDOC_HIGHLIGHT_TOKENS = TokenSet.create(START, END, LEADING_ASTERISK, TEXT, CODE_BLOCK_TEXT, CODE_SPAN_TEXT, MARKDOWN_LINK, MARKDOWN_ESCAPED_CHAR, MARKDOWN_INLINE_LINK, KDOC_LPAR, KDOC_RPAR);
    @NotNull TokenSet CONTENT_TOKENS = TokenSet.create(TEXT, CODE_BLOCK_TEXT, CODE_SPAN_TEXT, TAG_NAME, MARKDOWN_LINK, MARKDOWN_ESCAPED_CHAR, MARKDOWN_INLINE_LINK, KDOC_LPAR, KDOC_RPAR);
}

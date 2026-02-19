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
import org.jetbrains.kotlin.KotlinElementTypeProvider;

public interface KDocTokens {
    ILazyParseableElementType KDOC = KotlinElementTypeProvider.getInstance().getKdocType();

    int START_Id = 0;
    int END_Id = 1;
    int LEADING_ASTERISK_Id = 2;
    int TEXT_Id = 3;
    int CODE_BLOCK_TEXT_Id = 4;
    int TAG_NAME_Id = 5;
    int MARKDOWN_ESCAPED_CHAR_Id = 6;
    @Deprecated
    int MARKDOWN_INLINE_LINK_Id = 7;
    int KDOC_LPAR_Id= 8;
    int KDOC_RPAR_Id = 9;
    int CODE_SPAN_TEXT_Id = 10;

    KDocToken START                 = new KDocToken("KDOC_START", START_Id);
    KDocToken END                   = new KDocToken("KDOC_END", END_Id);
    KDocToken LEADING_ASTERISK      = new KDocToken("KDOC_LEADING_ASTERISK", LEADING_ASTERISK_Id);

    KDocToken TEXT                  = new KDocToken("KDOC_TEXT", TEXT_Id);
    KDocToken CODE_BLOCK_TEXT       = new KDocToken("KDOC_CODE_BLOCK_TEXT", CODE_BLOCK_TEXT_Id);
    KDocToken CODE_SPAN_TEXT       = new KDocToken("KDOC_CODE_SPAN_TEXT", CODE_SPAN_TEXT_Id);

    KDocToken TAG_NAME              = new KDocToken("KDOC_TAG_NAME", TAG_NAME_Id);

    ILazyParseableElementType MARKDOWN_LINK = KotlinElementTypeProvider.getInstance().getKdocMarkdownLinkType();

    KDocToken KDOC_LPAR = new KDocToken("KDOC_LPAR", KDOC_LPAR_Id);
    KDocToken KDOC_RPAR = new KDocToken("KDOC_RPAR", KDOC_RPAR_Id);

    KDocToken MARKDOWN_ESCAPED_CHAR = new KDocToken("KDOC_MARKDOWN_ESCAPED_CHAR", MARKDOWN_ESCAPED_CHAR_Id);
    @Deprecated
    KDocToken MARKDOWN_INLINE_LINK = new KDocToken("KDOC_MARKDOWN_INLINE_LINK", MARKDOWN_INLINE_LINK_Id);
    @SuppressWarnings("unused")
    TokenSet KDOC_HIGHLIGHT_TOKENS = TokenSet.create(START, END, LEADING_ASTERISK, TEXT, CODE_BLOCK_TEXT, CODE_SPAN_TEXT, MARKDOWN_LINK, MARKDOWN_ESCAPED_CHAR, MARKDOWN_INLINE_LINK, KDOC_LPAR, KDOC_RPAR);
    TokenSet CONTENT_TOKENS = TokenSet.create(TEXT, CODE_BLOCK_TEXT, CODE_SPAN_TEXT, TAG_NAME, MARKDOWN_LINK, MARKDOWN_ESCAPED_CHAR, MARKDOWN_INLINE_LINK, KDOC_LPAR, KDOC_RPAR);
}

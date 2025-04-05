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

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lang.PsiParser;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ILazyParseableElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.ElementTypeChecker;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.kdoc.parser.KDocLinkParser;
import org.jetbrains.kotlin.kdoc.parser.KDocParser;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocImpl;

@SuppressWarnings("WeakerAccess") // Let all static identifiers be public as well as corresponding elements
public class KDocTokens {
    static {
        // It forces initializing tokens in strict order that provides possibility to match indexes and static identifiers
        @SuppressWarnings("unused")
        IElementType dependentTokensInit = TokenType.WHITE_SPACE;
    }

    private final static int START_OFFSET = 9; // The specific value is calculated based on already initialized internal elements

    public final static int KDOC_INDEX = START_OFFSET + 1;
    public final static int START_INDEX = KDOC_INDEX + 1;
    public final static int END_INDEX = START_INDEX + 1;
    public final static int LEADING_ASTERISK_INDEX = END_INDEX + 1;
    public final static int TEXT_INDEX = LEADING_ASTERISK_INDEX + 1;
    public final static int CODE_BLOCK_TEXT_INDEX = TEXT_INDEX + 1;
    public final static int TAG_NAME_INDEX = CODE_BLOCK_TEXT_INDEX + 1;
    public final static int MARKDOWN_LINK_INDEX = TAG_NAME_INDEX + 1;
    public final static int KDOC_LPAR_INDEX = MARKDOWN_LINK_INDEX + 1;
    public final static int KDOC_RPAR_INDEX = KDOC_LPAR_INDEX + 1;
    public final static int MARKDOWN_ESCAPED_CHAR_INDEX = KDOC_RPAR_INDEX + 1;
    public final static int MARKDOWN_INLINE_LINK_INDEX = MARKDOWN_ESCAPED_CHAR_INDEX + 1;

    public static ILazyParseableElementType KDOC = new ILazyParseableElementType("KDoc", KotlinLanguage.INSTANCE) {
        @Override
        public ASTNode parseContents(ASTNode chameleon) {
            PsiElement parentElement = chameleon.getTreeParent().getPsi();
            Project project = parentElement.getProject();
            PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, new KDocLexer(), getLanguage(),
                                                                               chameleon.getText());
            PsiParser parser = new KDocParser();

            return parser.parse(this, builder).getFirstChildNode();
        }

        @Nullable
        @Override
        public ASTNode createNode(CharSequence text) {
            return new KDocImpl(text);
        }
    };

    public final static KDocToken START                 = new KDocToken("KDOC_START");
    public final static KDocToken END                   = new KDocToken("KDOC_END");
    public final static KDocToken LEADING_ASTERISK      = new KDocToken("KDOC_LEADING_ASTERISK");

    public final static KDocToken TEXT                  = new KDocToken("KDOC_TEXT");
    public final static KDocToken CODE_BLOCK_TEXT       = new KDocToken("KDOC_CODE_BLOCK_TEXT");

    public final static KDocToken TAG_NAME              = new KDocToken("KDOC_TAG_NAME");
    public final static  ILazyParseableElementType MARKDOWN_LINK = new ILazyParseableElementType("KDOC_MARKDOWN_LINK", KotlinLanguage.INSTANCE) {
        @Override
        public ASTNode parseContents(ASTNode chameleon) {
            return KDocLinkParser.parseMarkdownLink(this, chameleon);
        }
    };

    public final static KDocToken KDOC_LPAR = new KDocToken("KDOC_LPAR");
    public final static KDocToken KDOC_RPAR = new KDocToken("KDOC_RPAR");

    public final static KDocToken MARKDOWN_ESCAPED_CHAR = new KDocToken("KDOC_MARKDOWN_ESCAPED_CHAR");
    public final static KDocToken MARKDOWN_INLINE_LINK = new KDocToken("KDOC_MARKDOWN_INLINE_LINK");

    public final static TokenSet KDOC_HIGHLIGHT_TOKENS = TokenSet.create(START, END, LEADING_ASTERISK, TEXT, CODE_BLOCK_TEXT, MARKDOWN_LINK, MARKDOWN_ESCAPED_CHAR, MARKDOWN_INLINE_LINK, KDOC_LPAR, KDOC_RPAR);
    public final static TokenSet CONTENT_TOKENS = TokenSet.create(TEXT, CODE_BLOCK_TEXT, TAG_NAME, MARKDOWN_LINK, MARKDOWN_ESCAPED_CHAR, MARKDOWN_INLINE_LINK, KDOC_LPAR, KDOC_RPAR);

    static {
        ElementTypeChecker.checkExplicitStaticIndexesMatchImplicit(KDocTokens.class);
    }
}

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

package org.jetbrains.kotlin.kdoc.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens;
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes;

public class KDocTag extends KDocElementImpl {
    public KDocTag(@NotNull ASTNode node) {
        super(node);
    }

    public String getContent() {
        StringBuilder builder = new StringBuilder();

        boolean contentStarted = false;
        boolean afterAsterisk = false;
        boolean startsWithTagName = false;

        ASTNode[] children = getNode().getChildren(null);
        for (int i = 0; i < children.length; i++) {
            ASTNode node = children[i];
            IElementType type = node.getElementType();
            if (i == 0 && type == KDocTokens.TAG_NAME) {
                startsWithTagName = true;
            }
            if (KDocTokens.CONTENT_TOKENS.contains(type) || type == KDocElementTypes.KDOC_LINK) {
                contentStarted = true;
                builder.append(afterAsterisk ? StringUtil.trimLeading(node.getText()) : node.getText());
                afterAsterisk = false;
            }

            if (type == KDocTokens.LEADING_ASTERISK) {
                afterAsterisk = true;
            }

            if (type == TokenType.WHITE_SPACE) {
                if (i == 1 && startsWithTagName) {
                    builder.append(node.getText());
                }
                else if (contentStarted) {
                    builder.append(StringUtil.repeat("\n", StringUtil.countNewLines(node.getText())));
                }
            }

            if (type == KDocElementTypes.KDOC_TAG) {
                break;
            }
        }

        return builder.toString();
    }

    public String getContentWithTags() {
        StringBuilder content = new StringBuilder(getContent());
        KDocTag[] subTags = PsiTreeUtil.getChildrenOfType(this, KDocTag.class);
        if (subTags != null) {
            for (KDocTag tag : subTags) {
                content.append(tag.getContentWithTags()).append("\n");
            }
        }
        return content.toString();
    }
}

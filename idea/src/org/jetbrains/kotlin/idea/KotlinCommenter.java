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

package org.jetbrains.kotlin.idea;

import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.psi.PsiComment;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.lexer.KtTokens;

public class KotlinCommenter implements CodeDocumentationAwareCommenter {
    @Override
    public String getLineCommentPrefix() {
        return "//";
    }

    @Override
    public String getBlockCommentPrefix() {
        return "/*";
    }

    @Override
    public String getBlockCommentSuffix() {
        return "*/";
    }

    @Override
    public String getCommentedBlockCommentPrefix() {
        return null;
    }

    @Override
    public String getCommentedBlockCommentSuffix() {
        return null;
    }

    @Override
    public IElementType getLineCommentTokenType() {
        return KtTokens.EOL_COMMENT;
    }

    @Override
    public IElementType getBlockCommentTokenType() {
        return KtTokens.BLOCK_COMMENT;
    }

    @Override
    public IElementType getDocumentationCommentTokenType() {
        return KtTokens.DOC_COMMENT;
    }

    @Override
    public String getDocumentationCommentPrefix() {
        return "/**";
    }

    @Override
    public String getDocumentationCommentLinePrefix() {
        return "*";
    }

    @Override
    public String getDocumentationCommentSuffix() {
        return "*/";
    }

    @Override
    public boolean isDocumentationComment(PsiComment element) {
        return element instanceof KDoc;
    }
}

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

package org.jetbrains.kotlin.idea.editor;

import com.intellij.codeInsight.editorActions.JavaBackspaceHandler;
import com.intellij.codeInsight.editorActions.JavaTypedHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;

final class LtGtTypingUtils {
    private static final TokenSet INVALID_INSIDE_REFERENCE = TokenSet.create(KtTokens.SEMICOLON, KtTokens.LBRACE, KtTokens.RBRACE);

    private LtGtTypingUtils() {
    }

    static void handleKotlinAutoCloseLT(Editor editor) {
        JavaTypedHandler.handleAfterJavaLT(editor, KtTokens.LT, KtTokens.GT, INVALID_INSIDE_REFERENCE);
    }

    static boolean handleKotlinGTInsert(Editor editor) {
        return JavaTypedHandler.handleJavaGT(editor, KtTokens.LT, KtTokens.GT, INVALID_INSIDE_REFERENCE);
    }

    static void handleKotlinLTDeletion(Editor editor, int offset) {
        JavaBackspaceHandler.handleLTDeletion(editor, offset, KtTokens.LT, KtTokens.GT, INVALID_INSIDE_REFERENCE);
    }

    static boolean shouldAutoCloseAngleBracket(int offset, Editor editor) {
        return isAfterClassIdentifier(offset, editor) || isAfterToken(offset, editor, KtTokens.FUN_KEYWORD);
    }

    private static boolean isAfterClassIdentifier(int offset, Editor editor) {
        HighlighterIterator iterator = ((EditorEx) editor).getHighlighter().createIterator(offset);
        if (iterator.atEnd()) {
            return false;
        }

        if (iterator.getStart() > 0) {
            iterator.retreat();
        }

        return JavaTypedHandler.isClassLikeIdentifier(offset, editor, iterator, KtTokens.IDENTIFIER);
    }

    static boolean isAfterToken(int offset, Editor editor, KtToken tokenType) {
        HighlighterIterator iterator = ((EditorEx) editor).getHighlighter().createIterator(offset);
        if (iterator.atEnd()) {
            return false;
        }

        if (iterator.getStart() > 0) {
            iterator.retreat();
        }

        if (iterator.getTokenType() == KtTokens.WHITE_SPACE && iterator.getStart() > 0) {
            iterator.retreat();
        }

        return iterator.getTokenType() == tokenType;
    }
}

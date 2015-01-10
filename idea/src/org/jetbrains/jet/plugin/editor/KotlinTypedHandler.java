/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.editor;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.codeInsight.highlighting.BraceMatcher;
import com.intellij.codeInsight.highlighting.BraceMatchingUtil;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.lexer.JetTokens;

public class KotlinTypedHandler extends TypedHandlerDelegate {
    private final static TokenSet CONTROL_FLOW_EXPRESSIONS = TokenSet.create(
            JetNodeTypes.IF,
            JetNodeTypes.FOR,
            JetNodeTypes.WHILE);

    private boolean jetLTTyped;

    @Override
    public Result beforeCharTyped(char c, Project project, Editor editor, PsiFile file, FileType fileType) {
        if (!(file instanceof JetFile)) {
            return Result.CONTINUE;
        }

        switch (c) {
            case '<':
                jetLTTyped = CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET &&
                             JetLtGtTypingUtils.shouldAutoCloseAngleBracket(editor.getCaretModel().getOffset(), editor);
                return Result.CONTINUE;

            case '>':
                if (CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
                    if (JetLtGtTypingUtils.handleJetGTInsert(editor)) {
                        return Result.STOP;
                    }
                }
                return Result.CONTINUE;

            case '{':
                // Returning Result.CONTINUE will cause inserting "{}" for unmatched '{'

                int offset = editor.getCaretModel().getOffset();
                if (offset == 0) {
                    return Result.CONTINUE;
                }

                HighlighterIterator iterator = ((EditorEx) editor).getHighlighter().createIterator(offset - 1);
                while (!iterator.atEnd() && iterator.getTokenType() == TokenType.WHITE_SPACE) {
                    iterator.retreat();
                }

                if (iterator.atEnd() || iterator.getTokenType() != JetTokens.RPAR) {
                    return Result.CONTINUE;
                }

                PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());

                PsiElement leaf = file.findElementAt(offset);
                if (leaf != null) {
                    PsiElement parent = leaf.getParent();
                    if (parent != null && CONTROL_FLOW_EXPRESSIONS.contains(parent.getNode().getElementType())) {
                        ASTNode nonWhitespaceSibling = FormatterUtil.getPreviousNonWhitespaceSibling(leaf.getNode());
                        if (nonWhitespaceSibling != null && nonWhitespaceSibling.getText().equals(")")) {
                            // Check that ')' belongs to same parent

                            EditorModificationUtil.insertStringAtCaret(editor, "{", false, true);
                            indentBrace(project, editor, '{');

                            return Result.STOP;
                        }
                    }
                }

                return Result.CONTINUE;

            case '.':
                autoPopupMemberLookup(project, editor);
                return Result.CONTINUE;

            case '@':
                autoPopupLabelLookup(project, editor);
                return Result.CONTINUE;
        }

        return Result.CONTINUE;
    }

    private static void autoPopupMemberLookup(Project project, final Editor editor) {
        AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, new Condition<PsiFile>() {
            @Override
            public boolean value(PsiFile file) {
                int offset = editor.getCaretModel().getOffset();

                PsiElement lastElement = file.findElementAt(offset - 1);
                if (lastElement == null) return false;

                String text = lastElement.getText();
                return ".".equals(text) || "?.".equals(text);
            }
        });
    }

    private static void autoPopupLabelLookup(Project project, final Editor editor) {
        AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, new Condition<PsiFile>() {
            @Override
            public boolean value(PsiFile file) {
                int offset = editor.getCaretModel().getOffset();

                CharSequence chars = editor.getDocument().getCharsSequence();
                if (!endsWith(chars, offset, "this@")
                    && !endsWith(chars, offset, "return@")
                    && !endsWith(chars, offset, "break@")
                    && !endsWith(chars, offset, "continue@")) return false;

                PsiElement lastElement = file.findElementAt(offset - 1);
                if (lastElement == null) return false;

                return lastElement.getNode().getElementType() == JetTokens.LABEL_IDENTIFIER;
            }
        });
    }

    private static boolean endsWith(CharSequence chars, int offset, String text) {
        if (offset < text.length()) return false;
        return chars.subSequence(offset - text.length(), offset).toString().equals(text);
    }

    @Override
    public Result charTyped(char c, Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if (!(file instanceof JetFile)) {
            return Result.CONTINUE;
        }

        if (jetLTTyped) {
            jetLTTyped = false;
            JetLtGtTypingUtils.handleJetAutoCloseLT(editor);
            return Result.STOP;
        }

        if (c == '{' && CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
            PsiDocumentManager.getInstance(project).commitAllDocuments();

            int offset = editor.getCaretModel().getOffset();
            PsiElement previousElement = file.findElementAt(offset - 1);
            if (previousElement instanceof LeafPsiElement
                    && ((LeafPsiElement) previousElement).getElementType() == JetTokens.LONG_TEMPLATE_ENTRY_START) {
                editor.getDocument().insertString(offset, "}");
                return Result.STOP;
            }
        }

        return Result.CONTINUE;
    }

    /**
     * Copied from
     * @see com.intellij.codeInsight.editorActions.TypedHandler#indentBrace(Project, Editor, char)
     */
    private static void indentBrace(@NotNull final Project project, @NotNull final Editor editor, char braceChar) {
        final int offset = editor.getCaretModel().getOffset() - 1;
        Document document = editor.getDocument();
        CharSequence chars = document.getCharsSequence();
        if (offset < 0 || chars.charAt(offset) != braceChar) return;

        int spaceStart = CharArrayUtil.shiftBackward(chars, offset - 1, " \t");
        if (spaceStart < 0 || chars.charAt(spaceStart) == '\n' || chars.charAt(spaceStart) == '\r') {
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            documentManager.commitDocument(document);

            final PsiFile file = documentManager.getPsiFile(document);
            if (file == null || !file.isWritable()) return;
            PsiElement element = file.findElementAt(offset);
            if (element == null) return;

            EditorHighlighter highlighter = ((EditorEx) editor).getHighlighter();
            HighlighterIterator iterator = highlighter.createIterator(offset);

            FileType fileType = file.getFileType();
            BraceMatcher braceMatcher = BraceMatchingUtil.getBraceMatcher(fileType, iterator);
            boolean isBrace =
                    braceMatcher.isLBraceToken(iterator, chars, fileType) || braceMatcher.isRBraceToken(iterator, chars, fileType);
            if (element.getNode() != null && isBrace) {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        int newOffset = CodeStyleManager.getInstance(project).adjustLineIndent(file, offset);
                        editor.getCaretModel().moveToOffset(newOffset + 1);
                        editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                        editor.getSelectionModel().removeSelection();
                    }
                });
            }
        }
    }
}

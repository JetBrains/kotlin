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
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtQualifiedExpression;
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry;

public class KotlinTypedHandler extends TypedHandlerDelegate {
    private final static TokenSet CONTROL_FLOW_EXPRESSIONS = TokenSet.create(
            KtNodeTypes.IF,
            KtNodeTypes.ELSE,
            KtNodeTypes.FOR,
            KtNodeTypes.WHILE,
            KtNodeTypes.TRY);

    private final static TokenSet SUPPRESS_AUTO_INSERT_CLOSE_BRACE_AFTER = TokenSet.create(
            KtTokens.RPAR,
            KtTokens.ELSE_KEYWORD,
            KtTokens.TRY_KEYWORD
    );

    private boolean kotlinLTTyped;

    @Override
    public Result beforeCharTyped(char c, Project project, Editor editor, PsiFile file, FileType fileType) {
        if (!(file instanceof KtFile)) {
            return Result.CONTINUE;
        }

        switch (c) {
            case '<':
                kotlinLTTyped = CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET &&
                                LtGtTypingUtils.shouldAutoCloseAngleBracket(editor.getCaretModel().getOffset(), editor);
                autoPopupParameterInfo(project, editor);
                return Result.CONTINUE;

            case '>':
                if (CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
                    if (LtGtTypingUtils.handleKotlinGTInsert(editor)) {
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

                if (iterator.atEnd() || !(SUPPRESS_AUTO_INSERT_CLOSE_BRACE_AFTER.contains(iterator.getTokenType()))) {
                    return Result.CONTINUE;
                }

                int tokenBeforeBraceOffset = iterator.getStart();

                PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());

                PsiElement leaf = file.findElementAt(offset);
                if (leaf != null) {
                    PsiElement parent = leaf.getParent();
                    if (parent != null && CONTROL_FLOW_EXPRESSIONS.contains(parent.getNode().getElementType())) {
                        ASTNode nonWhitespaceSibling = FormatterUtil.getPreviousNonWhitespaceSibling(leaf.getNode());
                        if (nonWhitespaceSibling != null && nonWhitespaceSibling.getStartOffset() == tokenBeforeBraceOffset) {
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

            case ':':
                autoPopupCallableReferenceLookup(project, editor);
                return Result.CONTINUE;

            case '[':
                autoPopupParameterInfo(project, editor);
                return Result.CONTINUE;
        }

        return Result.CONTINUE;
    }

    private static void autoPopupParameterInfo(Project project, Editor editor) {
        int offset = editor.getCaretModel().getOffset();
        if (offset == 0) return;

        HighlighterIterator iterator = ((EditorEx) editor).getHighlighter().createIterator(offset - 1);
        IElementType tokenType = iterator.getTokenType();
        if (KtTokens.COMMENTS.contains(tokenType)
            || tokenType == KtTokens.REGULAR_STRING_PART
            || tokenType == KtTokens.OPEN_QUOTE
            || tokenType == KtTokens.CHARACTER_LITERAL) return;

        AutoPopupController.getInstance(project).autoPopupParameterInfo(editor, null);
    }

    private static void autoPopupMemberLookup(Project project, final Editor editor) {
        AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, new Condition<PsiFile>() {
            @Override
            public boolean value(PsiFile file) {
                int offset = editor.getCaretModel().getOffset();

                PsiElement lastToken = file.findElementAt(offset - 1);
                if (lastToken == null) return false;

                IElementType elementType = lastToken.getNode().getElementType();
                if (elementType == KtTokens.DOT || elementType == KtTokens.SAFE_ACCESS) return true;

                if (elementType == KtTokens.REGULAR_STRING_PART && lastToken.getTextRange().getStartOffset() == offset - 1) {
                    PsiElement prevSibling = lastToken.getParent().getPrevSibling();
                    return prevSibling != null && prevSibling instanceof KtSimpleNameStringTemplateEntry;
                }

                return false;
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

                return lastElement.getNode().getElementType() == KtTokens.AT;
            }
        });
    }

    private static void autoPopupCallableReferenceLookup(Project project, final Editor editor) {
        AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, new Condition<PsiFile>() {
            @Override
            public boolean value(PsiFile file) {
                int offset = editor.getCaretModel().getOffset();

                PsiElement lastElement = file.findElementAt(offset - 1);
                if (lastElement == null) return false;

                return lastElement.getNode().getElementType() == KtTokens.COLONCOLON;
            }
        });
    }

    private static boolean endsWith(CharSequence chars, int offset, String text) {
        if (offset < text.length()) return false;
        return chars.subSequence(offset - text.length(), offset).toString().equals(text);
    }

    @Override
    public Result charTyped(char c, Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if (!(file instanceof KtFile)) {
            return Result.CONTINUE;
        }

        if (kotlinLTTyped) {
            kotlinLTTyped = false;
            LtGtTypingUtils.handleKotlinAutoCloseLT(editor);
            return Result.STOP;
        }
        else if (c == '{' && CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
            PsiDocumentManager.getInstance(project).commitAllDocuments();

            int offset = editor.getCaretModel().getOffset();
            PsiElement previousElement = file.findElementAt(offset - 1);
            if (previousElement instanceof LeafPsiElement
                    && ((LeafPsiElement) previousElement).getElementType() == KtTokens.LONG_TEMPLATE_ENTRY_START) {
                editor.getDocument().insertString(offset, "}");
                return Result.STOP;
            }
        }
        else if (c == ':') {
            if (autoIndentCase(editor, project, file, KtClassOrObject.class)) {
                return Result.STOP;
            }
        }
        else if (c == '.') {
            if (autoIndentCase(editor, project, file, KtQualifiedExpression.class)) {
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

    private static boolean autoIndentCase(Editor editor, Project project, PsiFile file, Class<?> kclass) {
        int offset = editor.getCaretModel().getOffset();

        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());

        PsiElement currElement = file.findElementAt(offset - 1);
        if (currElement != null) {

            // Should be applied only if there's nothing but the whitespace in line before the element
            PsiElement prevLeaf = PsiTreeUtil.prevLeaf(currElement);
            if (!(prevLeaf instanceof PsiWhiteSpace && prevLeaf.getText().contains("\n"))) {
                return false;
            }

            PsiElement parent = currElement.getParent();
            if (parent != null && kclass.isInstance(parent)) {
                int curElementLength = currElement.getText().length();
                if (offset < curElementLength) return false;

                CodeStyleManager.getInstance(project).adjustLineIndent(file, offset - curElementLength);

                return true;
            }
        }
        return false;
    }
}

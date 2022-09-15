/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;

// TODO: extract PSI-independent parts, specifically coordinate classes
public class PsiDiagnosticUtils {
    public static String atLocation(@NotNull PsiElement element) {
        if (element.isValid()) {
            return atLocation(element.getContainingFile(), element.getTextRange());
        }

        PsiFile file = null;
        int offset = -1;
        try {
            file = element.getContainingFile();
            offset = element.getTextOffset();
        }
        catch (PsiInvalidElementAccessException invalidException) {
            // ignore
        }

        return "at offset: " + (offset != -1 ? offset : "<unknown>") + " file: " + (file != null ? file : "<unknown>");
    }

    public static String atLocation(KtExpression expression) {
        return atLocation(expression.getNode());
    }

    public static String atLocation(@NotNull ASTNode node) {
        int startOffset = node.getStartOffset();
        PsiElement element = PsiUtilsKt.closestPsiElement(node);
        if (element != null) {
            return atLocation(element);
        }

        return "at offset " + startOffset + " (line and file unknown: no PSI element)";
    }

    @NotNull
    public static String atLocation(@NotNull PsiFile file, @NotNull TextRange textRange) {
        Document document = file.getViewProvider().getDocument();
        return atLocation(file, textRange, document);
    }

    @NotNull
    public static String atLocation(PsiFile file, TextRange textRange, Document document) {
        int offset = textRange.getStartOffset();
        VirtualFile virtualFile = file.getVirtualFile();
        String pathSuffix = " in " + (virtualFile == null ? file.getName() : virtualFile.getPath());
        return offsetToLineAndColumn(document, offset) + pathSuffix;
    }

    @NotNull
    public static LineAndColumn offsetToLineAndColumn(@Nullable Document document, int offset) {
        if (document == null || document.getTextLength() == 0) {
            return new LineAndColumn(-1, offset, null);
        }

        int lineNumber = document.getLineNumber(offset);
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        int column = offset - lineStartOffset;

        int lineEndOffset = document.getLineEndOffset(lineNumber);
        CharSequence lineContent = document.getCharsSequence().subSequence(lineStartOffset, lineEndOffset);

        return new LineAndColumn(lineNumber + 1, column + 1, lineContent.toString());
    }

    public static final class LineAndColumn {

        public static final LineAndColumn NONE = new LineAndColumn(-1, -1, null);

        private final int line;
        private final int column;
        private final String lineContent;

        public LineAndColumn(int line, int column, @Nullable String lineContent) {
            this.line = line;
            this.column = column;
            this.lineContent = lineContent;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }

        @Nullable
        public String getLineContent() {
            return lineContent;
        }

        // NOTE: This method is used for presenting positions to the user
        @Override
        public String toString() {
            if (line < 0) {
                return "(offset: " + column + " line unknown)";
            }
            return "(" + line + "," + column + ")";
        }
    }

    public static final class LineAndColumnRange {

        public static final LineAndColumnRange NONE = new LineAndColumnRange(LineAndColumn.NONE, LineAndColumn.NONE);

        private final LineAndColumn start;
        private final LineAndColumn end;

        public LineAndColumnRange(LineAndColumn start, LineAndColumn end) {
            this.start = start;
            this.end = end;
        }

        public LineAndColumn getStart() {
            return start;
        }

        public LineAndColumn getEnd() {
            return end;
        }

        // NOTE: This method is used for presenting positions to the user
        @Override
        public String toString() {
            if (start.line == end.line) {
                return "(" + start.line + "," + start.column + "-" + end.column + ")";
            }

            return start + " - " + end;
        }
    }
}

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

/**
 * Utilities for rendering the source location of PSI elements as human-readable strings for use in diagnostic and log
 * messages.
 */
public class PsiDiagnosticUtils {
    /**
     * Returns a human-readable description of the source location of the given {@code element} (its file plus
     * line and column), falling back to an offset-based description if the element is invalid.
     */
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

    /**
     * Returns a human-readable description of the source location of the given {@code expression}.
     */
    public static String atLocation(KtExpression expression) {
        return atLocation(expression.getNode());
    }

    /**
     * Returns a human-readable description of the source location of the given AST {@code node}.
     */
    public static String atLocation(@NotNull ASTNode node) {
        int startOffset = node.getStartOffset();
        PsiElement element = PsiUtilsKt.closestPsiElement(node);
        if (element != null) {
            return atLocation(element);
        }

        return "at offset " + startOffset + " (line and file unknown: no PSI element)";
    }

    /**
     * Returns a human-readable description of the location of {@code textRange} within {@code file}.
     */
    @NotNull
    public static String atLocation(@NotNull PsiFile file, @NotNull TextRange textRange) {
        Document document = file.getViewProvider().getDocument();
        return atLocation(file, textRange, document);
    }

    /**
     * Returns a human-readable description of the location of {@code textRange} within {@code file}, using the given
     * {@code document} to compute the line and column.
     */
    @NotNull
    public static String atLocation(PsiFile file, TextRange textRange, Document document) {
        int offset = textRange.getStartOffset();
        VirtualFile virtualFile = file.getVirtualFile();
        String pathSuffix = " in " + (virtualFile == null ? file.getName() : virtualFile.getPath());
        return offsetToLineAndColumn(document, offset) + pathSuffix;
    }

    /**
     * Converts a character {@code offset} within {@code document} to a 1-based {@link LineAndColumn}, or a position
     * with an unknown line (line {@code -1}) if {@code document} is {@code null} or empty.
     */
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

    /**
     * A 1-based line and column position in a file, optionally carrying the text of the line.
     */
    public static final class LineAndColumn {

        /**
         * A sentinel value denoting an unknown position.
         */
        public static final LineAndColumn NONE = new LineAndColumn(-1, -1, null);

        private final int line;
        private final int column;
        private final String lineContent;

        public LineAndColumn(int line, int column, @Nullable String lineContent) {
            this.line = line;
            this.column = column;
            this.lineContent = lineContent;
        }

        /**
         * Returns the 1-based line number, or a negative value if it is unknown.
         */
        public int getLine() {
            return line;
        }

        /**
         * Returns the 1-based column number.
         */
        public int getColumn() {
            return column;
        }

        /**
         * Returns the text of the line, or {@code null} if it is unavailable.
         */
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

    /**
     * A range spanning from a start to an end {@link LineAndColumn} position.
     */
    public static final class LineAndColumnRange {

        /**
         * A sentinel value denoting an unknown range.
         */
        public static final LineAndColumnRange NONE = new LineAndColumnRange(LineAndColumn.NONE, LineAndColumn.NONE);

        private final LineAndColumn start;
        private final LineAndColumn end;

        public LineAndColumnRange(LineAndColumn start, LineAndColumn end) {
            this.start = start;
            this.end = end;
        }

        /**
         * Returns the start position of the range.
         */
        public LineAndColumn getStart() {
            return start;
        }

        /**
         * Returns the end position of the range.
         */
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

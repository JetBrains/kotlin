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

package org.jetbrains.kotlin.diagnostics;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DiagnosticUtils {
    @NotNull
    private static final Comparator<TextRange> TEXT_RANGE_COMPARATOR = (o1, o2) -> {
        if (o1.getStartOffset() != o2.getStartOffset()) {
            return o1.getStartOffset() - o2.getStartOffset();
        }
        return o1.getEndOffset() - o2.getEndOffset();
    };

    private DiagnosticUtils() {
    }

    public static String atLocation(DeclarationDescriptor descriptor) {
        PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
        if (element == null) {
            element = DescriptorToSourceUtils.descriptorToDeclaration(descriptor.getOriginal());
        }
        if (element == null && descriptor instanceof ASTNode) {
            element = getClosestPsiElement((ASTNode) descriptor);
        }
        if (element != null) {
            return atLocation(element);
        } else {
            return "unknown location";
        }
    }

    public static String atLocation(KtExpression expression) {
        return atLocation(expression.getNode());
    }

    public static String atLocation(@NotNull PsiElement element) {
        return atLocation(element.getNode());
    }

    public static String atLocation(@NotNull ASTNode node) {
        int startOffset = node.getStartOffset();
        PsiElement element = getClosestPsiElement(node);
        if (element != null && element.isValid()) {
            return atLocation(element.getContainingFile(), element.getTextRange());
        }
        return "' at offset " + startOffset + " (line and file unknown: no PSI element)";
    }

    @Nullable
    public static PsiElement getClosestPsiElement(@NotNull ASTNode node) {
        while (node.getPsi() == null) {
            node = node.getTreeParent();
        }
        return node.getPsi();
    }
    
    @NotNull
    public static PsiFile getContainingFile(@NotNull ASTNode node) {
        PsiElement closestPsiElement = getClosestPsiElement(node);
        assert closestPsiElement != null : "This node is not contained by a file";
        return closestPsiElement.getContainingFile();
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
        return offsetToLineAndColumn(document, offset).toString() + pathSuffix;
    }

    @NotNull
    public static LineAndColumn getLineAndColumn(@NotNull Diagnostic diagnostic) {
        PsiFile file = diagnostic.getPsiFile();
        List<TextRange> textRanges = diagnostic.getTextRanges();
        if (textRanges.isEmpty()) return LineAndColumn.NONE;
        TextRange firstRange = firstRange(textRanges);
        return getLineAndColumnInPsiFile(file, firstRange);
    }

    @NotNull
    public static LineAndColumn getLineAndColumnInPsiFile(PsiFile file, TextRange range) {
        Document document = file.getViewProvider().getDocument();
        return offsetToLineAndColumn(document, range.getStartOffset());
    }

    @NotNull
    public static LineAndColumn offsetToLineAndColumn(@Nullable Document document, int offset) {
        if (document == null) {
            return new LineAndColumn(-1, offset, null);
        }

        int lineNumber = document.getLineNumber(offset);
        int lineStartOffset = document.getLineStartOffset(lineNumber);
        int column = offset - lineStartOffset;

        int lineEndOffset = document.getLineEndOffset(lineNumber);
        CharSequence lineContent = document.getCharsSequence().subSequence(lineStartOffset, lineEndOffset);

        return new LineAndColumn(lineNumber + 1, column + 1, lineContent.toString());
    }

    public static void throwIfRunningOnServer(Throwable e) {
        // This is needed for the Web Demo server to log the exceptions coming from the analyzer instead of showing them in the editor.
        if (System.getProperty("kotlin.running.in.server.mode", "false").equals("true") || ApplicationManager.getApplication().isUnitTestMode()) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            if (e instanceof Error) {
                throw (Error) e;
            }
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public static TextRange firstRange(@NotNull List<TextRange> ranges) {
        return Collections.min(ranges, TEXT_RANGE_COMPARATOR);
    }

    @NotNull
    public static List<Diagnostic> sortedDiagnostics(@NotNull Collection<Diagnostic> diagnostics) {
        List<Diagnostic> result = Lists.newArrayList(diagnostics);
        result.sort((d1, d2) -> {
            String path1 = d1.getPsiFile().getViewProvider().getVirtualFile().getPath();
            String path2 = d2.getPsiFile().getViewProvider().getVirtualFile().getPath();
            if (!path1.equals(path2)) return path1.compareTo(path2);

            TextRange range1 = firstRange(d1.getTextRanges());
            TextRange range2 = firstRange(d2.getTextRanges());

            if (!range1.equals(range2)) {
                return TEXT_RANGE_COMPARATOR.compare(range1, range2);
            }

            return d1.getFactory().getName().compareTo(d2.getFactory().getName());
        });
        return result;
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

    public static boolean hasError(Diagnostics diagnostics) {
        for (Diagnostic diagnostic : diagnostics.all()) {
            if (diagnostic.getSeverity() == Severity.ERROR) return true;
        }

        return false;
    }
}

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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
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
            element = PsiUtilsKt.closestPsiElement((ASTNode) descriptor);
        }
        if (element != null) {
            return PsiDiagnosticUtils.atLocation(element);
        } else {
            return "unknown location";
        }
    }
    
    @NotNull
    public static PsiFile getContainingFile(@NotNull ASTNode node) {
        PsiElement closestPsiElement = PsiUtilsKt.closestPsiElement(node);
        assert closestPsiElement != null : "This node is not contained by a file";
        return closestPsiElement.getContainingFile();
    }

    @NotNull
    public static PsiDiagnosticUtils.LineAndColumn getLineAndColumn(@NotNull Diagnostic diagnostic) {
        PsiFile file = diagnostic.getPsiFile();
        List<TextRange> textRanges = diagnostic.getTextRanges();
        if (textRanges.isEmpty()) return PsiDiagnosticUtils.LineAndColumn.NONE;
        TextRange firstRange = firstRange(textRanges);
        if (file == null) {
            return PsiDiagnosticUtils.LineAndColumn.NONE;
        }
        return getLineAndColumnInPsiFile(file, firstRange);
    }

    @NotNull
    public static PsiDiagnosticUtils.LineAndColumn getLineAndColumnInPsiFile(PsiFile file, TextRange range) {
        Document document = file.getViewProvider().getDocument();
        return PsiDiagnosticUtils.offsetToLineAndColumn(document, range.getStartOffset());
    }

    @NotNull
    public static PsiDiagnosticUtils.LineAndColumnRange getLineAndColumnRange(@NotNull Diagnostic diagnostic) {
        PsiFile file = diagnostic.getPsiFile();
        List<TextRange> textRanges = diagnostic.getTextRanges();
        if (textRanges.isEmpty()) return PsiDiagnosticUtils.LineAndColumnRange.NONE;
        TextRange firstRange = firstRange(textRanges);
        if (file == null) {
            return PsiDiagnosticUtils.LineAndColumnRange.NONE;
        }
        return getLineAndColumnRangeInPsiFile(file, firstRange);
    }

    @NotNull
    public static PsiDiagnosticUtils.LineAndColumnRange getLineAndColumnRangeInPsiFile(PsiFile file, TextRange range) {
        Document document = file.getViewProvider().getDocument();
        return new PsiDiagnosticUtils.LineAndColumnRange(
                PsiDiagnosticUtils.offsetToLineAndColumn(document, range.getStartOffset()),
                PsiDiagnosticUtils.offsetToLineAndColumn(document, range.getEndOffset())
        );
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
            PsiFile file1 = d1.getPsiFile();
            PsiFile file2 = d2.getPsiFile();
            if (file1 != null && file2 != null) {
                String path1 = file1.getViewProvider().getVirtualFile().getPath();
                String path2 = file2.getViewProvider().getVirtualFile().getPath();
                if (!path1.equals(path2)) return path1.compareTo(path2);
            }

            TextRange range1 = firstRange(d1.getTextRanges());
            TextRange range2 = firstRange(d2.getTextRanges());

            if (!range1.equals(range2)) {
                return TEXT_RANGE_COMPARATOR.compare(range1, range2);
            }

            return d1.getFactory().getName().compareTo(d2.getFactory().getName());
        });
        return result;
    }

    public static boolean hasError(Diagnostics diagnostics) {
        for (Diagnostic diagnostic : diagnostics.all()) {
            if (diagnostic.getSeverity() == Severity.ERROR) return true;
        }

        return false;
    }
}

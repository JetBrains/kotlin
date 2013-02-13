package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetBlockExpression;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;

import java.util.ArrayList;
import java.util.List;

public class CodeInsightUtils {

    @Nullable
    public static JetExpression findExpression(@NotNull PsiFile file, int startOffset, int endOffset) {
        PsiElement element = findElementOfClassAtRange(file, startOffset, endOffset, JetExpression.class);
        if (element == null) {
            return null;
        }
        else if (!(element instanceof JetExpression)) {
            return null;
        }
        else if (element instanceof JetBlockExpression) {
            List<JetElement> statements = ((JetBlockExpression) element).getStatements();
            if (statements.size() == 1) {
                JetElement elem = statements.get(0);
                if (elem.getText().equals(element.getText()) && elem instanceof JetExpression) {
                    return (JetExpression) elem;
                }
            }
        }
        return (JetExpression) element;
    }

    @NotNull
    public static PsiElement[] findStatements(@NotNull PsiFile file, int startOffset, int endOffset) {
        PsiElement element1 = getElementAtOffsetIgnoreWhitespaceBefore(file, startOffset);
        PsiElement element2 = getElementAtOffsetIgnoreWhitespaceAfter(file, endOffset);

        if (element1 == null || element2 == null) return PsiElement.EMPTY_ARRAY;

        startOffset = element1.getTextRange().getStartOffset();
        endOffset = element2.getTextRange().getEndOffset();

        PsiElement parent = PsiTreeUtil.findCommonParent(element1, element2);
        if (parent == null) return PsiElement.EMPTY_ARRAY;
        while (true) {
            if (parent instanceof JetBlockExpression) break;
            if (parent == null || parent instanceof JetFile) return PsiElement.EMPTY_ARRAY;
            parent = parent.getParent();
        }

        if (!parent.equals(element1)) {
            while (!parent.equals(element1.getParent())) {
                element1 = element1.getParent();
            }
        }
        if (startOffset != element1.getTextRange().getStartOffset()) return PsiElement.EMPTY_ARRAY;

        if (!parent.equals(element2)) {
            while (!parent.equals(element2.getParent())) {
                element2 = element2.getParent();
            }
        }
        if (endOffset != element2.getTextRange().getEndOffset()) return PsiElement.EMPTY_ARRAY;

        PsiElement[] children = parent.getChildren();
        ArrayList<PsiElement> array = new ArrayList<PsiElement>();
        boolean flag = false;
        for (PsiElement child : children) {
            if (child.equals(element1)) {
                flag = true;
            }
            if (flag && !(child instanceof PsiWhiteSpace)) {
                array.add(child);
            }
            if (child.equals(element2)) {
                break;
            }
        }

        for (PsiElement element : array) {
            if (!(element instanceof JetExpression || element instanceof PsiWhiteSpace || element instanceof PsiComment)) {
                return PsiElement.EMPTY_ARRAY;
            }
        }

        return PsiUtilCore.toPsiElementArray(array);
    }

    @Nullable
    public static PsiElement findElementOfClassAtRange(@NotNull PsiFile file, int startOffset, int endOffset, Class<JetExpression> aClass) {
        PsiElement element1 = getElementAtOffsetIgnoreWhitespaceBefore(file, startOffset);
        PsiElement element2 = getElementAtOffsetIgnoreWhitespaceAfter(file, endOffset);

        if (element1 == null || element2 == null) return null;

        return PsiTreeUtil.findElementOfClassAtRange(file, element1.getTextRange().getStartOffset(),
                                                     element2.getTextRange().getEndOffset(), aClass);
    }

    @Nullable
    private static PsiElement getElementAtOffsetIgnoreWhitespaceBefore(@NotNull PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        if (element instanceof PsiWhiteSpace) {
            return file.findElementAt(element.getTextRange().getEndOffset());
        }
        return element;
    }

    @Nullable
    private static PsiElement getElementAtOffsetIgnoreWhitespaceAfter(@NotNull PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset - 1);
        if (element instanceof PsiWhiteSpace) {
            return file.findElementAt(element.getTextRange().getStartOffset() - 1);
        }
        return element;
    }

    private CodeInsightUtils() {
    }
}

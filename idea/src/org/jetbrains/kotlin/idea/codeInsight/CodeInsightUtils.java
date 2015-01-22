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

package org.jetbrains.kotlin.idea.codeInsight;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.JetType;

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

        // For cases like 'this@outerClass', don't return the label part
        if (JetPsiUtil.isLabelIdentifierExpression(element)) {
            element = PsiTreeUtil.getParentOfType(element, JetExpression.class);
        }

        if (element instanceof JetBlockExpression) {
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

        element1 = getTopmostParentInside(element1, parent);
        if (startOffset != element1.getTextRange().getStartOffset()) return PsiElement.EMPTY_ARRAY;

        element2 = getTopmostParentInside(element2, parent);
        if (endOffset != element2.getTextRange().getEndOffset()) return PsiElement.EMPTY_ARRAY;

        List<PsiElement> array = new ArrayList<PsiElement>();
        PsiElement stopElement = element2.getNextSibling();
        for (PsiElement currentElement = element1; currentElement != stopElement; currentElement = currentElement.getNextSibling()) {
            if (!(currentElement instanceof PsiWhiteSpace)) {
                array.add(currentElement);
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

        startOffset = element1.getTextRange().getStartOffset();
        endOffset = element2.getTextRange().getEndOffset();

        JetExpression jetExpression = PsiTreeUtil.findElementOfClassAtRange(file, startOffset, endOffset, aClass);
        if (jetExpression == null ||
            jetExpression.getTextRange().getStartOffset() != startOffset ||
            jetExpression.getTextRange().getEndOffset() != endOffset) {
            return null;
        }
        return jetExpression;
    }

    @Nullable
    public static PsiElement[] findElementsOfClassInRange(@NotNull PsiFile file, int startOffset, int endOffset, Class<? extends PsiElement> aClass) {
        PsiElement element1 = getElementAtOffsetIgnoreWhitespaceBefore(file, startOffset);
        PsiElement element2 = getElementAtOffsetIgnoreWhitespaceAfter(file, endOffset);

        if (element1 == null || element2 == null) return PsiElement.EMPTY_ARRAY;

        startOffset = element1.getTextRange().getStartOffset();
        endOffset = element2.getTextRange().getEndOffset();

        PsiElement parent = PsiTreeUtil.findCommonParent(element1, element2);
        if (parent == null) return PsiElement.EMPTY_ARRAY;

        element1 = getTopmostParentInside(element1, parent);
        if (startOffset != element1.getTextRange().getStartOffset()) return PsiElement.EMPTY_ARRAY;

        element2 = getTopmostParentInside(element2, parent);
        if (endOffset != element2.getTextRange().getEndOffset()) return PsiElement.EMPTY_ARRAY;

        PsiElement stopElement = element2.getNextSibling();
        List<PsiElement> array = new ArrayList<PsiElement>();
        for (PsiElement currentElement = element1; currentElement != stopElement && currentElement != null; currentElement = currentElement.getNextSibling()) {
            if (aClass.isInstance(currentElement)) {
                array.add(currentElement);
            }
            array.addAll(PsiTreeUtil.findChildrenOfType(currentElement, aClass));
        }

        return PsiUtilCore.toPsiElementArray(array);
    }

    @NotNull
    private static PsiElement getTopmostParentInside(@NotNull PsiElement element, @NotNull PsiElement parent) {
        if (!parent.equals(element)) {
            while (!parent.equals(element.getParent())) {
                element = element.getParent();
            }
        }
        return element;
    }

    @Nullable
    public static PsiElement getElementAtOffsetIgnoreWhitespaceBefore(@NotNull PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        if (element instanceof PsiWhiteSpace) {
            return file.findElementAt(element.getTextRange().getEndOffset());
        }
        return element;
    }

    @Nullable
    public static PsiElement getElementAtOffsetIgnoreWhitespaceAfter(@NotNull PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset - 1);
        if (element instanceof PsiWhiteSpace) {
            return file.findElementAt(element.getTextRange().getStartOffset() - 1);
        }
        return element;
    }

    @Nullable
    public static String defaultInitializer(JetType type) {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        if (type.isMarkedNullable()) {
            return "null";
        }
        else if (type.equals(builtIns.getIntType()) || type.equals(builtIns.getLongType()) ||
                 type.equals(builtIns.getShortType()) || type.equals(builtIns.getByteType())) {
            return "0";
        }
        else if (type.equals(builtIns.getFloatType())) {
            return "0.0f";
        }
        else if (type.equals(builtIns.getDoubleType())) {
            return "0.0";
        }
        else if (type.equals(builtIns.getCharType())) {
            return "'\\u0000'";
        }
        else if (type.equals(builtIns.getBooleanType())) {
            return "false";
        }
        else if (type.equals(builtIns.getUnitType())) {
            return "Unit";
        }
        else if (type.equals(builtIns.getStringType())) {
            return "\"\"";
        }

        return null;
    }

    public static void showErrorHint(
            @NotNull Project project, @NotNull Editor editor,
            @NotNull String message, @NotNull String title,
            @Nullable String helpId
    ) {
        if (ApplicationManager.getApplication().isUnitTestMode()) throw new RuntimeException(message);
        CommonRefactoringUtil.showErrorHint(project, editor, message, title, helpId);
    }

    private CodeInsightUtils() {
    }

    @Nullable
    public static Integer getStartLineOffset(@NotNull PsiFile file, int line) {
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null) return null;

        if (line >= document.getLineCount()) {
            return null;
        }

        int lineStartOffset = document.getLineStartOffset(line);
        return CharArrayUtil.shiftForward(document.getCharsSequence(), lineStartOffset, " \t");
    }

    @Nullable
    public static Integer getEndLineOffset(@NotNull PsiFile file, int line) {
        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null) return null;

        if (line >= document.getLineCount()) {
            return null;
        }

        int lineStartOffset = document.getLineEndOffset(line);
        return CharArrayUtil.shiftBackward(document.getCharsSequence(), lineStartOffset, " \t");
    }

    @Nullable
    public static PsiElement getTopmostElementAtOffset(@NotNull PsiElement element, int offset) {
        do {
            PsiElement parent = element.getParent();
            if (parent == null || (parent.getTextOffset() < offset) || parent instanceof JetBlockExpression) {
                break;
            }
            element = parent;
        }
        while(true);

        return element;
    }
}

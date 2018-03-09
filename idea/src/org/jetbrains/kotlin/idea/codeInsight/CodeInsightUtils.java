/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import kotlin.collections.ArraysKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier;
import org.jetbrains.kotlin.resolve.scopes.receivers.Qualifier;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.*;

public class CodeInsightUtils {

    @Nullable
    public static PsiElement findElement(
            @NotNull PsiFile file,
            int startOffset,
            int endOffset,
            @NotNull CodeInsightUtils.ElementKind elementKind
    ) {
        Class<? extends KtElement> elementClass;
        switch (elementKind) {
            case EXPRESSION: elementClass = KtExpression.class;
                break;
            case TYPE_ELEMENT: elementClass = KtTypeElement.class;
                break;
            case TYPE_CONSTRUCTOR: elementClass = KtSimpleNameExpression.class;
                break;
            default: throw new IllegalArgumentException(elementKind.name());
        }
        PsiElement element = findElementOfClassAtRange(file, startOffset, endOffset, elementClass);

        if (elementKind == ElementKind.TYPE_ELEMENT) return element;

        if (elementKind == ElementKind.TYPE_CONSTRUCTOR) {
            return element != null && KtPsiUtilKt.isTypeConstructorReference(element) ? element : null;
        }

        if (element instanceof KtScriptInitializer) {
            element = ((KtScriptInitializer) element).getBody();
        }

        if (element == null) return null;

        // TODO: Support binary operations in "Introduce..." refactorings
        if (element instanceof KtOperationReferenceExpression
            && ((KtOperationReferenceExpression) element).getReferencedNameElementType() != KtTokens.IDENTIFIER
            && element.getParent() instanceof KtBinaryExpression) {
            return null;
        }

        // For cases like 'this@outerClass', don't return the label part
        if (KtPsiUtil.isLabelIdentifierExpression(element)) {
            element = PsiTreeUtil.getParentOfType(element, KtExpression.class);
        }

        if (element instanceof KtBlockExpression) {
            List<KtExpression> statements = ((KtBlockExpression) element).getStatements();
            if (statements.size() == 1) {
                KtExpression statement = statements.get(0);
                if (statement.getText().equals(element.getText())) {
                    return statement;
                }
            }
        }

        KtExpression expression = (KtExpression) element;

        BindingContext context = ResolutionUtils.analyze(expression);

        Qualifier qualifier = context.get(BindingContext.QUALIFIER, expression);
        if (qualifier != null) {
            if (!(qualifier instanceof ClassQualifier)) return null;
            if (((ClassQualifier) qualifier).getDescriptor().getKind() != ClassKind.OBJECT) return null;
        }

        return expression;
    }

    public enum ElementKind {
        EXPRESSION,
        TYPE_ELEMENT,
        TYPE_CONSTRUCTOR
    }

    @NotNull
    public static PsiElement[] findElements(@NotNull PsiFile file, int startOffset, int endOffset, @NotNull ElementKind kind) {
        PsiElement element1 = getElementAtOffsetIgnoreWhitespaceBefore(file, startOffset);
        PsiElement element2 = getElementAtOffsetIgnoreWhitespaceAfter(file, endOffset);

        if (element1 == null || element2 == null) return PsiElement.EMPTY_ARRAY;

        startOffset = element1.getTextRange().getStartOffset();
        endOffset = element2.getTextRange().getEndOffset();

        if (startOffset >= endOffset) return PsiElement.EMPTY_ARRAY;

        PsiElement parent = PsiTreeUtil.findCommonParent(element1, element2);
        if (parent == null) return PsiElement.EMPTY_ARRAY;
        while (true) {
            if (parent instanceof KtBlockExpression) break;
            if (parent == null || parent instanceof KtFile) return PsiElement.EMPTY_ARRAY;
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
            boolean correctType = kind == ElementKind.EXPRESSION && element instanceof KtExpression
                                  || kind == ElementKind.TYPE_ELEMENT && element instanceof KtTypeElement
                                  || kind == ElementKind.TYPE_CONSTRUCTOR && KtPsiUtilKt.isTypeConstructorReference(element);
            if (!(correctType
                  || element.getNode().getElementType() == KtTokens.SEMICOLON
                  || element instanceof PsiWhiteSpace
                  || element instanceof PsiComment)) {
                return PsiElement.EMPTY_ARRAY;
            }
        }

        return PsiUtilCore.toPsiElementArray(array);
    }

    @Nullable
    public static <T extends PsiElement> T findElementOfClassAtRange(@NotNull PsiFile file, int startOffset, int endOffset, Class<T> aClass) {
        // When selected range is this@Fo<select>o</select> we'd like to return `@Foo`
        // But it's PSI looks like: (AT IDENTIFIER):JetLabel
        // So if we search parent starting exactly at IDENTIFIER then we find nothing
        // Solution is to retrieve label if we are on AT or IDENTIFIER
        PsiElement element1 = getParentLabelOrElement(getElementAtOffsetIgnoreWhitespaceBefore(file, startOffset));
        PsiElement element2 = getParentLabelOrElement(getElementAtOffsetIgnoreWhitespaceAfter(file, endOffset));

        if (element1 == null || element2 == null) return null;

        startOffset = element1.getTextRange().getStartOffset();
        endOffset = element2.getTextRange().getEndOffset();

        T newElement = PsiTreeUtil.findElementOfClassAtRange(file, startOffset, endOffset, aClass);
        if (newElement == null ||
            newElement.getTextRange().getStartOffset() != startOffset ||
            newElement.getTextRange().getEndOffset() != endOffset) {
            return null;
        }
        return newElement;
    }

    private static PsiElement getParentLabelOrElement(@Nullable PsiElement element) {
        if (element != null && element.getParent() instanceof KtLabelReferenceExpression) {
            return element.getParent();
        }
        return element;
    }

    @NotNull
    public static List<PsiElement> findElementsOfClassInRange(@NotNull PsiFile file, int startOffset, int endOffset, Class<? extends PsiElement> ... classes) {
        PsiElement element1 = getElementAtOffsetIgnoreWhitespaceBefore(file, startOffset);
        PsiElement element2 = getElementAtOffsetIgnoreWhitespaceAfter(file, endOffset);

        if (element1 == null || element2 == null) return Collections.emptyList();

        startOffset = element1.getTextRange().getStartOffset();
        endOffset = element2.getTextRange().getEndOffset();

        PsiElement parent = PsiTreeUtil.findCommonParent(element1, element2);
        if (parent == null) return Collections.emptyList();

        element1 = getTopmostParentInside(element1, parent);
        if (startOffset != element1.getTextRange().getStartOffset()) return Collections.emptyList();

        element2 = getTopmostParentInside(element2, parent);
        if (endOffset != element2.getTextRange().getEndOffset()) return Collections.emptyList();

        PsiElement stopElement = element2.getNextSibling();
        List<PsiElement> result = new ArrayList<PsiElement>();
        for (PsiElement currentElement = element1; currentElement != stopElement && currentElement != null; currentElement = currentElement.getNextSibling()) {
            for (Class aClass : classes) {
                if (aClass.isInstance(currentElement)) {
                    result.add(currentElement);
                }
                result.addAll(PsiTreeUtil.findChildrenOfType(currentElement, aClass));
            }
        }

        return result;
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
    public static String defaultInitializer(KotlinType type) {
        if (type.isMarkedNullable()) {
            return "null";
        }
        else if (isInt(type) || isLong(type) || isShort(type) || isByte(type)) {
            return "0";
        }
        else if (isFloat(type)) {
            return "0.0f";
        }
        else if (isDouble(type)) {
            return "0.0";
        }
        else if (isChar(type)) {
            return "'\\u0000'";
        }
        else if (isBoolean(type)) {
            return "false";
        }
        else if (isUnit(type)) {
            return "Unit";
        }
        else if (isString(type)) {
            return "\"\"";
        }

        return null;
    }

    public static void showErrorHint(
            @NotNull Project project, @NotNull Editor editor,
            @NotNull String message, @NotNull String title,
            @Nullable String helpId
    ) {
        if (ApplicationManager.getApplication().isUnitTestMode()) throw new CommonRefactoringUtil.RefactoringErrorHintException(message);
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

    @NotNull
    public static PsiElement getTopmostElementAtOffset(@NotNull PsiElement element, int offset) {
        do {
            PsiElement parent = element.getParent();
            if (parent == null || (parent.getTextOffset() < offset) || parent instanceof KtBlockExpression) {
                break;
            }
            element = parent;
        }
        while(true);

        return element;
    }

    @NotNull
    public static PsiElement getTopParentWithEndOffset(@NotNull PsiElement element, @NotNull Class<?> stopAt) {
        int endOffset = element.getTextOffset() + element.getTextLength();

        do {
            PsiElement parent = element.getParent();
            if (parent == null || (parent.getTextOffset() + parent.getTextLength()) != endOffset) {
                break;
            }
            element = parent;

            if (stopAt.isInstance(element)) {
                break;
            }
        }
        while(true);

        return element;
    }


    @Nullable
    public static <T> T getTopmostElementAtOffset(@NotNull PsiElement element, int offset, @NotNull Class<? extends T>... classes) {
        T lastElementOfType = null;
        if (anyIsInstance(element, classes)) {
            lastElementOfType = (T) element;
        }
        do {
            PsiElement parent = element.getParent();
            if (parent == null || (parent.getTextOffset() < offset) || parent instanceof KtBlockExpression) {
                break;
            }
            if (anyIsInstance(parent, classes)) {
                lastElementOfType = (T) parent;
            }
            element = parent;
        }
        while(true);

        return lastElementOfType;
    }

    private static <T> boolean anyIsInstance(PsiElement finalElement, @NotNull Class<? extends T>[] klass) {
        return ArraysKt.any(klass, aClass -> aClass.isInstance(finalElement));
    }
}

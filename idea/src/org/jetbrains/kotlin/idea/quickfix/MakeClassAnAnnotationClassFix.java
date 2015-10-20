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

package org.jetbrains.kotlin.idea.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.idea.references.ReferenceUtilKt;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

public class MakeClassAnAnnotationClassFix extends KotlinQuickFixAction<KtAnnotationEntry> {
    private final KtAnnotationEntry annotationEntry;
    private KtClass annotationClass;

    public MakeClassAnAnnotationClassFix(@NotNull KtAnnotationEntry annotationEntry) {
        super(annotationEntry);
        this.annotationEntry = annotationEntry;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }

        KtTypeReference typeReference = annotationEntry.getTypeReference();
        if (typeReference == null) {
            return false;
        }

        KtSimpleNameExpression referenceExpression = PsiTreeUtil.findChildOfType(typeReference, KtSimpleNameExpression.class);
        if (referenceExpression == null) {
            return false;
        }

        PsiReference reference = ReferenceUtilKt.getMainReference(referenceExpression);
        PsiElement target = reference.resolve();
        if (target instanceof KtClass) {
            annotationClass = (KtClass) target;
            return QuickFixUtil.canModifyElement(annotationClass);
        }

        return false;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("make.class.annotation.class", annotationClass.getName());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("make.class.annotation.class.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull KtFile file) throws IncorrectOperationException {
        KtPsiFactory factory = new KtPsiFactory(annotationClass.getProject());
        KtModifierList list = annotationClass.getModifierList();
        PsiElement added;
        if (list == null) {
            KtModifierList newModifierList = factory.createModifierList(KtTokens.ANNOTATION_KEYWORD);
            added = annotationClass.addBefore(newModifierList, annotationClass.getClassOrInterfaceKeyword());
        }
        else {
            PsiElement entry = factory.createModifier(KtTokens.ANNOTATION_KEYWORD);
            added = list.addBefore(entry, list.getFirstChild());
        }
        annotationClass.addAfter(factory.createWhiteSpace(), added);
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                KtAnnotationEntry annotation = QuickFixUtil.getParentElementOfType(diagnostic, KtAnnotationEntry.class);
                return annotation == null ? null : new MakeClassAnAnnotationClassFix(annotation);
            }
        };
    }
}


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

package org.jetbrains.jet.plugin.quickfix;

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
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.JetBundle;

import static org.jetbrains.jet.lexer.JetTokens.ANNOTATION_KEYWORD;

public class MakeClassAnAnnotationClassFix extends JetIntentionAction<JetAnnotationEntry> {
    private final JetAnnotationEntry annotationEntry;
    private JetClass annotationClass;

    public MakeClassAnAnnotationClassFix(@NotNull JetAnnotationEntry annotationEntry) {
        super(annotationEntry);
        this.annotationEntry = annotationEntry;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }

        JetTypeReference typeReference = annotationEntry.getTypeReference();
        if (typeReference == null) {
            return false;
        }

        JetSimpleNameExpression referenceExpression = PsiTreeUtil.findChildOfType(typeReference, JetSimpleNameExpression.class);
        if (referenceExpression == null) {
            return false;
        }

        PsiReference reference = referenceExpression.getReference();
        if (reference != null) {
            PsiElement target = reference.resolve();
            if (target instanceof JetClass) {
                annotationClass = (JetClass) target;
                return QuickFixUtil.canModifyElement(annotationClass);
            }
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
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        annotationClass.addModifier(ANNOTATION_KEYWORD);
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetAnnotationEntry annotation = QuickFixUtil.getParentElementOfType(diagnostic, JetAnnotationEntry.class);
                return annotation == null ? null : new MakeClassAnAnnotationClassFix(annotation);
            }
        };
    }
}


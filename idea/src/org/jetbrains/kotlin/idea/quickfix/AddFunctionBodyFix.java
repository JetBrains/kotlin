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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFunction;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.jetbrains.kotlin.psi.KtPsiFactoryKt;

public class AddFunctionBodyFix extends KotlinQuickFixAction<KtFunction> {
    public AddFunctionBodyFix(@NotNull KtFunction element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return KotlinBundle.message("add.function.body");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return KotlinBundle.message("add.function.body");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) && !getElement().hasBody();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        KtFunction newElement = (KtFunction) getElement().copy();
        KtPsiFactory psiFactory = KtPsiFactoryKt.KtPsiFactory(file);
        if (!(newElement.getLastChild() instanceof PsiWhiteSpace)) {
            newElement.add(psiFactory.createWhiteSpace());
        }
        if (!newElement.hasBody()) {
            newElement.add(psiFactory.createEmptyBody());
        }
        getElement().replace(newElement);
    }
    
    public static KotlinSingleIntentionActionFactory createFactory() {
        return new KotlinSingleIntentionActionFactory() {
            @Nullable
            @Override
            public KotlinQuickFixAction createAction(Diagnostic diagnostic) {
                PsiElement element = diagnostic.getPsiElement();
                KtFunction function = PsiTreeUtil.getParentOfType(element, KtFunction.class, false);
                if (function == null) return null;
                return new AddFunctionBodyFix(function);
            }
        };
    }
}

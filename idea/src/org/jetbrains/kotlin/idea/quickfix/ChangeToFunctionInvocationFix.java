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
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactoryKt;

public class ChangeToFunctionInvocationFix extends KotlinQuickFixAction<KtExpression> {

    public ChangeToFunctionInvocationFix(@NotNull KtExpression element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return KotlinBundle.message("change.to.function.invocation");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return KotlinBundle.message("change.to.function.invocation");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        KtExpression reference = (KtExpression) getElement().copy();
        getElement().replace(KtPsiFactoryKt.KtPsiFactory(file).createExpression(reference.getText() + "()"));
    }

    public static KotlinSingleIntentionActionFactory createFactory() {
        return new KotlinSingleIntentionActionFactory() {
            @Override
            public KotlinQuickFixAction<KtExpression> createAction(Diagnostic diagnostic) {
                if (diagnostic.getPsiElement() instanceof KtExpression) {
                    return new ChangeToFunctionInvocationFix((KtExpression) diagnostic.getPsiElement());
                }
                return null;
            }
        };
    }
}

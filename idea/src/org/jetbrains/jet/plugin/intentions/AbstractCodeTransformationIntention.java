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

package org.jetbrains.jet.plugin.intentions;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.jet.plugin.JetBundle;

public abstract class AbstractCodeTransformationIntention extends BaseIntentionAction {
    private final Transformer transformer;
    private final Function1<PsiElement, Boolean> isApplicable;

    protected AbstractCodeTransformationIntention(@NotNull Transformer transformer, @NotNull Function1<PsiElement, Boolean> isApplicable) {
        this.transformer = transformer;
        this.isApplicable = isApplicable;
        setText(JetBundle.message(transformer.getKey()));
    }

    @Nullable
    private PsiElement getTarget(@NotNull Editor editor, @NotNull PsiFile file) {
        PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        return PsiUtilPackage.getParentByTypeAndPredicate(element, JetElement.class, false, isApplicable);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message(transformer.getKey() + ".family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return getTarget(editor, file) != null;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) throws IncorrectOperationException {
        PsiElement target = getTarget(editor, file);

        assert target != null : "Intention is not applicable";

        transformer.transform(target, editor, (JetFile) file);
    }
}

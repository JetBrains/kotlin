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

package org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations;

import com.google.common.base.Predicate;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations.core.Transformer;

public abstract class AbstractCodeTransformationIntention<T extends Transformer> extends BaseIntentionAction {
    private final T transformer;
    private final Predicate<PsiElement> isApplicable;

    protected AbstractCodeTransformationIntention(@NotNull T transformer, @NotNull Predicate<PsiElement> isApplicable) {
        this.transformer = transformer;
        this.isApplicable = isApplicable;
        setText(JetBundle.message(transformer.getKey()));
    }

    @Nullable
    private PsiElement getTarget(@NotNull Editor editor, @NotNull PsiFile file) {
        PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        return JetPsiUtil.getParentByTypeAndPredicate(element, JetElement.class, isApplicable, false);
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

        transformer.transform(target);
    }
}

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
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.project.PluginJetFilesProvider;
import org.jetbrains.kotlin.psi.*;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.lexer.KtTokens.OVERRIDE_KEYWORD;
import static org.jetbrains.kotlin.lexer.KtTokens.PUBLIC_KEYWORD;

public class AddOverrideToEqualsHashCodeToStringFix extends KotlinQuickFixAction<PsiElement> {
    public AddOverrideToEqualsHashCodeToStringFix(@NotNull PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return KotlinBundle.message("add.override.to.equals.hashCode.toString");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return KotlinBundle.message("add.override.to.equals.hashCode.toString");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) && isEqualsHashCodeOrToString(getElement());
    }

    private static boolean isEqualsHashCodeOrToString(@Nullable PsiElement element) {
        if (!(element instanceof KtNamedFunction)) return false;
        KtNamedFunction function = (KtNamedFunction) element;
        String name = function.getName();

        if ("equals".equals(name)) {
            List<KtParameter> parameters = function.getValueParameters();
            if (parameters.size() != 1) return false;
            KtTypeReference parameterType = parameters.iterator().next().getTypeReference();
            return parameterType != null && "Any?".equals(parameterType.getText());
        }

        if ("hashCode".equals(name) || "toString".equals(name)) {
            return function.getValueParameters().isEmpty();
        }

        return false;
    }

    @Override
    protected void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        Collection<KtFile> files = PluginJetFilesProvider.allFilesInProject(file.getProject());

        for (KtFile jetFile : files) {
            for (Diagnostic diagnostic : ResolutionUtils.analyzeFully(jetFile).getDiagnostics()) {
                if (diagnostic.getFactory() != Errors.VIRTUAL_MEMBER_HIDDEN) continue;

                KtModifierListOwner element = (KtModifierListOwner) diagnostic.getPsiElement();
                if (!isEqualsHashCodeOrToString(element)) continue;

                element.addModifier(OVERRIDE_KEYWORD);
                element.removeModifier(PUBLIC_KEYWORD);
            }
        }
    }

    @NotNull
    public static KotlinSingleIntentionActionFactory createFactory() {
        return new KotlinSingleIntentionActionFactory() {
            @Nullable
            @Override
            public AddOverrideToEqualsHashCodeToStringFix createAction(@NotNull Diagnostic diagnostic) {
                return new AddOverrideToEqualsHashCodeToStringFix(diagnostic.getPsiElement());
            }
        };
    }

}

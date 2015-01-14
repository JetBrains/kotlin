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
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.project.PluginJetFilesProvider;
import org.jetbrains.kotlin.psi.*;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.lexer.JetTokens.OVERRIDE_KEYWORD;
import static org.jetbrains.kotlin.lexer.JetTokens.PUBLIC_KEYWORD;

public class AddOverrideToEqualsHashCodeToStringFix extends JetIntentionAction<PsiElement> {
    public AddOverrideToEqualsHashCodeToStringFix(@NotNull PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("add.override.to.equals.hashCode.toString");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.override.to.equals.hashCode.toString");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) && isEqualsHashCodeOrToString(element);
    }

    private static boolean isEqualsHashCodeOrToString(@Nullable PsiElement element) {
        if (!(element instanceof JetNamedFunction)) return false;
        JetNamedFunction function = (JetNamedFunction) element;
        String name = function.getName();

        if ("equals".equals(name)) {
            List<JetParameter> parameters = function.getValueParameters();
            if (parameters.size() != 1) return false;
            JetTypeReference parameterType = parameters.iterator().next().getTypeReference();
            return parameterType != null && "Any?".equals(parameterType.getText());
        }

        if ("hashCode".equals(name) || "toString".equals(name)) {
            return function.getValueParameters().isEmpty();
        }

        return false;
    }

    @Override
    protected void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        Collection<JetFile> files = PluginJetFilesProvider.allFilesInProject(file.getProject());

        for (JetFile jetFile : files) {
            for (Diagnostic diagnostic : ResolvePackage.analyzeFully(jetFile).getDiagnostics()) {
                if (diagnostic.getFactory() != Errors.VIRTUAL_MEMBER_HIDDEN) continue;

                JetModifierListOwner element = (JetModifierListOwner) diagnostic.getPsiElement();
                if (!isEqualsHashCodeOrToString(element)) continue;

                element.addModifier(OVERRIDE_KEYWORD);
                element.removeModifier(PUBLIC_KEYWORD);
            }
        }
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public AddOverrideToEqualsHashCodeToStringFix createAction(@NotNull Diagnostic diagnostic) {
                return new AddOverrideToEqualsHashCodeToStringFix(diagnostic.getPsiElement());
            }
        };
    }

}

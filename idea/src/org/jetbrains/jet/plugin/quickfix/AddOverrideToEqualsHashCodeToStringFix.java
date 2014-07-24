/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lexer.JetModifierKeywordToken;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.project.PluginJetFilesProvider;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lexer.JetTokens.*;

public class AddOverrideToEqualsHashCodeToStringFix extends JetIntentionAction<PsiElement> {
    private static final JetModifierKeywordToken[] MODIFIERS_TO_REPLACE = {PUBLIC_KEYWORD, OPEN_KEYWORD};

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
            return parameterType != null && ("Any?".equals(parameterType.getText()) || "jet.Any?".equals(parameterType.getText()));
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
            for (Diagnostic diagnostic : ResolvePackage.getBindingContext(jetFile).getDiagnostics()) {
                if (diagnostic.getFactory() != Errors.VIRTUAL_MEMBER_HIDDEN) continue;

                PsiElement element = diagnostic.getPsiElement();
                if (!isEqualsHashCodeOrToString(element)) continue;

                element.replace(AddModifierFix.addModifier(element, OVERRIDE_KEYWORD, MODIFIERS_TO_REPLACE, project, false));
            }
        }
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public AddOverrideToEqualsHashCodeToStringFix createAction(Diagnostic diagnostic) {
                return new AddOverrideToEqualsHashCodeToStringFix(diagnostic.getPsiElement());
            }
        };
    }

}

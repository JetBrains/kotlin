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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetTypeArgumentList;
import org.jetbrains.jet.plugin.JetBundle;

public class RemoveTypeArgumentsFix extends JetIntentionAction<JetTypeArgumentList> {
    private final JetTypeArgumentList typeArgList;

    public RemoveTypeArgumentsFix(@NotNull JetTypeArgumentList element) {
        super(element);
        typeArgList = element;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("remove.type.arguments");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.type.arguments");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        typeArgList.delete();
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetTypeArgumentList> createAction(Diagnostic diagnostic) {
                JetTypeArgumentList element = QuickFixUtil.getParentElementOfType(diagnostic, JetTypeArgumentList.class);
                if (element == null) return null;
                return new RemoveTypeArgumentsFix(element);
            }
        };
    }
}
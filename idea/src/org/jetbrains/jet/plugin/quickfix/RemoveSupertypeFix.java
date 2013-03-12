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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.plugin.JetBundle;

public class RemoveSupertypeFix extends JetIntentionAction<JetTypeReference> {
    private final JetTypeReference superClass;

    public RemoveSupertypeFix(@NotNull JetTypeReference superClass) {
        super(superClass);
        this.superClass = superClass;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("remove.supertype", superClass.getText());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.supertype.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        PsiElement superClassAndParens = superClass.getParent().getParent();
        // Find the preceding comma.
        // We can't just blindly take the PrevSibling, because it could be whitespace.
        // C1(), C2() versus C1(),C2()
        PsiElement comma = superClassAndParens.getPrevSibling();
        while (comma != null && !comma.getText().contains(",")) {
            comma = comma.getPrevSibling();
        }
        superClassAndParens.delete();
        if (comma != null) {
            comma.delete();
        }
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetTypeReference> createAction(Diagnostic diagnostic) {
                JetTypeReference superClass = QuickFixUtil.getParentElementOfType(diagnostic, JetTypeReference.class);
                if (superClass == null) return null;
                return new RemoveSupertypeFix(superClass);
            }
        };
    }
}
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
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.TokenType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.plugin.JetBundle;

public class RemoveValVarFromParametersFix extends JetIntentionAction<JetParameter> {
    public RemoveValVarFromParametersFix(@NotNull JetParameter element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        ASTNode valOrVarNode = element.getValOrVarNode();
        return JetBundle.message("remove.val.var.from.parameter", valOrVarNode != null ? valOrVarNode.getText() : "null");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("remove.val.var.from.parameter", "val/var");
    }

    @Override
    protected void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        ASTNode valOrVarNode = element.getValOrVarNode();
        if (valOrVarNode == null) return;

        ASTNode whitespace = valOrVarNode.getTreeNext();
        assert whitespace.getElementType() == TokenType.WHITE_SPACE;

        element.getNode().removeRange(valOrVarNode, whitespace.getTreeNext());
    }


    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(@NotNull Diagnostic diagnostic) {
                return new RemoveValVarFromParametersFix((JetParameter) diagnostic.getPsiElement().getParent());
            }
        };
    }
}

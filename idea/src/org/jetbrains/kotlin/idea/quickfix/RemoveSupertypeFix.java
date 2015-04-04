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

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.JetDelegationSpecifier;
import org.jetbrains.kotlin.psi.JetFile;

public class RemoveSupertypeFix extends JetIntentionAction<JetDelegationSpecifier> {
    private final JetDelegationSpecifier superClass;

    public RemoveSupertypeFix(@NotNull JetDelegationSpecifier superClass) {
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
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        // Find the preceding comma and delete it as well.
        // We must ignore whitespaces and comments when looking for the comma.
        PsiElement prevSibling = superClass.getPrevSibling();
        assert prevSibling != null: "A PSI element should exist before supertype declaration";
        ASTNode prev = PsiImplUtil.skipWhitespaceAndCommentsBack(prevSibling.getNode());
        assert prev != null: "A non-whitespace element should exist before supertype declaration";
        if (prev.getElementType() == JetTokens.COMMA) {
            prev.getPsi().delete();
        }
        superClass.delete();
    }

    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetDelegationSpecifier> createAction(Diagnostic diagnostic) {
                JetDelegationSpecifier superClass = QuickFixUtil.getParentElementOfType(diagnostic, JetDelegationSpecifier.class);
                if (superClass == null) return null;
                return new RemoveSupertypeFix(superClass);
            }
        };
    }
}

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
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier;
import org.jetbrains.jet.lang.psi.JetDelegatorToSuperClass;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.plugin.JetBundle;

import java.util.List;

public class ChangeToConstructorInvocationFix extends JetIntentionAction<JetDelegatorToSuperClass> {

    public ChangeToConstructorInvocationFix(@NotNull JetDelegatorToSuperClass element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("change.to.constructor.invocation");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.to.constructor.invocation");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetDelegatorToSuperClass delegator = (JetDelegatorToSuperClass) element.copy();
        JetClass aClass = JetPsiFactory.createClass(project, "class A : " + delegator.getText() + "()");
        List<JetDelegationSpecifier> delegationSpecifiers = aClass.getDelegationSpecifiers();
        assert delegationSpecifiers.size() == 1;
        JetDelegationSpecifier specifier = delegationSpecifiers.iterator().next();
        element.replace(specifier);
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Override
            public JetIntentionAction<JetDelegatorToSuperClass> createAction(Diagnostic diagnostic) {
                if (diagnostic.getPsiElement() instanceof JetDelegatorToSuperClass) {
                    return new ChangeToConstructorInvocationFix((JetDelegatorToSuperClass) diagnostic.getPsiElement());
                }
                return null;
            }
        };
    }
}

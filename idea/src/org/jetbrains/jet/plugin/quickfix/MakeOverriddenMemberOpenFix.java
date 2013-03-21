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
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lexer.JetTokens.OPEN_KEYWORD;

public class MakeOverriddenMemberOpenFix extends JetIntentionAction<JetDeclaration> {
    private final List<PsiElement> overriddenMembers = new ArrayList<PsiElement>();
    private final List<String> containingDeclarationsNames = new ArrayList<String>();

    public MakeOverriddenMemberOpenFix(@NotNull JetDeclaration declaration) {
        super(declaration);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file) || !(file instanceof JetFile)) {
            return false;
        }

        // When running single test 'isAvailable()' is invoked multiple times, so we need to clear lists.
        overriddenMembers.clear();
        containingDeclarationsNames.clear();

        ResolveSession resolveSession = WholeProjectAnalyzerFacade.getLazyResolveSessionForFile((JetFile) file);
        DeclarationDescriptor descriptor = resolveSession.resolveToDescriptor(element);
        if (!(descriptor instanceof CallableMemberDescriptor)) return false;
        CallableMemberDescriptor callableMemberDescriptor = (CallableMemberDescriptor) descriptor;
        for (CallableMemberDescriptor overriddenDescriptor : callableMemberDescriptor.getOverriddenDescriptors()) {
            if (!overriddenDescriptor.getModality().isOverridable()) {
                PsiElement overriddenMember =
                        BindingContextUtils.descriptorToDeclaration(resolveSession.getBindingContext(), overriddenDescriptor);
                if (overriddenMember == null || !QuickFixUtil.canModifyElement(overriddenMember)) {
                    return false;
                }
                String containingDeclarationName = overriddenDescriptor.getContainingDeclaration().getName().getName();
                overriddenMembers.add(overriddenMember);
                containingDeclarationsNames.add(containingDeclarationName);
            }
        }
        return overriddenMembers.size() > 0;
    }

    @NotNull
    @Override
    public String getText() {
        if (overriddenMembers.size() == 1) {
            return JetBundle.message("make.element.modifier", containingDeclarationsNames.get(0) + "." + element.getName(), OPEN_KEYWORD);
        }

        StringBuilder declarations = new StringBuilder();
        Collections.sort(containingDeclarationsNames);
        for (int i = 0; i < containingDeclarationsNames.size() - 2; i++) {
            declarations.append(containingDeclarationsNames.get(i));
            declarations.append(", ");
        }
        declarations.append(containingDeclarationsNames.get(containingDeclarationsNames.size() - 2));
        declarations.append(" and ");
        declarations.append(containingDeclarationsNames.get(containingDeclarationsNames.size() - 1));
        return JetBundle.message("make.element.in.classifiers.open", element.getName(), declarations.toString());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.modifier.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        for (PsiElement overriddenMember : overriddenMembers) {
            overriddenMember.replace(AddModifierFix.addModifierWithDefaultReplacement(overriddenMember, OPEN_KEYWORD, project, false));
        }
    }

    @NotNull
    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetDeclaration declaration = QuickFixUtil.getParentElementOfType(diagnostic, JetDeclaration.class);
                assert declaration != null;
                return new MakeOverriddenMemberOpenFix(declaration);
            }
        };
    }
}

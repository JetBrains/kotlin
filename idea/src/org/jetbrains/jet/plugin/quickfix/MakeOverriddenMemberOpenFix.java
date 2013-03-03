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

import java.util.*;

import static org.jetbrains.jet.lexer.JetTokens.OPEN_KEYWORD;

public class MakeOverriddenMemberOpenFix extends JetIntentionAction<JetDeclaration> {
    private final Set<PsiElement> overriddenMembers;
    private final Set<String> overriddenMembersNames;

    public MakeOverriddenMemberOpenFix(@NotNull JetDeclaration declaration) {
        super(declaration);
        overriddenMembers = new HashSet<PsiElement>();
        overriddenMembersNames = new TreeSet<String>();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file) || !(file instanceof JetFile)) {
            return false;
        }

        ResolveSession resolveSession = WholeProjectAnalyzerFacade.getLazyResolveSessionForFile((JetFile) element.getContainingFile());
        DeclarationDescriptor descriptor = resolveSession.resolveToDescriptor(element);
        if (!(descriptor instanceof CallableMemberDescriptor)) return false;
        CallableMemberDescriptor callableMemberDescriptor = (CallableMemberDescriptor) descriptor;
        for (CallableMemberDescriptor overriddenDescriptor : callableMemberDescriptor.getOverriddenDescriptors()) {
            if (!overriddenDescriptor.getModality().isOverridable()) {
                PsiElement overriddenMember = BindingContextUtils.descriptorToDeclaration(resolveSession.getBindingContext(), overriddenDescriptor);
                if (overriddenMember == null || !overriddenMember.isWritable()) {
                    return false;
                }
                String containingDeclarationName = overriddenDescriptor.getContainingDeclaration().getName().getName();
                overriddenMembers.add(overriddenMember);
                overriddenMembersNames.add(containingDeclarationName);
            }
        }
        return overriddenMembers.size() > 0;
    }

    @NotNull
    @Override
    public String getText() {
        if (overriddenMembers.size() == 1) {
            String typeName = overriddenMembersNames.iterator().next();
            return JetBundle.message("make.element.modifier", typeName + "." + element.getName(), OPEN_KEYWORD);
        }

        StringBuilder types = new StringBuilder();
        for(Iterator<String> iterator = overriddenMembersNames.iterator(); iterator.hasNext(); ) {
            String typeName = iterator.next();
            if (!iterator.hasNext()) {
                types.deleteCharAt(types.length() - 2);
                types.append("and ");
            }
            types.append(typeName);
            if (iterator.hasNext()) {
                types.append(", ");
            }
        }

        return JetBundle.message("make.overridden.members.open", element.getName(), types.toString());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.modifier.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        for(PsiElement overriddenMember: overriddenMembers) {
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

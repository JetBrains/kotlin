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

import com.google.common.collect.Sets;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies;

import java.util.*;

import static org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.*;
import static org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils.descriptorToDeclaration;
import static org.jetbrains.jet.lexer.JetTokens.OPEN_KEYWORD;

public class MakeOverriddenMemberOpenFix extends JetIntentionAction<JetDeclaration> {
    private final List<PsiElement> overriddenNonOverridableMembers = new ArrayList<PsiElement>();
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
        overriddenNonOverridableMembers.clear();
        containingDeclarationsNames.clear();

        ResolveSessionForBodies resolveSession = ResolvePackage.getLazyResolveSession((JetFile) file);
        DeclarationDescriptor descriptor = resolveSession.resolveToDescriptor(element);
        if (!(descriptor instanceof CallableMemberDescriptor)) return false;

        for (CallableMemberDescriptor overriddenDescriptor : getAllDeclaredNonOverridableOverriddenDescriptors(
                (CallableMemberDescriptor) descriptor)) {
            assert overriddenDescriptor.getKind() == DECLARATION : "Can only be applied to declarations.";
            PsiElement overriddenMember = descriptorToDeclaration(overriddenDescriptor);
            if (overriddenMember == null || !QuickFixUtil.canModifyElement(overriddenMember)) {
                return false;
            }
            String containingDeclarationName = overriddenDescriptor.getContainingDeclaration().getName().asString();
            overriddenNonOverridableMembers.add(overriddenMember);
            containingDeclarationsNames.add(containingDeclarationName);
        }
        return overriddenNonOverridableMembers.size() > 0;
    }

    @NotNull
    private static Collection<CallableMemberDescriptor> getAllDeclaredNonOverridableOverriddenDescriptors(
            @NotNull CallableMemberDescriptor callableMemberDescriptor
    ) {
        Set<CallableMemberDescriptor> result = Sets.newHashSet();
        Collection<CallableMemberDescriptor>
                nonOverridableOverriddenDescriptors = retainNonOverridableMembers(callableMemberDescriptor.getOverriddenDescriptors());
        for (CallableMemberDescriptor overriddenDescriptor : nonOverridableOverriddenDescriptors) {
            CallableMemberDescriptor.Kind kind = overriddenDescriptor.getKind();
            if (kind == DECLARATION) {
                result.add(overriddenDescriptor);
            }
            else if (kind == FAKE_OVERRIDE || kind == DELEGATION) {
                result.addAll(getAllDeclaredNonOverridableOverriddenDescriptors(overriddenDescriptor));
            }
            else if (kind == SYNTHESIZED) {
                // do nothing, final synthesized members can't be made open
            }
            else {
                throw new UnsupportedOperationException("Unexpected callable kind " + kind);
            }
        }
        return result;
    }

    @NotNull
    private static Collection<CallableMemberDescriptor> retainNonOverridableMembers(
            @NotNull Collection<? extends CallableMemberDescriptor> callableMemberDescriptors
    ) {
        return ContainerUtil.filter(callableMemberDescriptors, new Condition<CallableMemberDescriptor>() {
            @Override
            public boolean value(CallableMemberDescriptor descriptor) {
                return !descriptor.getModality().isOverridable();
            }
        });
    }

    @NotNull
    @Override
    public String getText() {
        if (overriddenNonOverridableMembers.size() == 1) {
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
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        for (PsiElement overriddenMember : overriddenNonOverridableMembers) {
            overriddenMember.replace(AddModifierFix.addModifierWithDefaultReplacement(overriddenMember, OPEN_KEYWORD, project, false));
        }
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
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

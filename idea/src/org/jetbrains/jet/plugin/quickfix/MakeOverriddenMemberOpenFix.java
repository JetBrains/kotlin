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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;

import static org.jetbrains.jet.lexer.JetTokens.FINAL_KEYWORD;
import static org.jetbrains.jet.lexer.JetTokens.OPEN_KEYWORD;

public class MakeOverriddenMemberOpenFix extends JetIntentionAction<JetDeclaration> {
    private PsiElement overriddenMember;

    public MakeOverriddenMemberOpenFix(@NotNull JetDeclaration declaration) {
        super(declaration);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }

        ResolveSession resolveSession = WholeProjectAnalyzerFacade.getLazyResolveSessionForFile((JetFile) element.getContainingFile());
        DeclarationDescriptor descriptor = resolveSession.resolveToDescriptor(element);
        if (!(descriptor instanceof CallableMemberDescriptor)) return false;
        CallableMemberDescriptor callableMemberDescriptor = (CallableMemberDescriptor) descriptor;
        for (CallableMemberDescriptor overriddenDescriptor : callableMemberDescriptor.getOverriddenDescriptors()) {
            if (overriddenDescriptor.getModality() == Modality.FINAL) {
                overriddenMember = BindingContextUtils.descriptorToDeclaration(resolveSession.getBindingContext(), overriddenDescriptor);
                break;
            }
        }
        return overriddenMember != null && overriddenMember.isWritable();
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("make.overridden.member.open");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("make.overridden.member.open.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        overriddenMember.replace(AddModifierFix.addModifierWithDefaultReplacement(overriddenMember, OPEN_KEYWORD, project, false));
    }

    @NotNull
    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetDeclaration declaration = QuickFixUtil.getParentElementOfType(diagnostic, JetDeclaration.class);
                return declaration == null ? null : new MakeOverriddenMemberOpenFix(declaration);
            }
        };
    }
}


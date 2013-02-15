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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager;

import static org.jetbrains.jet.lexer.JetTokens.FINAL_KEYWORD;
import static org.jetbrains.jet.lexer.JetTokens.OPEN_KEYWORD;

public class FinalSupertypeFix extends JetIntentionAction<JetClass> {
    private final JetClass childClass;
    private JetClass superClass;

    public FinalSupertypeFix(@NotNull JetClass childClass) {
        super(childClass);
        this.childClass = childClass;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }

        BindingContext context = KotlinCacheManager.getInstance(project).getDeclarationsFromProject().getBindingContext();
        ClassDescriptor childClassDescriptor = context.get(BindingContext.CLASS, childClass);
        if (childClassDescriptor == null) {
            return false;
        }
        for (JetType supertype: childClassDescriptor.getTypeConstructor().getSupertypes()) {
            ClassDescriptor superClassDescriptor = (ClassDescriptor) supertype.getConstructor().getDeclarationDescriptor();
            if (superClassDescriptor == null) {
                continue;
            }
            PsiElement declaration = BindingContextUtils.descriptorToDeclaration(context, superClassDescriptor);
            if (declaration instanceof JetClass) {
                superClass = (JetClass) declaration;
                if (!superClass.isTrait() && !superClass.isEnum() && superClass.getContainingFile().isWritable()) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("add.supertype.modifier", "open");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.supertype.modifier.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        assert superClass != null;
        JetToken[] modifiersThanCanBeReplaced = new JetKeywordToken[] { FINAL_KEYWORD };
        superClass.replace(AddModifierFix.addModifier(superClass, OPEN_KEYWORD, modifiersThanCanBeReplaced, project, false));
    }

    @NotNull
    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetClass childClass = QuickFixUtil.getParentElementOfType(diagnostic, JetClass.class);
                return childClass == null ? null : new FinalSupertypeFix(childClass);
            }
        };
    }
}


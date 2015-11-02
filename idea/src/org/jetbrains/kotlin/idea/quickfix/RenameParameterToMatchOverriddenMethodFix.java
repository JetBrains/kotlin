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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.resolve.BindingContext;

public class RenameParameterToMatchOverriddenMethodFix extends KotlinQuickFixAction<KtParameter>{
    private final KtParameter parameter;
    private String parameterFromSuperclassName;

    public RenameParameterToMatchOverriddenMethodFix(@NotNull KtParameter parameter) {
        super(parameter);
        this.parameter = parameter;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }

        BindingContext context = ResolutionUtils.analyze(parameter);
        VariableDescriptor parameterDescriptor = context.get(BindingContext.VALUE_PARAMETER, parameter);
        if (parameterDescriptor == null) {
            return false;
        }
        for (CallableDescriptor parameterFromSuperclass : parameterDescriptor.getOverriddenDescriptors()) {
            if (parameterFromSuperclassName == null) {
                parameterFromSuperclassName = parameterFromSuperclass.getName().asString();
            }
            else if (!parameterFromSuperclassName.equals(parameterFromSuperclass.getName().asString())) {
                return false;
            }
        }

        return parameterFromSuperclassName != null;
    }

    @NotNull
    @Override
    public String getText() {
        return KotlinBundle.message("rename.parameter.to.match.overridden.method");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return KotlinBundle.message("rename.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        new RenameProcessor(project, parameter, parameterFromSuperclassName, false, false).run();
    }

    @NotNull
    public static KotlinSingleIntentionActionFactory createFactory() {
        return new KotlinSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                KtParameter parameter = QuickFixUtil.getParentElementOfType(diagnostic, KtParameter.class);
                return parameter == null ? null : new RenameParameterToMatchOverriddenMethodFix(parameter);
            }
        };
    }
}

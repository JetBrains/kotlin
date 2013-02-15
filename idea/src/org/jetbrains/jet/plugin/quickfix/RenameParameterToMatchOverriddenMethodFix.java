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
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager;

public class RenameParameterToMatchOverriddenMethodFix extends JetIntentionAction<JetParameter>{
    private final JetParameter parameter;
    private String parameterFromSuperclassName;

    public RenameParameterToMatchOverriddenMethodFix(@NotNull JetParameter parameter) {
        super(parameter);
        this.parameter = parameter;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }

        BindingContext context = KotlinCacheManager.getInstance(project).getDeclarationsFromProject().getBindingContext();
        VariableDescriptor parameterDescriptor = context.get(BindingContext.VALUE_PARAMETER, parameter);
        if (parameterDescriptor == null) {
            return false;
        }
        for (CallableDescriptor parameterFromSuperclass : parameterDescriptor.getOverriddenDescriptors()) {
            if (parameterFromSuperclassName == null) {
                parameterFromSuperclassName = parameterFromSuperclass.getName().getName();
            }
            else if (!parameterFromSuperclassName.equals(parameterFromSuperclass.getName().getName())) {
                return false;
            }
        }

        return parameterFromSuperclassName != null;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("rename.parameter.to.match.overridden.method");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("rename.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        new RenameProcessor(project, parameter, parameterFromSuperclassName, false, false).run();
    }

    @NotNull
    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetParameter parameter = QuickFixUtil.getParentElementOfType(diagnostic, JetParameter.class);
                return parameter == null ? null : new RenameParameterToMatchOverriddenMethodFix(parameter);
            }
        };
    }
}

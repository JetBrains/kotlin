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
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager;
import org.jetbrains.jet.plugin.intentions.SpecifyTypeExplicitlyAction;

public class ChangeReturnTypeToMatchOverriddenMethodFix extends JetIntentionAction<JetFunction>{
    private final JetFunction function;
    private JetType matchingReturnType;

    public ChangeReturnTypeToMatchOverriddenMethodFix(@NotNull JetFunction function) {
        super(function);
        this.function = function;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }

        BindingContext context = KotlinCacheManager.getInstance(project).getDeclarationsFromProject(project).getBindingContext();
        FunctionDescriptor functionDescriptor = context.get(BindingContext.FUNCTION, function);
        if (functionDescriptor == null) {
            return false;
        }

        for (FunctionDescriptor overriddenFunctionDescriptor: functionDescriptor.getOverriddenDescriptors()) {
            JetType overriddenFunctionReturnType = overriddenFunctionDescriptor.getReturnType();
            if (overriddenFunctionReturnType == null) {
                return false;
            }
            if (matchingReturnType == null || JetTypeChecker.INSTANCE.isSubtypeOf(overriddenFunctionReturnType, matchingReturnType)) {
                matchingReturnType = overriddenFunctionReturnType;
            }
            else if (!JetTypeChecker.INSTANCE.isSubtypeOf(matchingReturnType, overriddenFunctionReturnType)) {
                return false;
            }
        }
        return matchingReturnType != null;
    }

    @NotNull
    @Override
    public String getText() {
        if (KotlinBuiltIns.getInstance().isUnit(matchingReturnType)) {
            return JetBundle.message("remove.return.type", matchingReturnType);
        }
        else {
            return JetBundle.message("change.return.type.to.match.overridden.method", matchingReturnType);
        }
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.return.type.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        SpecifyTypeExplicitlyAction.removeTypeAnnotation(function);
        if (!KotlinBuiltIns.getInstance().isUnit(matchingReturnType)) {
            SpecifyTypeExplicitlyAction.addTypeAnnotation(project, function, matchingReturnType);
        }
    }

    @NotNull
    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetFunction parameter = QuickFixUtil.getParentElementOfType(diagnostic, JetFunction.class);
                return parameter == null ? null : new ChangeReturnTypeToMatchOverriddenMethodFix(parameter);
            }
        };
    }
}

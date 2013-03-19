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
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManagerUtil;
import org.jetbrains.jet.plugin.intentions.SpecifyTypeExplicitlyAction;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

public class ChangeVariableTypeFix extends JetIntentionAction<JetVariableDeclaration> {
    private final JetType type;

    public ChangeVariableTypeFix(@NotNull JetVariableDeclaration element, @NotNull JetType type) {
        super(element);
        this.type = type;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("change.element.type", element.getName(), type);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.type.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        SpecifyTypeExplicitlyAction.removeTypeAnnotation(element);
        PsiElement nameIdentifier = element.getNameIdentifier();
        assert nameIdentifier != null : "ChangeVariableTypeFix applied to variable without name";
        Pair<PsiElement, PsiElement> typeWhiteSpaceAndColon = JetPsiFactory.createTypeWhiteSpaceAndColon(project, type.toString());
        element.addRangeAfter(typeWhiteSpaceAndColon.first, typeWhiteSpaceAndColon.second, nameIdentifier);
    }

    @NotNull
    public static JetIntentionActionFactory createFactoryForComponentFunctionReturnTypeMismatch() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetMultiDeclarationEntry entry = ChangeFunctionReturnTypeFix.getMultiDeclarationEntryThatTypeMismatchComponentFunction(diagnostic);
                BindingContext context = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) entry.getContainingFile()).getBindingContext();
                ResolvedCall<FunctionDescriptor> resolvedCall = context.get(BindingContext.COMPONENT_RESOLVED_CALL, entry);
                if (resolvedCall == null) return null;
                JetFunction componentFunction = (JetFunction) BindingContextUtils.descriptorToDeclaration(context, resolvedCall.getCandidateDescriptor());
                if (componentFunction == null) return null;
                JetType expectedType = resolvedCall.getCandidateDescriptor().getReturnType();
                return expectedType == null ? null : new ChangeVariableTypeFix(entry, expectedType);
            }
        };
    }

    @NotNull
    public static JetIntentionActionFactory createFactoryForPropertyTypeMismatchOnOverride() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetProperty property = QuickFixUtil.getParentElementOfType(diagnostic, JetProperty.class);
                assert property != null : "PROPERTY_TYPE_MISMATCH_ON_OVERRIDE reported on element that is not within any property";
                BindingContext context = KotlinCacheManagerUtil.getDeclarationsBindingContext(property);
                JetType type = QuickFixUtil.findLowerBoundOfOverriddenCallablesReturnTypes(context, property);
                return type == null ? null : new ChangeVariableTypeFix(property, type);
            }
        };
    }
}

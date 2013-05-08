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
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.NameUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.renderer.DescriptorRenderer;

public class ChangeFunctionParameterTypeFix extends JetIntentionAction<JetParameter> {
    private final String renderedType;
    private final String containingFunctionName;

    public ChangeFunctionParameterTypeFix(@NotNull JetParameter element, @NotNull JetType type) {
        super(element);
        renderedType = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(type);
        JetFunction function = PsiTreeUtil.getParentOfType(element, JetFunction.class);
        FqName functionFQName = JetPsiUtil.getFQName(function);
        containingFunctionName = functionFQName == null ? null : functionFQName.getFqName();
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) && containingFunctionName != null;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("change.function.parameter.type", element.getName(), containingFunctionName, renderedType);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.type.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        JetTypeReference typeReference = element.getTypeReference();
        assert typeReference != null : "Parameter without type annotation cannot cause type mismatch";
        typeReference.replace(JetPsiFactory.createType(project, renderedType));
    }

    @NotNull
    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                // Change type of function parameter in case TYPE_MISMATCH is reported on expression passed as value argument of call
                JetParameter correspondingParameter = null;
                JetType type = null;

                BindingContext context = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) diagnostic.getPsiFile()).getBindingContext();
                JetFunctionLiteralExpression functionLiteralExpression =
                        QuickFixUtil.getParentElementOfType(diagnostic, JetFunctionLiteralExpression.class);

                if (functionLiteralExpression != null && diagnostic.getPsiElement() == functionLiteralExpression.getBodyExpression()) {
                    correspondingParameter =
                        QuickFixUtil.getFunctionParameterCorrespondingToFunctionLiteralPassedOutsideArgumentList(functionLiteralExpression);
                    type = context.get(BindingContext.EXPRESSION_TYPE, functionLiteralExpression);
                }
                else {
                    JetValueArgument valueArgument = QuickFixUtil.getParentElementOfType(diagnostic, JetValueArgument.class);
                    if (valueArgument != null && valueArgument.getArgumentExpression() == diagnostic.getPsiElement()) {
                        correspondingParameter = QuickFixUtil.getFunctionParameterCorrespondingToValueArgumentPassedInCall(valueArgument);
                        type = context.get(BindingContext.EXPRESSION_TYPE, valueArgument.getArgumentExpression());
                    }
                }
                if (correspondingParameter != null && type != null) {
                    return new ChangeFunctionParameterTypeFix(correspondingParameter, type);
                }
                return null;
            }
        };
    }
}

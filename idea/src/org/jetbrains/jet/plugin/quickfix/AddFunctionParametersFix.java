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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.refactoring.JetNameValidator;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetChangeSignatureDialog;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetFunctionPlatformDescriptorImpl;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetParameterInfo;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.List;

public class AddFunctionParametersFix extends ChangeFunctionSignatureFix {
    private final JetCallElement callElement;
    private final boolean hasTypeMismatches;

    public AddFunctionParametersFix(
            @NotNull PsiElement declaration,
            @NotNull JetCallElement callElement,
            @NotNull FunctionDescriptor functionDescriptor,
            boolean hasTypeMismatches
    ) {
        super(declaration, callElement, functionDescriptor);
        this.callElement = callElement;
        this.hasTypeMismatches = hasTypeMismatches;
    }

    @NotNull
    @Override
    public String getText() {
        List<ValueParameterDescriptor> parameters = functionDescriptor.getValueParameters();
        List<? extends ValueArgument> arguments = callElement.getValueArguments();
        int newParametersCnt = arguments.size() - parameters.size();
        assert newParametersCnt > 0;
        String subjectSuffix = newParametersCnt > 1 ? "s" : "";

        if (functionDescriptor instanceof ConstructorDescriptor) {
            String className = functionDescriptor.getContainingDeclaration().getName().getName();

            if (hasTypeMismatches)
                return JetBundle.message("change.constructor.signature", className);
            else
                return JetBundle.message("add.parameters.to.constructor", subjectSuffix, className);
        }
        else {
            String functionName = functionDescriptor.getName().getName();

            if (hasTypeMismatches)
                return JetBundle.message("change.function.signature", functionName);
            else
                return JetBundle.message("add.parameters.to.function", subjectSuffix, functionName);
        }
    }

    @Override
    protected void invoke(@NotNull Project project, Editor editor, JetFile file) {
        BindingContext bindingContext = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) callElement.getContainingFile()).getBindingContext();
        JetFunctionPlatformDescriptorImpl platformDescriptor = new JetFunctionPlatformDescriptorImpl(functionDescriptor, element);
        final List<ValueParameterDescriptor> parameters = functionDescriptor.getValueParameters();
        List<? extends ValueArgument> arguments = callElement.getValueArguments();
        JetNameValidator validator = JetNameValidator.getCollectingValidator(callElement.getProject());

        for (int i = 0; i < arguments.size(); i ++) {
            ValueArgument argument = arguments.get(i);
            JetExpression expression = argument.getArgumentExpression();

            if (i < parameters.size()) {
                validator.validateName(parameters.get(i).getName().getName());
                JetType argumentType = expression != null ? bindingContext.get(BindingContext.EXPRESSION_TYPE, expression) : null;
                JetType parameterType = parameters.get(i).getType();

                if (argumentType != null && !JetTypeChecker.INSTANCE.isSubtypeOf(argumentType, parameterType))
                    platformDescriptor.getParameters().get(i).setTypeText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(argumentType));
            }
            else {
                JetParameterInfo parameterInfo = getNewParameterInfo(bindingContext, argument, validator);

                if (expression != null)
                    parameterInfo.setDefaultValueText(expression.getText());

                platformDescriptor.addParameter(parameterInfo);
            }
        }

        JetChangeSignatureDialog dialog = new JetChangeSignatureDialog(project, platformDescriptor, callElement, getText()) {
            @Override
            protected int getSelectedIdx() {
                return parameters.size();
            }
        };

        if (ApplicationManager.getApplication().isUnitTestMode() ||
            !hasTypeMismatches && !(functionDescriptor instanceof ConstructorDescriptor) && !hasOtherUsages())
            performRefactoringSilently(dialog);
        else
            dialog.show();
    }

    private boolean hasOtherUsages() {
        for (PsiReference reference : ReferencesSearch.search(element)) {
            PsiElement referenceElement = reference.getElement();

            if (referenceElement != null && referenceElement.getParent() instanceof JetReferenceExpression &&
                !callElement.equals(referenceElement.getParent().getParent()))
                return true;
        }

        return false;
    }
}

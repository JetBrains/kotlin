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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters1;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters2;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester;
import org.jetbrains.jet.plugin.refactoring.JetNameValidator;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetParameterInfo;

import java.util.List;

import static org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED;

public abstract class ChangeFunctionSignatureFix extends JetIntentionAction<PsiElement> {
    protected final PsiElement context;
    protected final FunctionDescriptor functionDescriptor;

    public ChangeFunctionSignatureFix(
            @NotNull PsiElement context,
            @NotNull FunctionDescriptor functionDescriptor
    ) {
        super(context);
        this.context = context;
        this.functionDescriptor = functionDescriptor;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.signature.family");
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }

        BindingContext bindingContext = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) file).getBindingContext();
        List<PsiElement> declarations = BindingContextUtils.callableDescriptorToDeclarations(bindingContext, functionDescriptor);
        if (declarations.isEmpty()) {
            return false;
        }

        for (PsiElement declaration : declarations) {
            if (!QuickFixUtil.canModifyElement(declaration)) {
                return false;
            }
        }

        return true;
    }

    protected static String getNewArgumentName(ValueArgument argument, JetNameValidator validator) {
        JetValueArgumentName argumentName = argument.getArgumentName();
        JetExpression expression = argument.getArgumentExpression();

        if (argumentName != null) {
            return validator.validateName(argumentName.getName());
        }
        else if (expression != null) {
            return JetNameSuggester.suggestNames(expression, validator, "param")[0];
        }

        return validator.validateName("param");
    }

    protected static JetParameterInfo getNewParameterInfo(
            BindingContext bindingContext,
            ValueArgument argument,
            JetNameValidator validator
    ) {
        String name = getNewArgumentName(argument, validator);
        JetExpression expression = argument.getArgumentExpression();
        JetType type = expression != null ? bindingContext.get(BindingContext.EXPRESSION_TYPE, expression) : null;
        type = type != null ? type : KotlinBuiltIns.getInstance().getNullableAnyType();
        return new JetParameterInfo(name, type);
    }

    private static boolean hasTypeMismatches(
            List<ValueParameterDescriptor> parameters,
            List<? extends ValueArgument> arguments,
            BindingContext bindingContext
    ) {
        for (int i = 0; i < parameters.size(); i++) {
            assert i < arguments .size(); // number of parameters must not be greater than the number of arguments (it's called only for TOO_MANY_ARGUMENTS error)
            JetExpression argumentExpression = arguments.get(i).getArgumentExpression();
            JetType argumentType =
                    argumentExpression != null ? bindingContext.get(BindingContext.EXPRESSION_TYPE, argumentExpression) : null;
            JetType parameterType = parameters.get(i).getType();

            if (argumentType == null || !JetTypeChecker.INSTANCE.isSubtypeOf(argumentType, parameterType)) {
                return true;
            }
        }

        return false;
    }

    public static JetSingleIntentionActionFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public ChangeFunctionSignatureFix createAction(Diagnostic diagnostic) {
                JetCallElement callElement = PsiTreeUtil.getParentOfType(diagnostic.getPsiElement(), JetCallElement.class);
                @SuppressWarnings("unchecked")
                CallableDescriptor descriptor = ((DiagnosticWithParameters1<PsiElement, CallableDescriptor>) diagnostic).getA();

                if (callElement != null) {
                    return createFix(callElement, callElement, descriptor);
                }

                return null;
            }
        };
    }

    public static JetSingleIntentionActionFactory createFactoryForParametersNumberMismatch() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public ChangeFunctionSignatureFix createAction(Diagnostic diagnostic) {
                @SuppressWarnings("unchecked")
                DiagnosticWithParameters2<JetFunctionLiteral, Integer, List<JetType>> diagnosticWithParameters =
                        (DiagnosticWithParameters2<JetFunctionLiteral, Integer, List<JetType>>) diagnostic;
                JetFunctionLiteral functionLiteral = diagnosticWithParameters.getPsiElement();
                BindingContext bindingContext =
                        AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) functionLiteral.getContainingFile()).getBindingContext();
                DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, functionLiteral);

                if (descriptor instanceof FunctionDescriptor) {
                    return new ChangeFunctionLiteralSignatureFix(functionLiteral, (FunctionDescriptor) descriptor,
                                                                 diagnosticWithParameters.getB());
                }
                else {
                    return null;
                }
            }
        };
    }

    public static JetSingleIntentionActionFactory createFactoryForUnusedParameter() {
        return new JetSingleIntentionActionFactory() {
            @Override
            public ChangeFunctionSignatureFix createAction(Diagnostic diagnostic) {
                @SuppressWarnings("unchecked")
                Object descriptor = ((DiagnosticWithParameters1<PsiNameIdentifierOwner, Object>) diagnostic).getA();

                if (descriptor instanceof ValueParameterDescriptor) {
                    return createFix(null, diagnostic.getPsiElement(), (CallableDescriptor) descriptor);
                }
                else {
                    return null;
                }
            }
        };
    }

    @Nullable
    private static ChangeFunctionSignatureFix createFix(JetCallElement callElement, PsiElement context, CallableDescriptor descriptor) {
        FunctionDescriptor functionDescriptor = null;

        if (descriptor instanceof FunctionDescriptor) {
            functionDescriptor = (FunctionDescriptor) descriptor;
        }
        else if (descriptor instanceof ValueParameterDescriptor) {
            DeclarationDescriptor containingDescriptor = descriptor.getContainingDeclaration();

            if (containingDescriptor instanceof FunctionDescriptor) {
                functionDescriptor = (FunctionDescriptor) containingDescriptor;
            }
        }

        if (functionDescriptor == null) {
            return null;
        }

        if (functionDescriptor.getKind() == SYNTHESIZED) {
            return null;
        }

        BindingContext bindingContext =
                AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) context.getContainingFile()).getBindingContext();
        if (descriptor instanceof ValueParameterDescriptor) {
            return new RemoveFunctionParametersFix(context, functionDescriptor, (ValueParameterDescriptor) descriptor);
        }
        else {
            List<ValueParameterDescriptor> parameters = functionDescriptor.getValueParameters();
            List<? extends ValueArgument> arguments = callElement.getValueArguments();

            if (arguments.size() > parameters.size()) {
                boolean hasTypeMismatches = hasTypeMismatches(parameters, arguments, bindingContext);
                return new AddFunctionParametersFix(callElement, functionDescriptor, hasTypeMismatches);
            }
        }

        return null;
    }
}

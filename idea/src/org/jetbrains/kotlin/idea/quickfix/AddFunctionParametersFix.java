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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.core.CollectingNameValidator;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.psi.JetCallElement;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.idea.refactoring.changeSignature.ChangeSignaturePackage.runChangeSignature;

public class AddFunctionParametersFix extends ChangeFunctionSignatureFix {
    private final JetCallElement callElement;
    private final boolean hasTypeMismatches;
    private final List<JetType> typesToShorten = new ArrayList<JetType>();

    public AddFunctionParametersFix(
            @NotNull JetCallElement callElement,
            @NotNull FunctionDescriptor functionDescriptor,
            boolean hasTypeMismatches
    ) {
        super(callElement, functionDescriptor);
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

        if (isConstructor()) {
            String className = functionDescriptor.getContainingDeclaration().getName().asString();

            if (hasTypeMismatches)
                return JetBundle.message("change.constructor.signature", className);
            else
                return JetBundle.message("add.parameters.to.constructor", subjectSuffix, className);
        }
        else {
            String functionName = functionDescriptor.getName().asString();

            if (hasTypeMismatches)
                return JetBundle.message("change.function.signature", functionName);
            else
                return JetBundle.message("add.parameters.to.function", subjectSuffix, functionName);
        }
    }

    @Override
    public boolean isAvailable(
            @NotNull Project project, Editor editor, PsiFile file
    ) {
        if (!super.isAvailable(project, editor, file)) {
            return false;
        }

        int newParametersCnt = callElement.getValueArguments().size() - functionDescriptor.getValueParameters().size();
        if (newParametersCnt <= 0) {
            // psi for this quickfix is no longer valid
            return false;
        }
        return true;
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    @Override
    protected void invoke(@NotNull Project project, Editor editor, JetFile file) {
        BindingContext bindingContext = ResolvePackage.analyzeFully((JetFile) callElement.getContainingFile());
        runChangeSignature(project, functionDescriptor, addParameterConfiguration(), bindingContext, callElement, getText());
    }

    private JetChangeSignatureConfiguration addParameterConfiguration() {
        return new JetChangeSignatureConfiguration() {
            @NotNull
            @Override
            public JetMethodDescriptor configure(@NotNull final JetMethodDescriptor originalDescriptor, @NotNull final BindingContext bindingContext) {
                return ChangeSignaturePackage.modify(
                        originalDescriptor,
                        new Function1<JetMutableMethodDescriptor, Unit>() {
                            @Override
                            public Unit invoke(JetMutableMethodDescriptor descriptor) {
                                List<ValueParameterDescriptor> parameters = functionDescriptor.getValueParameters();
                                List<? extends ValueArgument> arguments = callElement.getValueArguments();
                                CollectingNameValidator validator = new CollectingNameValidator();

                                for (int i = 0; i < arguments.size(); i ++) {
                                    ValueArgument argument = arguments.get(i);
                                    JetExpression expression = argument.getArgumentExpression();

                                    if (i < parameters.size()) {
                                        validator.addName(parameters.get(i).getName().asString());
                                        JetType argumentType = expression != null ? bindingContext.getType(expression) : null;
                                        JetType parameterType = parameters.get(i).getType();

                                        if (argumentType != null && !JetTypeChecker.DEFAULT.isSubtypeOf(argumentType, parameterType)) {
                                            descriptor.getParameters().get(i).setCurrentTypeText(IdeDescriptorRenderers.SOURCE_CODE.renderType(argumentType));
                                            typesToShorten.add(argumentType);
                                        }
                                    }
                                    else {
                                        JetParameterInfo parameterInfo =
                                                getNewParameterInfo((FunctionDescriptor) originalDescriptor.getBaseDescriptor(), bindingContext, argument, validator);
                                        typesToShorten.add(parameterInfo.getOriginalType());

                                        if (expression != null) {
                                            parameterInfo.setDefaultValueForCall(expression);
                                        }

                                        descriptor.addParameter(parameterInfo);
                                    }
                                }
                                return null;
                            }
                        }
                );
            }

            @Override
            public boolean performSilently(@NotNull Collection<? extends PsiElement> affectedFunctions) {
                if (affectedFunctions.size() != 1) {
                    return false;
                }
                PsiElement onlyFunction = affectedFunctions.iterator().next();
                return !hasTypeMismatches && !isConstructor() && !hasOtherUsages(onlyFunction);
            }

            @Override
            public boolean forcePerformForSelectedFunctionOnly() {
                return false;
            }
        };
    }

    private boolean hasOtherUsages(@NotNull PsiElement function) {
        for (PsiReference reference : ReferencesSearch.search(function)) {
            JetCallElement call = PsiTreeUtil.getParentOfType(reference.getElement(), JetCallElement.class);
            if (call != null && !callElement.equals(call)) {
                return true;
            }
        }

        return false;
    }

    private boolean isConstructor() {
        return functionDescriptor instanceof ConstructorDescriptor;
    }
}

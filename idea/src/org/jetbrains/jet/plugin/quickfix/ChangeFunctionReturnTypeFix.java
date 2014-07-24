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
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.jet.lang.resolve.bindingContextUtil.BindingContextUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.intentions.SpecifyTypeExplicitlyAction;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.LinkedList;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH;
import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class ChangeFunctionReturnTypeFix extends JetIntentionAction<JetFunction> {
    private final JetType type;
    private final String renderedType;
    private final ChangeFunctionLiteralReturnTypeFix changeFunctionLiteralReturnTypeFix;

    public ChangeFunctionReturnTypeFix(@NotNull JetFunction element, @NotNull JetType type) {
        super(element);
        this.type = type;
        renderedType = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(type);
        if (element instanceof JetFunctionLiteral) {
            JetFunctionLiteralExpression functionLiteralExpression = PsiTreeUtil.getParentOfType(element, JetFunctionLiteralExpression.class);
            assert functionLiteralExpression != null : "FunctionLiteral outside any FunctionLiteralExpression";
            changeFunctionLiteralReturnTypeFix = new ChangeFunctionLiteralReturnTypeFix(functionLiteralExpression, type);
        }
        else {
            changeFunctionLiteralReturnTypeFix = null;
        }
    }

    @NotNull
    @Override
    public String getText() {
        if (changeFunctionLiteralReturnTypeFix != null) {
            return changeFunctionLiteralReturnTypeFix.getText();
        }

        String functionName = element.getName();
        FqName fqName = element.getFqName();
        if (fqName != null) functionName = fqName.asString();

        if (KotlinBuiltIns.getInstance().isUnit(type) && element.hasBlockBody()) {
            return functionName == null ?
                   JetBundle.message("remove.no.name.function.return.type") :
                   JetBundle.message("remove.function.return.type", functionName);
        }
        return functionName == null ?
               JetBundle.message("change.no.name.function.return.type", renderedType) :
               JetBundle.message("change.function.return.type", functionName, renderedType);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.type.family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @Nullable Editor editor, @Nullable PsiFile file) {
        return super.isAvailable(project, editor, file) && !ErrorUtils.containsErrorType(type);
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @Nullable JetFile file) throws IncorrectOperationException {
        if (changeFunctionLiteralReturnTypeFix != null) {
            changeFunctionLiteralReturnTypeFix.invoke(project, editor, file);
        }
        else {
            SpecifyTypeExplicitlyAction.removeTypeAnnotation(element);
            if (!(KotlinBuiltIns.getInstance().isUnit(type) && element.hasBlockBody())) {
                addReturnTypeAnnotation(element, renderedType);
            }
        }
    }

    public static void addReturnTypeAnnotation(JetFunction function, String typeText) {
        PsiElement elementToPrecedeType = function.getValueParameterList();
        if (elementToPrecedeType == null) elementToPrecedeType = function.getNameIdentifier();
        assert elementToPrecedeType != null : "Return type of function without name can't mismatch anything";
        if (elementToPrecedeType.getNextSibling() instanceof PsiErrorElement) {
            // if a function doesn't have a value parameter list, a syntax error is raised, and it should follow the function name
            elementToPrecedeType = elementToPrecedeType.getNextSibling();
        }
        JetPsiFactory psiFactory = JetPsiFactory(function);
        function.addAfter(psiFactory.createType(typeText), elementToPrecedeType);
        function.addAfter(psiFactory.createColon(), elementToPrecedeType);
    }

    @NotNull
    public static JetMultiDeclarationEntry getMultiDeclarationEntryThatTypeMismatchComponentFunction(Diagnostic diagnostic) {
        String componentName = COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.cast(diagnostic).getA().asString();
        int componentIndex = Integer.valueOf(componentName.substring(DescriptorResolver.COMPONENT_FUNCTION_NAME_PREFIX.length()));
        JetMultiDeclaration multiDeclaration = QuickFixUtil.getParentElementOfType(diagnostic, JetMultiDeclaration.class);
        assert multiDeclaration != null : "COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH reported on expression that is not within any multi declaration";
        return multiDeclaration.getEntries().get(componentIndex - 1);
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactoryForComponentFunctionReturnTypeMismatch() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetMultiDeclarationEntry entry = getMultiDeclarationEntryThatTypeMismatchComponentFunction(diagnostic);
                BindingContext context = ResolvePackage.getBindingContext((JetFile) entry.getContainingFile().getContainingFile());
                ResolvedCall<FunctionDescriptor> resolvedCall = context.get(BindingContext.COMPONENT_RESOLVED_CALL, entry);
                if (resolvedCall == null) return null;
                JetFunction componentFunction = (JetFunction) DescriptorToSourceUtils
                        .descriptorToDeclaration(resolvedCall.getCandidateDescriptor());
                JetType expectedType = context.get(BindingContext.TYPE, entry.getTypeRef());
                if (componentFunction != null && expectedType != null) {
                    return new ChangeFunctionReturnTypeFix(componentFunction, expectedType);
                }
                else return null;
            }
        };
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactoryForHasNextFunctionTypeMismatch() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetExpression expression = QuickFixUtil.getParentElementOfType(diagnostic, JetExpression.class);
                assert expression != null : "HAS_NEXT_FUNCTION_TYPE_MISMATCH reported on element that is not within any expression";
                BindingContext context = ResolvePackage.getBindingContext(expression.getContainingJetFile());
                ResolvedCall<FunctionDescriptor> resolvedCall = context.get(BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, expression);
                if (resolvedCall == null) return null;
                JetFunction hasNextFunction = (JetFunction) DescriptorToSourceUtils
                        .descriptorToDeclaration(resolvedCall.getCandidateDescriptor());
                if (hasNextFunction != null) {
                    return new ChangeFunctionReturnTypeFix(hasNextFunction, KotlinBuiltIns.getInstance().getBooleanType());
                }
                else return null;
            }
        };
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactoryForCompareToTypeMismatch() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetBinaryExpression expression = QuickFixUtil.getParentElementOfType(diagnostic, JetBinaryExpression.class);
                assert expression != null : "COMPARE_TO_TYPE_MISMATCH reported on element that is not within any expression";
                BindingContext context = ResolvePackage.getBindingContext(expression.getContainingJetFile());
                ResolvedCall<?> resolvedCall = BindingContextUtilPackage.getResolvedCall(expression, context);
                if (resolvedCall == null) return null;
                PsiElement compareTo = DescriptorToSourceUtils.descriptorToDeclaration(resolvedCall.getCandidateDescriptor());
                if (!(compareTo instanceof JetFunction)) return null;
                return new ChangeFunctionReturnTypeFix((JetFunction) compareTo, KotlinBuiltIns.getInstance().getIntType());
            }
        };
    }

    @NotNull
    public static JetIntentionActionsFactory createFactoryForReturnTypeMismatchOnOverride() {
        return new JetIntentionActionsFactory() {
            @NotNull
            @Override
            public List<IntentionAction> createActions(Diagnostic diagnostic) {
                List<IntentionAction> actions = new LinkedList<IntentionAction>();

                JetFunction function = QuickFixUtil.getParentElementOfType(diagnostic, JetFunction.class);
                if (function != null) {
                    BindingContext context = ResolvePackage.getBindingContext(function);
                    JetType matchingReturnType = QuickFixUtil.findLowerBoundOfOverriddenCallablesReturnTypes(context, function);
                    if (matchingReturnType != null) {
                        actions.add(new ChangeFunctionReturnTypeFix(function, matchingReturnType));
                    }

                    SimpleFunctionDescriptor descriptor = context.get(BindingContext.FUNCTION, function);
                    if (descriptor == null) return actions;
                    JetType functionType = descriptor.getReturnType();
                    if (functionType == null) return actions;

                    List<FunctionDescriptor> overriddenMismatchingFunctions = new LinkedList<FunctionDescriptor>();
                    for (FunctionDescriptor overriddenFunction: descriptor.getOverriddenDescriptors()) {
                        JetType overriddenFunctionType = overriddenFunction.getReturnType();
                        if (overriddenFunctionType == null) continue;
                        if (!JetTypeChecker.DEFAULT.isSubtypeOf(functionType, overriddenFunctionType)) {
                            overriddenMismatchingFunctions.add(overriddenFunction);
                        }
                    }

                    if (overriddenMismatchingFunctions.size() == 1) {
                        PsiElement overriddenFunction = DescriptorToSourceUtils
                                .descriptorToDeclaration(overriddenMismatchingFunctions.get(0));
                        if (overriddenFunction instanceof JetFunction) {
                            actions.add(new ChangeFunctionReturnTypeFix((JetFunction) overriddenFunction, functionType));
                        }
                    }
                }
                return actions;
            }
        };
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactoryForChangingReturnTypeToUnit() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetFunction function = QuickFixUtil.getParentElementOfType(diagnostic, JetFunction.class);
                return function == null ? null : new ChangeFunctionReturnTypeFix(function, KotlinBuiltIns.getInstance().getUnitType());
            }
        };
    }
}

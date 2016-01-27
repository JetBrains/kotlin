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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.dataClassUtils.DataClassUtilsKt;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

import java.util.LinkedList;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH;
import static org.jetbrains.kotlin.idea.project.PlatformKt.getPlatform;
import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt.getBuiltIns;

public class ChangeFunctionReturnTypeFix extends KotlinQuickFixAction<KtFunction> {
    private final KotlinType type;
    private final ChangeFunctionLiteralReturnTypeFix changeFunctionLiteralReturnTypeFix;

    public ChangeFunctionReturnTypeFix(@NotNull KtFunction element, @NotNull KotlinType type) {
        super(element);
        this.type = type;
        if (element instanceof KtFunctionLiteral) {
            KtLambdaExpression functionLiteralExpression = PsiTreeUtil.getParentOfType(element, KtLambdaExpression.class);
            assert functionLiteralExpression != null : "FunctionLiteral outside any FunctionLiteralExpression: " +
                                                       PsiUtilsKt.getElementTextWithContext(element);
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

        String functionName = getElement().getName();
        FqName fqName = getElement().getFqName();
        if (fqName != null) functionName = fqName.asString();

        if (KotlinBuiltIns.isUnit(type) && getElement().hasBlockBody()) {
            return functionName == null ?
                   KotlinBundle.message("remove.no.name.function.return.type") :
                   KotlinBundle.message("remove.function.return.type", functionName);
        }
        String renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type);
        return functionName == null ?
               KotlinBundle.message("change.no.name.function.return.type", renderedType) :
               KotlinBundle.message("change.function.return.type", functionName, renderedType);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return KotlinBundle.message("change.type.family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @Nullable Editor editor, @NotNull PsiFile file) {
        return super.isAvailable(project, editor, file) &&
               !ErrorUtils.containsErrorType(type) &&
               !(getElement() instanceof KtConstructor);
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull KtFile file) throws IncorrectOperationException {
        if (changeFunctionLiteralReturnTypeFix != null) {
            changeFunctionLiteralReturnTypeFix.invoke(project, editor, file);
        }
        else {
            if (!(KotlinBuiltIns.isUnit(type) && getElement().hasBlockBody())) {
                KtTypeReference newTypeRef = KtPsiFactoryKt
                        .KtPsiFactory(project).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type));
                newTypeRef = getElement().setTypeReference(newTypeRef);
                assert newTypeRef != null;
                ShortenReferences.DEFAULT.process(newTypeRef);
            }
            else {
                getElement().setTypeReference(null);
            }
        }
    }

    @NotNull
    public static KtDestructuringDeclarationEntry getDestructuringDeclarationEntryThatTypeMismatchComponentFunction(Diagnostic diagnostic) {
        Name componentName = COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH.cast(diagnostic).getA();
        int componentIndex = DataClassUtilsKt.getComponentIndex(componentName);
        KtDestructuringDeclaration multiDeclaration = QuickFixUtil.getParentElementOfType(diagnostic, KtDestructuringDeclaration.class);
        assert multiDeclaration != null : "COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH reported on expression that is not within any multi declaration";
        return multiDeclaration.getEntries().get(componentIndex - 1);
    }

    @NotNull
    public static KotlinSingleIntentionActionFactory createFactoryForComponentFunctionReturnTypeMismatch() {
        return new KotlinSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(@NotNull Diagnostic diagnostic) {
                KtDestructuringDeclarationEntry entry = getDestructuringDeclarationEntryThatTypeMismatchComponentFunction(diagnostic);
                BindingContext context = ResolutionUtils.analyze(entry);
                ResolvedCall<FunctionDescriptor> resolvedCall = context.get(BindingContext.COMPONENT_RESOLVED_CALL, entry);
                if (resolvedCall == null) return null;
                KtFunction componentFunction = (KtFunction) DescriptorToSourceUtils
                        .descriptorToDeclaration(resolvedCall.getCandidateDescriptor());
                KotlinType expectedType = context.get(BindingContext.TYPE, entry.getTypeReference());
                if (componentFunction != null && expectedType != null) {
                    return new ChangeFunctionReturnTypeFix(componentFunction, expectedType);
                }
                else return null;
            }
        };
    }

    @NotNull
    public static KotlinSingleIntentionActionFactory createFactoryForHasNextFunctionTypeMismatch() {
        return new KotlinSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(@NotNull Diagnostic diagnostic) {
                KtExpression expression = QuickFixUtil.getParentElementOfType(diagnostic, KtExpression.class);
                assert expression != null : "HAS_NEXT_FUNCTION_TYPE_MISMATCH reported on element that is not within any expression";
                BindingContext context = ResolutionUtils.analyze(expression);
                ResolvedCall<FunctionDescriptor> resolvedCall = context.get(BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, expression);
                if (resolvedCall == null) return null;
                FunctionDescriptor hasNextDescriptor = resolvedCall.getCandidateDescriptor();
                KtFunction hasNextFunction = (KtFunction) DescriptorToSourceUtils
                        .descriptorToDeclaration(hasNextDescriptor);
                if (hasNextFunction != null) {
                    return new ChangeFunctionReturnTypeFix(hasNextFunction, getBuiltIns(hasNextDescriptor).getBooleanType());
                }
                else return null;
            }
        };
    }

    @NotNull
    public static KotlinSingleIntentionActionFactory createFactoryForCompareToTypeMismatch() {
        return new KotlinSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(@NotNull Diagnostic diagnostic) {
                KtBinaryExpression expression = QuickFixUtil.getParentElementOfType(diagnostic, KtBinaryExpression.class);
                assert expression != null : "COMPARE_TO_TYPE_MISMATCH reported on element that is not within any expression";
                BindingContext context = ResolutionUtils.analyze(expression);
                ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCall(expression, context);
                if (resolvedCall == null) return null;
                CallableDescriptor compareToDescriptor = resolvedCall.getCandidateDescriptor();
                PsiElement compareTo = DescriptorToSourceUtils.descriptorToDeclaration(compareToDescriptor);
                if (!(compareTo instanceof KtFunction)) return null;
                return new ChangeFunctionReturnTypeFix((KtFunction) compareTo, getBuiltIns(compareToDescriptor).getIntType());
            }
        };
    }

    @NotNull
    public static KotlinIntentionActionsFactory createFactoryForReturnTypeMismatchOnOverride() {
        return new KotlinIntentionActionsFactory() {
            @NotNull
            @Override
            protected List<IntentionAction> doCreateActions(@NotNull Diagnostic diagnostic) {
                List<IntentionAction> actions = new LinkedList<IntentionAction>();

                KtFunction function = QuickFixUtil.getParentElementOfType(diagnostic, KtFunction.class);
                if (function != null) {
                    FunctionDescriptor descriptor = (FunctionDescriptor) ResolutionUtils.resolveToDescriptor(function);

                    KotlinType matchingReturnType = QuickFixUtil.findLowerBoundOfOverriddenCallablesReturnTypes(descriptor);
                    if (matchingReturnType != null) {
                        actions.add(new ChangeFunctionReturnTypeFix(function, matchingReturnType));
                    }

                    KotlinType functionType = descriptor.getReturnType();
                    if (functionType == null) return actions;

                    List<FunctionDescriptor> overriddenMismatchingFunctions = new LinkedList<FunctionDescriptor>();
                    for (FunctionDescriptor overriddenFunction: descriptor.getOverriddenDescriptors()) {
                        KotlinType overriddenFunctionType = overriddenFunction.getReturnType();
                        if (overriddenFunctionType == null) continue;
                        if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(functionType, overriddenFunctionType)) {
                            overriddenMismatchingFunctions.add(overriddenFunction);
                        }
                    }

                    if (overriddenMismatchingFunctions.size() == 1) {
                        PsiElement overriddenFunction = DescriptorToSourceUtils
                                .descriptorToDeclaration(overriddenMismatchingFunctions.get(0));
                        if (overriddenFunction instanceof KtFunction) {
                            actions.add(new ChangeFunctionReturnTypeFix((KtFunction) overriddenFunction, functionType));
                        }
                    }
                }
                return actions;
            }
        };
    }

    @NotNull
    public static KotlinSingleIntentionActionFactory createFactoryForChangingReturnTypeToUnit() {
        return new KotlinSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(@NotNull Diagnostic diagnostic) {
                KtFunction function = QuickFixUtil.getParentElementOfType(diagnostic, KtFunction.class);
                return function == null ? null : new ChangeFunctionReturnTypeFix(function, getPlatform(function).getBuiltIns().getUnitType());
            }
        };
    }

    @NotNull
    public static KotlinSingleIntentionActionFactory createFactoryForChangingReturnTypeToNothing() {
        return new KotlinSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(@NotNull Diagnostic diagnostic) {
                KtFunction function = QuickFixUtil.getParentElementOfType(diagnostic, KtFunction.class);
                return function == null ? null : new ChangeFunctionReturnTypeFix(function, getPlatform(function).getBuiltIns().getNothingType());
            }
        };
    }
}

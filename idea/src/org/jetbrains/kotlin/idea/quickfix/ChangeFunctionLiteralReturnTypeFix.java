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
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeProjection;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

import java.util.LinkedList;
import java.util.List;

import static org.jetbrains.kotlin.idea.project.PlatformKt.getPlatform;

public class ChangeFunctionLiteralReturnTypeFix extends KotlinQuickFixAction<KtFunctionLiteralExpression> {
    private final KotlinType type;
    private final KtTypeReference functionLiteralReturnTypeRef;
    private IntentionAction appropriateQuickFix = null;

    public ChangeFunctionLiteralReturnTypeFix(@NotNull KtFunctionLiteralExpression functionLiteralExpression, @NotNull KotlinType type) {
        super(functionLiteralExpression);
        this.type = type;
        functionLiteralReturnTypeRef = functionLiteralExpression.getFunctionLiteral().getTypeReference();

        AnalysisResult analysisResult = ResolutionUtils.analyzeFullyAndGetResult(functionLiteralExpression.getContainingJetFile());
        BindingContext context = analysisResult.getBindingContext();
        KotlinType functionLiteralType = context.getType(functionLiteralExpression);
        assert functionLiteralType != null : "Type of function literal not available in binding context";

        KotlinBuiltIns builtIns = analysisResult.getModuleDescriptor().getBuiltIns();
        ClassDescriptor functionClass = builtIns.getFunction(functionLiteralType.getArguments().size() - 1);
        List<KotlinType> functionClassTypeParameters = new LinkedList<KotlinType>();
        for (TypeProjection typeProjection: functionLiteralType.getArguments()) {
            functionClassTypeParameters.add(typeProjection.getType());
        }
        // Replacing return type:
        functionClassTypeParameters.remove(functionClassTypeParameters.size() - 1);
        functionClassTypeParameters.add(type);
        KotlinType eventualFunctionLiteralType = TypeUtils.substituteParameters(functionClass, functionClassTypeParameters);

        KtProperty correspondingProperty = PsiTreeUtil.getParentOfType(functionLiteralExpression, KtProperty.class);
        if (correspondingProperty != null && QuickFixUtil.canEvaluateTo(correspondingProperty.getInitializer(), functionLiteralExpression)) {
            KtTypeReference correspondingPropertyTypeRef = correspondingProperty.getTypeReference();
            KotlinType propertyType = context.get(BindingContext.TYPE, correspondingPropertyTypeRef);
            if (propertyType != null && !KotlinTypeChecker.DEFAULT.isSubtypeOf(eventualFunctionLiteralType, propertyType)) {
                appropriateQuickFix = new ChangeVariableTypeFix(correspondingProperty, eventualFunctionLiteralType);
            }
            return;
        }

        ResolvedCall<? extends CallableDescriptor> resolvedCall = CallUtilKt.getParentResolvedCall(
                functionLiteralExpression, context, true);
        if (resolvedCall != null) {
            ValueArgument valueArgument = CallUtilKt.getValueArgumentForExpression(resolvedCall.getCall(), functionLiteralExpression);
            KtParameter correspondingParameter = QuickFixUtil.getParameterDeclarationForValueArgument(resolvedCall, valueArgument);
            if (correspondingParameter != null) {
                KtTypeReference correspondingParameterTypeRef = correspondingParameter.getTypeReference();
                KotlinType parameterType = context.get(BindingContext.TYPE, correspondingParameterTypeRef);
                if (parameterType != null && !KotlinTypeChecker.DEFAULT.isSubtypeOf(eventualFunctionLiteralType, parameterType)) {
                    appropriateQuickFix = new ChangeParameterTypeFix(correspondingParameter, eventualFunctionLiteralType);
                }
                return;
            }
        }


        KtFunction parentFunction = PsiTreeUtil.getParentOfType(functionLiteralExpression, KtFunction.class, true);
        if (parentFunction != null && QuickFixUtil.canFunctionOrGetterReturnExpression(parentFunction, functionLiteralExpression)) {
            KtTypeReference parentFunctionReturnTypeRef = parentFunction.getTypeReference();
            KotlinType parentFunctionReturnType = context.get(BindingContext.TYPE, parentFunctionReturnTypeRef);
            if (parentFunctionReturnType != null && !KotlinTypeChecker.DEFAULT.isSubtypeOf(eventualFunctionLiteralType, parentFunctionReturnType)) {
                appropriateQuickFix = new ChangeFunctionReturnTypeFix(parentFunction, eventualFunctionLiteralType);
            }
        }
    }

    @NotNull
    @Override
    public String getText() {
        if (appropriateQuickFix != null) {
            return appropriateQuickFix.getText();
        }
        return JetBundle.message("change.function.literal.return.type", IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type));
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.type.family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) &&
            (functionLiteralReturnTypeRef != null || (appropriateQuickFix != null && appropriateQuickFix.isAvailable(project, editor, file)));
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        if (functionLiteralReturnTypeRef != null) {
            KtTypeReference newTypeRef = KtPsiFactoryKt.KtPsiFactory(file).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type));
            newTypeRef = (KtTypeReference) functionLiteralReturnTypeRef.replace(newTypeRef);
            ShortenReferences.DEFAULT.process(newTypeRef);
        }
        if (appropriateQuickFix != null && appropriateQuickFix.isAvailable(project, editor, file)) {
            appropriateQuickFix.invoke(project, editor, file);
        }
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactoryForExpectedOrAssignmentTypeMismatch() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(@NotNull Diagnostic diagnostic) {
                KtFunctionLiteralExpression
                        functionLiteralExpression = QuickFixUtil.getParentElementOfType(diagnostic, KtFunctionLiteralExpression.class);
                if (functionLiteralExpression == null) return null;
                return new ChangeFunctionLiteralReturnTypeFix(functionLiteralExpression, getPlatform(functionLiteralExpression).getBuiltIns().getUnitType());
            }
        };
    }
}

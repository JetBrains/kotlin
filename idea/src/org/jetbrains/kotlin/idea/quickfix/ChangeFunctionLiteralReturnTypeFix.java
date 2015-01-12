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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeProjection;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.LinkedList;
import java.util.List;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class ChangeFunctionLiteralReturnTypeFix extends JetIntentionAction<JetFunctionLiteralExpression> {
    private final JetType type;
    private final JetTypeReference functionLiteralReturnTypeRef;
    private IntentionAction appropriateQuickFix = null;

    public ChangeFunctionLiteralReturnTypeFix(@NotNull JetFunctionLiteralExpression functionLiteralExpression, @NotNull JetType type) {
        super(functionLiteralExpression);
        this.type = type;
        functionLiteralReturnTypeRef = functionLiteralExpression.getFunctionLiteral().getTypeReference();

        BindingContext context = ResolvePackage.analyzeFully(functionLiteralExpression.getContainingJetFile());
        JetType functionLiteralType = context.get(BindingContext.EXPRESSION_TYPE, functionLiteralExpression);
        assert functionLiteralType != null : "Type of function literal not available in binding context";

        ClassDescriptor functionClass = KotlinBuiltIns.getInstance().getFunction(functionLiteralType.getArguments().size() - 1);
        List<JetType> functionClassTypeParameters = new LinkedList<JetType>();
        for (TypeProjection typeProjection: functionLiteralType.getArguments()) {
            functionClassTypeParameters.add(typeProjection.getType());
        }
        // Replacing return type:
        functionClassTypeParameters.remove(functionClassTypeParameters.size() - 1);
        functionClassTypeParameters.add(type);
        JetType eventualFunctionLiteralType = TypeUtils.substituteParameters(functionClass, functionClassTypeParameters);

        JetProperty correspondingProperty = PsiTreeUtil.getParentOfType(functionLiteralExpression, JetProperty.class);
        if (correspondingProperty != null && QuickFixUtil.canEvaluateTo(correspondingProperty.getInitializer(), functionLiteralExpression)) {
            JetTypeReference correspondingPropertyTypeRef = correspondingProperty.getTypeReference();
            JetType propertyType = context.get(BindingContext.TYPE, correspondingPropertyTypeRef);
            if (propertyType != null && !JetTypeChecker.DEFAULT.isSubtypeOf(eventualFunctionLiteralType, propertyType)) {
                appropriateQuickFix = new ChangeVariableTypeFix(correspondingProperty, eventualFunctionLiteralType);
            }
            return;
        }

        ResolvedCall<? extends CallableDescriptor> resolvedCall = CallUtilPackage.getParentResolvedCall(
                functionLiteralExpression, context, true);
        if (resolvedCall != null) {
            ValueArgument valueArgument = CallUtilPackage.getValueArgumentForExpression(resolvedCall.getCall(), functionLiteralExpression);
            JetParameter correspondingParameter = QuickFixUtil.getParameterDeclarationForValueArgument(resolvedCall, valueArgument);
            if (correspondingParameter != null) {
                JetTypeReference correspondingParameterTypeRef = correspondingParameter.getTypeReference();
                JetType parameterType = context.get(BindingContext.TYPE, correspondingParameterTypeRef);
                if (parameterType != null && !JetTypeChecker.DEFAULT.isSubtypeOf(eventualFunctionLiteralType, parameterType)) {
                    appropriateQuickFix = new ChangeParameterTypeFix(correspondingParameter, eventualFunctionLiteralType);
                }
                return;
            }
        }


        JetFunction parentFunction = PsiTreeUtil.getParentOfType(functionLiteralExpression, JetFunction.class, true);
        if (parentFunction != null && QuickFixUtil.canFunctionOrGetterReturnExpression(parentFunction, functionLiteralExpression)) {
            JetTypeReference parentFunctionReturnTypeRef = parentFunction.getTypeReference();
            JetType parentFunctionReturnType = context.get(BindingContext.TYPE, parentFunctionReturnTypeRef);
            if (parentFunctionReturnType != null && !JetTypeChecker.DEFAULT.isSubtypeOf(eventualFunctionLiteralType, parentFunctionReturnType)) {
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
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        if (functionLiteralReturnTypeRef != null) {
            functionLiteralReturnTypeRef.replace(JetPsiFactory(file).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type)));
            QuickFixUtil.shortenReferencesOfType(type, file);
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
                JetFunctionLiteralExpression functionLiteralExpression = QuickFixUtil.getParentElementOfType(diagnostic, JetFunctionLiteralExpression.class);
                if (functionLiteralExpression == null) return null;
                return new ChangeFunctionLiteralReturnTypeFix(functionLiteralExpression, KotlinBuiltIns.getInstance().getUnitType());
            }
        };
    }
}

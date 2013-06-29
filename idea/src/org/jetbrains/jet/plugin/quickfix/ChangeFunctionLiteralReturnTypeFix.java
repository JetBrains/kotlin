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
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.LinkedList;
import java.util.List;

public class ChangeFunctionLiteralReturnTypeFix extends JetIntentionAction<JetFunctionLiteralExpression> {
    private final String renderedType;
    private final JetTypeReference functionLiteralReturnTypeRef;
    private IntentionAction appropriateQuickFix = null;

    public ChangeFunctionLiteralReturnTypeFix(@NotNull JetFunctionLiteralExpression element, @NotNull JetType type) {
        super(element);
        renderedType = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(type);
        functionLiteralReturnTypeRef = element.getFunctionLiteral().getReturnTypeRef();

        BindingContext context = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) element.getContainingFile()).getBindingContext();
        JetType functionLiteralType = context.get(BindingContext.EXPRESSION_TYPE, element);
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

        JetProperty correspondingProperty = PsiTreeUtil.getParentOfType(element, JetProperty.class);
        if (correspondingProperty != null && QuickFixUtil.canEvaluateTo(correspondingProperty.getInitializer(), element)) {
            JetTypeReference correspondingPropertyTypeRef = correspondingProperty.getTypeRef();
            JetType propertyType = context.get(BindingContext.TYPE, correspondingPropertyTypeRef);
            if (propertyType != null && !JetTypeChecker.INSTANCE.isSubtypeOf(eventualFunctionLiteralType, propertyType)) {
                appropriateQuickFix = new ChangeVariableTypeFix(correspondingProperty, eventualFunctionLiteralType);
            }
            return;
        }

        JetParameter correspondingParameter = QuickFixUtil.getParameterCorrespondingToFunctionLiteralPassedOutsideArgumentList(element);
        if (correspondingParameter != null) {
            JetTypeReference correspondingParameterTypeRef = correspondingParameter.getTypeReference();
            JetType parameterType = context.get(BindingContext.TYPE, correspondingParameterTypeRef);
            if (parameterType != null && !JetTypeChecker.INSTANCE.isSubtypeOf(eventualFunctionLiteralType, parameterType)) {
                appropriateQuickFix = new ChangeParameterTypeFix(correspondingParameter, eventualFunctionLiteralType);
            }
            return;
        }

        JetFunction parentFunction = PsiTreeUtil.getParentOfType(element, JetFunction.class, true);
        if (parentFunction != null && QuickFixUtil.canFunctionOrGetterReturnExpression(parentFunction, element)) {
            JetTypeReference parentFunctionReturnTypeRef = parentFunction.getReturnTypeRef();
            JetType parentFunctionReturnType = context.get(BindingContext.TYPE, parentFunctionReturnTypeRef);
            if (parentFunctionReturnType != null && !JetTypeChecker.INSTANCE.isSubtypeOf(eventualFunctionLiteralType, parentFunctionReturnType)) {
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
        return JetBundle.message("change.function.literal.return.type", renderedType);
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
            functionLiteralReturnTypeRef.replace(JetPsiFactory.createType(project, renderedType));
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
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetFunctionLiteralExpression functionLiteralExpression = QuickFixUtil.getParentElementOfType(diagnostic, JetFunctionLiteralExpression.class);
                assert functionLiteralExpression != null : "ASSIGNMENT/EXPECTED_TYPE_MISMATCH reported outside any function literal";
                return new ChangeFunctionLiteralReturnTypeFix(functionLiteralExpression, KotlinBuiltIns.getInstance().getUnitType());
            }
        };
    }
}

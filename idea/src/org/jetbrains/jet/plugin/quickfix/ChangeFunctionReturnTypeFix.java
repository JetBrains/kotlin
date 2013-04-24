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
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters3;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManagerUtil;
import org.jetbrains.jet.plugin.intentions.SpecifyTypeExplicitlyAction;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

public class ChangeFunctionReturnTypeFix extends JetIntentionAction<JetNamedFunction> {
    private final JetType type;

    public ChangeFunctionReturnTypeFix(@NotNull JetNamedFunction element, @NotNull JetType type) {
        super(element);
        this.type = type;
    }

    @NotNull
    @Override
    public String getText() {
        String functionName = element.getName();
        FqName fqName = JetPsiUtil.getFQName(element);
        if (fqName != null) functionName = fqName.getFqName();

        if (KotlinBuiltIns.getInstance().isUnit(type) && element.hasBlockBody()) {
            return functionName == null ?
                   JetBundle.message("remove.no.name.function.return.type") :
                   JetBundle.message("remove.function.return.type", functionName);
        }
        return functionName == null ?
               JetBundle.message("change.no.name.function.return.type", type.toString()) :
               JetBundle.message("change.function.return.type", functionName, type.toString());
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.type.family");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        SpecifyTypeExplicitlyAction.removeTypeAnnotation(element);
        if (!(KotlinBuiltIns.getInstance().isUnit(type) && element.hasBlockBody())) {
            PsiElement elementToPrecedeType = element.getValueParameterList();
            if (elementToPrecedeType == null) elementToPrecedeType = element.getNameIdentifier();
            assert elementToPrecedeType != null : "Return type of function without name can't mismatch anything";
            if (elementToPrecedeType.getNextSibling() instanceof PsiErrorElement) {
                // if a function doesn't have a value parameter list, a syntax error is raised, and it should follow the function name
                elementToPrecedeType = elementToPrecedeType.getNextSibling();
            }
            Pair<PsiElement, PsiElement> typeWhiteSpaceAndColon = JetPsiFactory.createTypeWhiteSpaceAndColon(project, type.toString());
            element.addRangeAfter(typeWhiteSpaceAndColon.first, typeWhiteSpaceAndColon.second, elementToPrecedeType);
        }
    }

    @NotNull
    public static JetMultiDeclarationEntry getMultiDeclarationEntryThatTypeMismatchComponentFunction(Diagnostic diagnostic) {
        String componentName = ((DiagnosticWithParameters3<JetExpression, Name, JetType, JetType>) diagnostic).getA().getName();
        int componentIndex = Integer.valueOf(componentName.substring(DescriptorResolver.COMPONENT_FUNCTION_NAME_PREFIX.length()));
        JetMultiDeclaration multiDeclaration = QuickFixUtil.getParentElementOfType(diagnostic, JetMultiDeclaration.class);
        assert multiDeclaration != null : "COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH reported on expression that is not within any multi declaration";
        return multiDeclaration.getEntries().get(componentIndex - 1);
    }

    @NotNull
    public static JetIntentionActionFactory createFactoryForComponentFunctionReturnTypeMismatch() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetMultiDeclarationEntry entry = getMultiDeclarationEntryThatTypeMismatchComponentFunction(diagnostic);
                BindingContext context = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) entry.getContainingFile().getContainingFile()).getBindingContext();
                ResolvedCall<FunctionDescriptor> resolvedCall = context.get(BindingContext.COMPONENT_RESOLVED_CALL, entry);
                if (resolvedCall == null) return null;
                JetNamedFunction componentFunction = (JetNamedFunction) BindingContextUtils.descriptorToDeclaration(context, resolvedCall.getCandidateDescriptor());
                JetType expectedType = context.get(BindingContext.TYPE, entry.getTypeRef());
                if (componentFunction != null && expectedType != null) {
                    return new ChangeFunctionReturnTypeFix(componentFunction, expectedType);
                }
                else return null;
            }
        };
    }

    @NotNull
    public static JetIntentionActionFactory createFactoryForHasNextFunctionTypeMismatch() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetExpression expression = QuickFixUtil.getParentElementOfType(diagnostic, JetExpression.class);
                assert expression != null : "HAS_NEXT_FUNCTION_TYPE_MISMATCH reported on element that is not within any expression";
                BindingContext context = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) expression.getContainingFile()).getBindingContext();
                ResolvedCall<FunctionDescriptor> resolvedCall = context.get(BindingContext.LOOP_RANGE_HAS_NEXT_RESOLVED_CALL, expression);
                if (resolvedCall == null) return null;
                JetNamedFunction hasNextFunction = (JetNamedFunction) BindingContextUtils.descriptorToDeclaration(context, resolvedCall.getCandidateDescriptor());
                if (hasNextFunction != null) {
                    return new ChangeFunctionReturnTypeFix(hasNextFunction, KotlinBuiltIns.getInstance().getBooleanType());
                }
                else return null;
            }
        };
    }

    @NotNull
    public static JetIntentionActionFactory createFactoryForCompareToTypeMismatch() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetBinaryExpression expression = QuickFixUtil.getParentElementOfType(diagnostic, JetBinaryExpression.class);
                assert expression != null : "COMPARE_TO_TYPE_MISMATCH reported on element that is not within any expression";
                BindingContext context = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) expression.getContainingFile()).getBindingContext();
                ResolvedCall<? extends CallableDescriptor> resolvedCall = context.get(BindingContext.RESOLVED_CALL, expression.getOperationReference());
                if (resolvedCall == null) return null;
                PsiElement compareTo = BindingContextUtils.descriptorToDeclaration(context, resolvedCall.getCandidateDescriptor());
                if (!(compareTo instanceof JetNamedFunction)) return null;
                return new ChangeFunctionReturnTypeFix((JetNamedFunction) compareTo, KotlinBuiltIns.getInstance().getIntType());
            }
        };
    }

    @NotNull
    public static JetIntentionActionFactory createFactoryForReturnTypeMismatchOnOverride() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetNamedFunction function = QuickFixUtil.getParentElementOfType(diagnostic, JetNamedFunction.class);
                if (function == null) return null;
                BindingContext context = KotlinCacheManagerUtil.getDeclarationsBindingContext(function);
                JetType matchingReturnType = QuickFixUtil.findLowerBoundOfOverriddenCallablesReturnTypes(context, function);
                return matchingReturnType == null ? null : new ChangeFunctionReturnTypeFix(function, matchingReturnType);
            }
        };
    }
}

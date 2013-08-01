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
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.intentions.SpecifyTypeExplicitlyAction;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.LinkedList;
import java.util.List;

public class ChangeVariableTypeFix extends JetIntentionAction<JetVariableDeclaration> {
    private final String renderedType;
    private final JetType type;

    public ChangeVariableTypeFix(@NotNull JetVariableDeclaration element, @NotNull JetType type) {
        super(element);
        renderedType = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(type);
        this.type = type;
    }

    @NotNull
    @Override
    public String getText() {
        String propertyName = element.getName();
        FqName fqName = JetPsiUtil.getFQName(element);
        if (fqName != null) propertyName = fqName.asString();

        return JetBundle.message("change.element.type", propertyName, renderedType);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.type.family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return super.isAvailable(project, editor, file) && !ErrorUtils.containsErrorType(type);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        SpecifyTypeExplicitlyAction.removeTypeAnnotation(element);
        PsiElement nameIdentifier = element.getNameIdentifier();
        assert nameIdentifier != null : "ChangeVariableTypeFix applied to variable without name";
        Pair<PsiElement, PsiElement> typeWhiteSpaceAndColon = JetPsiFactory.createTypeWhiteSpaceAndColon(project, renderedType);
        element.addRangeAfter(typeWhiteSpaceAndColon.first, typeWhiteSpaceAndColon.second, nameIdentifier);

        if (element instanceof JetProperty) {
            JetPropertyAccessor getter = ((JetProperty) element).getGetter();
            JetTypeReference getterReturnTypeRef = getter == null ? null : getter.getReturnTypeReference();
            if (getterReturnTypeRef != null) {
                getterReturnTypeRef.replace(JetPsiFactory.createType(project, renderedType));
            }

            JetPropertyAccessor setter = ((JetProperty) element).getSetter();
            JetParameter setterParameter = setter == null ? null : setter.getParameter();
            JetTypeReference setterParameterTypeRef = setterParameter == null ? null : setterParameter.getTypeReference();
            if (setterParameterTypeRef != null) {
                setterParameterTypeRef.replace(JetPsiFactory.createType(project, renderedType));
            }
        }
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactoryForComponentFunctionReturnTypeMismatch() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetMultiDeclarationEntry entry = ChangeFunctionReturnTypeFix.getMultiDeclarationEntryThatTypeMismatchComponentFunction(diagnostic);
                BindingContext context = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) entry.getContainingFile()).getBindingContext();
                ResolvedCall<FunctionDescriptor> resolvedCall = context.get(BindingContext.COMPONENT_RESOLVED_CALL, entry);
                if (resolvedCall == null) return null;
                JetFunction componentFunction = (JetFunction) BindingContextUtils.descriptorToDeclaration(context, resolvedCall.getCandidateDescriptor());
                if (componentFunction == null) return null;
                JetType expectedType = resolvedCall.getCandidateDescriptor().getReturnType();
                return expectedType == null ? null : new ChangeVariableTypeFix(entry, expectedType);
            }
        };
    }

    @NotNull
    public static JetIntentionActionsFactory createFactoryForPropertyOrReturnTypeMismatchOnOverride() {
        return new JetIntentionActionsFactory() {
            @NotNull
            @Override
            public List<IntentionAction> createActions(Diagnostic diagnostic) {
                List<IntentionAction> actions = new LinkedList<IntentionAction>();

                JetProperty property = QuickFixUtil.getParentElementOfType(diagnostic, JetProperty.class);
                if (property != null) {
                    BindingContext context = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) property.getContainingFile()).getBindingContext();
                    JetType lowerBoundOfOverriddenPropertiesTypes = QuickFixUtil.findLowerBoundOfOverriddenCallablesReturnTypes(context, property);

                    PropertyDescriptor descriptor = (PropertyDescriptor) context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property);
                    assert descriptor != null : "Descriptor of property not available in binding context";
                    JetType propertyType = descriptor.getReturnType();
                    assert propertyType != null : "Property type cannot be null if it mismatch something";

                    List<PropertyDescriptor> overriddenMismatchingProperties = new LinkedList<PropertyDescriptor>();
                    boolean canChangeOverriddenPropertyType = true;
                    for (PropertyDescriptor overriddenProperty: descriptor.getOverriddenDescriptors()) {
                        JetType overriddenPropertyType = overriddenProperty.getReturnType();
                        if (overriddenPropertyType != null) {
                            if (!JetTypeChecker.INSTANCE.isSubtypeOf(propertyType, overriddenPropertyType)) {
                                overriddenMismatchingProperties.add(overriddenProperty);
                            }
                            else if (overriddenProperty.isVar() && !JetTypeChecker.INSTANCE.equalTypes(overriddenPropertyType, propertyType)) {
                                canChangeOverriddenPropertyType = false;
                            }
                            if (overriddenProperty.isVar() && lowerBoundOfOverriddenPropertiesTypes != null &&
                                !JetTypeChecker.INSTANCE.equalTypes(lowerBoundOfOverriddenPropertiesTypes, overriddenPropertyType)) {
                                lowerBoundOfOverriddenPropertiesTypes = null;
                            }
                        }
                    }

                    if (lowerBoundOfOverriddenPropertiesTypes != null) {
                        actions.add(new ChangeVariableTypeFix(property, lowerBoundOfOverriddenPropertiesTypes));
                    }

                    if (overriddenMismatchingProperties.size() == 1 && canChangeOverriddenPropertyType) {
                        PsiElement overriddenProperty = BindingContextUtils.descriptorToDeclaration(context, overriddenMismatchingProperties.get(0));
                        if (overriddenProperty instanceof JetProperty) {
                            actions.add(new ChangeVariableTypeFix((JetProperty) overriddenProperty, propertyType));
                        }
                    }
                }
                return actions;
            }
        };
    }
}

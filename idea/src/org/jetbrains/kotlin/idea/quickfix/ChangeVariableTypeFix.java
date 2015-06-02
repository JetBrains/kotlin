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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class ChangeVariableTypeFix extends JetIntentionAction<JetVariableDeclaration> {
    private final static Logger LOG = Logger.getInstance(ChangeVariableTypeFix.class);

    private final JetType type;

    public ChangeVariableTypeFix(@NotNull JetVariableDeclaration element, @NotNull JetType type) {
        super(element);
        this.type = type;
    }

    @NotNull
    @Override
    public String getText() {
        String propertyName = element.getName();
        FqName fqName = element.getFqName();
        if (fqName != null) propertyName = fqName.asString();

        return JetBundle.message("change.element.type", propertyName, IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type));
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
        JetPsiFactory psiFactory = JetPsiFactory(file);

        PsiElement nameIdentifier = element.getNameIdentifier();
        assert nameIdentifier != null : "ChangeVariableTypeFix applied to variable without name";

        JetTypeReference replacingTypeReference = psiFactory.createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type));
        ArrayList<JetTypeReference> toShorten = new ArrayList<JetTypeReference>();
        toShorten.add(element.setTypeReference(replacingTypeReference));

        if (element instanceof JetProperty) {
            JetPropertyAccessor getter = ((JetProperty) element).getGetter();
            JetTypeReference getterReturnTypeRef = getter == null ? null : getter.getReturnTypeReference();
            if (getterReturnTypeRef != null) {
                toShorten.add((JetTypeReference) getterReturnTypeRef.replace(replacingTypeReference));
            }

            JetPropertyAccessor setter = ((JetProperty) element).getSetter();
            JetParameter setterParameter = setter == null ? null : setter.getParameter();
            JetTypeReference setterParameterTypeRef = setterParameter == null ? null : setterParameter.getTypeReference();
            if (setterParameterTypeRef != null) {
                toShorten.add((JetTypeReference) setterParameterTypeRef.replace(replacingTypeReference));
            }
        }

        ShortenReferences.DEFAULT.process(toShorten);
    }

    @NotNull
    public static JetSingleIntentionActionFactory createFactoryForComponentFunctionReturnTypeMismatch() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(@NotNull Diagnostic diagnostic) {
                JetMultiDeclarationEntry entry = ChangeFunctionReturnTypeFix.getMultiDeclarationEntryThatTypeMismatchComponentFunction(diagnostic);
                BindingContext context = ResolvePackage.analyzeFully(entry.getContainingJetFile());
                ResolvedCall<FunctionDescriptor> resolvedCall = context.get(BindingContext.COMPONENT_RESOLVED_CALL, entry);
                if (resolvedCall == null) return null;
                JetFunction componentFunction = (JetFunction) DescriptorToSourceUtils
                        .descriptorToDeclaration(resolvedCall.getCandidateDescriptor());
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
            protected List<IntentionAction> doCreateActions(@NotNull Diagnostic diagnostic) {
                List<IntentionAction> actions = new LinkedList<IntentionAction>();

                if (diagnostic.getPsiElement() instanceof JetProperty) {
                    JetProperty property = (JetProperty) diagnostic.getPsiElement();
                    BindingContext context = ResolvePackage.analyzeFully(property.getContainingJetFile());
                    JetType lowerBoundOfOverriddenPropertiesTypes = QuickFixUtil.findLowerBoundOfOverriddenCallablesReturnTypes(context, property);

                    DeclarationDescriptor descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, property);
                    if (!(descriptor instanceof PropertyDescriptor)) {
                        // Probably can happen in incomplete code.
                        LOG.error("Property descriptor is expected: " + PsiUtilPackage.getElementTextWithContext(property));
                        return actions;
                    }

                    PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;

                    JetType propertyType = propertyDescriptor.getReturnType();
                    assert propertyType != null : "Property type cannot be null if it mismatch something";

                    List<PropertyDescriptor> overriddenMismatchingProperties = new LinkedList<PropertyDescriptor>();
                    boolean canChangeOverriddenPropertyType = true;
                    for (PropertyDescriptor overriddenProperty: propertyDescriptor.getOverriddenDescriptors()) {
                        JetType overriddenPropertyType = overriddenProperty.getReturnType();
                        if (overriddenPropertyType != null) {
                            if (!JetTypeChecker.DEFAULT.isSubtypeOf(propertyType, overriddenPropertyType)) {
                                overriddenMismatchingProperties.add(overriddenProperty);
                            }
                            else if (overriddenProperty.isVar() && !JetTypeChecker.DEFAULT.equalTypes(overriddenPropertyType, propertyType)) {
                                canChangeOverriddenPropertyType = false;
                            }
                            if (overriddenProperty.isVar() && lowerBoundOfOverriddenPropertiesTypes != null &&
                                !JetTypeChecker.DEFAULT.equalTypes(lowerBoundOfOverriddenPropertiesTypes, overriddenPropertyType)) {
                                lowerBoundOfOverriddenPropertiesTypes = null;
                            }
                        }
                    }

                    if (lowerBoundOfOverriddenPropertiesTypes != null) {
                        actions.add(new ChangeVariableTypeFix(property, lowerBoundOfOverriddenPropertiesTypes));
                    }

                    if (overriddenMismatchingProperties.size() == 1 && canChangeOverriddenPropertyType) {
                        PsiElement overriddenProperty = DescriptorToSourceUtils
                                .descriptorToDeclaration(overriddenMismatchingProperties.get(0));
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

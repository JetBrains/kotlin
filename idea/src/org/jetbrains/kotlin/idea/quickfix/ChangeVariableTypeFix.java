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
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.idea.util.ShortenReferences;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.KtType;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ChangeVariableTypeFix extends KotlinQuickFixAction<KtVariableDeclaration> {
    private final static Logger LOG = Logger.getInstance(ChangeVariableTypeFix.class);

    private final KtType type;

    public ChangeVariableTypeFix(@NotNull KtVariableDeclaration element, @NotNull KtType type) {
        super(element);
        this.type = type;
    }

    @NotNull
    @Override
    public String getText() {
        String propertyName = getElement().getName();
        FqName fqName = getElement().getFqName();
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
    public void invoke(@NotNull Project project, Editor editor, KtFile file) throws IncorrectOperationException {
        KtPsiFactory psiFactory = KtPsiFactoryKt.KtPsiFactory(file);

        PsiElement nameIdentifier = getElement().getNameIdentifier();
        assert nameIdentifier != null : "ChangeVariableTypeFix applied to variable without name";

        KtTypeReference replacingTypeReference = psiFactory.createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type));
        ArrayList<KtTypeReference> toShorten = new ArrayList<KtTypeReference>();
        toShorten.add(getElement().setTypeReference(replacingTypeReference));

        if (getElement() instanceof KtProperty) {
            KtPropertyAccessor getter = ((KtProperty) getElement()).getGetter();
            KtTypeReference getterReturnTypeRef = getter == null ? null : getter.getReturnTypeReference();
            if (getterReturnTypeRef != null) {
                toShorten.add((KtTypeReference) getterReturnTypeRef.replace(replacingTypeReference));
            }

            KtPropertyAccessor setter = ((KtProperty) getElement()).getSetter();
            KtParameter setterParameter = setter == null ? null : setter.getParameter();
            KtTypeReference setterParameterTypeRef = setterParameter == null ? null : setterParameter.getTypeReference();
            if (setterParameterTypeRef != null) {
                toShorten.add((KtTypeReference) setterParameterTypeRef.replace(replacingTypeReference));
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
                KtMultiDeclarationEntry
                        entry = ChangeFunctionReturnTypeFix.getMultiDeclarationEntryThatTypeMismatchComponentFunction(diagnostic);
                BindingContext context = ResolutionUtils.analyze(entry);
                ResolvedCall<FunctionDescriptor> resolvedCall = context.get(BindingContext.COMPONENT_RESOLVED_CALL, entry);
                if (resolvedCall == null) return null;
                KtFunction componentFunction = (KtFunction) DescriptorToSourceUtils
                        .descriptorToDeclaration(resolvedCall.getCandidateDescriptor());
                if (componentFunction == null) return null;
                KtType expectedType = resolvedCall.getCandidateDescriptor().getReturnType();
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

                if (diagnostic.getPsiElement() instanceof KtProperty) {
                    KtProperty property = (KtProperty) diagnostic.getPsiElement();
                    DeclarationDescriptor descriptor = ResolutionUtils.resolveToDescriptor(property);
                    if (!(descriptor instanceof PropertyDescriptor)) return actions;
                    PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;

                    KtType
                            lowerBoundOfOverriddenPropertiesTypes = QuickFixUtil.findLowerBoundOfOverriddenCallablesReturnTypes(propertyDescriptor);

                    KtType propertyType = propertyDescriptor.getReturnType();
                    assert propertyType != null : "Property type cannot be null if it mismatch something";

                    List<PropertyDescriptor> overriddenMismatchingProperties = new LinkedList<PropertyDescriptor>();
                    boolean canChangeOverriddenPropertyType = true;
                    for (PropertyDescriptor overriddenProperty: propertyDescriptor.getOverriddenDescriptors()) {
                        KtType overriddenPropertyType = overriddenProperty.getReturnType();
                        if (overriddenPropertyType != null) {
                            if (!KotlinTypeChecker.DEFAULT.isSubtypeOf(propertyType, overriddenPropertyType)) {
                                overriddenMismatchingProperties.add(overriddenProperty);
                            }
                            else if (overriddenProperty.isVar() && !KotlinTypeChecker.DEFAULT.equalTypes(overriddenPropertyType, propertyType)) {
                                canChangeOverriddenPropertyType = false;
                            }
                            if (overriddenProperty.isVar() && lowerBoundOfOverriddenPropertiesTypes != null &&
                                !KotlinTypeChecker.DEFAULT.equalTypes(lowerBoundOfOverriddenPropertiesTypes, overriddenPropertyType)) {
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
                        if (overriddenProperty instanceof KtProperty) {
                            actions.add(new ChangeVariableTypeFix((KtProperty) overriddenProperty, propertyType));
                        }
                    }
                }

                return actions;
            }
        };
    }
}

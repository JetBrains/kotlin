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

package org.jetbrains.jet.plugin.refactoring.changeSignature;

import com.google.common.collect.Sets;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.refactoring.changeSignature.MethodDescriptor;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.jetAsJava.KotlinLightMethod;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class JetChangeSignatureData implements JetMethodDescriptor {
    @NotNull
    private final FunctionDescriptor baseDescriptor;
    @NotNull
    private final PsiElement baseDeclaration;
    @NotNull
    private final List<JetParameterInfo> parameters;
    @NotNull
    private final Collection<FunctionDescriptor> descriptorsForSignatureChange;
    private Collection<PsiElement> affectedFunctions = null;

    public JetChangeSignatureData(
            @NotNull FunctionDescriptor baseDescriptor,
            @NotNull PsiElement baseDeclaration,
            @NotNull Collection<FunctionDescriptor> descriptorsForSignatureChange
    ) {
        this.baseDescriptor = baseDescriptor;
        this.baseDeclaration = baseDeclaration;
        this.descriptorsForSignatureChange = descriptorsForSignatureChange;
        final List<JetParameter> valueParameters = this.baseDeclaration instanceof JetFunction
                                                   ? ((JetFunction) this.baseDeclaration).getValueParameters()
                                                   : ((JetClass) this.baseDeclaration).getPrimaryConstructorParameters();
        this.parameters = new ArrayList<JetParameterInfo>(
                ContainerUtil.map(this.baseDescriptor.getValueParameters(), new Function<ValueParameterDescriptor, JetParameterInfo>() {
                    @Override
                    public JetParameterInfo fun(ValueParameterDescriptor param) {
                        JetParameter parameter = valueParameters.get(param.getIndex());
                        return new JetParameterInfo(param.getIndex(), param.getName().asString(), param.getType(),
                                                    parameter.getDefaultValue(), parameter.getValOrVarNode());
                    }
                }));
    }

    @Override
    @NotNull
    public List<JetParameterInfo> getParameters() {
        return parameters;
    }

    public void addParameter(JetParameterInfo parameter) {
        parameters.add(parameter);
    }

    public void removeParameter(int index) {
        parameters.remove(index);
    }

    public void clearParameters() {
        parameters.clear();
    }

    @Override
    @NotNull
    public Collection<PsiElement> getAffectedFunctions() {
        if (affectedFunctions == null) {
            affectedFunctions = Sets.newHashSet();
            for (FunctionDescriptor descriptor : descriptorsForSignatureChange) {
                affectedFunctions.addAll(computeHierarchyFrom(descriptor));
            }
        }
        return affectedFunctions;
    }

    @NotNull
    private Collection<PsiElement> computeHierarchyFrom(@NotNull FunctionDescriptor baseDescriptor) {
        PsiElement declaration = DescriptorToDeclarationUtil.getDeclaration(baseDeclaration.getProject(), baseDescriptor);
        Set<PsiElement> result = Sets.newHashSet();
        result.add(declaration);
        if (!(declaration instanceof JetNamedFunction)) {
            return result;
        }
        PsiMethod lightMethod = LightClassUtil.getLightClassMethod((JetNamedFunction) declaration);
        // there are valid situations when light method is null: local functions and literals
        if (lightMethod == null) {
            return result;
        }
        Collection<PsiMethod> overridingMethods = OverridingMethodsSearch.search(lightMethod).findAll();
        List<PsiMethod> jetLightMethods = ContainerUtil.filter(overridingMethods, new Condition<PsiMethod>() {
            @Override
            public boolean value(PsiMethod method) {
                return method instanceof KotlinLightMethod;
            }
        });
        List<JetDeclaration> jetFunctions = ContainerUtil.map(jetLightMethods, new Function<PsiMethod, JetDeclaration>() {
            @Override
            public JetDeclaration fun(PsiMethod method) {
                return ((KotlinLightMethod) method).getOrigin();
            }
        });
        result.addAll(jetFunctions);
        return result;
    }


    @Override
    public String getName() {
        if (baseDescriptor instanceof ConstructorDescriptor) {
            return baseDescriptor.getContainingDeclaration().getName().asString();
        }
        else if (baseDescriptor instanceof AnonymousFunctionDescriptor) {
            return "";
        }
        else {
            return baseDescriptor.getName().asString();
        }
    }

    @Override
    public int getParametersCount() {
        return baseDescriptor.getValueParameters().size();
    }

    @Override
    public Visibility getVisibility() {
        return baseDescriptor.getVisibility();
    }

    @Override
    public PsiElement getMethod() {
        return baseDeclaration;
    }

    @Override
    public boolean canChangeVisibility() {
        DeclarationDescriptor parent = baseDescriptor.getContainingDeclaration();
        return !(baseDescriptor instanceof AnonymousFunctionDescriptor ||
                 parent instanceof ClassDescriptor && ((ClassDescriptor) parent).getKind() == ClassKind.TRAIT);
    }

    @Override
    public boolean canChangeParameters() {
        return true;
    }

    @Override
    public boolean canChangeName() {
        return !(baseDescriptor instanceof ConstructorDescriptor ||
                 baseDescriptor instanceof AnonymousFunctionDescriptor);
    }

    @Override
    public MethodDescriptor.ReadWriteOption canChangeReturnType() {
        return baseDescriptor instanceof ConstructorDescriptor ? ReadWriteOption.None : ReadWriteOption.ReadWrite;
    }

    @Override
    public boolean isConstructor() {
        return baseDescriptor instanceof ConstructorDescriptor;
    }

    @NotNull
    @Override
    public PsiElement getContext() {
        return baseDeclaration;
    }

    @Nullable
    @Override
    public FunctionDescriptor getDescriptor() {
        return baseDescriptor;
    }

    @Override
    @Nullable
    public String getReturnTypeText() {
        JetType returnType = baseDescriptor.getReturnType();
        return returnType != null ? DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(returnType) : null;
    }
}

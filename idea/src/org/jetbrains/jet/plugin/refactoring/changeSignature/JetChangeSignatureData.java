/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.refactoring.changeSignature.MethodDescriptor;
import com.intellij.refactoring.changeSignature.OverriderUsageInfo;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.KotlinLightMethod;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil;
import org.jetbrains.jet.plugin.refactoring.changeSignature.usages.JetFunctionDefinitionUsage;
import org.jetbrains.jet.plugin.util.IdeDescriptorRenderers;

import java.util.*;

public final class JetChangeSignatureData implements JetMethodDescriptor {
    @NotNull
    private final FunctionDescriptor baseDescriptor;
    @NotNull
    private final PsiElement baseDeclaration;
    @NotNull
    private final List<JetParameterInfo> parameters;
    @NotNull
    private final Collection<FunctionDescriptor> descriptorsForSignatureChange;
    private Collection<UsageInfo> affectedFunctions = null;

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
                                                   : this.baseDeclaration instanceof JetClass
                                                     ? ((JetClass) this.baseDeclaration).getPrimaryConstructorParameters()
                                                     : null;
        this.parameters = new ArrayList<JetParameterInfo>(
                ContainerUtil.map(this.baseDescriptor.getValueParameters(), new Function<ValueParameterDescriptor, JetParameterInfo>() {
                    @Override
                    public JetParameterInfo fun(ValueParameterDescriptor param) {
                        JetParameter parameter = valueParameters != null ? valueParameters.get(param.getIndex()) : null;
                        return new JetParameterInfo(
                                param.getIndex(),
                                param.getName().asString(),
                                param.getType(),
                                parameter != null ? parameter.getDefaultValue() : null,
                                parameter != null ? parameter.getValOrVarNode() : null
                        );
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
    public Collection<UsageInfo> getAffectedFunctions() {
        if (affectedFunctions == null) {
            affectedFunctions = KotlinPackage.flatMapTo(
                    descriptorsForSignatureChange,
                    new HashSet<UsageInfo>(),
                    new Function1<FunctionDescriptor, Iterable<? extends UsageInfo>>() {
                        @Override
                        public Iterable<? extends UsageInfo> invoke(FunctionDescriptor descriptor) {
                            PsiElement declaration = DescriptorToDeclarationUtil.INSTANCE$.getDeclaration(baseDeclaration.getProject(),
                                                                                                          descriptor);
                            assert declaration != null : "No declaration found for " + descriptor;

                            Set<UsageInfo> result = Sets.newHashSet();
                            result.add(new JetFunctionDefinitionUsage(declaration, false));

                            if (!(declaration instanceof JetNamedFunction)) return result;

                            final PsiMethod baseLightMethod = LightClassUtil.getLightClassMethod((JetNamedFunction) declaration);
                            // there are valid situations when light method is null: local functions and literals
                            if (baseLightMethod == null) return result;

                            return KotlinPackage.filterNotNullTo(
                                    KotlinPackage.map(
                                            OverridingMethodsSearch.search(baseLightMethod).findAll(),
                                            new Function1<PsiMethod, UsageInfo>() {
                                                @Override
                                                public UsageInfo invoke(PsiMethod method) {
                                                    if (method instanceof KotlinLightMethod) {
                                                        JetDeclaration declaration = ((KotlinLightMethod) method).getOrigin();
                                                        return declaration != null ? new JetFunctionDefinitionUsage(declaration, true) : null;
                                                    }

                                                    return new OverriderUsageInfo(method, baseLightMethod, true, true, true);
                                                }
                                            }
                                    ),
                                    result
                            );
                        }
                    }
            );
        }
        return affectedFunctions;
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
        return returnType != null ? IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(returnType) : null;
    }
}

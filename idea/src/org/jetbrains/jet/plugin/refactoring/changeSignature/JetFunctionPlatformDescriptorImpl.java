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

import com.intellij.psi.PsiElement;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.ArrayList;
import java.util.List;

public class JetFunctionPlatformDescriptorImpl implements JetFunctionPlatformDescriptor {
    private final FunctionDescriptor funDescriptor;
    private final PsiElement funElement;
    private final List<JetParameterInfo> parameters;

    public JetFunctionPlatformDescriptorImpl(FunctionDescriptor descriptor, PsiElement element) {
        funDescriptor = descriptor;
        funElement = element;
        final List<JetParameter> valueParameters = funElement instanceof JetFunction
                                              ? ((JetFunction) funElement).getValueParameters()
                                              : ((JetClass) funElement).getPrimaryConstructorParameters();
        parameters = new ArrayList<JetParameterInfo>(ContainerUtil.map(funDescriptor.getValueParameters(), new Function<ValueParameterDescriptor, JetParameterInfo>() {
            @Override
            public JetParameterInfo fun(ValueParameterDescriptor param) {
                JetParameter parameter = valueParameters.get(param.getIndex());
                return new JetParameterInfo(param.getIndex(), param.getName().getName(), param.getType(), parameter.getDefaultValue(), parameter.getValOrVarNode());
            }
        }));
    }

    @Override
    public String getName() {
        if (funDescriptor instanceof ConstructorDescriptor)
            return funDescriptor.getContainingDeclaration().getName().getName();
        else if (funDescriptor instanceof AnonymousFunctionDescriptor)
            return "";
        else
            return funDescriptor.getName().getName();
    }

    @Override
    public List<JetParameterInfo> getParameters() {
        return parameters;
    }

    public void addParameter(JetParameterInfo parameter) {
        parameters.add(parameter);
    }

    public void removeParameter(int index) {
        parameters.remove(index);
    }

    @Override
    public int getParametersCount() {
        return funDescriptor.getValueParameters().size();
    }

    @Override
    public Visibility getVisibility() {
        return funDescriptor.getVisibility();
    }

    @Override
    public PsiElement getMethod() {
        return funElement;
    }

    @NotNull
    @Override
    public PsiElement getContext() {
        return funElement;
    }

    @Override
    public boolean isConstructor() {
        return funDescriptor instanceof ConstructorDescriptor;
    }

    @Override
    public boolean canChangeVisibility() {
        DeclarationDescriptor parent = funDescriptor.getContainingDeclaration();
        return !(funDescriptor instanceof AnonymousFunctionDescriptor || parent instanceof ClassDescriptor && ((ClassDescriptor) parent).getKind() == ClassKind.TRAIT);
    }

    @Override
    public boolean canChangeParameters() {
        return true;
    }

    @Override
    public boolean canChangeName() {
        return !(funDescriptor instanceof ConstructorDescriptor || funDescriptor instanceof AnonymousFunctionDescriptor);
    }

    @Override
    public ReadWriteOption canChangeReturnType() {
        return isConstructor() ? ReadWriteOption.None : ReadWriteOption.ReadWrite;
    }

    @Override
    public FunctionDescriptor getDescriptor() {
        return funDescriptor;
    }

    @Override
    @Nullable
    public String getReturnTypeText() {
        JetType returnType = funDescriptor.getReturnType();
        return returnType != null ? DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(returnType) : null;
    }
}

/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.List;
import java.util.Set;

/**
* @author Stepan Koltsov
*/
public class NamedMembers {

    public NamedMembers(@NotNull Name name) {
        this.name = name;
    }

    @NotNull
    private final Name name;

    @NotNull
    private final List<PsiMethodWrapper> methods = Lists.newArrayList();

    @NotNull
    private final List<PropertyAccessorData> propertyAccessors = Lists.newArrayList();

    private Set<VariableDescriptor> propertyDescriptors;

    /** Including from supertypes */
    private Set<FunctionDescriptor> functionDescriptors;

    void addMethod(@NotNull PsiMethodWrapper method) {
        methods.add(method);
    }

    void addPropertyAccessor(@NotNull PropertyAccessorData propertyAccessorData) {
        propertyAccessors.add(propertyAccessorData);
    }

    @NotNull
    public List<PsiMethodWrapper> getMethods() {
        return methods;
    }

    @NotNull
    public Name getName() {
        return name;
    }

    @NotNull
    public List<PropertyAccessorData> getPropertyAccessors() {
        return propertyAccessors;
    }

    @Nullable
    public Set<VariableDescriptor> getPropertyDescriptors() {
        return propertyDescriptors;
    }

    public void setPropertyDescriptors(@NotNull Set<VariableDescriptor> propertyDescriptors) {
        this.propertyDescriptors = propertyDescriptors;
    }

    @Nullable
    public Set<FunctionDescriptor> getFunctionDescriptors() {
        return functionDescriptors;
    }

    public void setFunctionDescriptors(@NotNull Set<FunctionDescriptor> functionDescriptors) {
        this.functionDescriptors = functionDescriptors;
    }
}

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

package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;

public class FilteringScope extends JetScopeAdapter {
    private final Predicate<DeclarationDescriptor> predicate;

    public FilteringScope(
            @NotNull JetScope workerScope,
            @NotNull Predicate<DeclarationDescriptor> predicate
    ) {
        super(workerScope);
        this.predicate = predicate;
    }

    @NotNull
    @Override
    public Collection<FunctionDescriptor> getFunctions(@NotNull Name name) {
        return Collections2.filter(super.getFunctions(name), predicate);
    }

    @Nullable
    private <D extends DeclarationDescriptor> D filterDescriptor(@Nullable D descriptor) {
        return descriptor != null && predicate.apply(descriptor) ? descriptor : null;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull Name name) {
        return filterDescriptor(super.getNamespace(name));
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        return filterDescriptor(super.getClassifier(name));
    }

    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
        return filterDescriptor(super.getObjectDescriptor(name));
    }

    @NotNull
    @Override
    public Collection<ClassDescriptor> getObjectDescriptors() {
        return Collections2.filter(super.getObjectDescriptors(), predicate);
    }

    @NotNull
    @Override
    public Collection<VariableDescriptor> getProperties(@NotNull Name name) {
        return Collections2.filter(super.getProperties(name), predicate);
    }

    @Override
    public VariableDescriptor getLocalVariable(@NotNull Name name) {
        return filterDescriptor(super.getLocalVariable(name));
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        return Collections2.filter(super.getAllDescriptors(), predicate);
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(LabelName labelName) {
        return Collections2.filter(super.getDeclarationsByLabel(labelName), predicate);
    }

    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull Name fieldName) {
        return filterDescriptor(super.getPropertyByFieldReference(fieldName));
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors() {
        return Collections2.filter(super.getOwnDeclaredDescriptors(), predicate);
    }
}

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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.utils.Printer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class JetScopeImpl implements JetScope {
    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        return null;
    }

    @NotNull
    @Override
    public Collection<VariableDescriptor> getProperties(@NotNull Name name) {
        return Collections.emptySet();
    }

    @Override
    public VariableDescriptor getLocalVariable(@NotNull Name name) {
        return null;
    }

    @Nullable
    @Override
    public PackageViewDescriptor getPackage(@NotNull Name name) {
        return null;
    }

    @NotNull
    @Override
    public Collection<FunctionDescriptor> getFunctions(@NotNull Name name) {
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(LabelName labelName) {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getImplicitReceiversHierarchy() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors() {
        return Collections.emptyList();
    }

    // This method should not be implemented here by default: every scope class has its unique structure pattern
    @TestOnly
    @Override
    public abstract void printScopeStructure(@NotNull Printer p);
}

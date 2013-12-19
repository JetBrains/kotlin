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
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.utils.Printer;

import java.util.Collection;
import java.util.List;

public class FilteringScope implements JetScope {
    @NotNull private final JetScope workerScope;
    @NotNull private final Predicate<DeclarationDescriptor> predicate;

    public FilteringScope(@NotNull JetScope workerScope, @NotNull Predicate<DeclarationDescriptor> predicate) {
        this.workerScope = workerScope;
        this.predicate = predicate;
    }

    @NotNull
    @Override
    public Collection<FunctionDescriptor> getFunctions(@NotNull Name name) {
        return Collections2.filter(workerScope.getFunctions(name), predicate);
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return workerScope.getContainingDeclaration();
    }

    @Nullable
    private <D extends DeclarationDescriptor> D filterDescriptor(@Nullable D descriptor) {
        return descriptor != null && predicate.apply(descriptor) ? descriptor : null;
    }

    @Nullable
    @Override
    public PackageViewDescriptor getPackage(@NotNull Name name) {
        return filterDescriptor(workerScope.getPackage(name));
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        return filterDescriptor(workerScope.getClassifier(name));
    }

    @NotNull
    @Override
    public Collection<VariableDescriptor> getProperties(@NotNull Name name) {
        return Collections2.filter(workerScope.getProperties(name), predicate);
    }

    @Override
    public VariableDescriptor getLocalVariable(@NotNull Name name) {
        return filterDescriptor(workerScope.getLocalVariable(name));
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        return Collections2.filter(workerScope.getAllDescriptors(), predicate);
    }

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getImplicitReceiversHierarchy() {
        return workerScope.getImplicitReceiversHierarchy();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull LabelName labelName) {
        return Collections2.filter(workerScope.getDeclarationsByLabel(labelName), predicate);
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors() {
        return Collections2.filter(workerScope.getOwnDeclaredDescriptors(), predicate);
    }

    @TestOnly
    @Override
    public void printScopeStructure(@NotNull Printer p) {
        p.println(getClass().getSimpleName(), " {");
        p.pushIndent();

        p.print("workerScope = ");
        workerScope.printScopeStructure(p.withholdIndentOnce());

        p.popIndent();
        p.println("}");
    }
}

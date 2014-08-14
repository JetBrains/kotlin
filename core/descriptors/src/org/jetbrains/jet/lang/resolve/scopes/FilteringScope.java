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

import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.utils.Printer;

import java.util.Collection;
import java.util.List;

public class FilteringScope implements JetScope {
    private final JetScope workerScope;
    private final Function1<DeclarationDescriptor, Boolean> predicate;

    public FilteringScope(@NotNull JetScope workerScope, @NotNull Function1<DeclarationDescriptor, Boolean> predicate) {
        this.workerScope = workerScope;
        this.predicate = predicate;
    }

    @NotNull
    @Override
    public Collection<FunctionDescriptor> getFunctions(@NotNull Name name) {
        return KotlinPackage.filter(workerScope.getFunctions(name), predicate);
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return workerScope.getContainingDeclaration();
    }

    @Nullable
    private <D extends DeclarationDescriptor> D filterDescriptor(@Nullable D descriptor) {
        return descriptor != null && predicate.invoke(descriptor) ? descriptor : null;
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
        return KotlinPackage.filter(workerScope.getProperties(name), predicate);
    }

    @Override
    public VariableDescriptor getLocalVariable(@NotNull Name name) {
        return filterDescriptor(workerScope.getLocalVariable(name));
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        return KotlinPackage.filter(workerScope.getAllDescriptors(), predicate);
    }

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getImplicitReceiversHierarchy() {
        return workerScope.getImplicitReceiversHierarchy();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull Name labelName) {
        return KotlinPackage.filter(workerScope.getDeclarationsByLabel(labelName), predicate);
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors() {
        return KotlinPackage.filter(workerScope.getOwnDeclaredDescriptors(), predicate);
    }

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

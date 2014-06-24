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

package org.jetbrains.jet.lang.resolve.lazy.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.declarations.PackageMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class LazyPackageMemberScope extends AbstractLazyMemberScope<PackageFragmentDescriptor, PackageMemberDeclarationProvider> {

    public LazyPackageMemberScope(@NotNull ResolveSession resolveSession,
            @NotNull PackageMemberDeclarationProvider declarationProvider,
            @NotNull PackageFragmentDescriptor thisPackage) {
        super(resolveSession, declarationProvider, thisPackage, resolveSession.getTrace());
    }

    @Nullable
    @Override
    public PackageViewDescriptor getPackage(@NotNull Name name) {
        return null;
    }

    @NotNull
    @Override
    protected JetScope getScopeForMemberDeclarationResolution(JetDeclaration declaration) {
        return resolveSession.getScopeProvider().getFileScope(declaration.getContainingJetFile());
    }

    @Override
    protected void getNonDeclaredFunctions(@NotNull Name name, @NotNull Set<FunctionDescriptor> result) {
        // No extra functions
    }

    @Override
    protected void getNonDeclaredProperties(@NotNull Name name, @NotNull Set<VariableDescriptor> result) {
        // No extra properties
    }

    @NotNull
    @Override
    protected Collection<DeclarationDescriptor> computeExtraDescriptors() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        // Do not add details here, they may compromise the laziness during debugging
        return "lazy scope for package " + thisDescriptor.getName();
    }
}

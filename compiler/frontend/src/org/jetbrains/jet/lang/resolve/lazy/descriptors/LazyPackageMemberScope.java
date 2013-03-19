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

package org.jetbrains.jet.lang.resolve.lazy.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.lazy.LazyCodeAnalyzer;
import org.jetbrains.jet.lang.resolve.lazy.declarations.PackageMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.Set;

public class LazyPackageMemberScope extends AbstractLazyMemberScope<PackageFragmentDescriptor, PackageMemberDeclarationProvider> {

    public LazyPackageMemberScope(
            @NotNull LazyCodeAnalyzer analyzer,
            @NotNull PackageFragmentDescriptor thisPackageFragment,
            @NotNull PackageMemberDeclarationProvider declarationProvider
    ) {
        super(analyzer, declarationProvider, thisPackageFragment);
    }

    @Override
    public PackageViewDescriptor getPackage(@NotNull Name name) {
        // Packages are never children of package fragments
        return null;
    }

    @Nullable
    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        // TODO: creating an FqName every time may be a performance problem
        Name actualName = analyzer.resolveClassifierAlias(DescriptorUtils.getFQName(thisDescriptor).toSafe(), name);
        return super.getClassifier(actualName);
    }

    @NotNull
    @Override
    protected JetScope getScopeForMemberDeclarationResolution(JetDeclaration declaration) {
        return analyzer.getInjector().getScopeProvider().getFileScope((JetFile) declaration.getContainingFile());
    }

    @Override
    protected ReceiverParameterDescriptor getImplicitReceiver() {
        return ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;
    }

    @Override
    protected void getNonDeclaredFunctions(@NotNull Name name, @NotNull Set<FunctionDescriptor> result) {
        // No extra functions
    }

    @Override
    protected void getNonDeclaredProperties(@NotNull Name name, @NotNull Set<VariableDescriptor> result) {
        // No extra properties
    }

    @Override
    protected void addExtraDescriptors(@NotNull Collection<DeclarationDescriptor> result) {
        // package fragments do not own subpackages
    }

    @Override
    public String toString() {
        // Do not add details here, they may compromise the laziness during debugging
        return "lazy scope for package " + thisDescriptor.getName();
    }
}

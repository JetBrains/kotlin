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
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentKind;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.PackageLikeDescriptorBase;
import org.jetbrains.jet.lang.resolve.lazy.ForceResolveUtil;
import org.jetbrains.jet.lang.resolve.lazy.LazyCodeAnalyzer;
import org.jetbrains.jet.lang.resolve.lazy.LazyDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.declarations.PackageMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

public class LazyPackageDescriptor extends PackageLikeDescriptorBase implements LazyDescriptor, PackageFragmentDescriptor {

    private final SubModuleDescriptor subModule;
    private final JetScope memberScope;

    public LazyPackageDescriptor(
            @NotNull LazyCodeAnalyzer analyzer,
            @NotNull SubModuleDescriptor subModule,
            @NotNull FqName fqName,
            @NotNull PackageMemberDeclarationProvider declarationProvider
    ) {
        super(fqName);

        this.subModule = subModule;
        this.memberScope = new LazyPackageMemberScope(analyzer, this, declarationProvider);
    }

    @NotNull
    @Override
    public PackageFragmentKind getKind() {
        return PackageFragmentKind.SOURCE;
    }

    @NotNull
    @Override
    public SubModuleDescriptor getContainingDeclaration() {
        return subModule;
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return memberScope;
    }

    @Override
    public void forceResolveAllContents() {
        ForceResolveUtil.forceResolveAllContents(memberScope);
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPackageFragmentDescriptor(this, data);
    }
}

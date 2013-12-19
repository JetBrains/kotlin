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
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.DeclarationDescriptorImpl;
import org.jetbrains.jet.lang.resolve.lazy.ForceResolveUtil;
import org.jetbrains.jet.lang.resolve.lazy.LazyDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.declarations.PackageMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.*;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collections;

public class LazyPackageDescriptor extends DeclarationDescriptorImpl implements LazyDescriptor, PackageFragmentDescriptor {
    private final ModuleDescriptor module;
    private final JetScope memberScope;
    private final FqName fqName;
    private final PackageMemberDeclarationProvider declarationProvider;

    public LazyPackageDescriptor(
            @NotNull ModuleDescriptor module,
            @NotNull FqName fqName,
            @NotNull ResolveSession resolveSession,
            @NotNull PackageMemberDeclarationProvider declarationProvider
    ) {
        super(Collections.<AnnotationDescriptor>emptyList(), fqName.shortNameOrSpecial());
        this.module = module;
        this.fqName = fqName;
        this.declarationProvider = declarationProvider;

        this.memberScope = new LazyPackageMemberScope(resolveSession, declarationProvider, this);
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return memberScope;
    }

    @NotNull
    @Override
    public ModuleDescriptor getContainingDeclaration() {
        return module;
    }

    @Nullable
    @Override
    public DeclarationDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        return this;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPackageFragmentDescriptor(this, data);
    }

    @NotNull
    @Override
    public FqName getFqName() {
        return fqName;
    }

    @Override
    public void forceResolveAllContents() {
        ForceResolveUtil.forceResolveAllContents(memberScope);
    }

    @NotNull
    public PackageMemberDeclarationProvider getDeclarationProvider() {
        return declarationProvider;
    }
}

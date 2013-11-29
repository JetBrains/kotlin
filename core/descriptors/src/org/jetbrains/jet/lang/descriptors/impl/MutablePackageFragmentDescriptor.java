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

package org.jetbrains.jet.lang.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collections;

public class MutablePackageFragmentDescriptor extends DeclarationDescriptorImpl implements PackageFragmentDescriptor {

    private final ModuleDescriptor module;
    private final FqName fqName;
    private final WritableScope scope;
    private final NamespaceLikeBuilder builder;

    public MutablePackageFragmentDescriptor(
            @NotNull ModuleDescriptor module,
            @NotNull FqName fqName
    ) {
        super(Collections.<AnnotationDescriptor>emptyList(), fqName.shortNameOrSpecial());
        this.module = module;
        this.fqName = fqName;

        scope = new WritableScopeImpl(JetScope.EMPTY, this, RedeclarationHandler.DO_NOTHING, "Members of " + fqName + " in " + module);
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);
        builder = new ScopeBasedNamespaceLikeBuilder(this, scope);
    }

    @NotNull
    @Override
    public ModuleDescriptor getContainingDeclaration() {
        return module;
    }

    @NotNull
    @Override
    public FqName getFqName() {
        return fqName;
    }

    @NotNull
    @Override
    public WritableScope getMemberScope() {
        return scope;
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
    public NamespaceLikeBuilder getBuilder() {
        return builder;
    }
}

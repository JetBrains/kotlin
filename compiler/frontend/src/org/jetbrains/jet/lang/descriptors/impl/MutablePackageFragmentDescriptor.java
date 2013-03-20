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
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;

public class MutablePackageFragmentDescriptor extends PackageLikeDescriptorBase implements PackageFragmentDescriptor {

    private final PackageFragmentKind kind;
    private final SubModuleDescriptor subModuleDescriptor;
    private final WritableScope scope;
    private final NamespaceLikeBuilder builder;

    public MutablePackageFragmentDescriptor(
            @NotNull SubModuleDescriptor subModuleDescriptor,
            @NotNull PackageFragmentKind kind,
            @NotNull FqName fqName
    ) {
        super(fqName);
        this.subModuleDescriptor = subModuleDescriptor;
        this.kind = kind;
        this.scope = new WritableScopeImpl(JetScope.EMPTY, this, RedeclarationHandler.DO_NOTHING,
                                           "Members of " + fqName + " in " + subModuleDescriptor);
        scope.changeLockLevel(WritableScope.LockLevel.BOTH);
        this.builder = new ScopeBasedNamespaceLikeBuilder(this, scope);
    }

    @NotNull
    @Override
    public PackageFragmentKind getKind() {
        return kind;
    }

    @NotNull
    @Override
    public SubModuleDescriptor getContainingDeclaration() {
        return subModuleDescriptor;
    }

    @NotNull
    @Override
    public WritableScope getMemberScope() {
        return scope;
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

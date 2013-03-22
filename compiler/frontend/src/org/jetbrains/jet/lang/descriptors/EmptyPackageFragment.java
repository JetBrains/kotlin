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

package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.impl.PackageLikeDescriptorBase;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

public class EmptyPackageFragment extends PackageLikeDescriptorBase implements PackageFragmentDescriptor {
    private final SubModuleDescriptor subModule;
    private final PackageFragmentKind kind;

    public EmptyPackageFragment(@NotNull SubModuleDescriptor subModule, @NotNull PackageFragmentKind kind, @NotNull FqName fqName) {
        super(fqName);
        this.subModule = subModule;
        this.kind = kind;
    }

    @NotNull
    @Override
    public SubModuleDescriptor getContainingDeclaration() {
        return subModule;
    }

    @NotNull
    @Override
    public PackageFragmentKind getKind() {
        return kind;
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return JetScope.EMPTY;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPackageFragmentDescriptor(this, data);
    }
}

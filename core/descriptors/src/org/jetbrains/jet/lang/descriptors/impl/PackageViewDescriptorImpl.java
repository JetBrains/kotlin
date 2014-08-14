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
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.ChainedScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.ArrayList;
import java.util.List;

public class PackageViewDescriptorImpl extends DeclarationDescriptorImpl implements PackageViewDescriptor {
    private final ModuleDescriptor module;
    private final FqName fqName;
    private final JetScope memberScope;

    public PackageViewDescriptorImpl(
            @NotNull ModuleDescriptor module,
            @NotNull FqName fqName,
            @NotNull List<PackageFragmentDescriptor> fragments
    ) {
        super(Annotations.EMPTY, fqName.shortNameOrSpecial());
        this.module = module;
        this.fqName = fqName;

        List<JetScope> scopes = new ArrayList<JetScope>(fragments.size() + 1);
        assert !fragments.isEmpty() : fqName + " in " + module;
        for (PackageFragmentDescriptor fragment : fragments) {
            scopes.add(fragment.getMemberScope());
        }
        scopes.add(new SubpackagesScope(this));

        memberScope = new ChainedScope(this, "package view scope for " + fqName + " in " + module.getName(),
                                       scopes.toArray(new JetScope[scopes.size()]));
    }

    @Nullable
    @Override
    public PackageViewDescriptor getContainingDeclaration() {
        return fqName.isRoot() ? null : module.getPackage(fqName.parent());
    }

    @Nullable
    @Override
    public DeclarationDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        return this;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPackageViewDescriptor(this, data);
    }

    @NotNull
    @Override
    public FqName getFqName() {
        return fqName;
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return memberScope;
    }

    @Override
    @NotNull
    public ModuleDescriptor getModule() {
        return module;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PackageViewDescriptorImpl that = (PackageViewDescriptorImpl) o;

        if (!fqName.equals(that.fqName)) return false;
        if (!module.equals(that.module)) return false;

        return true;
    }


    @Override
    public int hashCode() {
        int result = module.hashCode();
        result = 31 * result + fqName.hashCode();
        return result;
    }
}

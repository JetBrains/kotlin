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
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collections;
import java.util.List;

public abstract class AbstractSubModuleDescriptor extends DeclarationDescriptorImpl implements SubModuleDescriptor {
    private final ModuleDescriptor module;

    public AbstractSubModuleDescriptor(@NotNull ModuleDescriptor module, @NotNull Name name) {
        super(Collections.<AnnotationDescriptor>emptyList(), name);
        this.module = module;
    }

    @Nullable
    @Override
    public PackageViewDescriptor getPackageView(@NotNull FqName fqName) {
        List<PackageFragmentDescriptor> fragments = DescriptorUtils.getPackageFragmentsIncludingDependencies(this, fqName);
        if (fragments.isEmpty()) return null;

        return new PackageViewFromSubModule(this, fqName, fragments);
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
        return visitor.visitSubModuleDescriptor(this, data);
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        visitor.visitSubModuleDescriptor(this, null);
    }
}

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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collection;
import java.util.Collections;

public class MutableModuleDescriptor extends DeclarationDescriptorImpl implements ModuleDescriptor {

    private final Collection<SubModuleDescriptor> subModules = Lists.newArrayList();
    private final PlatformToKotlinClassMap platformToKotlinClassMap;

    public MutableModuleDescriptor(@NotNull Name name, @NotNull PlatformToKotlinClassMap platformToKotlinClassMap) {
        super(Collections.<AnnotationDescriptor>emptyList(), name);
        this.platformToKotlinClassMap = platformToKotlinClassMap;
    }

    @NotNull
    @Override
    public Collection<SubModuleDescriptor> getSubModules() {
        return subModules;
    }

    public void addSubModule(@NotNull SubModuleDescriptor subModule) {
        subModules.add(subModule);
    }

    @Nullable
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return null;
    }

    @NotNull
    @Override
    public PlatformToKotlinClassMap getPlatformToKotlinClassMap() {
        return platformToKotlinClassMap;
    }

    @Nullable
    @Override
    public DeclarationDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        return this;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitModuleDescriptor(this, data);
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        visitor.visitModuleDescriptor(this, null);
    }
}

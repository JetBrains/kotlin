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

package org.jetbrains.jet.lang.resolve.java.descriptor;

import com.intellij.util.NullableFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.DeclarationDescriptorImpl;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaPackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.java.scope.JavaPackageFragmentScope;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collections;

public class JavaPackageFragmentDescriptor extends DeclarationDescriptorImpl implements PackageFragmentDescriptor {
    private final JavaPackageFragmentProvider provider;
    private final FqName fqName;
    private final JavaPackageFragmentScope memberScope;

    private JavaPackageFragmentDescriptor(
            @NotNull JavaPackageFragmentProvider provider,
            @NotNull FqName fqName,
            @NotNull NullableFunction<JavaPackageFragmentDescriptor, JavaPackageFragmentScope> scopeFactory
    ) {
        super(Collections.<AnnotationDescriptor>emptyList(), fqName.shortNameOrSpecial());
        this.provider = provider;
        this.fqName = fqName;
        this.memberScope = scopeFactory.fun(this);
    }

    @Nullable
    public static JavaPackageFragmentDescriptor create(
            @NotNull JavaPackageFragmentProvider provider,
            @NotNull FqName fqName,
            @NotNull NullableFunction<JavaPackageFragmentDescriptor, JavaPackageFragmentScope> scopeFactory
    ) {
        JavaPackageFragmentDescriptor descriptor = new JavaPackageFragmentDescriptor(provider, fqName, scopeFactory);
        return descriptor.memberScope == null ? null : descriptor;
    }

    @NotNull
    @Override
    public ModuleDescriptor getContainingDeclaration() {
        return provider.getModule();
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
    public JavaPackageFragmentScope getMemberScope() {
        return memberScope;
    }

    @NotNull
    @Override
    public FqName getFqName() {
        return fqName;
    }

    @NotNull
    public JavaPackageFragmentProvider getProvider() {
        return provider;
    }
}

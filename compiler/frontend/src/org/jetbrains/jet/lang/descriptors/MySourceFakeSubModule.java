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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collection;
import java.util.List;

/**
 * The order of dependencies matters, in particular, it is important what dependencies come before
 * the sources of the subModule, and what come later. The instance of this class in the dependency list
 * identifies the position of the subModule itself in the resolution of packages. E.g.:
 *
 *   sm0
 *     lib1
 *     MY_SOURCES
 *     sm2
 *
 * Means that package fragments from lib1 come first, then come fragments from sm0 (identified by MY_SOURCES), and then sm2
 *
 */
/*package*/ class MySourceFakeSubModule implements SubModuleDescriptor {
    public static final SubModuleDescriptor MY_SOURCE = new MySourceFakeSubModule();

    private MySourceFakeSubModule() {
    }

    @NotNull
    @Override
    public ModuleDescriptor getContainingDeclaration() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public PackageFragmentProvider getPackageFragmentProvider() {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public PackageViewDescriptor getPackageView(@NotNull FqName fqName) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Collection<SubModuleDescriptor> getDependencies() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public List<ImportPath> getDefaultImports() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getOriginal() {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public DeclarationDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Name getName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "MY_SOURCE";
    }
}

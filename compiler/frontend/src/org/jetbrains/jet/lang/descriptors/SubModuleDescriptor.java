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
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.List;

/**
 * Submodules roughly correspond to root types, i.e. 'src" and 'test'
 */
public interface SubModuleDescriptor extends DeclarationDescriptor, PackageFragmentProvider {

    SubModuleDescriptor MY_SOURCE = MySourceFakeSubModule.MY_SOURCE;

    @NotNull
    @Override
    ModuleDescriptor getContainingDeclaration();

    /**
     * @return package fragments for the given fqName as declared in this submodule
     */
    @Override
    @NotNull
    List<PackageFragmentDescriptor> getPackageFragments(@NotNull FqName fqName);

    /**
     * @return a package as seen from this submodule (i.e. including its own declarations and immediate dependencies)
     *         {@code null} is this package does not exist from the point of view of this submodule
     */
    @Nullable
    PackageViewDescriptor getPackageView(@NotNull FqName fqName);

    @NotNull
    Collection<SubModuleDescriptor> getDependencies();

    @NotNull
    List<ImportPath> getDefaultImports();
}

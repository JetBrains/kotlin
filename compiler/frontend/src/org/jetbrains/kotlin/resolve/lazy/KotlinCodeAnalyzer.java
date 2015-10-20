/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.lazy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.incremental.components.LookupLocation;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.resolve.BindingContext;

public interface KotlinCodeAnalyzer extends TopLevelDescriptorProvider {

    @NotNull
    ModuleDescriptor getModuleDescriptor();

    @NotNull
    ClassDescriptor getClassDescriptor(@NotNull KtClassOrObject classOrObject, @NotNull LookupLocation location);

    @NotNull
    BindingContext getBindingContext();

    @NotNull
    DeclarationDescriptor resolveToDescriptor(@NotNull KtDeclaration declaration);

    @NotNull
    DeclarationScopeProvider getDeclarationScopeProvider();

    @NotNull
    FileScopeProvider getFileScopeProvider();

    /**
     * Forces all descriptors to be resolved.
     *
     * Use this method when laziness plays against you, e.g. when lazy descriptors may be accessed in a multi-threaded setting
     */
    void forceResolveAll();

    @NotNull
    PackageFragmentProvider getPackageFragmentProvider();
}

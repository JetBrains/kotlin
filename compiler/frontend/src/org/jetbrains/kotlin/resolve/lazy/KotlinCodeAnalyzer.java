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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetScript;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor;

import java.util.Collection;

public interface KotlinCodeAnalyzer {
    @Nullable
    LazyPackageDescriptor getPackageFragment(@NotNull FqName fqName);

    @NotNull
    ModuleDescriptor getModuleDescriptor();

    @NotNull
    @ReadOnly
    Collection<ClassDescriptor> getTopLevelClassDescriptors(@NotNull FqName fqName);

    @NotNull
    ClassDescriptor getClassDescriptor(@NotNull JetClassOrObject classOrObject);

    @NotNull
    ScriptDescriptor getScriptDescriptor(@NotNull JetScript script);

    @NotNull
    BindingContext getBindingContext();

    @NotNull
    DeclarationDescriptor resolveToDescriptor(@NotNull JetDeclaration declaration);

    @NotNull
    ScopeProvider getScopeProvider();

    /**
     * Forces all descriptors to be resolved.
     *
     * Use this method when laziness plays against you, e.g. when lazy descriptors may be accessed in a multi-threaded setting
     */
    void forceResolveAll();

    @NotNull
    PackageFragmentProvider getPackageFragmentProvider();
}

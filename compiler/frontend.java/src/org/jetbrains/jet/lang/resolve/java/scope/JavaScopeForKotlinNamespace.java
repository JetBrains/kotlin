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

package org.jetbrains.jet.lang.resolve.java.scope;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.java.JavaClassClassResolutionFacade;
import org.jetbrains.jet.lang.resolve.java.provider.KotlinPackagePsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.Set;

public final class JavaScopeForKotlinNamespace extends JavaPackageScope {
    @NotNull
    private final KotlinPackagePsiDeclarationProvider declarationProvider;

    public JavaScopeForKotlinNamespace(
            @NotNull PackageFragmentDescriptor descriptor,
            @NotNull KotlinPackagePsiDeclarationProvider declarationProvider,
            @NotNull FqName packageFQN,
            @NotNull JavaClassClassResolutionFacade javaClassClassResolutionFacade
    ) {
        super(descriptor, declarationProvider, packageFQN, javaClassClassResolutionFacade);
        this.declarationProvider = declarationProvider;
    }

    @NotNull
    @Override
    protected Set<FunctionDescriptor> computeFunctionDescriptor(@NotNull Name name) {
        return javaDescriptorResolver.resolveFunctionGroup(name, declarationProvider, descriptor);
    }

    @NotNull
    @Override
    protected Collection<ClassDescriptor> computeInnerClasses() {
        return javaDescriptorResolver.resolveInnerClasses(declarationProvider);
    }
}

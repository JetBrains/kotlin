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

package org.jetbrains.kotlin.codegen.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.org.objectweb.asm.Type;

public class PackageFacadeContext extends PackageContext implements DelegatingFacadeContext {
    private final Type publicFacadeType;

    public PackageFacadeContext(
            @NotNull PackageFragmentDescriptor contextDescriptor,
            @NotNull CodegenContext parent,
            @NotNull Type packagePartType
    ) {
        this(contextDescriptor, parent, packagePartType, packagePartType);
    }

    public PackageFacadeContext(
            @NotNull PackageFragmentDescriptor contextDescriptor,
            @NotNull CodegenContext parent,
            @NotNull Type packagePartType,
            @NotNull Type publicFacadeType
    ) {
        super(contextDescriptor, parent, packagePartType, null);

        this.publicFacadeType = publicFacadeType;
    }

    @Override
    @Nullable
    public Type getDelegateToClassType() {
        return getPackagePartType();
    }

    public Type getPublicFacadeType() {
        return publicFacadeType;
    }
}

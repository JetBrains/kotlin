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
import org.jetbrains.kotlin.codegen.OwnerKind;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.org.objectweb.asm.Type;

public class PackageContext extends FieldOwnerContext<PackageFragmentDescriptor> implements DelegatingToPartContext, FacadePartWithSourceFile {
    private final Type packagePartType;
    private final KtFile sourceFile;

    public PackageContext(
            @NotNull PackageFragmentDescriptor contextDescriptor,
            @NotNull CodegenContext parent,
            @Nullable Type packagePartType,
            @Nullable KtFile sourceFile
    ) {
        super(contextDescriptor, OwnerKind.PACKAGE, parent, null, null, null);
        this.packagePartType = packagePartType;
        this.sourceFile = sourceFile;
    }

    @Override
    public String toString() {
        return "Package: " + getContextDescriptor().getName();
    }

    @Nullable
    @Override
    public Type getImplementationOwnerClassType() {
        return packagePartType;
    }

    @Nullable
    @Override
    public KtFile getSourceFile() {
        return sourceFile;
    }
}

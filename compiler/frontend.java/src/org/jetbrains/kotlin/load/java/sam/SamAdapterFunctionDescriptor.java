/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.sam;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor;
import org.jetbrains.kotlin.name.Name;

/* package */ class SamAdapterFunctionDescriptor extends JavaMethodDescriptor implements SamAdapterDescriptor<JavaMethodDescriptor> {
    private final JavaMethodDescriptor declaration;

    public SamAdapterFunctionDescriptor(@NotNull JavaMethodDescriptor declaration) {
        this(declaration.getContainingDeclaration(), null, Kind.SYNTHESIZED, declaration);
    }

    private SamAdapterFunctionDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable SimpleFunctionDescriptor original,
            @NotNull Kind kind,
            @NotNull JavaMethodDescriptor declaration
    ) {
        super(containingDeclaration, original, declaration.getAnnotations(), declaration.getName(), kind, declaration.getSource());
        this.declaration = declaration;
        setParameterNamesStatus(declaration.hasStableParameterNames(), declaration.hasSynthesizedParameterNames());
    }

    @NotNull
    @Override
    protected JavaMethodDescriptor createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind,
            @Nullable Name newName,
            @NotNull Annotations annotations,
            @NotNull SourceElement source
    ) {
        return new SamAdapterFunctionDescriptor(newOwner, (SimpleFunctionDescriptor) original, kind, declaration);
    }

    @NotNull
    @Override
    public JavaMethodDescriptor getBaseDescriptorForSynthetic() {
        return declaration;
    }
}

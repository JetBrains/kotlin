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
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassConstructorDescriptor;

/* package */ class SamAdapterClassConstructorDescriptor extends JavaClassConstructorDescriptor
        implements SamAdapterDescriptor<JavaClassConstructorDescriptor> {
    private final JavaClassConstructorDescriptor declaration;

    public SamAdapterClassConstructorDescriptor(@NotNull JavaClassConstructorDescriptor declaration) {
        super(declaration.getContainingDeclaration(), null, declaration.getAnnotations(),
              declaration.isPrimary(), Kind.SYNTHESIZED, declaration.getSource());
        this.declaration = declaration;
        setHasStableParameterNames(declaration.hasStableParameterNames());
        setHasSynthesizedParameterNames(declaration.hasSynthesizedParameterNames());
    }

    private SamAdapterClassConstructorDescriptor(
            @NotNull ClassDescriptor containingDeclaration,
            @Nullable JavaClassConstructorDescriptor original,
            @NotNull Annotations annotations,
            boolean isPrimary,
            @NotNull Kind kind,
            @NotNull SourceElement source,
            @NotNull JavaClassConstructorDescriptor declaration
    ) {
        super(containingDeclaration, original, annotations, isPrimary, kind, source);
        this.declaration = declaration;
    }

    @NotNull
    @Override
    protected JavaClassConstructorDescriptor createDescriptor(
            @NotNull ClassDescriptor newOwner,
            @Nullable JavaClassConstructorDescriptor original,
            @NotNull Kind kind,
            @NotNull SourceElement sourceElement,
            @NotNull Annotations annotations
    ) {
        return new SamAdapterClassConstructorDescriptor(newOwner, original, annotations, isPrimary, kind, sourceElement, declaration);
    }

    @NotNull
    @Override
    public JavaClassConstructorDescriptor getBaseDescriptorForSynthetic() {
        return declaration;
    }
}

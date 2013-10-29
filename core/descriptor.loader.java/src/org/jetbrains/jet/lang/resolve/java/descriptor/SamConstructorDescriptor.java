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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.SynthesizedCallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;

public class SamConstructorDescriptor extends SimpleFunctionDescriptorImpl
        implements SynthesizedCallableMemberDescriptor<JavaClassDescriptor> {
    private final JavaClassDescriptor samInterface;

    public SamConstructorDescriptor(
            @NotNull ClassOrNamespaceDescriptor containingDeclaration,
            @NotNull JavaClassDescriptor samInterface
    ) {
        super(containingDeclaration, samInterface.getAnnotations(), samInterface.getName(), Kind.SYNTHESIZED);
        this.samInterface = samInterface;
    }

    @NotNull
    @Override
    public JavaClassDescriptor getBaseForSynthesized() {
        return samInterface;
    }
}

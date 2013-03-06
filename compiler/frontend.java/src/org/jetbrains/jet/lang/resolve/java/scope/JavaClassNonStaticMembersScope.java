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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.lang.resolve.java.provider.ClassPsiDeclarationProvider;

import java.util.Collection;

public final class JavaClassNonStaticMembersScope extends JavaClassMembersScope {

    private Collection<ConstructorDescriptor> constructors = null;
    private ConstructorDescriptor primaryConstructor = null;
    @NotNull
    private final ClassDescriptor descriptor;

    public JavaClassNonStaticMembersScope(
            @NotNull ClassDescriptor descriptor,
            @NotNull ClassPsiDeclarationProvider psiDeclarationProvider,
            @NotNull JavaSemanticServices semanticServices
    ) {
        super(descriptor, psiDeclarationProvider, semanticServices);
        this.descriptor = descriptor;
    }


    @NotNull
    public Collection<ConstructorDescriptor> getConstructors() {
        initConstructorsIfNeeded();
        return constructors;
    }

    @Nullable
    public ConstructorDescriptor getPrimaryConstructor() {
        initConstructorsIfNeeded();
        return primaryConstructor;
    }

    private void initConstructorsIfNeeded() {
        if (constructors == null) {
            constructors = javaDescriptorResolver.resolveConstructors(declarationProvider, descriptor);

            for (ConstructorDescriptor constructor : constructors) {
                if (constructor.isPrimary()) {
                    if (primaryConstructor != null) {
                        throw new IllegalStateException(
                                "Class has more than one primary constructor: " + primaryConstructor + "\n" + constructor);
                    }
                    primaryConstructor = constructor;
                }
            }
        }
    }
}

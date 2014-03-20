/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.ConstructorDescriptorImpl;

public class JavaConstructorDescriptor extends ConstructorDescriptorImpl {
    private Boolean hasStableParameterNames;

    public JavaConstructorDescriptor(
            @NotNull ClassDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            boolean isPrimary
    ) {
        super(containingDeclaration, annotations, isPrimary);
    }

    private JavaConstructorDescriptor(
            @NotNull ClassDescriptor containingDeclaration,
            @NotNull JavaConstructorDescriptor original,
            @NotNull Annotations annotations,
            boolean isPrimary
    ) {
        super(containingDeclaration, original, annotations, isPrimary);
    }

    @Override
    public boolean hasStableParameterNames() {
        assert hasStableParameterNames != null : "hasStableParameterNames was not set: " + this;
        return hasStableParameterNames;
    }

    public void setHasStableParameterNames(boolean hasStableParameterNames) {
        this.hasStableParameterNames = hasStableParameterNames;
    }

    @Override
    protected JavaConstructorDescriptor createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal, Kind kind) {
        if (kind != Kind.DECLARATION) {
            throw new IllegalStateException("Attempt at creating a constructor that is not a declaration: \n" +
                                            "copy from: " + this + "\n" +
                                            "newOwner: " + newOwner + "\n" +
                                            "kind: " + kind);
        }
        JavaConstructorDescriptor result =
                new JavaConstructorDescriptor((ClassDescriptor) newOwner, this, Annotations.EMPTY /* TODO */, isPrimary);
        result.setHasStableParameterNames(hasStableParameterNames());
        return result;
    }
}

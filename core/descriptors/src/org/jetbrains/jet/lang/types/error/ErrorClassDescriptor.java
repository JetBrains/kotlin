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

package org.jetbrains.jet.lang.types.error;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ConstructorDescriptorImpl;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.types.ErrorUtils.*;

public final class ErrorClassDescriptor extends ClassDescriptorImpl {
    public ErrorClassDescriptor(@NotNull String debugMessage) {
        super(getErrorModule(), Name.special("<ERROR CLASS: " + debugMessage + ">"), Modality.OPEN, Collections.<JetType>emptyList());

        ConstructorDescriptorImpl errorConstructor =
                new ConstructorDescriptorImpl(this, Collections.<AnnotationDescriptor>emptyList(), true);

        errorConstructor.initialize(
                Collections.<TypeParameterDescriptor>emptyList(), // TODO
                Collections.<ValueParameterDescriptor>emptyList(), // TODO
                Visibilities.INTERNAL
        );
        errorConstructor.setReturnType(createErrorType("<ERROR RETURN TYPE>"));

        initialize(createErrorScope("ERROR_CLASS"), Collections.<ConstructorDescriptor>singleton(errorConstructor), errorConstructor);
    }

    @NotNull
    @Override
    public ClassDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        return this;
    }

    @Override
    public String toString() {
        return getName().asString();
    }

    @NotNull
    @Override
    public JetScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments) {
        return createErrorScope("Error scope for class " + getName() + " with arguments: " + typeArguments);
    }
}

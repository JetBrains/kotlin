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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ClassDescriptorImpl;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.jet.lang.types.ErrorUtils.*;

public final class ErrorClassDescriptor extends ClassDescriptorImpl {
    public ErrorClassDescriptor(@NotNull String debugMessage) {
        super(ErrorUtils.getErrorModule(), Collections.<AnnotationDescriptor>emptyList(), Modality.OPEN,
              Name.special("<ERROR CLASS: " + debugMessage + ">"));
    }

    @NotNull
    @Override
    public Collection<ConstructorDescriptor> getConstructors() {
        return getErrorConstructorGroup();
    }

    @NotNull
    @Override
    public Modality getModality() {
        return Modality.OPEN;
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

    public void initializeErrorClass() {
        initialize(true, Collections.<TypeParameterDescriptor>emptyList(), Collections.<JetType>emptyList(),
                   createErrorScope("ERROR_CLASS"), getErrorConstructorGroup(), getErrorConstructor(), false);
    }
}

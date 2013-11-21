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

package org.jetbrains.jet.lang.resolve.constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.types.TypeConstructor;

import java.util.Collections;
import java.util.List;

public abstract class NumberValueTypeConstructor implements TypeConstructor {
    @NotNull
    @Override
    public List<TypeParameterDescriptor> getParameters() {
        return Collections.emptyList();
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public boolean isDenotable() {
        return false;
    }

    @Nullable
    @Override
    public ClassifierDescriptor getDeclarationDescriptor() {
        return null;
    }

    @NotNull
    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return Collections.emptyList();
    }
}

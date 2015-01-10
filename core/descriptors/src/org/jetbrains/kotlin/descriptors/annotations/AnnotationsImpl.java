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

package org.jetbrains.kotlin.descriptors.annotations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.kotlin.name.FqName;

import java.util.Iterator;
import java.util.List;

public class AnnotationsImpl implements Annotations {
    private final List<AnnotationDescriptor> annotations;

    public AnnotationsImpl(@NotNull List<AnnotationDescriptor> annotations) {
        this.annotations = annotations;
    }

    @NotNull
    public List<AnnotationDescriptor> getAnnotationDescriptors() {
        return annotations;
    }

    @Override
    public boolean isEmpty() {
        return getAnnotationDescriptors().isEmpty();
    }

    @Nullable
    @Override
    public AnnotationDescriptor findAnnotation(@NotNull FqName fqName) {
        for (AnnotationDescriptor annotation : annotations) {
            ClassifierDescriptor descriptor = annotation.getType().getConstructor().getDeclarationDescriptor();
            if (descriptor instanceof ClassDescriptor && fqName.toUnsafe().equals(DescriptorUtils.getFqName(descriptor))) {
                return annotation;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Iterator<AnnotationDescriptor> iterator() {
        return getAnnotationDescriptors().iterator();
    }
}

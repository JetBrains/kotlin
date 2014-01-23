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

package org.jetbrains.jet.lang.descriptors.annotations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collections;
import java.util.Iterator;

public interface Annotations extends Iterable<AnnotationDescriptor> {
    Annotations EMPTY = new Annotations() {
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Nullable
        @Override
        public AnnotationDescriptor findAnnotation(@NotNull FqName fqName) {
            return null;
        }

        @NotNull
        @Override
        public Iterator<AnnotationDescriptor> iterator() {
            return Collections.<AnnotationDescriptor>emptyList().iterator();
        }
    };

    boolean isEmpty();

    @Nullable
    AnnotationDescriptor findAnnotation(@NotNull FqName fqName);
}

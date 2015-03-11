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

package org.jetbrains.kotlin.load.java.components;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation;
import org.jetbrains.kotlin.load.java.structure.JavaAnnotationOwner;
import org.jetbrains.kotlin.name.FqName;

import java.util.Collection;
import java.util.Collections;

public interface ExternalAnnotationResolver {
    ExternalAnnotationResolver EMPTY = new ExternalAnnotationResolver() {
        @Nullable
        @Override
        public JavaAnnotation findExternalAnnotation(@NotNull JavaAnnotationOwner owner, @NotNull FqName fqName) {
            return null;
        }

        @NotNull
        @Override
        public Collection<JavaAnnotation> findExternalAnnotations(@NotNull JavaAnnotationOwner owner) {
            return Collections.emptyList();
        }
    };

    @Nullable
    JavaAnnotation findExternalAnnotation(@NotNull JavaAnnotationOwner owner, @NotNull FqName fqName);

    @NotNull
    Collection<JavaAnnotation> findExternalAnnotations(@NotNull JavaAnnotationOwner owner);
}

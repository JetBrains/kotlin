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

package org.jetbrains.jet.lang.resolve.java.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.name.FqName;

import java.util.Collection;

public interface JavaClass extends JavaClassifier, JavaTypeParameterListOwner, JavaModifierListOwner, JavaAnnotationOwner {
    @NotNull
    Collection<JavaClass> getInnerClasses();

    @Nullable
    FqName getFqName();

    boolean isInterface();

    boolean isAnnotationType();

    boolean isEnum();

    @Nullable
    JavaClass getOuterClass();

    @NotNull
    Collection<JavaClassifierType> getSupertypes();

    @NotNull
    Collection<JavaMethod> getMethods();

    @NotNull
    Collection<JavaField> getFields();

    @NotNull
    Collection<JavaConstructor> getConstructors();

    @NotNull
    JavaClassifierType getDefaultType();

    @NotNull
    OriginKind getOriginKind();

    @NotNull
    JavaType createImmediateType(@NotNull JavaTypeSubstitutor substitutor);

    enum OriginKind {
        COMPILED,
        SOURCE,
        KOTLIN_LIGHT_CLASS
    }
}

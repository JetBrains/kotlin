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
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

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
    Collection<JavaMethod> getAllMethods();

    @NotNull
    Collection<JavaField> getFields();

    @NotNull
    Collection<JavaField> getAllFields();

    /**
     * Searches this class only, no base classes are searched
     */
    @Nullable
    JavaField findDeclaredFieldByName(@NotNull Name name);

    @NotNull
    Collection<JavaMethod> getConstructors();

    @NotNull
    JavaClassifierType getDefaultType();

    @NotNull
    OriginKind getOriginKind();

    enum OriginKind {
        COMPILED,
        SOURCE,
        KOTLIN_LIGHT_CLASS
    }
}

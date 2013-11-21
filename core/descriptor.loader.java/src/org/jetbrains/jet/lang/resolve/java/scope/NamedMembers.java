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
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaField;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.List;

public final class NamedMembers {

    public NamedMembers(@NotNull Name name) {
        this.name = name;
    }

    @NotNull
    private final Name name;

    @NotNull
    private final List<JavaMethod> methods = new ArrayList<JavaMethod>();

    @NotNull
    private final List<JavaFieldData> fields = new ArrayList<JavaFieldData>();

    @Nullable
    private JavaClass samInterface;

    void addMethod(@NotNull JavaMethod method) {
        methods.add(method);
    }

    void addField(@NotNull JavaField field, boolean staticFieldCopiedFromSuperClass) {
        fields.add(new JavaFieldData(field, staticFieldCopiedFromSuperClass));
    }

    void setSamInterface(@NotNull JavaClass samInterface) {
        this.samInterface = samInterface;
    }

    @NotNull
    public Name getName() {
        return name;
    }

    @NotNull
    public List<JavaMethod> getMethods() {
        return methods;
    }

    @NotNull
    public List<JavaFieldData> getFields() {
        return fields;
    }

    @Nullable
    public JavaClass getSamInterface() {
        return samInterface;
    }

    public static class JavaFieldData {
        private final JavaField field;
        private final boolean staticFieldCopiedFromSuperClass;

        public JavaFieldData(@NotNull JavaField field, boolean staticFieldCopiedFromSuperClass) {
            this.field = field;
            this.staticFieldCopiedFromSuperClass = staticFieldCopiedFromSuperClass;
        }

        @NotNull
        public JavaField getField() {
            return field;
        }

        public boolean isStaticFieldCopiedFromSuperClass() {
            return staticFieldCopiedFromSuperClass;
        }
    }
}

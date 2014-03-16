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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

public class EnumValue extends CompileTimeConstant<ClassDescriptor> {

    public EnumValue(@NotNull ClassDescriptor value) {
        super(value, true);
    }

    @NotNull
    @Override
    public JetType getType(@NotNull KotlinBuiltIns kotlinBuiltIns) {
        JetType type = value.getClassObjectType();
        assert type != null : "Enum entry should have a class object: " + value;
        return type;
    }

    @NotNull
    @Override
    public ClassDescriptor getValue() {
        ClassDescriptor value = super.getValue();
        assert value != null : "Guaranteed by constructor";
        return value;
    }

    @Override
    public <R, D> R accept(AnnotationArgumentVisitor<R, D> visitor, D data) {
        return visitor.visitEnumValue(this, data);
    }

    @Override
    public String toString() {
        return getType(KotlinBuiltIns.getInstance()) + "." + value.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return value.equals(((EnumValue) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}


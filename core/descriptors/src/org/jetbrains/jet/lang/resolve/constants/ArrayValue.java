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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.kotlin.types.JetType;

import java.util.List;

public class ArrayValue extends CompileTimeConstant<List<CompileTimeConstant<?>>> {

    private final JetType type;

    public ArrayValue(@NotNull List<CompileTimeConstant<?>> value,
                      @NotNull JetType type,
                      boolean canBeUsedInAnnotations,
                      boolean usesVariableAsConstant) {
        super(value, canBeUsedInAnnotations, false, usesVariableAsConstant);
        assert KotlinBuiltIns.getInstance().isArray(type) ||
               KotlinBuiltIns.getInstance().isPrimitiveArray(type) : "Type should be an array, but was " + type + ": " + value;
        this.type = type;
    }

    @NotNull
    @Override
    public List<CompileTimeConstant<?>> getValue() {
        List<CompileTimeConstant<?>> value = super.getValue();
        assert value != null : "Guaranteed by constructor";
        return value;
    }

    @NotNull
    @Override
    public JetType getType(@NotNull KotlinBuiltIns kotlinBuiltIns) {
        return type;
    }

    @Override
    public <R, D> R accept(AnnotationArgumentVisitor<R, D> visitor, D data) {
        return visitor.visitArrayValue(this, data);
    }

    @Override
    public String toString() {
       return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArrayValue that = (ArrayValue) o;

        if (value == null) {
            return that.value == null;
        }

        int i = 0;
        for (Object thisObject : value) {
            if (!thisObject.equals(that.value.get(i))) {
                return false;
            }
            i++;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        if (value == null) return hashCode;
        for (Object o : value) {
            hashCode += o.hashCode();
        }
        return hashCode;
    }
}


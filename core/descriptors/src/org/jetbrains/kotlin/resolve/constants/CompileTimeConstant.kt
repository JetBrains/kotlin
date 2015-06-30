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

package org.jetbrains.kotlin.resolve.constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.kotlin.types.JetType;

public abstract class CompileTimeConstant<T> {
    protected final T value;
    private final int flags;

   /*
    * if is pure is false then constant type cannot be changed
    * ex1. val a: Long = 1.toInt() (TYPE_MISMATCH error, 1.toInt() isn't pure)
    * ex2. val b: Int = a (TYPE_MISMATCH error, a isn't pure)
    *
    */
    private static final int IS_PURE_MASK = 1;
    private static final int CAN_BE_USED_IN_ANNOTATIONS_MASK = 1 << 1;
    private static final int USES_VARIABLE_AS_CONSTANT_MASK = 1 << 2;

    protected CompileTimeConstant(T value,
                                  boolean canBeUsedInAnnotations,
                                  boolean isPure,
                                  boolean usesVariableAsConstant) {
        this.value = value;
        flags = (isPure ? IS_PURE_MASK : 0) |
                (canBeUsedInAnnotations ? CAN_BE_USED_IN_ANNOTATIONS_MASK : 0) |
                (usesVariableAsConstant ? USES_VARIABLE_AS_CONSTANT_MASK : 0);
    }

    public boolean canBeUsedInAnnotations() {
        return (flags & CAN_BE_USED_IN_ANNOTATIONS_MASK) != 0;
    }

    public boolean isPure() {
        return (flags & IS_PURE_MASK) != 0;
    }

    public boolean usesVariableAsConstant() {
        return (flags & USES_VARIABLE_AS_CONSTANT_MASK) != 0;
    }

    @Nullable
    public T getValue() {
        return value;
    }

    @NotNull
    public abstract JetType getType(@NotNull KotlinBuiltIns kotlinBuiltIns);

    public abstract <R, D> R accept(AnnotationArgumentVisitor<R, D> visitor, D data);
}

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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.types.JetType;

public class AnnotationValue extends CompileTimeConstant<AnnotationDescriptor> {
    
    public AnnotationValue(@NotNull AnnotationDescriptor value) {
        super(value, true, false, false);
    }

    @NotNull
    @Override
    public AnnotationDescriptor getValue() {
        AnnotationDescriptor value = super.getValue();
        assert value != null : "Guaranteed by constructor";
        return value;
    }

    @Override
    @NotNull
    public JetType getType(@NotNull KotlinBuiltIns kotlinBuiltIns) {
        return value.getType();
    }

    @Override
    public <R, D> R accept(AnnotationArgumentVisitor<R, D> visitor, D data) {
        return visitor.visitAnnotationValue(this, data);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

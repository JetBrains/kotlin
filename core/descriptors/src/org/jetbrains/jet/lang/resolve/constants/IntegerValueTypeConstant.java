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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;

import java.util.Collections;

public class IntegerValueTypeConstant extends IntegerValueConstant<Number> {

    private final IntegerValueTypeConstructor typeConstructor;

    public IntegerValueTypeConstant(@NotNull Number value, boolean canBeUsedInAnnotations) {
        super(value, canBeUsedInAnnotations, true);
        this.typeConstructor = new IntegerValueTypeConstructor(value.longValue());
    }

    @NotNull
    @Override
    public JetType getType(@NotNull KotlinBuiltIns kotlinBuiltIns) {
        return new JetTypeImpl(
                Annotations.EMPTY, typeConstructor,
                false, Collections.<TypeProjection>emptyList(),
                ErrorUtils.createErrorScope("Scope for number value type (" + typeConstructor.toString() + ")", true));
    }

    @Nullable
    @Override
    @Deprecated
    public Number getValue() {
        throw new UnsupportedOperationException("Use IntegerValueTypeConstant.getValue(expectedType) instead");
    }

    @NotNull
    public JetType getType(@NotNull JetType expectedType) {
        return TypeUtils.getPrimitiveNumberType(typeConstructor, expectedType);
    }

    @NotNull
    public Number getValue(@NotNull JetType expectedType) {
        Number numberValue = typeConstructor.getValue();
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();

        JetType valueType = getType(expectedType);
        if (valueType.equals(builtIns.getIntType())) {
            return numberValue.intValue();
        }
        else if (valueType.equals(builtIns.getByteType())) {
            return numberValue.byteValue();
        }
        else if (valueType.equals(builtIns.getShortType())) {
            return numberValue.shortValue();
        }
        else {
            return numberValue.longValue();
        }
    }

    @Override
    public <R, D> R accept(AnnotationArgumentVisitor<R, D> visitor, D data) {
        return visitor.visitNumberTypeValue(this, data);
    }

    @Override
    public String toString() {
        return typeConstructor.toString();
    }
}

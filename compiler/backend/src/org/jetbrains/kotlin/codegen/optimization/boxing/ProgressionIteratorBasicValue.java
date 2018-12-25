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

package org.jetbrains.kotlin.codegen.optimization.boxing;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.codegen.intrinsics.IteratorNext;
import org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue;
import org.jetbrains.kotlin.codegen.range.RangeCodegenUtilKt;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType;
import org.jetbrains.org.objectweb.asm.Type;

public class ProgressionIteratorBasicValue extends StrictBasicValue {
    private final static ImmutableMap<String, Type> VALUES_TYPENAME_TO_TYPE;

    static {
        ImmutableMap.Builder<String, Type> builder = ImmutableMap.builder();
        for (PrimitiveType primitiveType : RangeCodegenUtilKt.getSupportedRangeTypes()) {
            builder.put(primitiveType.getTypeName().asString(), Type.getType(JvmPrimitiveType.get(primitiveType).getDesc()));
        }
        VALUES_TYPENAME_TO_TYPE = builder.build();
    }

    private static final ImmutableMap<PrimitiveType, ProgressionIteratorBasicValue> ITERATOR_VALUE_BY_ELEMENT_PRIMITIVE_TYPE;

    static {
        ImmutableMap.Builder<PrimitiveType, ProgressionIteratorBasicValue> builder = ImmutableMap.builder();
        for (PrimitiveType elementType : RangeCodegenUtilKt.getSupportedRangeTypes()) {
            builder.put(elementType, new ProgressionIteratorBasicValue(elementType.getTypeName().asString()));
        }
        ITERATOR_VALUE_BY_ELEMENT_PRIMITIVE_TYPE = builder.build();
    }

    @NotNull
    private static Type getValuesType(@NotNull String valuesTypeName) {
        Type type = VALUES_TYPENAME_TO_TYPE.get(valuesTypeName);
        assert type != null : "Unexpected type " + valuesTypeName;
        return type;
    }

    private final Type valuesPrimitiveType;
    private final String valuesPrimitiveTypeName;

    private ProgressionIteratorBasicValue(@NotNull String valuesPrimitiveTypeName) {
        super(IteratorNext.Companion.getPrimitiveIteratorType(Name.identifier(valuesPrimitiveTypeName)));
        this.valuesPrimitiveType = getValuesType(valuesPrimitiveTypeName);
        this.valuesPrimitiveTypeName = valuesPrimitiveTypeName;
    }


    @Nullable
    public static ProgressionIteratorBasicValue byProgressionClassType(@NotNull Type progressionClassType) {
        FqName classFqName = new FqName(progressionClassType.getClassName());
        PrimitiveType elementType = RangeCodegenUtilKt.getPrimitiveRangeOrProgressionElementType(classFqName);
        return ITERATOR_VALUE_BY_ELEMENT_PRIMITIVE_TYPE.get(elementType);
    }

    @NotNull
    public Type getValuesPrimitiveType() {
        return valuesPrimitiveType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ProgressionIteratorBasicValue value = (ProgressionIteratorBasicValue) o;

        if (!valuesPrimitiveType.equals(value.valuesPrimitiveType)) return false;

        return true;
    }

    @NotNull
    public String getNextMethodName() {
        return "next" + valuesPrimitiveTypeName;
    }

    @NotNull
    public String getNextMethodDesc() {
        return "()" + getValuesPrimitiveType().getDescriptor();
    }
}

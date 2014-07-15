/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.optimization.boxing;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

public class ProgressionIteratorBasicValue extends BasicValue {
    private final static ImmutableMap<String, Type> VALUES_TYPENAME_TO_TYPE;

    static {
        VALUES_TYPENAME_TO_TYPE = ImmutableMap.<String, Type>builder().
                put("Byte", Type.BYTE_TYPE).
                put("Char", Type.CHAR_TYPE).
                put("Short", Type.SHORT_TYPE).
                put("Int", Type.INT_TYPE).
                put("Long", Type.LONG_TYPE).
                put("Float", Type.FLOAT_TYPE).
                put("Double", Type.DOUBLE_TYPE).
                build();
    }

    @NotNull
    private static Type getValuesType(@NotNull String valuesTypeName) {
        Type type = VALUES_TYPENAME_TO_TYPE.get(valuesTypeName);
        assert type != null : "Unexpected type " + valuesTypeName;
        return type;
    }

    private final Type valuesPrimitiveType;
    private final String valuesPrimitiveTypeName;

    public ProgressionIteratorBasicValue(@NotNull String valuesPrimitiveTypeName) {
        super(Type.getObjectType("kotlin/" + valuesPrimitiveTypeName + "Iterator"));
        this.valuesPrimitiveType = getValuesType(valuesPrimitiveTypeName);
        this.valuesPrimitiveTypeName = valuesPrimitiveTypeName;
    }

    public Type getValuesPrimitiveType() {
        return valuesPrimitiveType;
    }

    public String getValuesPrimitiveTypeName() {
        return valuesPrimitiveTypeName;
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
        return "next" + getValuesPrimitiveTypeName();
    }

    @NotNull
    public String getNextMethodDesc() {
        return "()" + getValuesPrimitiveType().getDescriptor();
    }
}

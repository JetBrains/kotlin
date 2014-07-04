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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

public class RangeIteratorBasicValue extends BasicValue {
    private static Type getValuesType(@NotNull String valuesTypeName) {
        if (valuesTypeName.equals("Byte")) {
            return Type.BYTE_TYPE;
        }
        if (valuesTypeName.equals("Char")) {
            return Type.CHAR_TYPE;
        }
        if (valuesTypeName.equals("Short")) {
            return Type.SHORT_TYPE;
        }
        if (valuesTypeName.equals("Int")) {
            return Type.INT_TYPE;
        }
        if (valuesTypeName.equals("Long")) {
            return Type.LONG_TYPE;
        }
        if (valuesTypeName.equals("Float")) {
            return Type.FLOAT_TYPE;
        }
        if (valuesTypeName.equals("Double")) {
            return Type.DOUBLE_TYPE;
        }

        throw new RuntimeException("Unexpected type " + valuesTypeName);
    }

    private final Type valuesPrimitiveType;
    private final String valuesPrimitiveTypeName;

    public RangeIteratorBasicValue(@NotNull String valuesPrimitiveTypeName) {
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

        RangeIteratorBasicValue value = (RangeIteratorBasicValue) o;

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

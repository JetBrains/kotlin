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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.asm4.Type;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;

public enum JvmPrimitiveType {
    BOOLEAN(PrimitiveType.BOOLEAN, "boolean", "java.lang.Boolean", Type.BOOLEAN_TYPE),
    CHAR(PrimitiveType.CHAR, "char", "java.lang.Character", Type.CHAR_TYPE),
    BYTE(PrimitiveType.BYTE, "byte", "java.lang.Byte", Type.BYTE_TYPE),
    SHORT(PrimitiveType.SHORT, "short", "java.lang.Short", Type.SHORT_TYPE),
    INT(PrimitiveType.INT, "int", "java.lang.Integer", Type.INT_TYPE),
    FLOAT(PrimitiveType.FLOAT, "float", "java.lang.Float", Type.FLOAT_TYPE),
    LONG(PrimitiveType.LONG, "long", "java.lang.Long", Type.LONG_TYPE),
    DOUBLE(PrimitiveType.DOUBLE, "double", "java.lang.Double", Type.DOUBLE_TYPE),
    ;
    
    private final PrimitiveType primitiveType;
    private final String name;
    private final JvmClassName wrapper;
    private final Type asmType;

    private JvmPrimitiveType(PrimitiveType primitiveType, String name, String wrapperClassName, Type asmType) {
        this.primitiveType = primitiveType;
        this.name = name;
        this.wrapper = JvmClassName.byFqNameWithoutInnerClasses(wrapperClassName);
        this.asmType = asmType;
    }

    public PrimitiveType getPrimitiveType() {
        return primitiveType;
    }

    public String getName() {
        return name;
    }

    public JvmClassName getWrapper() {
        return wrapper;
    }

    public Type getAsmType() {
        return asmType;
    }
}

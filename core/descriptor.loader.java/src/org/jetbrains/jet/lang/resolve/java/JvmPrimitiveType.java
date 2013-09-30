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

import org.jetbrains.jet.lang.types.lang.PrimitiveType;

public enum JvmPrimitiveType {
    BOOLEAN(PrimitiveType.BOOLEAN, "boolean", "java.lang.Boolean"),
    CHAR(PrimitiveType.CHAR, "char", "java.lang.Character"),
    BYTE(PrimitiveType.BYTE, "byte", "java.lang.Byte"),
    SHORT(PrimitiveType.SHORT, "short", "java.lang.Short"),
    INT(PrimitiveType.INT, "int", "java.lang.Integer"),
    FLOAT(PrimitiveType.FLOAT, "float", "java.lang.Float"),
    LONG(PrimitiveType.LONG, "long", "java.lang.Long"),
    DOUBLE(PrimitiveType.DOUBLE, "double", "java.lang.Double"),
    ;
    
    private final PrimitiveType primitiveType;
    private final String name;
    private final JvmClassName wrapper;

    private JvmPrimitiveType(PrimitiveType primitiveType, String name, String wrapperClassName) {
        this.primitiveType = primitiveType;
        this.name = name;
        this.wrapper = JvmClassName.byFqNameWithoutInnerClasses(wrapperClassName);
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
}

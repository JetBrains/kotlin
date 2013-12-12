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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Type;

import java.util.HashMap;
import java.util.Map;

public class AsmTypeConstants {
    private static final Map<Class<?>, Type> TYPES_MAP = new HashMap<Class<?>, Type>();

    public static final Type OBJECT_TYPE = getType(Object.class);
    public static final Type JAVA_STRING_TYPE = getType(String.class);
    public static final Type JAVA_THROWABLE_TYPE = getType(Throwable.class);
    public static final Type JAVA_ARRAY_GENERIC_TYPE = getType(Object[].class);

    public static final Type JET_UNIT_TYPE = Type.getObjectType("jet/Unit");
    public static final Type JET_FUNCTION0_TYPE = Type.getObjectType("jet/Function0");
    public static final Type JET_FUNCTION1_TYPE = Type.getObjectType("jet/Function1");
    public static final Type JET_INT_RANGE_TYPE = Type.getObjectType("jet/IntRange");
    public static final Type JET_SHARED_VAR_TYPE = Type.getObjectType("jet/runtime/SharedVar$Object");
    public static final Type JET_SHARED_INT_TYPE = Type.getObjectType("jet/runtime/SharedVar$Int");
    public static final Type JET_SHARED_DOUBLE_TYPE = Type.getObjectType("jet/runtime/SharedVar$Double");
    public static final Type JET_SHARED_FLOAT_TYPE = Type.getObjectType("jet/runtime/SharedVar$Float");
    public static final Type JET_SHARED_BYTE_TYPE = Type.getObjectType("jet/runtime/SharedVar$Byte");
    public static final Type JET_SHARED_SHORT_TYPE = Type.getObjectType("jet/runtime/SharedVar$Short");
    public static final Type JET_SHARED_CHAR_TYPE = Type.getObjectType("jet/runtime/SharedVar$Char");
    public static final Type JET_SHARED_LONG_TYPE = Type.getObjectType("jet/runtime/SharedVar$Long");
    public static final Type JET_SHARED_BOOLEAN_TYPE = Type.getObjectType("jet/runtime/SharedVar$Boolean");


    public static Type getType(@NotNull Class<?> javaClass) {
        Type type = TYPES_MAP.get(javaClass);
        if (type == null) {
            type = Type.getType(javaClass);
            TYPES_MAP.put(javaClass, type);
        }
        return type;
    }

    private AsmTypeConstants() {
    }
}

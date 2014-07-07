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
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;

import java.util.HashSet;
import java.util.Set;

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

    private static final Set<FqName> WRAPPERS_CLASS_NAMES;

    static {
        WRAPPERS_CLASS_NAMES = new HashSet<FqName>();

        for (JvmPrimitiveType primitiveType : values()) {
            WRAPPERS_CLASS_NAMES.add(primitiveType.getWrapperFqName());
        }
    }

    public static boolean isWrapperClassName(@NotNull FqName className) {
        return WRAPPERS_CLASS_NAMES.contains(className);
    }

    private final PrimitiveType primitiveType;
    private final String name;
    private final FqName wrapperFqName;

    private JvmPrimitiveType(@NotNull PrimitiveType primitiveType, @NotNull String name, @NotNull String wrapperClassName) {
        this.primitiveType = primitiveType;
        this.name = name;
        this.wrapperFqName = new FqName(wrapperClassName);
    }

    @NotNull
    public PrimitiveType getPrimitiveType() {
        return primitiveType;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public FqName getWrapperFqName() {
        return wrapperFqName;
    }
}

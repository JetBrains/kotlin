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

package org.jetbrains.kotlin.resolve.jvm.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType;
import org.jetbrains.org.objectweb.asm.Type;

import static org.jetbrains.org.objectweb.asm.Type.*;

public class PrimitiveTypesUtil {
    private PrimitiveTypesUtil() {
    }

    public static Type asmTypeForPrimitive(@NotNull JvmPrimitiveType jvmPrimitiveType) {
        switch (jvmPrimitiveType) {
            case BOOLEAN: return BOOLEAN_TYPE;
            case CHAR: return CHAR_TYPE;
            case BYTE: return BYTE_TYPE;
            case SHORT: return SHORT_TYPE;
            case INT: return INT_TYPE;
            case FLOAT: return FLOAT_TYPE;
            case LONG: return LONG_TYPE;
            case DOUBLE: return DOUBLE_TYPE;
            default: throw new IllegalStateException("Unknown primitive type: " + jvmPrimitiveType);
        }
    }
}

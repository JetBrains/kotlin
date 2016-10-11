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

package org.jetbrains.kotlin.codegen.optimization.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

import static org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue.*;

public class OptimizationBasicInterpreter extends BasicInterpreter {
    @Override
    @Nullable
    public StrictBasicValue newValue(@Nullable Type type) {
        if (type == null) {
            return UNINITIALIZED_VALUE;
        }

        switch (type.getSort()) {
            case Type.VOID:
                return null;
            case Type.INT:
                return INT_VALUE;
            case Type.FLOAT:
                return FLOAT_VALUE;
            case Type.LONG:
                return LONG_VALUE;
            case Type.DOUBLE:
                return DOUBLE_VALUE;
            case Type.BOOLEAN:
                return BOOLEAN_VALUE;
            case Type.CHAR:
                return CHAR_VALUE;
            case Type.BYTE:
                return BYTE_VALUE;
            case Type.SHORT:
                return SHORT_VALUE;
            case Type.OBJECT:
                return new StrictBasicValue(type);
            case Type.ARRAY:
                return StrictBasicValue.REFERENCE_VALUE;
            default:
                throw new IllegalArgumentException("Unknown type sort " + type.getSort());
        }
    }

    @NotNull
    @Override
    public BasicValue merge(
            @NotNull BasicValue v, @NotNull BasicValue w
    ) {
        if (v.equals(w)) return v;

        if (v == StrictBasicValue.UNINITIALIZED_VALUE || w == StrictBasicValue.UNINITIALIZED_VALUE) {
            return StrictBasicValue.UNINITIALIZED_VALUE;
        }

        // if merge of two references then `lub` is java/lang/Object
        // arrays also are BasicValues with reference type's
        if (v.getType().getSort() == Type.OBJECT && w.getType().getSort() == Type.OBJECT) {
            return StrictBasicValue.REFERENCE_VALUE;
        }

        assert v.getType().getSort() != Type.ARRAY && w.getType().getSort() != Type.ARRAY : "There should not be arrays";


        // if merge of something can be stored in int var (int, char, boolean, byte, character)
        if (v.getType().getOpcode(Opcodes.ISTORE) == Opcodes.ISTORE &&
            w.getType().getOpcode(Opcodes.ISTORE) == Opcodes.ISTORE) {
            return StrictBasicValue.INT_VALUE;
        }

        return StrictBasicValue.UNINITIALIZED_VALUE;
    }
}

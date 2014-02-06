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

package org.jetbrains.jet.lang.resolve.constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

public class ConstantUtils {

    @Nullable
    private static CompileTimeConstant<?> getIntegerValue(
            long value,
            boolean canBeUsedInAnnotations,
            boolean isPureIntConstant,
            @NotNull JetType expectedType
    ) {
        if (TypeUtils.noExpectedType(expectedType) || expectedType.isError()) {
            return new IntegerValueTypeConstant(value, canBeUsedInAnnotations);
        }

        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();

        JetType notNullExpectedType = TypeUtils.makeNotNullable(expectedType);
        if (notNullExpectedType.equals(builtIns.getLongType())) {
            return new LongValue(value, canBeUsedInAnnotations, isPureIntConstant);
        }
        else if (notNullExpectedType.equals(builtIns.getShortType())) {
            if (value == (long) ((short) value)) {
                return new ShortValue((short) value, canBeUsedInAnnotations, isPureIntConstant);
            }
            return getDefaultIntegerValue(value, canBeUsedInAnnotations, isPureIntConstant);
        }
        if (notNullExpectedType.equals(builtIns.getByteType())) {
            if (value == (long) ((byte) value)) {
                return new ByteValue((byte) value, canBeUsedInAnnotations, isPureIntConstant);
            }
            return getDefaultIntegerValue(value, canBeUsedInAnnotations, isPureIntConstant);
        }
        else {
            return getDefaultIntegerValue(value, canBeUsedInAnnotations, isPureIntConstant);
        }
    }

    @NotNull
    private static CompileTimeConstant<?> getDefaultIntegerValue(
            long value, boolean canBeUsedInAnnotations,
            boolean isPureIntConstant
    ) {
        if (value == (long) ((int) value)) {
            return new IntValue((int) value, canBeUsedInAnnotations, isPureIntConstant);
        }
        else {
            return new LongValue(value, canBeUsedInAnnotations, isPureIntConstant);
        }
    }

    @Nullable
    public static CompileTimeConstant<?> createCompileTimeConstant(
            @Nullable Object value,
            boolean canBeUsedInAnnotations,
            boolean isPureIntConstant,
            @Nullable JetType expectedType
    ) {
        if (expectedType == null) {
            if (value instanceof Integer) {
                return new IntValue((Integer) value, canBeUsedInAnnotations, isPureIntConstant);
            }
            else if (value instanceof Long) {
                return new LongValue((Long) value, canBeUsedInAnnotations, isPureIntConstant);
            }
            else if (value instanceof Byte) {
                return new ByteValue((Byte) value, canBeUsedInAnnotations, isPureIntConstant);
            }
            else if (value instanceof Short) {
                return new ShortValue((Short) value, canBeUsedInAnnotations, isPureIntConstant);
            }
        }
        if (value instanceof Integer) {
            return getIntegerValue((Integer) value, canBeUsedInAnnotations, isPureIntConstant, expectedType);
        }
        else if (value instanceof Long) {
            return getIntegerValue((Long) value, canBeUsedInAnnotations, isPureIntConstant, expectedType);
        }
        else if (value instanceof Byte) {
            return getIntegerValue((Byte) value, canBeUsedInAnnotations, isPureIntConstant, expectedType);
        }
        else if (value instanceof Short) {
            return getIntegerValue((Short) value, canBeUsedInAnnotations, isPureIntConstant, expectedType);
        }
        else if (value instanceof Character) {
            return new CharValue((Character) value, canBeUsedInAnnotations, isPureIntConstant);
        }
        else if (value instanceof Float) {
            return new FloatValue((Float) value, canBeUsedInAnnotations);
        }
        else if (value instanceof Double) {
            return new DoubleValue((Double) value, canBeUsedInAnnotations);
        }
        else if (value instanceof Boolean) {
            return new BooleanValue((Boolean) value, canBeUsedInAnnotations);
        }
        else if (value instanceof String) {
            return new StringValue((String) value, canBeUsedInAnnotations);
        }
        return null;
    }

    private ConstantUtils() {
    }
}

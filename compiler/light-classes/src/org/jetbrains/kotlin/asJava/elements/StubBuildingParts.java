/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.Type;

import java.lang.reflect.Array;

// private parts from com.intellij.psi.impl.compiled.StubBuildingVisitor
public class StubBuildingParts {

    private static final String DOUBLE_POSITIVE_INF = "1.0 / 0.0";
    private static final String DOUBLE_NEGATIVE_INF = "-1.0 / 0.0";
    private static final String DOUBLE_NAN = "0.0d / 0.0";
    private static final String FLOAT_POSITIVE_INF = "1.0f / 0.0";
    private static final String FLOAT_NEGATIVE_INF = "-1.0f / 0.0";
    private static final String FLOAT_NAN = "0.0f / 0.0";


    @Nullable
    public static String constToString(@Nullable Object value, @Nullable String type, boolean anno, Function<String, String> mapping) {
        if (value == null) return null;

        if (value instanceof String) {
            return "\"" + StringUtil.escapeStringCharacters((String)value) + "\"";
        }

        if (value instanceof Boolean || value instanceof Short || value instanceof Byte) {
            return value.toString();
        }

        if (value instanceof Character) {
            return "'" + StringUtil.escapeCharCharacters(value.toString()) + "'";
        }

        if (value instanceof Long) {
            return value.toString() + 'L';
        }

        if (value instanceof Integer) {
            if ("boolean".equals(type)) {
                if (value.equals(0)) return "false";
                if (value.equals(1)) return "true";
            }
            if ("char".equals(type)) {
                char ch = (char)((Integer)value).intValue();
                return "'" + StringUtil.escapeCharCharacters(String.valueOf(ch)) + "'";
            }
            return value.toString();
        }

        if (value instanceof Double) {
            double d = (Double)value;
            if (Double.isInfinite(d)) {
                return d > 0 ? DOUBLE_POSITIVE_INF : DOUBLE_NEGATIVE_INF;
            }
            if (Double.isNaN(d)) {
                return DOUBLE_NAN;
            }
            return Double.toString(d);
        }

        if (value instanceof Float) {
            float v = (Float)value;

            if (Float.isInfinite(v)) {
                return v > 0 ? FLOAT_POSITIVE_INF : FLOAT_NEGATIVE_INF;
            }
            else if (Float.isNaN(v)) {
                return FLOAT_NAN;
            }
            else {
                return Float.toString(v) + 'f';
            }
        }

        if (value.getClass().isArray()) {
            StringBuilder buffer = new StringBuilder();
            buffer.append('{');
            for (int i = 0, length = Array.getLength(value); i < length; i++) {
                if (i > 0) buffer.append(", ");
                buffer.append(constToString(Array.get(value, i), type, anno, mapping));
            }
            buffer.append('}');
            return buffer.toString();
        }

        if (anno && value instanceof Type) {
            return toJavaType((Type)value, mapping) + ".class";
        }

        return null;
    }

    private static String toJavaType(Type type, Function<String, String> mapping) {
        int dimensions = 0;
        if (type.getSort() == Type.ARRAY) {
            dimensions = type.getDimensions();
            type = type.getElementType();
        }
        String text = type.getSort() == Type.OBJECT ? mapping.fun(type.getInternalName()) : type.getClassName();
        if (dimensions > 0) text += StringUtil.repeat("[]", dimensions);
        return text;
    }
}

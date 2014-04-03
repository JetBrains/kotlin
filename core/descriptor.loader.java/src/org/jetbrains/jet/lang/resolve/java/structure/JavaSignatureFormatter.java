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

package org.jetbrains.jet.lang.resolve.java.structure;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JavaSignatureFormatter {
    private static JavaSignatureFormatter instance;

    private JavaSignatureFormatter() {
    }

    @NotNull
    public static JavaSignatureFormatter getInstance() {
        if (instance == null) {
            instance = new JavaSignatureFormatter();
        }
        return instance;
    }

    /**
     * @return a formatted signature of a method, showing method name and fully qualified names of its parameter types, e.g.:
     * {@code "foo(double, java.lang.String)"}
     */
    @NotNull
    public String formatMethod(@NotNull JavaMethod method) {
        StringBuilder buffer = new StringBuilder();

        buffer.append(method.getName());

        buffer.append('(');
        boolean firstParameter = true;
        for (JavaValueParameter parameter : method.getValueParameters()) {
            if (!firstParameter) buffer.append(", ");
            firstParameter = false;

            buffer.append(formatType(parameter.getType()));
        }

        buffer.append(')');

        return buffer.toString();
    }

    @NotNull
    private String formatType(@NotNull JavaType type) {
        if (type instanceof JavaPrimitiveType) {
            return ((JavaPrimitiveType) type).getCanonicalText();
        }
        else if (type instanceof JavaArrayType) {
            return formatType(((JavaArrayType) type).getComponentType()) + "[]";
        }
        else if (type instanceof JavaClassifierType) {
            return formatClassifierType((JavaClassifierType) type);
        }
        else if (type instanceof JavaWildcardType) {
            JavaWildcardType wildcardType = (JavaWildcardType) type;
            if (wildcardType.isExtends()) {
                //noinspection ConstantConditions
                return "? extends " + formatType(wildcardType.getBound());
            } else {
                return "?";
            }
        } else {
            throw new IllegalArgumentException("Wrong type: " + type);
        }
    }

    @NotNull
    private String formatClassifierType(@NotNull JavaClassifierType type) {
        JavaClassifier classifier = type.getClassifier();

        if (classifier == null) return "[UNRESOLVED: " + type + "]";

        if (classifier instanceof JavaTypeParameter) {
            return classifier.getName().asString();
        }

        //noinspection ConstantConditions
        StringBuilder buffer = new StringBuilder(((JavaClass) classifier).getFqName().asString());

        List<JavaType> typeArguments = type.getTypeArguments();
        if (!typeArguments.isEmpty()) {
            buffer.append("<");
            boolean firstArgument = true;
            for (JavaType typeArgument : typeArguments) {
                if (!firstArgument) buffer.append(",");
                firstArgument = false;

                buffer.append(formatType(typeArgument));
            }
            buffer.append(">");
        }

        return buffer.toString();
    }
}

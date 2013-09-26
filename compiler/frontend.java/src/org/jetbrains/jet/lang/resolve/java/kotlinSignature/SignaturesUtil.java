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

package org.jetbrains.jet.lang.resolve.java.kotlinSignature;

import com.intellij.openapi.util.text.StringUtil;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaAnnotationResolver;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotationArgument;
import org.jetbrains.jet.lang.resolve.java.structure.JavaLiteralAnnotationArgument;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMember;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import static org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils.fqNameByClass;

public class SignaturesUtil {
    public static final FqName KOTLIN_SIGNATURE = fqNameByClass(KotlinSignature.class);
    public static final Name KOTLIN_SIGNATURE_VALUE_FIELD_NAME = Name.identifier("value");

    private SignaturesUtil() {
    }

    @Nullable
    public static String getKotlinSignature(@NotNull JavaAnnotationResolver annotationResolver, @NotNull JavaMember member) {
        JavaAnnotation annotation = annotationResolver.findAnnotationWithExternal(member, KOTLIN_SIGNATURE);

        if (annotation != null) {
            JavaAnnotationArgument argument = annotation.findArgument(KOTLIN_SIGNATURE_VALUE_FIELD_NAME);
            if (argument instanceof JavaLiteralAnnotationArgument) {
                Object value = ((JavaLiteralAnnotationArgument) argument).getValue();
                if (value instanceof String) {
                    return StringUtil.unescapeStringCharacters((String) value);
                }
            }
        }

        return null;
    }
}

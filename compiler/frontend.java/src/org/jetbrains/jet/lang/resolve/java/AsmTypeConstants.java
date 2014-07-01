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
import org.jetbrains.org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.lang.types.lang.KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME;

public class AsmTypeConstants {
    private static final Map<Class<?>, Type> TYPES_MAP = new HashMap<Class<?>, Type>();

    public static final Type OBJECT_TYPE = getType(Object.class);
    public static final Type JAVA_STRING_TYPE = getType(String.class);
    public static final Type JAVA_THROWABLE_TYPE = getType(Throwable.class);

    public static final Type UNIT_TYPE = Type.getObjectType(BUILT_INS_PACKAGE_FQ_NAME + "/Unit");
    public static final Type FUNCTION0_TYPE = Type.getObjectType(BUILT_INS_PACKAGE_FQ_NAME + "/Function0");
    public static final Type FUNCTION1_TYPE = Type.getObjectType(BUILT_INS_PACKAGE_FQ_NAME + "/Function1");
    public static final Type INT_RANGE_TYPE = Type.getObjectType(BUILT_INS_PACKAGE_FQ_NAME + "/IntRange");
    public static final Type PROPERTY_METADATA_TYPE = Type.getObjectType(BUILT_INS_PACKAGE_FQ_NAME + "/PropertyMetadata");
    public static final Type PROPERTY_METADATA_IMPL_TYPE = Type.getObjectType(BUILT_INS_PACKAGE_FQ_NAME + "/PropertyMetadataImpl");

    public static final Type K_MEMBER_PROPERTY_TYPE = Type.getObjectType("kotlin/reflect/KMemberProperty");
    public static final Type K_MUTABLE_MEMBER_PROPERTY_TYPE = Type.getObjectType("kotlin/reflect/KMutableMemberProperty");

    public static final Type K_CLASS_IMPL_TYPE = reflectInternal("KClassImpl");
    public static final Type K_PACKAGE_IMPL_TYPE = reflectInternal("KPackageImpl");
    public static final Type K_TOP_LEVEL_VARIABLE_IMPL_TYPE = reflectInternal("KTopLevelVariableImpl");
    public static final Type K_MUTABLE_TOP_LEVEL_VARIABLE_IMPL_TYPE = reflectInternal("KMutableTopLevelVariableImpl");
    public static final Type K_TOP_LEVEL_EXTENSION_PROPERTY_IMPL_TYPE = reflectInternal("KTopLevelExtensionPropertyImpl");
    public static final Type K_MUTABLE_TOP_LEVEL_EXTENSION_PROPERTY_IMPL_TYPE = reflectInternal("KMutableTopLevelExtensionPropertyImpl");

    public static final String REFLECTION_INTERNAL_PACKAGE = reflectInternal("InternalPackage").getInternalName();

    public static final Type OBJECT_REF_TYPE = Type.getObjectType("kotlin/jvm/internal/Ref$ObjectRef");

    @NotNull
    private static Type reflectInternal(@NotNull String className) {
        return Type.getObjectType("kotlin/reflect/jvm/internal/" + className);
    }

    @NotNull
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

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

package org.jetbrains.kotlin.resolve.jvm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class AsmTypes {
    private static final Map<Class<?>, Type> TYPES_MAP = new HashMap<Class<?>, Type>();

    public static final Type OBJECT_TYPE = getType(Object.class);
    public static final Type JAVA_STRING_TYPE = getType(String.class);
    public static final Type JAVA_THROWABLE_TYPE = getType(Throwable.class);
    public static final Type JAVA_CLASS_TYPE = getType(Class.class);

    public static final Type UNIT_TYPE = Type.getObjectType("kotlin/Unit");
    public static final Type PROPERTY_METADATA_TYPE = Type.getObjectType("kotlin/PropertyMetadata");
    public static final Type PROPERTY_METADATA_IMPL_TYPE = Type.getObjectType("kotlin/PropertyMetadataImpl");

    public static final Type LAMBDA = Type.getObjectType("kotlin/jvm/internal/Lambda");
    public static final Type FUNCTION_REFERENCE = Type.getObjectType("kotlin/jvm/internal/FunctionReference");
    public static final Type PROPERTY_REFERENCE0 = Type.getObjectType("kotlin/jvm/internal/PropertyReference0");
    public static final Type PROPERTY_REFERENCE1 = Type.getObjectType("kotlin/jvm/internal/PropertyReference1");
    public static final Type MUTABLE_PROPERTY_REFERENCE0 = Type.getObjectType("kotlin/jvm/internal/MutablePropertyReference0");
    public static final Type MUTABLE_PROPERTY_REFERENCE1 = Type.getObjectType("kotlin/jvm/internal/MutablePropertyReference1");

    public static final Type K_CLASS_TYPE = reflect("KClass");
    public static final Type K_CLASS_ARRAY_TYPE = Type.getObjectType("[" + K_CLASS_TYPE.getDescriptor());
    public static final Type K_PACKAGE_TYPE = reflect("KPackage");
    public static final Type K_DECLARATION_CONTAINER_TYPE = reflect("KDeclarationContainer");

    public static final Type K_FUNCTION = reflect("KFunction");

    public static final Type K_PROPERTY0_TYPE = reflect("KProperty0");
    public static final Type K_PROPERTY1_TYPE = reflect("KProperty1");
    public static final Type K_MUTABLE_PROPERTY0_TYPE = reflect("KMutableProperty0");
    public static final Type K_MUTABLE_PROPERTY1_TYPE = reflect("KMutableProperty1");

    public static final String REFLECTION = "kotlin/jvm/internal/Reflection";

    public static final String REF_TYPE_PREFIX = "kotlin/jvm/internal/Ref$";
    public static final Type OBJECT_REF_TYPE = Type.getObjectType(REF_TYPE_PREFIX + "ObjectRef");

    public static final Type DEFAULT_CONSTRUCTOR_MARKER = Type.getObjectType("kotlin/jvm/internal/DefaultConstructorMarker");

    @NotNull
    private static Type reflect(@NotNull String className) {
        return Type.getObjectType("kotlin/reflect/" + className);
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

    private AsmTypes() {
    }
}

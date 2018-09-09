/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class AsmTypes {
    private static final Map<Class<?>, Type> TYPES_MAP = new HashMap<>();

    public static final Type OBJECT_TYPE = getType(Object.class);
    public static final Type JAVA_STRING_TYPE = getType(String.class);
    public static final Type JAVA_THROWABLE_TYPE = getType(Throwable.class);
    public static final Type JAVA_CLASS_TYPE = getType(Class.class);
    public static final Type ENUM_TYPE = getType(Enum.class);
    public static final Type NUMBER_TYPE = getType(Number.class);
    public static final Type BOOLEAN_WRAPPER_TYPE = getType(Boolean.class);
    public static final Type CHARACTER_WRAPPER_TYPE = getType(Character.class);
    public static final Type VOID_WRAPPER_TYPE = getType(Void.class);

    public static final Type UNIT_TYPE = Type.getObjectType("kotlin/Unit");

    public static final Type LAMBDA = Type.getObjectType("kotlin/jvm/internal/Lambda");
    public static final Type FUNCTION_REFERENCE = Type.getObjectType("kotlin/jvm/internal/FunctionReference");
    public static final Type PROPERTY_REFERENCE0 = Type.getObjectType("kotlin/jvm/internal/PropertyReference0");
    public static final Type PROPERTY_REFERENCE1 = Type.getObjectType("kotlin/jvm/internal/PropertyReference1");
    public static final Type PROPERTY_REFERENCE2 = Type.getObjectType("kotlin/jvm/internal/PropertyReference2");
    public static final Type MUTABLE_PROPERTY_REFERENCE0 = Type.getObjectType("kotlin/jvm/internal/MutablePropertyReference0");
    public static final Type MUTABLE_PROPERTY_REFERENCE1 = Type.getObjectType("kotlin/jvm/internal/MutablePropertyReference1");
    public static final Type MUTABLE_PROPERTY_REFERENCE2 = Type.getObjectType("kotlin/jvm/internal/MutablePropertyReference2");

    public static final Type RESULT_FAILURE = Type.getObjectType("kotlin/Result$Failure");

    public static final Type[] PROPERTY_REFERENCE_IMPL = {
            Type.getObjectType("kotlin/jvm/internal/PropertyReference0Impl"),
            Type.getObjectType("kotlin/jvm/internal/PropertyReference1Impl"),
            Type.getObjectType("kotlin/jvm/internal/PropertyReference2Impl")
    };
    public static final Type[] MUTABLE_PROPERTY_REFERENCE_IMPL = {
            Type.getObjectType("kotlin/jvm/internal/MutablePropertyReference0Impl"),
            Type.getObjectType("kotlin/jvm/internal/MutablePropertyReference1Impl"),
            Type.getObjectType("kotlin/jvm/internal/MutablePropertyReference2Impl")
    };

    public static final Type K_CLASS_TYPE = reflect("KClass");
    public static final Type K_CLASS_ARRAY_TYPE = Type.getObjectType("[" + K_CLASS_TYPE.getDescriptor());
    public static final Type K_DECLARATION_CONTAINER_TYPE = reflect("KDeclarationContainer");

    public static final Type K_FUNCTION = reflect("KFunction");

    public static final Type K_PROPERTY_TYPE = reflect("KProperty");
    public static final Type K_PROPERTY0_TYPE = reflect("KProperty0");
    public static final Type K_PROPERTY1_TYPE = reflect("KProperty1");
    public static final Type K_PROPERTY2_TYPE = reflect("KProperty2");
    public static final Type K_MUTABLE_PROPERTY0_TYPE = reflect("KMutableProperty0");
    public static final Type K_MUTABLE_PROPERTY1_TYPE = reflect("KMutableProperty1");
    public static final Type K_MUTABLE_PROPERTY2_TYPE = reflect("KMutableProperty2");

    public static final Type SUSPEND_FUNCTION_TYPE = Type.getObjectType("kotlin/coroutines/jvm/internal/SuspendFunction");

    public static final String REFLECTION = "kotlin/jvm/internal/Reflection";

    public static final String REF_TYPE_PREFIX = "kotlin/jvm/internal/Ref$";
    public static final Type OBJECT_REF_TYPE = Type.getObjectType(REF_TYPE_PREFIX + "ObjectRef");

    public static final Type DEFAULT_CONSTRUCTOR_MARKER = Type.getObjectType("kotlin/jvm/internal/DefaultConstructorMarker");

    @NotNull
    private static Type reflect(@NotNull String className) {
        return Type.getObjectType("kotlin/reflect/" + className);
    }

    public static boolean isSharedVarType(@NotNull Type type) {
        return type.getSort() == Type.OBJECT && type.getInternalName().startsWith(REF_TYPE_PREFIX);
    }

    @NotNull
    public static Type sharedTypeForPrimitive(@NotNull PrimitiveType primitiveType) {
        String typeName = primitiveType.getTypeName().getIdentifier();
        return Type.getObjectType(REF_TYPE_PREFIX + typeName + "Ref");
    }

    @NotNull
    public static Type valueTypeForPrimitive(PrimitiveType primitiveType) {
        switch (primitiveType) {
            case BOOLEAN:
                return Type.BOOLEAN_TYPE;
            case CHAR:
                return Type.CHAR_TYPE;
            case BYTE:
                return Type.BYTE_TYPE;
            case SHORT:
                return Type.SHORT_TYPE;
            case INT:
                return Type.INT_TYPE;
            case FLOAT:
                return Type.FLOAT_TYPE;
            case LONG:
                return Type.LONG_TYPE;
            case DOUBLE:
                return Type.DOUBLE_TYPE;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @NotNull
    public static Type getType(@NotNull Class<?> javaClass) {
        return TYPES_MAP.computeIfAbsent(javaClass, k -> Type.getType(javaClass));
    }

    private AsmTypes() {
    }
}

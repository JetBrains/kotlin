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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.platform.JavaToKotlinClassMapBuilder;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class KotlinToJavaTypesMap extends JavaToKotlinClassMapBuilder {
    private static KotlinToJavaTypesMap instance = null;

    @NotNull
    public static KotlinToJavaTypesMap getInstance() {
        if (instance == null) {
            instance = new KotlinToJavaTypesMap();
        }
        return instance;
    }

    private final Map<FqName, Type> asmTypes = new HashMap<FqName, Type>();
    private final Map<FqName, Type> asmNullableTypes = new HashMap<FqName, Type>();
    private final Map<FqName, FqName> kotlinToJavaFqName = new HashMap<FqName, FqName>();

    private KotlinToJavaTypesMap() {
        init();
        initPrimitives();
    }

    private void initPrimitives() {
        FqName builtInsFqName = KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME;
        for (JvmPrimitiveType type : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = type.getPrimitiveType();
            FqName fqName = builtInsFqName.child(primitiveType.getTypeName());

            register(fqName, Type.getType(type.getDesc()));

            FqName wrapperFqName = type.getWrapperFqName();
            registerNullable(fqName, Type.getObjectType(JvmClassName.byFqNameWithoutInnerClasses(wrapperFqName).getInternalName()));
            registerFqName(fqName, wrapperFqName);

            register(builtInsFqName.child(primitiveType.getArrayTypeName()), Type.getType("[" + type.getDesc()));
        }
    }

    @Nullable
    public Type getJavaAnalog(@NotNull FqName fqName, boolean isNullable) {
        if (isNullable) {
            Type nullableType = asmNullableTypes.get(fqName);
            if (nullableType != null) {
                return nullableType;
            }
        }
        return asmTypes.get(fqName);
    }

    /**
     * E.g.
     * kotlin.Throwable -> java.lang.Throwable
     * kotlin.deprecated -> java.lang.annotation.Deprecated
     * kotlin.Int -> java.lang.Integer
     * kotlin.IntArray -> null
     */
    @Nullable
    public FqName getKotlinToJavaFqName(@NotNull FqName fqName) {
        return kotlinToJavaFqName.get(fqName);
    }

    @Override
    protected void register(@NotNull Class<?> javaClass, @NotNull ClassDescriptor kotlinDescriptor, @NotNull Direction direction) {
        if (direction == Direction.BOTH || direction == Direction.KOTLIN_TO_JAVA) {
            FqName fqName = DescriptorUtils.getFqNameSafe(kotlinDescriptor);
            register(fqName, AsmTypes.getType(javaClass));
            registerFqName(fqName, new FqName(javaClass.getCanonicalName()));
        }
    }

    @Override
    protected void register(
            @NotNull Class<?> javaClass,
            @NotNull ClassDescriptor kotlinDescriptor,
            @NotNull ClassDescriptor kotlinMutableDescriptor
    ) {
        register(javaClass, kotlinDescriptor, Direction.BOTH);
        register(javaClass, kotlinMutableDescriptor, Direction.BOTH);
    }

    private void register(@NotNull FqName fqName, @NotNull Type type) {
        asmTypes.put(fqName, type);
    }

    private void registerNullable(@NotNull FqName fqName, @NotNull Type nullableType) {
        asmNullableTypes.put(fqName, nullableType);
    }

    private void registerFqName(@NotNull FqName kotlinFqName, @NotNull FqName javaFqName) {
        kotlinToJavaFqName.put(kotlinFqName, javaFqName);
    }
}

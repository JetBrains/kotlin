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
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.JavaToKotlinClassMapBuilder;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType;

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

    private final Map<FqNameUnsafe, ClassId> map = new HashMap<FqNameUnsafe, ClassId>();

    private KotlinToJavaTypesMap() {
        init();
        initPrimitives();
    }

    private void initPrimitives() {
        for (JvmPrimitiveType type : JvmPrimitiveType.values()) {
            register(
                    KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.child(type.getPrimitiveType().getTypeName()).toUnsafe(),
                    ClassId.topLevel(type.getWrapperFqName())
            );
        }
    }

    /**
     * E.g.
     * kotlin.Throwable -> java.lang.Throwable
     * kotlin.deprecated -> java.lang.Deprecated
     * kotlin.Int -> java.lang.Integer
     * kotlin.IntArray -> null
     */
    @Nullable
    public ClassId mapKotlinFqNameToJava(@NotNull FqNameUnsafe kotlinFqName) {
        return map.get(kotlinFqName);
    }

    @Override
    protected void register(@NotNull Class<?> javaClass, @NotNull ClassDescriptor kotlinDescriptor, @NotNull Direction direction) {
        if (direction == Direction.BOTH || direction == Direction.KOTLIN_TO_JAVA) {
            register(DescriptorUtils.getFqName(kotlinDescriptor), computeClassId(javaClass));
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

    private void register(@NotNull FqNameUnsafe kotlinFqName, @NotNull ClassId javaClassId) {
        map.put(kotlinFqName, javaClassId);
    }

    @NotNull
    private static ClassId computeClassId(@NotNull Class<?> clazz) {
        assert !clazz.isPrimitive() && !clazz.isArray() : "Invalid class: " + clazz;
        Class<?> outer = clazz.getDeclaringClass();
        return outer == null
               ? ClassId.topLevel(new FqName(clazz.getCanonicalName()))
               : computeClassId(outer).createNestedClassId(Name.identifier(clazz.getSimpleName()));
    }
}

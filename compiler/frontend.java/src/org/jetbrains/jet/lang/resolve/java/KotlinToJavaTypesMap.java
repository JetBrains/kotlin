/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;

import java.lang.annotation.Annotation;
import java.util.*;

/**
* @author svtk
*/
public class KotlinToJavaTypesMap {
    private static KotlinToJavaTypesMap instance = null;

    @NotNull
    public static KotlinToJavaTypesMap getInstance() {
        if (instance == null) {
            instance = new KotlinToJavaTypesMap();
        }
        return instance;
    }

    private final Map<FqName, Type> asmTypes = Maps.newHashMap();
    private final Map<FqName, Type> asmNullableTypes = Maps.newHashMap();
    private final Set<String> mappedTypeNames = Sets.newHashSet();

    private KotlinToJavaTypesMap() {
        init();
        initPrimitives();
    }

    private void init() {
        JetStandardLibrary standardLibrary = JetStandardLibrary.getInstance();

        register(JetStandardClasses.getAny(), Object.class);
        register(standardLibrary.getNumber(), Number.class);
        register(standardLibrary.getString(), String.class);
        register(standardLibrary.getThrowable(), Throwable.class);
        register(standardLibrary.getCharSequence(), CharSequence.class);
        register(standardLibrary.getComparable(), Comparable.class);
        register(standardLibrary.getEnum(), Enum.class);
        register(standardLibrary.getAnnotation(), Annotation.class);
        register(standardLibrary.getIterable(), Iterable.class);
        register(standardLibrary.getIterator(), Iterator.class);
        register(standardLibrary.getMutableIterable(), Iterable.class);
        register(standardLibrary.getMutableIterator(), Iterator.class);
        
        register(standardLibrary.getCollection(), Collection.class);
        register(standardLibrary.getMutableCollection(), Collection.class);
        
        register(standardLibrary.getList(), List.class);
        register(standardLibrary.getMutableList(), List.class);
        
        register(standardLibrary.getSet(), Set.class);
        register(standardLibrary.getMutableSet(), Set.class);
        
        register(standardLibrary.getMap(), Map.class);
        register(standardLibrary.getMutableMap(), Map.class);

        register(standardLibrary.getMapEntry(), Map.Entry.class);
        register(standardLibrary.getMutableMapEntry(), Map.Entry.class);

        register(standardLibrary.getListIterator(), ListIterator.class);
        register(standardLibrary.getMutableListIterator(), ListIterator.class);
    }

    private void initPrimitives() {
        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            FqName className = jvmPrimitiveType.getPrimitiveType().getClassName();

            register(className, jvmPrimitiveType.getAsmType());
            registerNullable(className, jvmPrimitiveType.getWrapper().getAsmType());
        }
        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            register(primitiveType.getArrayClassName(), jvmPrimitiveType.getAsmArrayType());
        }
    }

    @Nullable
    public Type getJavaAnalog(@NotNull JetType jetType) {
        ClassifierDescriptor classifier = jetType.getConstructor().getDeclarationDescriptor();
        assert classifier != null;
        FqNameUnsafe className = DescriptorUtils.getFQName(classifier);
        if (!className.isSafe()) return null;
        FqName fqName = className.toSafe();
        if (jetType.isNullable()) {
            Type nullableType = asmNullableTypes.get(fqName);
            if (nullableType != null) {
                return nullableType;
            }
        }
        return asmTypes.get(fqName);
    }

    private void register(@NotNull ClassDescriptor kotlinDescriptor, @NotNull Class<?> javaClass) {
        register(kotlinDescriptor, AsmTypeConstants.getType(javaClass));
    }

    private void register(@NotNull ClassDescriptor kotlinDescriptor, @NotNull Type javaType) {
        FqNameUnsafe fqName = DescriptorUtils.getFQName(kotlinDescriptor);
        assert fqName.isSafe();
        register(fqName.toSafe(), javaType);
    }

    private void register(@NotNull FqName fqName, @NotNull Type type) {
        mappedTypeNames.add(type.getClassName());
        asmTypes.put(fqName, type);
    }

    private void registerNullable(@NotNull FqName fqName, @NotNull Type nullableType) {
        asmNullableTypes.put(fqName, nullableType);
    }

    public boolean isForceReal(@NotNull JvmClassName className) {
        return JvmPrimitiveType.getByWrapperClass(className) != null
               || mappedTypeNames.contains(className.getFqName().getFqName());
    }
}

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

package org.jetbrains.jet.codegen;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;
import org.jetbrains.jet.lang.types.ref.ClassName;

import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.codegen.JetTypeMapper.*;
import static org.jetbrains.jet.lang.types.lang.JetStandardLibraryNames.*;

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

    private final Map<FqNameUnsafe, Type> asmTypes = Maps.newHashMap();
    private final Map<FqNameUnsafe, Type> asmNullableTypes = Maps.newHashMap();
    private final Set<String> asmTypeNames = Sets.newHashSet();

    private KotlinToJavaTypesMap() {
        init();
    }

    @Nullable
    public Type getJavaAnalog(@NotNull JetType jetType) {
        ClassifierDescriptor classifier = jetType.getConstructor().getDeclarationDescriptor();
        assert classifier != null;
        FqNameUnsafe className = DescriptorUtils.getFQName(classifier);
        if (jetType.isNullable()) {
            Type nullableType = asmNullableTypes.get(className);
            if (nullableType != null) {
                return nullableType;
            }
        }
        return asmTypes.get(className);
    }

    private void register(@NotNull ClassName className, @NotNull Type type) {
        asmTypeNames.add(type.getClassName());
        asmTypes.put(className.getFqName().toUnsafe(), type);
    }

    private void registerNullable(@NotNull ClassName className, @NotNull Type nullableType) {
        asmNullableTypes.put(className.getFqName().toUnsafe(), nullableType);
    }

    public void init() {
        register(NOTHING, JET_NOTHING_TYPE);

        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            ClassName className = jvmPrimitiveType.getPrimitiveType().getClassName();

            register(className, jvmPrimitiveType.getAsmType());
            registerNullable(className, jvmPrimitiveType.getWrapper().getAsmType());
        }

        register(ANY, OBJECT_TYPE);
        register(NUMBER, JAVA_NUMBER_TYPE);
        register(STRING, JAVA_STRING_TYPE);
        register(CHAR_SEQUENCE, JAVA_CHAR_SEQUENCE_TYPE);
        register(THROWABLE, JAVA_THROWABLE_TYPE);
        register(COMPARABLE, JAVA_COMPARABLE_TYPE);
        register(ENUM, JAVA_ENUM_TYPE);
        register(ITERABLE, JAVA_ITERABLE_TYPE);
        register(ITERATOR, JAVA_ITERATOR_TYPE);
        register(MUTABLE_ITERABLE, JAVA_ITERABLE_TYPE);
        register(MUTABLE_ITERATOR, JAVA_ITERATOR_TYPE);

        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            register(primitiveType.getArrayClassName(), jvmPrimitiveType.getAsmArrayType());
        }
    }

    public boolean isForceReal(JvmClassName className) {
        return JvmPrimitiveType.getByWrapperClass(className) != null
               || asmTypeNames.contains(className.getFqName().getFqName());
    }
}

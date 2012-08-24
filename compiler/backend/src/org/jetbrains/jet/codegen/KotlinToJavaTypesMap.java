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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.JvmPrimitiveType;
import org.jetbrains.jet.lang.resolve.name.FqNameBase;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;
import org.jetbrains.jet.lang.types.ref.ClassName;

import java.util.Map;

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

    private final Map<SpecialTypeKey, Type> asmTypes = Maps.newHashMap();

    private KotlinToJavaTypesMap() {
        init();
    }

    @Nullable
    public Type getJavaAnalog(@NotNull JetType jetType) {
        ClassifierDescriptor classifier = jetType.getConstructor().getDeclarationDescriptor();
        assert classifier != null;
        return asmTypes.get(new SpecialTypeKey(DescriptorUtils.getFQName(classifier), jetType.isNullable()));
    }

    private void register(@NotNull ClassName className, @NotNull Type type) {
        register(className, type, type);
    }

    private void register(@NotNull ClassName className, @NotNull Type nonNullType, @NotNull Type nullableType) {
        asmTypes.put(new SpecialTypeKey(className.getFqName().toUnsafe(), true), nullableType);
        asmTypes.put(new SpecialTypeKey(className.getFqName().toUnsafe(), false), nonNullType);
    }

    public void init() {
        register(NOTHING, TYPE_NOTHING);

        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            register(primitiveType.getClassName(), jvmPrimitiveType.getAsmType(), jvmPrimitiveType.getWrapper().getAsmType());
        }

        register(NUMBER, JL_NUMBER_TYPE);
        register(STRING, JL_STRING_TYPE);
        register(CHAR_SEQUENCE, JL_CHAR_SEQUENCE_TYPE);
        register(THROWABLE, TYPE_THROWABLE);
        register(COMPARABLE, JL_COMPARABLE_TYPE);
        register(ENUM, JL_ENUM_TYPE);
        register(ITERABLE, JL_ITERABLE_TYPE);
        register(ITERATOR, JL_ITERATOR_TYPE);
        register(MUTABLE_ITERABLE, JL_ITERABLE_TYPE);
        register(MUTABLE_ITERATOR, JL_ITERATOR_TYPE);

        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            register(primitiveType.getArrayClassName(), jvmPrimitiveType.getAsmArrayType());
        }
    }

    public static boolean isForceReal(JvmClassName className) {
        return JvmPrimitiveType.getByWrapperClass(className) != null
               || className.getFqName().getFqName().equals("java.lang.String")
               || className.getFqName().getFqName().equals("java.lang.CharSequence")
               || className.getFqName().getFqName().equals("java.lang.Object")
               || className.getFqName().getFqName().equals("java.lang.Number")
               || className.getFqName().getFqName().equals("java.lang.Enum")
               || className.getFqName().getFqName().equals("java.lang.Comparable");
    }


    private static final class SpecialTypeKey {
        @NotNull
        private final FqNameUnsafe className;
        private final boolean nullable;

        private SpecialTypeKey(@NotNull FqNameUnsafe className, boolean nullable) {
            this.className = className;
            this.nullable = nullable;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SpecialTypeKey that = (SpecialTypeKey) o;

            if (nullable != that.nullable) return false;
            if (!className.equals(that.className)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = className.hashCode();
            result = 31 * result + (nullable ? 1 : 0);
            return result;
        }
    }
}

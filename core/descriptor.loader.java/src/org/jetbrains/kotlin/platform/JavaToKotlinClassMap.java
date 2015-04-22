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

package org.jetbrains.kotlin.platform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.resolve.DescriptorUtils;

import java.util.*;

public class JavaToKotlinClassMap extends JavaToKotlinClassMapBuilder implements PlatformToKotlinClassMap {
    public static final JavaToKotlinClassMap INSTANCE = new JavaToKotlinClassMap();

    private final Map<FqName, ClassDescriptor> javaToKotlin = new HashMap<FqName, ClassDescriptor>();
    private final Map<FqName, ClassDescriptor> javaToKotlinCovariant = new HashMap<FqName, ClassDescriptor>();
    private final Map<FqNameUnsafe, ClassId> kotlinToJava = new HashMap<FqNameUnsafe, ClassId>();

    private JavaToKotlinClassMap() {
        init();
    }

    /**
     * E.g.
     * java.lang.String -> kotlin.String
     * java.lang.Deprecated -> kotlin.deprecated
     * java.lang.Integer -> kotlin.Int
     * java.util.List -> kotlin.List
     * java.lang.Void -> null
     */
    @Nullable
    public ClassDescriptor mapJavaToKotlin(@NotNull FqName fqName) {
        return javaToKotlin.get(fqName);
    }

    /**
     * E.g.
     * java.util.Collection -> kotlin.MutableCollection
     * java.util.Map.Entry -> kotlin.MutableMap.MutableEntry
     * java.lang.String -> null
     */
    @Nullable
    public ClassDescriptor mapJavaToKotlinCovariant(@NotNull FqName fqName) {
        return javaToKotlinCovariant.get(fqName);
    }

    /**
     * E.g.
     * kotlin.Throwable -> java.lang.Throwable
     * kotlin.Int -> java.lang.Integer
     * kotlin.Nothing -> java.lang.Void
     * kotlin.IntArray -> null
     */
    @Nullable
    public ClassId mapKotlinToJava(@NotNull FqNameUnsafe kotlinFqName) {
        return kotlinToJava.get(kotlinFqName);
    }

    @Override
    protected void register(@NotNull ClassId javaClassId, @NotNull ClassDescriptor kotlinDescriptor, @NotNull Direction direction) {
        if (direction == Direction.BOTH || direction == Direction.JAVA_TO_KOTLIN) {
            addJavaToKotlin(javaClassId, kotlinDescriptor);
        }
        if (direction == Direction.BOTH || direction == Direction.KOTLIN_TO_JAVA) {
            addKotlinToJava(javaClassId, kotlinDescriptor);
        }
    }

    @Override
    protected void register(
            @NotNull ClassId javaClassId,
            @NotNull ClassDescriptor kotlinDescriptor,
            @NotNull ClassDescriptor kotlinMutableDescriptor
    ) {
        addJavaToKotlin(javaClassId, kotlinDescriptor);
        addJavaToKotlinCovariant(javaClassId, kotlinMutableDescriptor);
        addKotlinToJava(javaClassId, kotlinDescriptor);
        addKotlinToJava(javaClassId, kotlinMutableDescriptor);
    }

    private void addJavaToKotlin(@NotNull ClassId javaClassId, @NotNull ClassDescriptor kotlinDescriptor) {
        javaToKotlin.put(javaClassId.asSingleFqName(), kotlinDescriptor);
    }

    private void addJavaToKotlinCovariant(@NotNull ClassId javaClassId, @NotNull ClassDescriptor kotlinDescriptor) {
        javaToKotlinCovariant.put(javaClassId.asSingleFqName(), kotlinDescriptor);
    }

    private void addKotlinToJava(@NotNull ClassId javaClassId, @NotNull ClassDescriptor kotlinDescriptor) {
        kotlinToJava.put(DescriptorUtils.getFqName(kotlinDescriptor), javaClassId);
    }

    @NotNull
    public Collection<ClassDescriptor> mapPlatformClass(@NotNull FqName fqName) {
        ClassDescriptor kotlinAnalog = mapJavaToKotlin(fqName);
        ClassDescriptor kotlinCovariantAnalog = mapJavaToKotlinCovariant(fqName);
        List<ClassDescriptor> descriptors = new ArrayList<ClassDescriptor>(2);
        if (kotlinAnalog != null) {
            descriptors.add(kotlinAnalog);
        }
        if (kotlinCovariantAnalog != null) {
            descriptors.add(kotlinCovariantAnalog);
        }
        return descriptors;
    }

    @Override
    @NotNull
    public Collection<ClassDescriptor> mapPlatformClass(@NotNull ClassDescriptor classDescriptor) {
        FqNameUnsafe className = DescriptorUtils.getFqName(classDescriptor);
        if (!className.isSafe()) {
            return Collections.emptyList();
        }
        return mapPlatformClass(className.toSafe());
    }

    // TODO: get rid of this method, it's unclear what it does
    @NotNull
    public List<ClassDescriptor> allKotlinClasses() {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();

        List<ClassDescriptor> result = new ArrayList<ClassDescriptor>();
        result.addAll(javaToKotlin.values());
        result.addAll(javaToKotlinCovariant.values());

        for (PrimitiveType type : PrimitiveType.values()) {
            result.add(builtIns.getPrimitiveArrayClassDescriptor(type));
        }

        result.add(builtIns.getUnit());
        result.add(builtIns.getNothing());

        return result;
    }
}

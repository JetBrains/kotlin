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
import org.jetbrains.kotlin.builtins.CompanionObjectMapping;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.builtins.PrimitiveType;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType;

import java.lang.annotation.Annotation;
import java.util.*;

public class JavaToKotlinClassMap implements PlatformToKotlinClassMap {
    public static final JavaToKotlinClassMap INSTANCE = new JavaToKotlinClassMap();

    private final Map<FqName, ClassDescriptor> javaToKotlin = new HashMap<FqName, ClassDescriptor>();
    private final Map<FqNameUnsafe, ClassId> kotlinToJava = new HashMap<FqNameUnsafe, ClassId>();

    private final Map<ClassDescriptor, ClassDescriptor> mutableToReadOnly = new HashMap<ClassDescriptor, ClassDescriptor>();
    private final Map<ClassDescriptor, ClassDescriptor> readOnlyToMutable = new HashMap<ClassDescriptor, ClassDescriptor>();

    private JavaToKotlinClassMap() {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();

        add(Object.class, builtIns.getAny());
        add(String.class, builtIns.getString());
        add(CharSequence.class, builtIns.getCharSequence());
        add(Throwable.class, builtIns.getThrowable());
        add(Cloneable.class, builtIns.getCloneable());
        add(Number.class, builtIns.getNumber());
        add(Comparable.class, builtIns.getComparable());
        add(Enum.class, builtIns.getEnum());
        add(Annotation.class, builtIns.getAnnotation());

        add(Iterable.class, builtIns.getIterable(), builtIns.getMutableIterable());
        add(Iterator.class, builtIns.getIterator(), builtIns.getMutableIterator());
        add(Collection.class, builtIns.getCollection(), builtIns.getMutableCollection());
        add(List.class, builtIns.getList(), builtIns.getMutableList());
        add(Set.class, builtIns.getSet(), builtIns.getMutableSet());
        add(Map.class, builtIns.getMap(), builtIns.getMutableMap());
        add(Map.Entry.class, builtIns.getMapEntry(), builtIns.getMutableMapEntry());
        add(ListIterator.class, builtIns.getListIterator(), builtIns.getMutableListIterator());

        for (JvmPrimitiveType jvmType : JvmPrimitiveType.values()) {
            add(ClassId.topLevel(jvmType.getWrapperFqName()), builtIns.getPrimitiveClassDescriptor(jvmType.getPrimitiveType()));
        }

        for (ClassDescriptor descriptor : CompanionObjectMapping.allClassesWithIntrinsicCompanions()) {
            ClassDescriptor companion = descriptor.getCompanionObjectDescriptor();
            assert companion != null : "No companion object found for " + descriptor;
            add(ClassId.topLevel(new FqName("kotlin.jvm.internal." + descriptor.getName().asString() + "CompanionObject")), companion);
        }

        // TODO: support also functions with >= 23 parameters
        for (int i = 0; i < 23; i++) {
            add(ClassId.topLevel(new FqName("kotlin.jvm.functions.Function" + i)), builtIns.getFunction(i));

            for (String kFun : Arrays.asList(
                    "kotlin.reflect.KFunction",
                    "kotlin.reflect.KMemberFunction",
                    "kotlin.reflect.KExtensionFunction"
            )) {
                addKotlinToJava(ClassId.topLevel(new FqName(kFun)), new FqNameUnsafe(kFun + i));
            }
        }

        addJavaToKotlin(classId(Deprecated.class), builtIns.getDeprecatedAnnotation());

        addKotlinToJava(classId(Void.class), builtIns.getNothing());
    }

    /**
     * E.g.
     * java.lang.String -> kotlin.String
     * java.lang.Deprecated -> kotlin.deprecated
     * java.lang.Integer -> kotlin.Int
     * kotlin.jvm.internal.IntCompanionObject -> kotlin.Int.Companion
     * java.util.List -> kotlin.List
     * java.util.Map.Entry -> kotlin.Map.Entry
     * java.lang.Void -> null
     * kotlin.jvm.functions.Function3 -> kotlin.Function3
     */
    @Nullable
    public ClassDescriptor mapJavaToKotlin(@NotNull FqName fqName) {
        return javaToKotlin.get(fqName);
    }

    /**
     * E.g.
     * kotlin.Throwable -> java.lang.Throwable
     * kotlin.Int -> java.lang.Integer
     * kotlin.Int.Companion -> kotlin.jvm.internal.IntCompanionObject
     * kotlin.Nothing -> java.lang.Void
     * kotlin.IntArray -> null
     * kotlin.Function3 -> kotlin.jvm.functions.Function3
     * kotlin.reflect.KFunction3 -> kotlin.reflect.KFunction
     */
    @Nullable
    public ClassId mapKotlinToJava(@NotNull FqNameUnsafe kotlinFqName) {
        return kotlinToJava.get(kotlinFqName);
    }

    private void add(
            @NotNull Class<?> javaClass,
            @NotNull ClassDescriptor kotlinDescriptor,
            @NotNull ClassDescriptor kotlinMutableDescriptor
    ) {
        ClassId javaClassId = classId(javaClass);

        add(javaClassId, kotlinDescriptor);
        addKotlinToJava(javaClassId, kotlinMutableDescriptor);

        mutableToReadOnly.put(kotlinMutableDescriptor, kotlinDescriptor);
        readOnlyToMutable.put(kotlinDescriptor, kotlinMutableDescriptor);
    }

    private void add(@NotNull ClassId javaClassId, @NotNull ClassDescriptor kotlinDescriptor) {
        addJavaToKotlin(javaClassId, kotlinDescriptor);
        addKotlinToJava(javaClassId, kotlinDescriptor);
    }

    private void add(@NotNull Class<?> javaClass, @NotNull ClassDescriptor kotlinDescriptor) {
        add(classId(javaClass), kotlinDescriptor);
    }

    private void addJavaToKotlin(@NotNull ClassId javaClassId, @NotNull ClassDescriptor kotlinDescriptor) {
        javaToKotlin.put(javaClassId.asSingleFqName(), kotlinDescriptor);
    }

    private void addKotlinToJava(@NotNull ClassId javaClassId, @NotNull ClassDescriptor kotlinDescriptor) {
        addKotlinToJava(javaClassId, DescriptorUtils.getFqName(kotlinDescriptor));
    }

    private void addKotlinToJava(@NotNull ClassId javaClassId, @NotNull FqNameUnsafe kotlinFqName) {
        kotlinToJava.put(kotlinFqName, javaClassId);
    }

    @NotNull
    private static ClassId classId(@NotNull Class<?> clazz) {
        assert !clazz.isPrimitive() && !clazz.isArray() : "Invalid class: " + clazz;
        Class<?> outer = clazz.getDeclaringClass();
        return outer == null
               ? ClassId.topLevel(new FqName(clazz.getCanonicalName()))
               : classId(outer).createNestedClassId(Name.identifier(clazz.getSimpleName()));
    }

    @NotNull
    public Collection<ClassDescriptor> mapPlatformClass(@NotNull FqName fqName) {
        ClassDescriptor kotlinAnalog = mapJavaToKotlin(fqName);
        if (kotlinAnalog == null) return Collections.emptySet();

        ClassDescriptor kotlinMutableAnalog = readOnlyToMutable.get(kotlinAnalog);
        if (kotlinMutableAnalog == null) return Collections.singleton(kotlinAnalog);

        return Arrays.asList(kotlinAnalog, kotlinMutableAnalog);
    }

    @Override
    @NotNull
    public Collection<ClassDescriptor> mapPlatformClass(@NotNull ClassDescriptor classDescriptor) {
        FqNameUnsafe className = DescriptorUtils.getFqName(classDescriptor);
        return className.isSafe() ? mapPlatformClass(className.toSafe()) : Collections.<ClassDescriptor>emptySet();
    }

    public boolean isMutableCollection(@NotNull ClassDescriptor mutable) {
        return mutableToReadOnly.containsKey(mutable);
    }

    public boolean isReadOnlyCollection(@NotNull ClassDescriptor readOnly) {
        return readOnlyToMutable.containsKey(readOnly);
    }

    @NotNull
    public ClassDescriptor convertMutableToReadOnly(@NotNull ClassDescriptor mutable) {
        ClassDescriptor readOnly = mutableToReadOnly.get(mutable);
        if (readOnly == null) {
            throw new IllegalArgumentException("Given class " + mutable + " is not a mutable collection");
        }
        return readOnly;
    }

    @NotNull
    public ClassDescriptor convertReadOnlyToMutable(@NotNull ClassDescriptor readOnly) {
        ClassDescriptor mutable = readOnlyToMutable.get(readOnly);
        if (mutable == null) {
            throw new IllegalArgumentException("Given class " + readOnly + " is not a read-only collection");
        }
        return mutable;
    }

    // TODO: get rid of this method, it's unclear what it does
    @NotNull
    public List<ClassDescriptor> allKotlinClasses() {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();

        List<ClassDescriptor> result = new ArrayList<ClassDescriptor>();
        result.addAll(javaToKotlin.values());
        result.addAll(readOnlyToMutable.values());

        for (PrimitiveType type : PrimitiveType.values()) {
            result.add(builtIns.getPrimitiveArrayClassDescriptor(type));
        }

        result.add(builtIns.getUnit());
        result.add(builtIns.getNothing());

        return result;
    }
}

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

import com.google.common.collect.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
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
public class JavaToKotlinClassMap implements PlatformToKotlinClassMap {
    private static JavaToKotlinClassMap instance = null;

    @NotNull
    public static JavaToKotlinClassMap getInstance() {
        if (instance == null) {
            instance = new JavaToKotlinClassMap();
        }
        return instance;
    }

    private final Map<FqName, ClassDescriptor> classDescriptorMap = Maps.newHashMap();
    private final Map<FqName, ClassDescriptor> classDescriptorMapForCovariantPositions = Maps.newHashMap();
    private final Map<String, JetType> primitiveTypesMap = Maps.newHashMap();
    private final Multimap<FqName, ClassDescriptor> packagesWithMappedClasses = HashMultimap.create();

    private JavaToKotlinClassMap() {
        init();
        initPrimitives();
    }

    private void init() {
        JetStandardLibrary standardLibrary = JetStandardLibrary.getInstance();

        register(Object.class, JetStandardClasses.getAny());
        register(String.class, standardLibrary.getString());
        register(CharSequence.class, standardLibrary.getCharSequence());
        register(Throwable.class, standardLibrary.getThrowable());
        register(Number.class, standardLibrary.getNumber());
        register(Comparable.class, standardLibrary.getComparable());
        register(Enum.class, standardLibrary.getEnum());
        register(Annotation.class, standardLibrary.getAnnotation());
        register(Iterable.class, standardLibrary.getIterable());
        register(Iterator.class, standardLibrary.getIterator());

        registerCovariant(Iterable.class, standardLibrary.getMutableIterable());
        registerCovariant(Iterator.class, standardLibrary.getMutableIterator());

        register(Collection.class, standardLibrary.getCollection());
        registerCovariant(Collection.class, standardLibrary.getMutableCollection());

        register(List.class, standardLibrary.getList());
        registerCovariant(List.class, standardLibrary.getMutableList());

        register(Set.class, standardLibrary.getSet());
        registerCovariant(Set.class, standardLibrary.getMutableSet());

        register(Map.class, standardLibrary.getMap());
        registerCovariant(Map.class, standardLibrary.getMutableMap());

        register(Map.Entry.class, standardLibrary.getMapEntry());
        registerCovariant(Map.Entry.class, standardLibrary.getMutableMapEntry());

        register(ListIterator.class, standardLibrary.getListIterator());
        registerCovariant(ListIterator.class, standardLibrary.getMutableListIterator());
    }

    private void initPrimitives() {
        JetStandardLibrary standardLibrary = JetStandardLibrary.getInstance();
        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            register(jvmPrimitiveType.getWrapper().getFqName(), standardLibrary.getPrimitiveClassDescriptor(primitiveType));
        }

        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            primitiveTypesMap.put(jvmPrimitiveType.getName(), JetStandardLibrary.getInstance().getPrimitiveJetType(primitiveType));
            primitiveTypesMap.put("[" + jvmPrimitiveType.getName(), JetStandardLibrary.getInstance().getPrimitiveArrayJetType(primitiveType));
            primitiveTypesMap.put(jvmPrimitiveType.getWrapper().getFqName().getFqName(), JetStandardLibrary.getInstance().getNullablePrimitiveJetType(
                    primitiveType));
        }
        primitiveTypesMap.put("void", JetStandardClasses.getUnitType());
    }

    @Nullable
    public JetType mapPrimitiveKotlinClass(@NotNull String name) {
        return primitiveTypesMap.get(name);
    }

    @Nullable
    public ClassDescriptor mapKotlinClass(@NotNull FqName fqName, @NotNull JavaTypeTransformer.TypeUsage typeUsage) {
        if (typeUsage == JavaTypeTransformer.TypeUsage.MEMBER_SIGNATURE_COVARIANT
                || typeUsage == JavaTypeTransformer.TypeUsage.SUPERTYPE) {
            ClassDescriptor descriptor = classDescriptorMapForCovariantPositions.get(fqName);
            if (descriptor != null) {
                return descriptor;
            }
        }
        return classDescriptorMap.get(fqName);
    }

    private static FqName getJavaClassFqName(@NotNull Class<?> javaClass) {
        return new FqName(javaClass.getName().replace("$", "."));
    }

    private void register(@NotNull Class<?> javaClass, @NotNull ClassDescriptor kotlinDescriptor) {
        register(getJavaClassFqName(javaClass), kotlinDescriptor);
    }

    private void register(@NotNull FqName javaClassName, @NotNull ClassDescriptor kotlinDescriptor) {
        classDescriptorMap.put(javaClassName, kotlinDescriptor);
        packagesWithMappedClasses.put(javaClassName.parent(), kotlinDescriptor);
    }

    private void registerCovariant(@NotNull Class<?> javaClass, @NotNull ClassDescriptor kotlinDescriptor) {
        registerCovariant(getJavaClassFqName(javaClass), kotlinDescriptor);
    }

    private void registerCovariant(@NotNull FqName javaClassName, @NotNull ClassDescriptor kotlinDescriptor) {
        classDescriptorMapForCovariantPositions.put(javaClassName, kotlinDescriptor);
        packagesWithMappedClasses.put(javaClassName.parent(), kotlinDescriptor);
    }

    @Override
    @NotNull
    public Collection<ClassDescriptor> mapPlatformClass(@NotNull ClassDescriptor classDescriptor) {
        FqNameUnsafe className = DescriptorUtils.getFQName(classDescriptor);
        if (!className.isSafe()) {
            return Collections.emptyList();
        }
        FqName fqName = className.toSafe();
        ClassDescriptor kotlinAnalog = classDescriptorMap.get(fqName);
        ClassDescriptor kotlinCovariantAnalog = classDescriptorMapForCovariantPositions.get(fqName);
        List<ClassDescriptor> descriptors = Lists.newArrayList();
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
    public Collection<ClassDescriptor> mapPlatformClassesInside(@NotNull DeclarationDescriptor containingDeclaration) {
        FqNameUnsafe fqName = DescriptorUtils.getFQName(containingDeclaration);
        if (!fqName.isSafe()) {
            return Collections.emptyList();
        }
        return packagesWithMappedClasses.get(fqName.toSafe());
    }
}

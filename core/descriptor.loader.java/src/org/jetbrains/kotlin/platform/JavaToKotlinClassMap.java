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
import org.jetbrains.kotlin.load.java.components.TypeUsage;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType;

import java.util.*;

public class JavaToKotlinClassMap extends JavaToKotlinClassMapBuilder implements PlatformToKotlinClassMap {
    public static final JavaToKotlinClassMap INSTANCE = new JavaToKotlinClassMap();

    private final Map<FqName, ClassDescriptor> classDescriptorMap = new HashMap<FqName, ClassDescriptor>();
    private final Map<FqName, ClassDescriptor> classDescriptorMapForCovariantPositions = new HashMap<FqName, ClassDescriptor>();
    private final Set<ClassDescriptor> allKotlinClasses = new LinkedHashSet<ClassDescriptor>();

    private JavaToKotlinClassMap() {
        init();
        initPrimitives();
    }

    private void initPrimitives() {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();

        for (JvmPrimitiveType jvmType : JvmPrimitiveType.values()) {
            PrimitiveType type = jvmType.getPrimitiveType();
            register(jvmType.getWrapperFqName(), builtIns.getPrimitiveClassDescriptor(type));
            allKotlinClasses.add(builtIns.getPrimitiveArrayClassDescriptor(type));
        }

        allKotlinClasses.add(builtIns.getUnit());
    }

    @Nullable
    public ClassDescriptor mapKotlinClass(@NotNull FqName fqName, @NotNull TypeUsage typeUsage) {
        if (typeUsage == TypeUsage.MEMBER_SIGNATURE_COVARIANT || typeUsage == TypeUsage.SUPERTYPE) {
            ClassDescriptor descriptor = classDescriptorMapForCovariantPositions.get(fqName);
            if (descriptor != null) {
                return descriptor;
            }
        }
        return classDescriptorMap.get(fqName);
    }

    @NotNull
    private static FqName fqNameByClass(@NotNull Class<?> clazz) {
        return new FqName(clazz.getCanonicalName());
    }

    @Override
    protected void register(@NotNull Class<?> javaClass, @NotNull ClassDescriptor kotlinDescriptor, @NotNull Direction direction) {
        if (direction == Direction.BOTH || direction == Direction.JAVA_TO_KOTLIN) {
            register(fqNameByClass(javaClass), kotlinDescriptor);
        }
    }

    @Override
    protected void register(
            @NotNull Class<?> javaClass,
            @NotNull ClassDescriptor kotlinDescriptor,
            @NotNull ClassDescriptor kotlinMutableDescriptor
    ) {
        FqName javaClassName = fqNameByClass(javaClass);
        register(javaClassName, kotlinDescriptor);
        registerCovariant(javaClassName, kotlinMutableDescriptor);
    }

    private void register(@NotNull FqName javaClassName, @NotNull ClassDescriptor kotlinDescriptor) {
        classDescriptorMap.put(javaClassName, kotlinDescriptor);
        allKotlinClasses.add(kotlinDescriptor);
    }

    private void registerCovariant(@NotNull FqName javaClassName, @NotNull ClassDescriptor kotlinDescriptor) {
        classDescriptorMapForCovariantPositions.put(javaClassName, kotlinDescriptor);
        allKotlinClasses.add(kotlinDescriptor);
    }

    @NotNull
    public Collection<ClassDescriptor> mapPlatformClass(@NotNull FqName fqName) {
        ClassDescriptor kotlinAnalog = classDescriptorMap.get(fqName);
        ClassDescriptor kotlinCovariantAnalog = classDescriptorMapForCovariantPositions.get(fqName);
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

    @NotNull
    public Set<ClassDescriptor> allKotlinClasses() {
        return Collections.unmodifiableSet(allKotlinClasses);
    }
}

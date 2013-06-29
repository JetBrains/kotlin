/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.intellij.psi.CommonClassNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;

import java.util.*;

public class JavaToKotlinClassMap extends JavaToKotlinClassMapBuilder implements PlatformToKotlinClassMap {
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

    private void initPrimitives() {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            register(jvmPrimitiveType.getWrapper().getFqName(), builtIns.getPrimitiveClassDescriptor(primitiveType));
        }

        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            primitiveTypesMap.put(jvmPrimitiveType.getName(), KotlinBuiltIns.getInstance().getPrimitiveJetType(primitiveType));
            primitiveTypesMap.put("[" + jvmPrimitiveType.getName(), KotlinBuiltIns.getInstance().getPrimitiveArrayJetType(primitiveType));
            primitiveTypesMap.put(jvmPrimitiveType.getWrapper().getFqName().asString(), KotlinBuiltIns.getInstance().getNullablePrimitiveJetType(
                    primitiveType));
        }
        primitiveTypesMap.put("void", KotlinBuiltIns.getInstance().getUnitType());
    }

    @Nullable
    public JetType mapPrimitiveKotlinClass(@NotNull String name) {
        return primitiveTypesMap.get(name);
    }

    @Nullable
    public ClassDescriptor mapKotlinClass(@NotNull FqName fqName, @NotNull TypeUsage typeUsage) {
        if (typeUsage == TypeUsage.MEMBER_SIGNATURE_COVARIANT
                || typeUsage == TypeUsage.SUPERTYPE) {
            ClassDescriptor descriptor = classDescriptorMapForCovariantPositions.get(fqName);
            if (descriptor != null) {
                return descriptor;
            }
        }
        return classDescriptorMap.get(fqName);
    }

    @Nullable
    public AnnotationDescriptor mapToAnnotationClass(@NotNull FqName fqName) {
        ClassDescriptor classDescriptor = classDescriptorMap.get(fqName);
        if (classDescriptor != null && fqName.asString().equals(CommonClassNames.JAVA_LANG_DEPRECATED)) {
            return DescriptorResolverUtils.getAnnotationDescriptorForJavaLangDeprecated(classDescriptor);
        }
        return null;
    }

    private static FqName getJavaClassFqName(@NotNull Class<?> javaClass) {
        return new FqName(javaClass.getName().replace('$', '.'));
    }

    @Override
    protected void register(
            @NotNull Class<?> javaClass,
            @NotNull ClassDescriptor kotlinDescriptor,
            @NotNull Direction direction
    ) {
        if (direction == Direction.BOTH || direction == Direction.JAVA_TO_KOTLIN) {
            register(getJavaClassFqName(javaClass), kotlinDescriptor);
        }
    }

    @Override
    protected void register(
            @NotNull Class<?> javaClass,
            @NotNull ClassDescriptor kotlinDescriptor,
            @NotNull ClassDescriptor kotlinMutableDescriptor,
            @NotNull Direction direction
    ) {
        if (direction == Direction.BOTH || direction == Direction.JAVA_TO_KOTLIN) {
            FqName javaClassName = getJavaClassFqName(javaClass);
            register(javaClassName, kotlinDescriptor);
            registerCovariant(javaClassName, kotlinMutableDescriptor);
        }
    }

    private void register(@NotNull FqName javaClassName, @NotNull ClassDescriptor kotlinDescriptor) {
        classDescriptorMap.put(javaClassName, kotlinDescriptor);
        packagesWithMappedClasses.put(javaClassName.parent(), kotlinDescriptor);
    }

    private void registerCovariant(@NotNull FqName javaClassName, @NotNull ClassDescriptor kotlinDescriptor) {
        classDescriptorMapForCovariantPositions.put(javaClassName, kotlinDescriptor);
        packagesWithMappedClasses.put(javaClassName.parent(), kotlinDescriptor);
    }

    @NotNull
    public Collection<ClassDescriptor> mapPlatformClass(@NotNull FqName fqName) {
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
    public Collection<ClassDescriptor> mapPlatformClass(@NotNull ClassDescriptor classDescriptor) {
        FqNameUnsafe className = DescriptorUtils.getFQName(classDescriptor);
        if (!className.isSafe()) {
            return Collections.emptyList();
        }
        return mapPlatformClass(className.toSafe());
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

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;

import java.util.Map;

/**
 * @author svtk
 */
public class JavaToKotlinTypesMap {
    private static JavaToKotlinTypesMap instance = null;

    @NotNull
    public static JavaToKotlinTypesMap getInstance() {
        if (instance == null) {
            instance = new JavaToKotlinTypesMap();
        }
        return instance;
    }

    private final Map<FqName, ClassDescriptor> classDescriptorMap = Maps.newHashMap();
    private final Map<FqName, ClassDescriptor> classDescriptorMapForCovariantPositions = Maps.newHashMap();
    private final Map<String, JetType> primitiveTypesMap = Maps.newHashMap();

    private JavaToKotlinTypesMap() {
        init();
        initPrimitive();
    }

    @Nullable
    public JetType getPrimitiveKotlinAnalog(@NotNull String name) {
        return primitiveTypesMap.get(name);
    }

    @Nullable
    public ClassDescriptor getKotlinAnalog(@NotNull FqName fqName, @NotNull JavaTypeTransformer.TypeUsage typeUsage) {
        if (typeUsage == JavaTypeTransformer.TypeUsage.MEMBER_SIGNATURE_COVARIANT) {
            ClassDescriptor descriptor = classDescriptorMapForCovariantPositions.get(fqName);
            if (descriptor != null) {
                return descriptor;
            }
        }
        return classDescriptorMap.get(fqName);
    }

    private void init() {
        JetStandardLibrary standardLibrary = JetStandardLibrary.getInstance();

        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            register(jvmPrimitiveType.getWrapper().getFqName(), standardLibrary.getPrimitiveClassDescriptor(primitiveType));
        }
        register("java.lang.Object", JetStandardClasses.getAny());
        register("java.lang.String", standardLibrary.getString());
        register("java.lang.CharSequence", standardLibrary.getCharSequence());
        register("java.lang.Throwable", standardLibrary.getThrowable());
        register("java.lang.Number", standardLibrary.getNumber());
        register("java.lang.Comparable", standardLibrary.getComparable());
        register("java.lang.Enum", standardLibrary.getEnum());
        register("java.lang.Iterable", standardLibrary.getIterable());
        register("java.util.Iterator", standardLibrary.getIterator());

        registerCovariant("java.lang.Iterable", standardLibrary.getMutableIterable());
        registerCovariant("java.util.Iterator", standardLibrary.getMutableIterator());
    }

    private void register(String className, ClassDescriptor classDescriptor) {
        register(new FqName(className), classDescriptor);
    }

    private void register(FqName fqName, ClassDescriptor classDescriptor) {
        classDescriptorMap.put(fqName, classDescriptor);
    }

    private void registerCovariant(String className, ClassDescriptor classDescriptor) {
        registerCovariant(new FqName(className), classDescriptor);
    }

    private void registerCovariant(FqName fqName, ClassDescriptor classDescriptor) {
        classDescriptorMapForCovariantPositions.put(fqName, classDescriptor);
    }

    private void initPrimitive() {
        for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
            PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
            primitiveTypesMap.put(jvmPrimitiveType.getName(), JetStandardLibrary.getInstance().getPrimitiveJetType(primitiveType));
            primitiveTypesMap.put("[" + jvmPrimitiveType.getName(), JetStandardLibrary.getInstance().getPrimitiveArrayJetType(primitiveType));
            primitiveTypesMap.put(jvmPrimitiveType.getWrapper().getFqName().getFqName(), JetStandardLibrary.getInstance().getNullablePrimitiveJetType(
                    primitiveType));
        }
        primitiveTypesMap.put("void", JetStandardClasses.getUnitType());
    }
}

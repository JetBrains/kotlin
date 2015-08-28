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

package org.jetbrains.kotlin.load.kotlin;

import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.serialization.ClassData;
import org.jetbrains.kotlin.serialization.PackageData;
import org.jetbrains.kotlin.serialization.deserialization.ClassDataProvider;
import org.jetbrains.kotlin.serialization.deserialization.DeserializationComponents;
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedNewPackageMemberScope;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPackageMemberScope;
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader.Kind.CLASS;

public final class DeserializedDescriptorResolver {
    private final ErrorReporter errorReporter;
    private DeserializationComponents components;

    public DeserializedDescriptorResolver(@NotNull ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    // component dependency cycle
    @Inject
    public void setComponents(@NotNull DeserializationComponentsForJava context) {
        this.components = context.getComponents();
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull KotlinJvmBinaryClass kotlinClass) {
        String[] data = readData(kotlinClass, CLASS);
        if (data != null) {
            ClassData classData = JvmProtoBufUtil.readClassDataFrom(data);
            KotlinJvmBinarySourceElement sourceElement = new KotlinJvmBinarySourceElement(kotlinClass);
            ClassDataProvider classDataProvider = new ClassDataProvider(classData, sourceElement);
            return components.getClassDeserializer().deserializeClass(kotlinClass.getClassId(), classDataProvider);
        }
        return null;
    }

    @Nullable
    public JetScope createKotlinPackageScope(@NotNull PackageFragmentDescriptor descriptor, @NotNull KotlinJvmBinaryClass kotlinClass) {
        //TODO add assertion from readData(kotlinClass, CLASS);
        String[] data = kotlinClass.getClassHeader().getAnnotationData();
        if (data != null) {
            //all classes are included in java scope
            PackageData packageData = JvmProtoBufUtil.readPackageDataFrom(data);
            return new DeserializedPackageMemberScope(
                    descriptor, packageData.getPackageProto(), packageData.getNameResolver(), components,
                    new Function0<Collection<Name>>() {
                        @Override
                        public Collection<Name> invoke() {
                            return Collections.emptyList();
                        }
                    }
            );
        }
        return null;
    }

    @NotNull
    public JetScope createKotlinNewPackageScope(@NotNull PackageFragmentDescriptor descriptor, @NotNull List<KotlinJvmBinaryClass> packageParts) {
        List<JetScope> list = new ArrayList<JetScope>();
        for (KotlinJvmBinaryClass callable : packageParts) {
            JetScope scope = createKotlinPackageScope(descriptor, callable);
            assert scope != null : "Can't create scope for " + callable;
            list.add(scope);
        }
        return new DeserializedNewPackageMemberScope(descriptor, list);
    }

    @Nullable
    public String[] readData(@NotNull KotlinJvmBinaryClass kotlinClass, @NotNull KotlinClassHeader.Kind expectedKind) {
        KotlinClassHeader header = kotlinClass.getClassHeader();
        if (!header.getIsCompatibleAbiVersion()) {
            errorReporter.reportIncompatibleAbiVersion(kotlinClass.getClassId(), kotlinClass.getLocation(), header.getVersion());
        }
        else if (header.getKind() == expectedKind) {
            return header.getAnnotationData();
        }

        return null;
    }
}

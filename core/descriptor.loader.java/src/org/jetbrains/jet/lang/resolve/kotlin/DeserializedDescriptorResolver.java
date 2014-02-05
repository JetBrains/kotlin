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

package org.jetbrains.jet.lang.resolve.kotlin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.ClassData;
import org.jetbrains.jet.descriptors.serialization.ClassId;
import org.jetbrains.jet.descriptors.serialization.DescriptorFinder;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBufUtil;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaPackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.storage.StorageManager;

import javax.inject.Inject;
import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils.kotlinFqNameToJavaFqName;
import static org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader.Kind.CLASS;
import static org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader.Kind.PACKAGE_FACADE;

public final class DeserializedDescriptorResolver {
    private AnnotationDescriptorDeserializer annotationDeserializer;

    private StorageManager storageManager;

    private JavaPackageFragmentProvider javaPackageFragmentProvider;

    private JavaDescriptorResolver javaDescriptorResolver;

    private ErrorReporter errorReporter;

    @NotNull
    private final DescriptorFinder javaDescriptorFinder = new DescriptorFinder() {
        @Nullable
        @Override
        public ClassDescriptor findClass(@NotNull ClassId classId) {
            return javaDescriptorResolver.resolveClass(kotlinFqNameToJavaFqName(classId.asSingleFqName()));
        }

        @NotNull
        @Override
        public Collection<Name> getClassNames(@NotNull FqName packageName) {
            return javaPackageFragmentProvider.getClassNamesInPackage(packageName);
        }
    };

    @Inject
    public void setAnnotationDeserializer(AnnotationDescriptorDeserializer annotationDeserializer) {
        this.annotationDeserializer = annotationDeserializer;
    }

    @Inject
    public void setJavaPackageFragmentProvider(JavaPackageFragmentProvider javaPackageFragmentProvider) {
        this.javaPackageFragmentProvider = javaPackageFragmentProvider;
    }

    @Inject
    public void setJavaDescriptorResolver(JavaDescriptorResolver javaDescriptorResolver) {
        this.javaDescriptorResolver = javaDescriptorResolver;
    }

    @Inject
    public void setErrorReporter(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @Inject
    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull KotlinJvmBinaryClass kotlinClass) {
        String[] data = readData(kotlinClass, CLASS);
        if (data != null) {
            ClassData classData = JavaProtoBufUtil.readClassDataFrom(data);
            return new DeserializedClassDescriptor(storageManager, annotationDeserializer, javaDescriptorFinder,
                                                   javaPackageFragmentProvider, classData.getNameResolver(), classData.getClassProto());
        }
        return null;
    }

    @Nullable
    public JetScope createKotlinPackageScope(@NotNull PackageFragmentDescriptor descriptor, @NotNull KotlinJvmBinaryClass kotlinClass) {
        String[] data = readData(kotlinClass, PACKAGE_FACADE);
        if (data != null) {
            return new DeserializedPackageMemberScope(storageManager, descriptor, annotationDeserializer, javaDescriptorFinder,
                                                      JavaProtoBufUtil.readPackageDataFrom(data));
        }
        return null;
    }

    @Nullable
    private String[] readData(@NotNull KotlinJvmBinaryClass kotlinClass, @NotNull KotlinClassHeader.Kind expectedKind) {
        KotlinClassHeader header = kotlinClass.getClassHeader();
        if (header == null) return null;

        if (header.getKind() == KotlinClassHeader.Kind.INCOMPATIBLE_ABI_VERSION) {
            errorReporter.reportIncompatibleAbiVersion(kotlinClass, header.getVersion());
        }
        else if (header.getKind() == expectedKind) {
            return header.getAnnotationData();
        }

        return null;
    }
}

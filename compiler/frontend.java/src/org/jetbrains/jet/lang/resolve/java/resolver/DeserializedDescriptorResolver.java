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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import javax.inject.Inject;
import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.java.AbiVersionUtil.isAbiVersionCompatible;
import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;
import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.INCLUDE_KOTLIN_SOURCES;
import static org.jetbrains.jet.lang.resolve.java.resolver.DeserializedResolverUtils.kotlinFqNameToJavaFqName;

public final class DeserializedDescriptorResolver {

    private AnnotationDescriptorDeserializer annotationDeserializer;

    private final LockBasedStorageManager storageManager = new LockBasedStorageManager();

    private JavaNamespaceResolver javaNamespaceResolver;

    private JavaClassResolver javaClassResolver;

    @NotNull
    private final DescriptorFinder javaDescriptorFinder = new DescriptorFinder() {
        @Nullable
        @Override
        public ClassDescriptor findClass(@NotNull ClassId classId) {
            return javaClassResolver.resolveClass(kotlinFqNameToJavaFqName(classId.asSingleFqName()), IGNORE_KOTLIN_SOURCES);
        }

        @Nullable
        @Override
        public NamespaceDescriptor findPackage(@NotNull FqName name) {
            return javaNamespaceResolver.resolveNamespace(name, IGNORE_KOTLIN_SOURCES);
        }

        @NotNull
        @Override
        public Collection<Name> getClassNames(@NotNull FqName packageName) {
            return javaNamespaceResolver.getClassNamesInPackage(packageName);
        }
    };

    @Inject
    public void setAnnotationDeserializer(AnnotationDescriptorDeserializer annotationDeserializer) {
        this.annotationDeserializer = annotationDeserializer;
    }

    @Inject
    public void setJavaNamespaceResolver(JavaNamespaceResolver javaNamespaceResolver) {
        this.javaNamespaceResolver = javaNamespaceResolver;
    }

    @Inject
    public void setJavaClassResolver(JavaClassResolver javaClassResolver) {
        this.javaClassResolver = javaClassResolver;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull ClassId id, @NotNull VirtualFile file, @NotNull ErrorReporter reporter) {
        ClassData classData = readClassDataFromClassFile(file, reporter);
        if (classData == null) {
            return null;
        }
        return createDeserializedClass(classData, id);
    }

    @Nullable
    public JetScope createKotlinPackageScope(
            @NotNull NamespaceDescriptor descriptor,
            @NotNull VirtualFile file,
            @NotNull ErrorReporter reporter
    ) {
        PackageData packageData = readPackageDataFromClassFile(file, reporter);
        if (packageData == null) {
            return null;
        }
        return new DeserializedPackageMemberScope(storageManager, descriptor, annotationDeserializer, javaDescriptorFinder, packageData);
    }

    @Nullable
    private ClassDescriptor createDeserializedClass(@NotNull ClassData classData, @NotNull ClassId classId) {
        DeclarationDescriptor owner = classId.isTopLevelClass()
                                      ? javaNamespaceResolver.resolveNamespace(classId.getPackageFqName(), INCLUDE_KOTLIN_SOURCES)
                                      : javaClassResolver.resolveClass(kotlinFqNameToJavaFqName(classId.getOuterClassId().asSingleFqName()),
                                                                       IGNORE_KOTLIN_SOURCES);
        assert owner != null : "No owner found for " + classId;

        return new DeserializedClassDescriptor(classId, storageManager, owner, classData.getNameResolver(),
                                               annotationDeserializer, javaDescriptorFinder, classData.getClassProto(), null);
    }

    @Nullable
    private static ClassData readClassDataFromClassFile(@NotNull VirtualFile file, @NotNull ErrorReporter reporter) {
        String[] data = readData(file, reporter);
        return data == null ? null : JavaProtoBufUtil.readClassDataFrom(data);
    }

    @Nullable
    private static PackageData readPackageDataFromClassFile(@NotNull VirtualFile file, @NotNull ErrorReporter reporter) {
        String[] data = readData(file, reporter);
        return data == null ? null : JavaProtoBufUtil.readPackageDataFrom(data);
    }

    @Nullable
    private static String[] readData(@NotNull VirtualFile virtualFile, @NotNull ErrorReporter reporter) {
        KotlinClassFileHeader header = KotlinClassFileHeader.readKotlinHeaderFromClassFile(virtualFile);
        int version = header.getVersion();
        if (!isAbiVersionCompatible(version) && header.getType() != KotlinClassFileHeader.HeaderType.NONE) {
            reporter.reportIncompatibleAbiVersion(version);
            return null;
        }
        return header.getAnnotationData();
    }
}

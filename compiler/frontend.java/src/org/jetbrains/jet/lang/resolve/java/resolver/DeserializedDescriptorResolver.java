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
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import javax.inject.Inject;
import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.java.AbiVersionUtil.isAbiVersionCompatible;
import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.INCLUDE_KOTLIN;
import static org.jetbrains.jet.lang.resolve.java.resolver.DeserializedResolverUtils.getVirtualFile;
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
            return javaClassResolver.resolveClass(kotlinFqNameToJavaFqName(classId.asSingleFqName()));
        }

        @Nullable
        @Override
        public NamespaceDescriptor findPackage(@NotNull FqName name) {
            return javaNamespaceResolver.resolveNamespace(name);
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
    public ClassDescriptor resolveClass(
            @NotNull FqName fqName,
            @NotNull PsiClass psiClass,
            @NotNull ClassOrNamespaceDescriptor containingDeclaration,
            @NotNull ErrorReporter reporter
    ) {
        VirtualFile virtualFile = getVirtualFile(psiClass, fqName, containingDeclaration);
        if (virtualFile == null) {
            // TODO: use ErrorReporter here
            return null;
        }
        ClassData classData = readClassDataFromClassFile(virtualFile, reporter);
        if (classData == null) {
            return null;
        }
        return deserializeClass(classData, fqName, containingDeclaration);
    }

    @Nullable
    public JetScope createKotlinPackageScope(
            @NotNull PsiClass kotlinPackagePsiClass,
            @NotNull NamespaceDescriptor packageDescriptor,
            @NotNull ErrorReporter reporter
    ) {
        VirtualFile virtualFile = kotlinPackagePsiClass.getContainingFile().getVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        PackageData packageData = readPackageDataFromClassFile(virtualFile, reporter);
        if (packageData == null) {
            return null;
        }
        return new DeserializedPackageMemberScope(storageManager, packageDescriptor, annotationDeserializer, javaDescriptorFinder,
                                                  packageData);
    }

    @Nullable
    private ClassDescriptor deserializeClass(
            @NotNull ClassData classData,
            @NotNull FqName fqName,
            @NotNull ClassOrNamespaceDescriptor containingDeclaration
    ) {
        ClassId classId = ClassId.fromFqNameAndContainingDeclaration(fqName, containingDeclaration);

        DeclarationDescriptor owner = classId.isTopLevelClass()
                                      ? javaNamespaceResolver.resolveNamespace(classId.getPackageFqName(), INCLUDE_KOTLIN)
                                      : javaClassResolver
                                              .resolveClass(kotlinFqNameToJavaFqName(classId.getOuterClassId().asSingleFqName()));
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
        KotlinClassFileHeader headerData = KotlinClassFileHeader.readKotlinHeaderFromClassFile(virtualFile);
        int version = headerData.getVersion();
        String[] annotationData = headerData.getAnnotationData();
        if (isAbiVersionCompatible(version)) {
            return annotationData;
        }
        if (annotationData != null) {
            reporter.reportIncompatibleAbiVersion(version);
        }
        return null;
    }
}

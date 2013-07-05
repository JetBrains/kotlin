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
import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.ClassReader;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.AbiVersionUtil;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.utils.ExceptionUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.asm4.ClassReader.*;
import static org.jetbrains.jet.lang.resolve.java.AbiVersionUtil.isAbiVersionCompatible;
import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.INCLUDE_KOTLIN;
import static org.jetbrains.jet.lang.resolve.java.resolver.DeserializedResolverUtils.getVirtualFile;
import static org.jetbrains.jet.lang.resolve.java.resolver.DeserializedResolverUtils.kotlinFqNameToJavaFqName;

public final class DeserializedDescriptorResolver {
    @Nullable
    public static ClassData readClassDataNoErrorReporting(@NotNull VirtualFile virtualFile) {
        String[] data = visitClassFile(virtualFile).getData();
        return data != null ? JavaProtoBufUtil.readClassDataFrom(data) : null;
    }

    @Nullable
    public static PackageData readPackageDataNoErrorReporting(@NotNull VirtualFile virtualFile) {
        String[] data = visitClassFile(virtualFile).getData();
        return data != null ? JavaProtoBufUtil.readPackageDataFrom(data) : null;
    }

    private AnnotationDescriptorDeserializer annotationDeserializer;

    private final LockBasedStorageManager storageManager = new LockBasedStorageManager();

    private JavaNamespaceResolver javaNamespaceResolver;

    private JavaClassResolver javaClassResolver;

    private ErrorReporter errorReporter;

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

    @Inject
    public void setErrorReporter(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @Nullable
    public ClassDescriptor resolveClass(
            @NotNull FqName fqName,
            @NotNull PsiClass psiClass,
            @NotNull ClassOrNamespaceDescriptor containingDeclaration
    ) {
        VirtualFile virtualFile = getVirtualFile(psiClass, fqName, containingDeclaration);
        if (virtualFile == null) {
            // TODO: use ErrorReporter here
            return null;
        }
        ClassData classData = readClassDataFromClassFile(virtualFile, psiClass);
        if (classData == null) {
            return null;
        }
        return deserializeClass(classData, fqName, containingDeclaration);
    }

    @Nullable
    public JetScope createKotlinPackageScope(@NotNull PsiClass kotlinPackagePsiClass, @NotNull NamespaceDescriptor packageDescriptor) {
        VirtualFile virtualFile = kotlinPackagePsiClass.getContainingFile().getVirtualFile();
        if (virtualFile == null) {
            return null;
        }
        PackageData packageData = readPackageDataFromClassFile(virtualFile, kotlinPackagePsiClass);
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
                                      : javaClassResolver.resolveClass(kotlinFqNameToJavaFqName(classId.getOuterClassId().asSingleFqName()));
        assert owner != null : "No owner found for " + classId;

        return new DeserializedClassDescriptor(classId, storageManager, owner, classData.getNameResolver(),
                                               annotationDeserializer, javaDescriptorFinder, classData.getClassProto(), null);
    }

    @Nullable
    private ClassData readClassDataFromClassFile(@NotNull VirtualFile virtualFile, @NotNull PsiClass psiClass) {
        String[] data = readData(virtualFile, psiClass);
        return data == null ? null : JavaProtoBufUtil.readClassDataFrom(data);
    }

    @Nullable
    private PackageData readPackageDataFromClassFile(@NotNull VirtualFile virtualFile, @NotNull PsiClass psiClass) {
        String[] data = readData(virtualFile, psiClass);
        return data == null ? null : JavaProtoBufUtil.readPackageDataFrom(data);
    }

    @Nullable
    private String[] readData(@NotNull VirtualFile virtualFile, @NotNull PsiClass psiClass) {
        GetKotlinInfoDataVisitor visitor = visitClassFile(virtualFile);
        int version = visitor.getVersion();
        String[] data = visitor.getData();
        if (isAbiVersionCompatible(version)) {
            return data;
        }
        if (data != null) {
            errorReporter.reportIncompatibleAbiVersion(psiClass, version);
        }
        return null;
    }

    @NotNull
    private static GetKotlinInfoDataVisitor visitClassFile(@NotNull VirtualFile virtualFile) {
        try {
            InputStream inputStream = virtualFile.getInputStream();
            try {
                ClassReader reader = new ClassReader(inputStream);
                GetKotlinInfoDataVisitor visitor = new GetKotlinInfoDataVisitor();
                reader.accept(visitor, SKIP_CODE | SKIP_FRAMES | SKIP_DEBUG);
                return visitor;
            }
            finally {
                inputStream.close();
            }
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    private static class GetKotlinInfoDataVisitor extends ClassVisitor {
        public GetKotlinInfoDataVisitor() {
            super(Opcodes.ASM4);
        }

        private int version = AbiVersionUtil.INVALID_VERSION;

        @Nullable
        private String[] data = null;

        @Override
        public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
            if (!desc.equals(JvmStdlibNames.KOTLIN_CLASS.getDescriptor()) &&
                !desc.equals(JvmStdlibNames.KOTLIN_PACKAGE.getDescriptor())) {
                return null;
            }

            return new AnnotationVisitor(Opcodes.ASM4) {
                @Override
                public void visit(String name, Object value) {
                    if (name.equals(JvmStdlibNames.ABI_VERSION_NAME)) {
                        version = (Integer) value;
                    }
                    else if (isAbiVersionCompatible(version)) {
                        throw new IllegalStateException("Unexpected argument " + name + " for annotation " + desc);
                    }
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    if (name.equals(JvmStdlibNames.KOTLIN_INFO_DATA_FIELD)) {
                        return stringArrayVisitor();
                    }
                    else if (isAbiVersionCompatible(version)) {
                        throw new IllegalStateException("Unexpected array argument " + name + " for annotation " + desc);
                    }

                    return super.visitArray(name);
                }

                @NotNull
                private AnnotationVisitor stringArrayVisitor() {
                    final List<String> strings = new ArrayList<String>(1);
                    return new AnnotationVisitor(Opcodes.ASM4) {
                        @Override
                        public void visit(String name, Object value) {
                            if (!(value instanceof String)) {
                                throw new IllegalStateException("Unexpected argument value: " + value);
                            }

                            strings.add((String) value);
                        }

                        @Override
                        public void visitEnd() {
                            data = strings.toArray(new String[strings.size()]);
                        }
                    };
                }
            };
        }

        public int getVersion() {
            return version;
        }

        @Nullable
        public String[] getData() {
            return data;
        }
    }
}

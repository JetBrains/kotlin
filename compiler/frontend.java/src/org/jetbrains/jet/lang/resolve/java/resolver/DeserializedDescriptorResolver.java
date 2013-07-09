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
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.utils.ExceptionUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.asm4.ClassReader.*;
import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.INCLUDE_KOTLIN;
import static org.jetbrains.jet.lang.resolve.java.resolver.DeserializedResolverUtils.getVirtualFile;
import static org.jetbrains.jet.lang.resolve.java.resolver.DeserializedResolverUtils.kotlinFqNameToJavaFqName;

public final class DeserializedDescriptorResolver {
    public static final String KOTLIN_INFO_TYPE = JvmStdlibNames.KOTLIN_INFO_CLASS.getAsmType().toString();

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
            @NotNull ClassOrNamespaceDescriptor containingDeclaration
    ) {
        VirtualFile virtualFile = getVirtualFile(psiClass, fqName, containingDeclaration);
        if (virtualFile == null) {
            return null;
        }
        ClassData classData = readClassDataFromClassFile(virtualFile);
        if (classData == null) {
            return null;
        }
        return deserializeClass(classData, fqName, containingDeclaration);
    }

    @NotNull
    public JetScope createKotlinPackageScope(
            @NotNull FqName fqName,
            @NotNull PsiClass kotlinPackagePsiClass,
            @NotNull NamespaceDescriptor packageDescriptor
    ) {
        VirtualFile virtualFile = kotlinPackagePsiClass.getContainingFile().getVirtualFile();
        if (virtualFile == null) {
            throw new IllegalStateException("Could not find virtual file for " + fqName.asString());
        }
        PackageData packageData = readPackageDataFromClassFile(virtualFile);
        if (packageData == null) {
            throw new IllegalStateException("No KotlinInfo annotation stored for " + fqName.asString());
        }
        return DeserializedPackageMemberScope.createScopeFromPackageData(packageDescriptor, packageData, javaDescriptorFinder,
                                                                         annotationDeserializer, storageManager);
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
    private static ClassData readClassDataFromClassFile(@NotNull VirtualFile virtualFile) {
        String[] data = getKotlinInfoDataFromClassFile(virtualFile);
        if (data == null) return null;
        return JavaProtoBufUtil.readClassDataFrom(data);
    }

    @Nullable
    private static PackageData readPackageDataFromClassFile(@NotNull VirtualFile virtualFile) {
        String[] data = getKotlinInfoDataFromClassFile(virtualFile);
        if (data == null) return null;
        return JavaProtoBufUtil.readPackageDataFrom(data);
    }

    @Nullable
    private static String[] getKotlinInfoDataFromClassFile(@NotNull VirtualFile virtualFile) {
        GetKotlinInfoDataVisitor visitor = visitClassFile(virtualFile);
        if (visitor == null) {
            return null;
        }
        String[] data = visitor.getData();
        if (data == null) {
            return null;
        }
        return data;
    }

    @Nullable
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

        @Nullable
        private String[] data = null;

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (!desc.equals(KOTLIN_INFO_TYPE)) {
                return null;
            }

            return new AnnotationVisitor(Opcodes.ASM4) {
                @Override
                public AnnotationVisitor visitArray(String name) {
                    if (!name.equals("data")) {
                        throw new IllegalStateException("Unexpected argument " + name + " for annotation " + KOTLIN_INFO_TYPE);
                    }

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

        @Nullable
        public String[] getData() {
            return data;
        }
    }
}

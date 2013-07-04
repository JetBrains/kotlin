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
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.utils.ExceptionUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.asm4.ClassReader.*;
import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.INCLUDE_KOTLIN;

public final class DeserializedDescriptorResolver {

    private static final String KOTLIN_INFO_TYPE = JvmStdlibNames.KOTLIN_INFO_CLASS.getAsmType().toString();

    public static final AnnotationDeserializer DUMMY_ANNOTATION_DESERIALIZER = new AnnotationDeserializer() {
        @NotNull
        @Override
        public List<AnnotationDescriptor> loadClassAnnotations(@NotNull ProtoBuf.Class classProto) {
            // This is a hack for tests: only data annotations are present in test data so far
            AnnotationDescriptor annotationDescriptor = new AnnotationDescriptor();
            annotationDescriptor.setAnnotationType(KotlinBuiltIns.getInstance().getDataClassAnnotation().getDefaultType());
            return Collections.singletonList(annotationDescriptor);
        }

        @NotNull
        @Override
        public List<AnnotationDescriptor> loadCallableAnnotations(@NotNull ProtoBuf.Callable callableProto) {
            throw new UnsupportedOperationException(); // TODO
        }

        @NotNull
        @Override
        public List<AnnotationDescriptor> loadSetterAnnotations(@NotNull ProtoBuf.Callable callableProto) {
            throw new UnsupportedOperationException(); // TODO
        }

        @NotNull
        @Override
        public List<AnnotationDescriptor> loadValueParameterAnnotations(@NotNull ProtoBuf.Callable.ValueParameter parameterProto) {
            throw new UnsupportedOperationException(); // TODO
        }
    };

    @NotNull
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
    };

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
                                                                         DUMMY_ANNOTATION_DESERIALIZER, storageManager);
    }

    @Nullable
    private static VirtualFile getVirtualFile(
            @NotNull PsiClass psiClass,
            @NotNull FqName classFqName,
            @NotNull ClassOrNamespaceDescriptor containingDeclaration
    ) {
        VirtualFile mostOuterClassVirtualFile = psiClass.getContainingFile().getVirtualFile();
        if (mostOuterClassVirtualFile == null) {
            throw new IllegalStateException("Could not find virtual file for " + classFqName.asString());
        }
        String fileExtension = mostOuterClassVirtualFile.getExtension();
        if (fileExtension == null || !fileExtension.equals("class")) {
            return null;
        }
        ClassId id = ClassId.fromFqNameAndContainingDeclaration(classFqName, containingDeclaration);
        FqNameUnsafe relativeClassName = id.getRelativeClassName();
        assert relativeClassName.isSafe() : "Relative class name " + relativeClassName.asString() + " should be safe at this point";
        String classNameWithBucks = relativeClassName.asString().replace(".", "$") + ".class";
        VirtualFile virtualFile = mostOuterClassVirtualFile.getParent().findChild(classNameWithBucks);
        if (virtualFile == null) {
            throw new IllegalStateException("No virtual file for " + classFqName.asString());
        }
        return virtualFile;
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
                                               DUMMY_ANNOTATION_DESERIALIZER, javaDescriptorFinder, classData.getClassProto(), null);
    }

    @Nullable
    private static ClassData readClassDataFromClassFile(@NotNull VirtualFile virtualFile) {
        byte[] data = getKotlinInfoDataFromClassFile(virtualFile);
        if (data == null) return null;
        return ClassData.read(data);
    }

    @Nullable
    private static PackageData readPackageDataFromClassFile(@NotNull VirtualFile virtualFile) {
        byte[] data = getKotlinInfoDataFromClassFile(virtualFile);
        if (data == null) return null;
        return PackageData.read(data);
    }

    @Nullable
    private static byte[] getKotlinInfoDataFromClassFile(@NotNull VirtualFile virtualFile) {
        GetKotlinInfoDataVisitor visitor = visitClassFile(virtualFile);
        if (visitor == null) {
            return null;
        }
        byte[] data = visitor.getData();
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
        private byte[] data = null;

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (!desc.equals(KOTLIN_INFO_TYPE)) {
                return null;
            }
            return new AnnotationVisitor(Opcodes.ASM4) {
                @Override
                public void visit(String name, Object value) {
                    if (name.equals("data")) {
                        data = (byte[]) value;
                    }
                    else {
                        throw new IllegalStateException("Unexpected property " + name + " for annotation " + KOTLIN_INFO_TYPE);
                    }
                }
            };
        }

        @Nullable
        public byte[] getData() {
            return data;
        }
    }

    @NotNull
    private static FqName kotlinFqNameToJavaFqName(@NotNull FqNameUnsafe kotlinFqName) {
        List<String> correctedSegments = new ArrayList<String>();
        for (Name segment : kotlinFqName.pathSegments()) {
            if (segment.asString().startsWith("<class-object-for")) {
                correctedSegments.add(JvmAbi.CLASS_OBJECT_CLASS_NAME);
            }
            else {
                assert !segment.isSpecial();
                correctedSegments.add(segment.asString());
            }
        }
        return FqName.fromSegments(correctedSegments);
    }
}

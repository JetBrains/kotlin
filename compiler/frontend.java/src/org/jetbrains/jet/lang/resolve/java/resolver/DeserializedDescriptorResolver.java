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

import com.intellij.openapi.diagnostic.Logger;
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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.lazy.storage.LockBasedStorageManager;
import org.jetbrains.jet.lang.resolve.lazy.storage.StorageManager;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.asm4.ClassReader.*;
import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.INCLUDE_KOTLIN;

public final class DeserializedDescriptorResolver {

    private static final Logger LOG = Logger.getInstance(DeserializedDescriptorResolver.class);
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

    @Nullable
    private static VirtualFile getVirtualFile(
            @NotNull PsiClass psiClass,
            @NotNull FqName classFqName,
            @NotNull ClassOrNamespaceDescriptor containingDeclaration
    ) {
        VirtualFile mostOuterClassVirtualFile = psiClass.getContainingFile().getVirtualFile();
        if (mostOuterClassVirtualFile == null) {
            LOG.error("No virtual file for " + psiClass.getQualifiedName());
            return null;
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
            LOG.error("No virtual file for " + psiClass.getQualifiedName());
            return null;
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
        AbstractClassResolver classResolver =
                new DeserializedClassResolver(storageManager, DUMMY_ANNOTATION_DESERIALIZER, classId, classData);
        ClassDescriptor classDescriptor = classResolver.findClassInternally(classId);
        assert classDescriptor != null : "Could not correctly deserialize class " + fqName.asString();
        return classDescriptor;
    }

    @Nullable
    private static ClassData readClassDataFromClassFile(@NotNull VirtualFile virtualFile) {
        try {
            InputStream inputStream = virtualFile.getInputStream();
            try {
                ClassReader reader = new ClassReader(inputStream);
                GetKotlinInfoDataVisitor visitor = new GetKotlinInfoDataVisitor();
                reader.accept(visitor, SKIP_CODE | SKIP_FRAMES | SKIP_DEBUG);
                return visitor.getClassData();
            }
            finally {
                inputStream.close();
            }
        }
        catch (IOException e) {
            LOG.error(e);
        }
        return null;
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
                        LOG.error("Unexpected property " + name + " for annotation " + KOTLIN_INFO_TYPE);
                    }
                }
            };
        }

        @Nullable
        private ClassData getClassData() {
            if (data == null) {
                return null;
            }
            return ClassSerializationUtil.readClassDataFrom(data);
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

    public class DeserializedClassResolver extends AbstractClassResolver {
        @NotNull
        private final ClassId classID;
        @NotNull
        private final ClassData classData;

        public DeserializedClassResolver(
                @NotNull StorageManager storageManager,
                @NotNull AnnotationDeserializer annotationDeserializer,
                @NotNull ClassId id,
                @NotNull ClassData data
        ) {
            super(storageManager, annotationDeserializer);
            this.classID = id;
            this.classData = data;
        }

        @Nullable
        @Override
        protected ClassData getClassData(@NotNull ClassId classId) {
            if (!classId.equals(classID)) {
                return null;
            }
            return classData;
        }

        @NotNull
        @Override
        protected DeclarationDescriptor getPackage(@NotNull FqName fqName) {
            NamespaceDescriptor namespaceDescriptor = javaNamespaceResolver.resolveNamespace(fqName, INCLUDE_KOTLIN);
            assert namespaceDescriptor != null;
            return namespaceDescriptor;
        }

        @NotNull
        @Override
        protected ClassId getClassId(@NotNull ClassDescriptor classDescriptor) {
            return ClassSerializationUtil.getClassId(classDescriptor, DescriptorNamer.DEFAULT);
        }

        @Nullable
        @Override
        protected ClassDescriptor resolveClassExternally(@NotNull ClassId classId) {
            return javaClassResolver.resolveClass(kotlinFqNameToJavaFqName(classId.asSingleFqName()));
        }

        @NotNull
        @Override
        protected Name getClassObjectName(@NotNull ClassDescriptor outerClass) {
            return DescriptorUtils.getClassObjectName(outerClass.getName());
        }

        @Override
        protected void classDescriptorCreated(@NotNull ClassDescriptor classDescriptor) {
            //nothing to do here
        }
    }
}

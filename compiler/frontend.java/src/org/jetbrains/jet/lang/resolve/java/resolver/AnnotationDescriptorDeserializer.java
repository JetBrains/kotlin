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
import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.ClassReader;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationDeserializer;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.PsiClassFinder;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.utils.ExceptionUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.asm4.ClassReader.*;
import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN;
import static org.jetbrains.jet.lang.resolve.java.resolver.DeserializedResolverUtils.*;

public class AnnotationDescriptorDeserializer implements AnnotationDeserializer {
    private PsiClassFinder psiClassFinder;

    private JavaClassResolver javaClassResolver;

    @Inject
    public void setPsiClassFinder(PsiClassFinder psiClassFinder) {
        this.psiClassFinder = psiClassFinder;
    }

    @Inject
    public void setJavaClassResolver(JavaClassResolver javaClassResolver) {
        this.javaClassResolver = javaClassResolver;
    }

    @NotNull
    @Override
    public List<AnnotationDescriptor> loadClassAnnotations(
            @NotNull ClassDescriptor descriptor,
            @NotNull ProtoBuf.Class classProto
    ) {
        FqName fqName = kotlinFqNameToJavaFqName(naiveKotlinFqName(descriptor));
        PsiClass psiClass = psiClassFinder.findPsiClass(fqName, PsiClassFinder.RuntimeClassesHandleMode.IGNORE /* TODO: ?! */);
        if (psiClass == null) {
            throw new IllegalStateException("Psi class is not found for class: " + descriptor);
        }
        VirtualFile virtualFile = getVirtualFile(psiClass, fqName, (ClassOrNamespaceDescriptor) descriptor.getContainingDeclaration());
        if (virtualFile == null) {
            throw new IllegalStateException("Virtual file is not found for class: " + descriptor) ;
        }

        try {
            return loadClassAnnotationsFromFile(virtualFile);
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    @NotNull
    private List<AnnotationDescriptor> loadClassAnnotationsFromFile(@NotNull VirtualFile virtualFile) throws IOException {
        final List<AnnotationDescriptor> result = new ArrayList<AnnotationDescriptor>();

        new ClassReader(virtualFile.getInputStream()).accept(new ClassVisitor(Opcodes.ASM4) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (ignoreAnnotation(desc)) return null;

                FqName annotationFqName = convertJvmDescriptorToFqName(desc);
                ClassDescriptor annotationClass = javaClassResolver.resolveClass(annotationFqName, ERROR_IF_FOUND_IN_KOTLIN);
                assert annotationClass != null : "Annotation class is not found: " + desc;
                return resolveAnnotation(annotationClass, result);
            }
        }, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);

        return result;
    }

    private static boolean ignoreAnnotation(@NotNull String desc) {
        // TODO: JvmAbi.JETBRAINS_NOT_NULL_ANNOTATION ?
        return desc.equals(DeserializedDescriptorResolver.KOTLIN_INFO_TYPE) || desc.startsWith("Ljet/runtime/typeinfo/");
    }

    @NotNull
    private static FqName convertJvmDescriptorToFqName(@NotNull String desc) {
        assert desc.startsWith("L") && desc.endsWith(";") : "Not a JVM descriptor: " + desc;
        String fqName = desc.substring(1, desc.length() - 1).replace('$', '.').replace('/', '.');
        return new FqName(fqName);
    }

    @NotNull
    private static AnnotationVisitor resolveAnnotation(
            @NotNull final ClassDescriptor annotationClass,
            @NotNull final List<AnnotationDescriptor> result
    ) {
        final AnnotationDescriptor annotation = new AnnotationDescriptor();
        annotation.setAnnotationType(annotationClass.getDefaultType());

        return new AnnotationVisitor(Opcodes.ASM4) {
            // TODO: arrays, annotations, enums
            @Override
            public void visit(String name, Object value) {
                ValueParameterDescriptor parameter =
                        DescriptorResolverUtils.getValueParameterDescriptorForAnnotationParameter(Name.identifier(name), annotationClass);
                if (parameter != null) {
                    CompileTimeConstant<?> argument = JavaCompileTimeConstResolver.resolveCompileTimeConstantValue(value, null);
                    if (argument != null) {
                        annotation.setValueArgument(parameter, argument);
                    }
                }
            }

            @Override
            public void visitEnd() {
                result.add(annotation);
            }
        };
    }

    @NotNull
    @Override
    public List<AnnotationDescriptor> loadCallableAnnotations(@NotNull ProtoBuf.Callable callableProto) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public List<AnnotationDescriptor> loadValueParameterAnnotations(@NotNull ProtoBuf.Callable.ValueParameter parameterProto) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public List<AnnotationDescriptor> loadSetterAnnotations(@NotNull ProtoBuf.Callable callableProto) {
        throw new UnsupportedOperationException(); // TODO
    }
}

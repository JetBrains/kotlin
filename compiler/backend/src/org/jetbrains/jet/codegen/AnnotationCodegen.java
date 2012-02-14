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

package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.types.JetType;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * @author alex.tkachman
 */
public abstract class AnnotationCodegen {
    public void genAnnotations(Annotated annotated, JetTypeMapper typeMapper) {
        if(annotated == null)
            return;

        List<AnnotationDescriptor> annotations = annotated.getAnnotations();
        if(annotations == null)
            return;

        for (AnnotationDescriptor annotationDescriptor : annotations) {
            List<CompileTimeConstant<?>> valueArguments = annotationDescriptor.getValueArguments();
            if(!valueArguments.isEmpty()) {
                throw new UnsupportedOperationException("Only annotations without values are supported by backend so far");
            }

            JetType type = annotationDescriptor.getType();
            ClassifierDescriptor classifierDescriptor = type.getConstructor().getDeclarationDescriptor();
            RetentionPolicy rp = getRetentionPolicy(classifierDescriptor, typeMapper);
            if(rp != RetentionPolicy.SOURCE) {
                String internalName = typeMapper.mapType(type).getDescriptor();
                AnnotationVisitor annotationVisitor = visitAnnotation(internalName, rp == RetentionPolicy.RUNTIME);
                annotationVisitor.visitEnd();
            }
        }
    }

    private static RetentionPolicy getRetentionPolicy(ClassifierDescriptor descriptor, JetTypeMapper typeMapper) {
        RetentionPolicy rp = RetentionPolicy.RUNTIME;
        /*
        @todo : when JavaDescriptoResolver provides ennough info
        for (AnnotationDescriptor annotationDescriptor : descriptor.getAnnotations()) {
            String internalName = typeMapper.mapType(annotationDescriptor.getType()).getInternalName();
            if("java/lang/annotation/RetentionPolicy".equals(internalName)) {
                CompileTimeConstant<?> compileTimeConstant = annotationDescriptor.getValueArguments().get(0);
                System.out.println(compileTimeConstant);
                break;
            }
        }
        */
        return rp;  //To change body of created methods use File | Settings | File Templates.
    }

    abstract AnnotationVisitor visitAnnotation(String descr, boolean visible);

    public static AnnotationCodegen forClass(final ClassVisitor cv) {
        return new AnnotationCodegen() {
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return cv.visitAnnotation(descr, visible);
            }
        };
    }

    public static AnnotationCodegen forMethod(final MethodVisitor mv) {
        return new AnnotationCodegen() {
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return mv.visitAnnotation(descr, visible);
            }
        };
    }

    public static AnnotationCodegen forField(final FieldVisitor mv) {
        return new AnnotationCodegen() {
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return mv.visitAnnotation(descr, visible);
            }
        };
    }
}

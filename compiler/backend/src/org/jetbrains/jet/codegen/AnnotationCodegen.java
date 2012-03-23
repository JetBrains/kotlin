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

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.DefaultValueArgument;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.types.JetType;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;

/**
 * @author alex.tkachman
 */
public abstract class AnnotationCodegen {
    public void genAnnotations(Annotated annotated, JetTypeMapper typeMapper) {
        if(annotated == null)
            return;
        
        if(!(annotated instanceof DeclarationDescriptor))
            return;

        BindingContext bindingContext = typeMapper.bindingContext;
        PsiElement psiElement = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, (DeclarationDescriptor) annotated);

        JetModifierList modifierList = null;
        if(annotated instanceof ConstructorDescriptor && psiElement instanceof JetClass) {
            modifierList = ((JetClass)psiElement).getPrimaryConstructorModifierList();
        }
        else if (psiElement instanceof JetModifierListOwner) {
            modifierList = ((JetModifierListOwner) psiElement).getModifierList();
        }

        if(modifierList == null)
            return;

        List<JetAnnotationEntry> annotationEntries = modifierList.getAnnotationEntries();
        for (JetAnnotationEntry annotationEntry : annotationEntries) {
            ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, annotationEntry.getCalleeExpression());
            assert resolvedCall != null;

            AnnotationDescriptor annotationDescriptor = bindingContext.get(BindingContext.ANNOTATION, annotationEntry);
            assert annotationDescriptor != null;

            JetType type = annotationDescriptor.getType();
            ClassifierDescriptor classifierDescriptor = type.getConstructor().getDeclarationDescriptor();
            RetentionPolicy rp = getRetentionPolicy(classifierDescriptor, typeMapper);
            if(rp != RetentionPolicy.SOURCE) {
                String internalName = typeMapper.mapType(type).getDescriptor();
                AnnotationVisitor annotationVisitor = visitAnnotation(internalName, rp == RetentionPolicy.RUNTIME);

                for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : resolvedCall.getValueArguments().entrySet()) {
                    ResolvedValueArgument valueArgument = entry.getValue();
                    if (!(valueArgument instanceof DefaultValueArgument)) {
                        List<ValueArgument> valueArguments = valueArgument.getArguments();
                        assert  valueArguments.size() == 1 : "Number of assertions on " + resolvedCall.getResultingDescriptor() + " = " + valueArguments.size(); // todo
                        CompileTimeConstant<?> compileTimeConstant = bindingContext.get(BindingContext.COMPILE_TIME_VALUE, valueArguments.get(0).getArgumentExpression());
                        assert compileTimeConstant != null;

                        Object value = compileTimeConstant.getValue();
                        annotationVisitor.visit(entry.getKey().getName(), value);
                    }
                }

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

    public static AnnotationCodegen forField(final FieldVisitor fv) {
        return new AnnotationCodegen() {
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return fv.visitAnnotation(descr, visible);
            }
        };
    }
}

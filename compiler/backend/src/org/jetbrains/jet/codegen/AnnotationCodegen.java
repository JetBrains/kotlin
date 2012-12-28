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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.FieldVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.VarargValueArgument;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;

import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.BindingContextUtils.descriptorToDeclaration;

public abstract class AnnotationCodegen {
    private final JetTypeMapper typeMapper;
    private final BindingContext bindingContext;

    private AnnotationCodegen(JetTypeMapper mapper) {
        typeMapper = mapper;
        bindingContext = typeMapper.getBindingContext();
    }

    public void genAnnotations(Annotated annotated) {
        if (annotated == null) {
            return;
        }

        if (!(annotated instanceof DeclarationDescriptor)) {
            return;
        }

        PsiElement psiElement = descriptorToDeclaration(bindingContext, (DeclarationDescriptor) annotated);

        JetModifierList modifierList = null;
        if (annotated instanceof ConstructorDescriptor && psiElement instanceof JetClass) {
            modifierList = ((JetClass) psiElement).getPrimaryConstructorModifierList();
        }
        else if (psiElement instanceof JetModifierListOwner) {
            modifierList = ((JetModifierListOwner) psiElement).getModifierList();
        }

        if (modifierList == null) {
            return;
        }

        List<JetAnnotationEntry> annotationEntries = modifierList.getAnnotationEntries();
        for (JetAnnotationEntry annotationEntry : annotationEntries) {
            ResolvedCall<? extends CallableDescriptor> resolvedCall =
                    bindingContext.get(BindingContext.RESOLVED_CALL, annotationEntry.getCalleeExpression());
            if (resolvedCall == null) continue; // Skipping annotations if they are not resolved. Needed for JetLightClass generation

            AnnotationDescriptor annotationDescriptor = bindingContext.get(BindingContext.ANNOTATION, annotationEntry);
            if (annotationDescriptor == null) continue; // Skipping annotations if they are not resolved. Needed for JetLightClass generation

            JetType type = annotationDescriptor.getType();
            genAnnotation(resolvedCall, type);
        }
    }

    private void genAnnotation(
            ResolvedCall<? extends CallableDescriptor> resolvedCall,
            JetType type
    ) {
        ClassifierDescriptor classifierDescriptor = type.getConstructor().getDeclarationDescriptor();
        RetentionPolicy rp = getRetentionPolicy(classifierDescriptor, typeMapper);
        if (rp == RetentionPolicy.SOURCE) {
            return;
        }

        String internalName = typeMapper.mapType(type).getDescriptor();
        AnnotationVisitor annotationVisitor = visitAnnotation(internalName, rp == RetentionPolicy.RUNTIME);

        getAnnotation(resolvedCall, annotationVisitor);

        annotationVisitor.visitEnd();
    }

    private void getAnnotation(ResolvedCall<? extends CallableDescriptor> resolvedCall, AnnotationVisitor annotationVisitor) {
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : resolvedCall.getValueArguments().entrySet()) {
            ResolvedValueArgument valueArgument = entry.getValue();
            if (valueArgument instanceof DefaultValueArgument) {
                continue;
            }

            Name keyName = entry.getKey().getName();
            genAnnotationValueArgument(annotationVisitor, valueArgument, keyName.getName());
        }
    }

    private void genAnnotationValueArgument(AnnotationVisitor annotationVisitor, ResolvedValueArgument valueArgument, String keyName) {
        List<ValueArgument> valueArguments = valueArgument.getArguments();
        if (valueArgument instanceof VarargValueArgument) {
            AnnotationVisitor visitor = annotationVisitor.visitArray(keyName);
            for (ValueArgument argument : valueArguments) {
                genAnnotationExpressionValue(visitor, null, argument.getArgumentExpression());
            }
            visitor.visitEnd();
        }
        else {
            assert valueArguments.size() == 1 : "Number of arguments on " + keyName + " = " + valueArguments.size(); // todo
            JetExpression expression = valueArguments.get(0).getArgumentExpression();
            genAnnotationExpressionValue(annotationVisitor, keyName, expression);
        }
    }

    private void genAnnotationExpressionValue(AnnotationVisitor annotationVisitor, @Nullable String keyName, JetExpression expression) {
        CompileTimeConstant<?> compileTimeConstant =
                bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expression);

        if (compileTimeConstant != null) {
            Object value = compileTimeConstant.getValue();
            annotationVisitor.visit(keyName, value);
            return;
        }

        if (expression instanceof JetDotQualifiedExpression) {
            JetDotQualifiedExpression qualifiedExpression = (JetDotQualifiedExpression) expression;
            ResolvedCall<? extends CallableDescriptor> call =
                    bindingContext.get(BindingContext.RESOLVED_CALL, qualifiedExpression.getSelectorExpression());
            if (call != null) {
                if (call.getResultingDescriptor() instanceof PropertyDescriptor) {
                    PropertyDescriptor descriptor = (PropertyDescriptor) call.getResultingDescriptor();
                    annotationVisitor.visitEnum(keyName, typeMapper.mapType(descriptor).getDescriptor(), descriptor.getName().getName());
                    return;
                }
            }
        }
        else {
            if (expression instanceof JetCallExpression) {
                JetCallExpression callExpression = (JetCallExpression) expression;
                ResolvedCall<? extends CallableDescriptor> call =
                        bindingContext.get(BindingContext.RESOLVED_CALL, callExpression.getCalleeExpression());
                if (call != null) {
                    List<AnnotationDescriptor> annotations = call.getResultingDescriptor().getOriginal().getAnnotations();
                    String value = null;
                    if (annotations != null) {
                        for (AnnotationDescriptor annotation : annotations) {
                            //noinspection ConstantConditions
                            if ("Intrinsic".equals(annotation.getType().getConstructor().getDeclarationDescriptor().getName().getName())) {
                                value = (String) annotation.getAllValueArguments().values().iterator().next().getValue();
                                break;
                            }
                        }
                    }
                    if (IntrinsicMethods.KOTLIN_JAVA_CLASS_FUNCTION.equals(value)) {
                        //noinspection ConstantConditions
                        annotationVisitor.visit(keyName, typeMapper
                                .mapType(call.getResultingDescriptor().getReturnType().getArguments().get(0).getType()));
                        return;
                    }
                    else if (IntrinsicMethods.KOTLIN_ARRAYS_ARRAY.equals(value)) {
                        AnnotationVisitor visitor = annotationVisitor.visitArray(keyName);
                        VarargValueArgument next = (VarargValueArgument) call.getValueArguments().values().iterator().next();
                        for (ValueArgument argument : next.getArguments()) {
                            genAnnotationExpressionValue(visitor, null, argument.getArgumentExpression());
                        }
                        visitor.visitEnd();
                        return;
                    }
                    else if (call.getResultingDescriptor() instanceof ConstructorDescriptor) {
                        ConstructorDescriptor descriptor = (ConstructorDescriptor) call.getResultingDescriptor();
                        AnnotationVisitor visitor = annotationVisitor.visitAnnotation(keyName, typeMapper
                                .mapType(descriptor.getContainingDeclaration()).getDescriptor());
                        getAnnotation(call, visitor);
                        visitor.visitEnd();
                        return;
                    }
                }
            }
        }

        throw new IllegalStateException("Don't know how to compile annotation value");
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

    public static AnnotationCodegen forClass(final ClassVisitor cv, JetTypeMapper mapper) {
        return new AnnotationCodegen(mapper) {
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return cv.visitAnnotation(descr, visible);
            }
        };
    }

    public static AnnotationCodegen forMethod(final MethodVisitor mv, JetTypeMapper mapper) {
        return new AnnotationCodegen(mapper) {
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return mv.visitAnnotation(descr, visible);
            }
        };
    }

    public static AnnotationCodegen forField(final FieldVisitor fv, JetTypeMapper mapper) {
        return new AnnotationCodegen(mapper) {
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return fv.visitAnnotation(descr, visible);
            }
        };
    }

    public static AnnotationCodegen forParameter(final int parameter, final MethodVisitor mv, JetTypeMapper mapper) {
        return new AnnotationCodegen(mapper) {
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return mv.visitParameterAnnotation(parameter, descr, visible);
            }
        };
    }
}

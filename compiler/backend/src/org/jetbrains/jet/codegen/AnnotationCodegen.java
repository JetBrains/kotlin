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

package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.AnnotationVisitor;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.FieldVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetAnnotationEntry;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetModifierList;
import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.resolve.constants.StringValue;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

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

            genAnnotation(annotationDescriptor);
        }
    }

    public void generateAnnotationDefaultValue(CompileTimeConstant value) {
        AnnotationVisitor visitor = visitAnnotation(null, false);  // Parameters are unimportant
        genCompileTimeValue(null, value, visitor);
        visitor.visitEnd();
    }

    private void genAnnotation(AnnotationDescriptor annotationDescriptor) {
        ClassifierDescriptor classifierDescriptor = annotationDescriptor.getType().getConstructor().getDeclarationDescriptor();
        RetentionPolicy rp = getRetentionPolicy(classifierDescriptor, typeMapper);
        if (rp == RetentionPolicy.SOURCE) {
            return;
        }

        String internalName = typeMapper.mapType(annotationDescriptor.getType()).getDescriptor();
        AnnotationVisitor annotationVisitor = visitAnnotation(internalName, rp == RetentionPolicy.RUNTIME);

        genAnnotationArguments(annotationDescriptor, annotationVisitor);
        annotationVisitor.visitEnd();
    }

    private void genAnnotationArguments(AnnotationDescriptor annotationDescriptor, AnnotationVisitor annotationVisitor) {
        for (Map.Entry<ValueParameterDescriptor, CompileTimeConstant<?>> entry : annotationDescriptor.getAllValueArguments().entrySet()) {
            ValueParameterDescriptor descriptor = entry.getKey();
            String name = descriptor.getName().asString();
            genCompileTimeValue(name, entry.getValue(), annotationVisitor);
        }
    }

    private void genCompileTimeValue(
            @Nullable final String name,
            @NotNull CompileTimeConstant<?> value,
            @NotNull final AnnotationVisitor annotationVisitor
    ) {
        AnnotationArgumentVisitor argumentVisitor = new AnnotationArgumentVisitor<Void, Void>() {
            @Override
            public Void visitLongValue(@NotNull LongValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitIntValue(IntValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitShortValue(ShortValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitByteValue(ByteValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitDoubleValue(DoubleValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitFloatValue(FloatValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitBooleanValue(BooleanValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitCharValue(CharValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitStringValue(StringValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitEnumValue(EnumValue value, Void data) {
                String propertyName = value.getValue().getName().asString();
                annotationVisitor.visitEnum(name, typeMapper.mapType(value.getType(KotlinBuiltIns.getInstance())).getDescriptor(), propertyName);
                return null;
            }

            @Override
            public Void visitArrayValue(ArrayValue value, Void data) {
                AnnotationVisitor visitor = annotationVisitor.visitArray(name);
                for (CompileTimeConstant<?> argument : value.getValue()) {
                    genCompileTimeValue(null, argument, visitor);
                }
                visitor.visitEnd();
                return null;
            }

            @Override
            public Void visitAnnotationValue(AnnotationValue value, Void data) {
                String internalAnnName = typeMapper.mapType(value.getValue().getType()).getDescriptor();
                AnnotationVisitor visitor = annotationVisitor.visitAnnotation(name, internalAnnName);
                genAnnotationArguments(value.getValue(), visitor);
                visitor.visitEnd();
                return null;
            }

            @Override
            public Void visitJavaClassValue(JavaClassValue value, Void data) {
                annotationVisitor.visit(name, typeMapper.mapType(value.getValue()));
                return null;
            }

            private Void visitSimpleValue(CompileTimeConstant value) {
                annotationVisitor.visit(name, value.getValue());
                return null;
            }

            @Override
            public Void visitErrorValue(ErrorValue value, Void data) {
                return visitUnsupportedValue(value);
            }

            @Override
            public Void visitNullValue(NullValue value, Void data) {
                return visitUnsupportedValue(value);
            }

            private Void visitUnsupportedValue(CompileTimeConstant value) {
                throw new IllegalStateException("Don't know how to compile annotation value " + value);
            }
        };

        value.accept(argumentVisitor, null);
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

    public static AnnotationCodegen forAnnotationDefaultValue(final MethodVisitor mv, JetTypeMapper mapper) {
        return new AnnotationCodegen(mapper) {
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return mv.visitAnnotationDefault();
            }
        };
    }
}

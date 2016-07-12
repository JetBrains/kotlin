/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.annotation.WrappedAnnotated;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.*;
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.resolve.AnnotationChecker;
import org.jetbrains.kotlin.resolve.constants.*;
import org.jetbrains.kotlin.resolve.constants.StringValue;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.org.objectweb.asm.*;

import java.lang.annotation.*;
import java.util.*;

public abstract class AnnotationCodegen {

    public static final class JvmFlagAnnotation {
        private final FqName fqName;
        private final int jvmFlag;

        public JvmFlagAnnotation(@NotNull String fqName, int jvmFlag) {
            this.fqName = new FqName(fqName);
            this.jvmFlag = jvmFlag;
        }

        public boolean hasAnnotation(@NotNull Annotated annotated) {
            return Annotations.Companion.findAnyAnnotation(annotated.getAnnotations(), fqName) != null;
        }

        public int getJvmFlag() {
            return jvmFlag;
        }
    }

    public static final List<JvmFlagAnnotation> FIELD_FLAGS = Arrays.asList(
            new JvmFlagAnnotation("kotlin.jvm.Volatile", Opcodes.ACC_VOLATILE),
            new JvmFlagAnnotation("kotlin.jvm.Transient", Opcodes.ACC_TRANSIENT)
    );

    public static final List<JvmFlagAnnotation> METHOD_FLAGS = Arrays.asList(
            new JvmFlagAnnotation("kotlin.jvm.Strictfp", Opcodes.ACC_STRICT),
            new JvmFlagAnnotation("kotlin.jvm.Synchronized", Opcodes.ACC_SYNCHRONIZED)
    );

    private static final AnnotationVisitor NO_ANNOTATION_VISITOR = new AnnotationVisitor(Opcodes.ASM5) {};

    private final KotlinTypeMapper typeMapper;

    private AnnotationCodegen(KotlinTypeMapper mapper) {
        typeMapper = mapper;
    }

    /**
     * @param returnType can be null if not applicable (e.g. {@code annotated} is a class)
     */
    public void genAnnotations(@Nullable Annotated annotated, @Nullable Type returnType) {
        genAnnotations(annotated, returnType, null);
    }

    public void genAnnotations(@Nullable Annotated annotated, @Nullable Type returnType, @Nullable AnnotationUseSiteTarget allowedTarget) {
        if (annotated == null) {
            return;
        }

        Set<String> annotationDescriptorsAlreadyPresent = new HashSet<String>();

        Annotations annotations = annotated.getAnnotations();

        for (AnnotationWithTarget annotationWithTarget : annotations.getAllAnnotations()) {
            AnnotationDescriptor annotation = annotationWithTarget.getAnnotation();
            AnnotationUseSiteTarget annotationTarget = annotationWithTarget.getTarget();

            // Skip targeted annotations by default
            if (allowedTarget == null && annotationTarget != null) continue;

            // Skip if the target is not the same
            if (allowedTarget != null && annotationTarget != null && allowedTarget != annotationTarget) continue;

            Set<KotlinTarget> applicableTargets = AnnotationChecker.applicableTargetSet(annotation);
            if (annotated instanceof AnonymousFunctionDescriptor
                && !applicableTargets.contains(KotlinTarget.FUNCTION)
                && !applicableTargets.contains(KotlinTarget.PROPERTY_GETTER)
                && !applicableTargets.contains(KotlinTarget.PROPERTY_SETTER)) {
                assert (applicableTargets.contains(KotlinTarget.EXPRESSION)) :
                        "Inconsistent target list for lambda annotation: " + applicableTargets + " on " + annotated;
                continue;
            }
            if (annotated instanceof ClassDescriptor
                && !applicableTargets.contains(KotlinTarget.CLASS)
                && !applicableTargets.contains(KotlinTarget.ANNOTATION_CLASS)) {
                ClassDescriptor classDescriptor = (ClassDescriptor) annotated;
                if (classDescriptor.getVisibility() == Visibilities.LOCAL) {
                    assert applicableTargets.contains(KotlinTarget.EXPRESSION) :
                            "Inconsistent target list for object literal annotation: " + applicableTargets + " on " + annotated;
                    continue;
                }
            }

            String descriptor = genAnnotation(annotation);
            if (descriptor != null) {
                annotationDescriptorsAlreadyPresent.add(descriptor);
            }
        }

        generateAdditionalAnnotations(annotated, returnType, annotationDescriptorsAlreadyPresent);
    }

    private void generateAdditionalAnnotations(
            @NotNull Annotated annotated,
            @Nullable Type returnType,
            @NotNull Set<String> annotationDescriptorsAlreadyPresent
    ) {
        Annotated unwrapped = annotated;
        if (annotated instanceof WrappedAnnotated) {
            unwrapped = ((WrappedAnnotated) annotated).getOriginalAnnotated();
        }

        if (unwrapped instanceof CallableDescriptor) {
            CallableDescriptor descriptor = (CallableDescriptor) unwrapped;

            // No need to annotate privates, synthetic accessors and their parameters
            if (isInvisibleFromTheOutside(descriptor)) return;
            if (descriptor instanceof ValueParameterDescriptor && isInvisibleFromTheOutside(descriptor.getContainingDeclaration())) return;

            if (returnType != null && !AsmUtil.isPrimitive(returnType)) {
                generateNullabilityAnnotation(descriptor.getReturnType(), annotationDescriptorsAlreadyPresent);
            }
        }
        if (unwrapped instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) unwrapped;
            if (classDescriptor.getKind() == ClassKind.ANNOTATION_CLASS) {
                generateDocumentedAnnotation(classDescriptor, annotationDescriptorsAlreadyPresent);
                generateRetentionAnnotation(classDescriptor, annotationDescriptorsAlreadyPresent);
                generateTargetAnnotation(classDescriptor, annotationDescriptorsAlreadyPresent);
            }
        }
    }

    private static boolean isInvisibleFromTheOutside(@Nullable DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableMemberDescriptor && KotlinTypeMapper.isAccessor((CallableMemberDescriptor) descriptor)) return false;
        if (descriptor instanceof MemberDescriptor) {
            return AsmUtil.getVisibilityAccessFlag((MemberDescriptor) descriptor) == Opcodes.ACC_PRIVATE;
        }
        return false;
    }

    private void generateNullabilityAnnotation(@Nullable KotlinType type, @NotNull Set<String> annotationDescriptorsAlreadyPresent) {
        if (type == null) return;

        if (isBareTypeParameterWithNullableUpperBound(type)) {
            // This is to account for the case of, say
            //   class Function<R> { fun invoke(): R }
            // it would be a shame to put @Nullable on the return type of the function, and force all callers to check for null,
            // so we put no annotations
            return;
        }

        if (FlexibleTypesKt.isFlexible(type)) {
            // A flexible type whose lower bound in not-null and upper bound is nullable, should not be annotated
            Flexibility flexibility = FlexibleTypesKt.flexibility(type);

            if (!TypeUtils.isNullableType(flexibility.getLowerBound()) && TypeUtils.isNullableType(flexibility.getUpperBound())) {
                AnnotationDescriptor notNull = type.getAnnotations().findAnnotation(JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION);
                if (notNull != null) {
                    generateAnnotationIfNotPresent(annotationDescriptorsAlreadyPresent, NotNull.class);
                }
                return;
            }
        }

        boolean isNullableType = TypeUtils.isNullableType(type);

        Class<?> annotationClass = isNullableType ? Nullable.class : NotNull.class;

        generateAnnotationIfNotPresent(annotationDescriptorsAlreadyPresent, annotationClass);
    }

    private static final Map<KotlinTarget, ElementType> annotationTargetMap =
            new EnumMap<KotlinTarget, ElementType>(KotlinTarget.class);

    static {
        annotationTargetMap.put(KotlinTarget.CLASS, ElementType.TYPE);
        annotationTargetMap.put(KotlinTarget.ANNOTATION_CLASS, ElementType.ANNOTATION_TYPE);
        annotationTargetMap.put(KotlinTarget.CONSTRUCTOR, ElementType.CONSTRUCTOR);
        annotationTargetMap.put(KotlinTarget.LOCAL_VARIABLE, ElementType.LOCAL_VARIABLE);
        annotationTargetMap.put(KotlinTarget.FUNCTION, ElementType.METHOD);
        annotationTargetMap.put(KotlinTarget.PROPERTY_GETTER, ElementType.METHOD);
        annotationTargetMap.put(KotlinTarget.PROPERTY_SETTER, ElementType.METHOD);
        annotationTargetMap.put(KotlinTarget.FIELD, ElementType.FIELD);
        annotationTargetMap.put(KotlinTarget.VALUE_PARAMETER, ElementType.PARAMETER);
    }

    private void generateTargetAnnotation(@NotNull ClassDescriptor classDescriptor, @NotNull Set<String> annotationDescriptorsAlreadyPresent) {
        String descriptor = Type.getType(Target.class).getDescriptor();
        if (!annotationDescriptorsAlreadyPresent.add(descriptor)) return;
        Set<KotlinTarget> targets = AnnotationChecker.Companion.applicableTargetSet(classDescriptor);
        Set<ElementType> javaTargets;
        if (targets == null) {
            javaTargets = getJavaTargetList(classDescriptor);
            if (javaTargets == null) return;
        }
        else {
            javaTargets = EnumSet.noneOf(ElementType.class);
            for (KotlinTarget target : targets) {
                if (annotationTargetMap.get(target) == null) continue;
                javaTargets.add(annotationTargetMap.get(target));
            }
        }
        AnnotationVisitor visitor = visitAnnotation(descriptor, true);
        AnnotationVisitor arrayVisitor = visitor.visitArray("value");
        for (ElementType javaTarget : javaTargets) {
            arrayVisitor.visitEnum(null, Type.getType(ElementType.class).getDescriptor(), javaTarget.name());
        }
        arrayVisitor.visitEnd();
        visitor.visitEnd();
    }

    private void generateRetentionAnnotation(@NotNull ClassDescriptor classDescriptor, @NotNull Set<String> annotationDescriptorsAlreadyPresent) {
        RetentionPolicy policy = getRetentionPolicy(classDescriptor);
        String descriptor = Type.getType(Retention.class).getDescriptor();
        if (!annotationDescriptorsAlreadyPresent.add(descriptor)) return;
        AnnotationVisitor visitor = visitAnnotation(descriptor, true);
        visitor.visitEnum("value", Type.getType(RetentionPolicy.class).getDescriptor(), policy.name());
        visitor.visitEnd();
    }

    private void generateDocumentedAnnotation(@NotNull ClassDescriptor classDescriptor, @NotNull Set<String> annotationDescriptorsAlreadyPresent) {
        boolean documented = DescriptorUtilsKt.isDocumentedAnnotation(classDescriptor);
        if (!documented) return;
        String descriptor = Type.getType(Documented.class).getDescriptor();
        if (!annotationDescriptorsAlreadyPresent.add(descriptor)) return;
        AnnotationVisitor visitor = visitAnnotation(descriptor, true);
        visitor.visitEnd();
    }

    private void generateAnnotationIfNotPresent(Set<String> annotationDescriptorsAlreadyPresent, Class<?> annotationClass) {
        String descriptor = Type.getType(annotationClass).getDescriptor();
        if (!annotationDescriptorsAlreadyPresent.contains(descriptor)) {
            visitAnnotation(descriptor, false).visitEnd();
        }
    }

    private static boolean isBareTypeParameterWithNullableUpperBound(@NotNull KotlinType type) {
        ClassifierDescriptor classifier = type.getConstructor().getDeclarationDescriptor();
        return !type.isMarkedNullable() && classifier instanceof TypeParameterDescriptor && TypeUtils.hasNullableSuperType(type);
    }

    public void generateAnnotationDefaultValue(@NotNull ConstantValue<?> value, @NotNull KotlinType expectedType) {
        AnnotationVisitor visitor = visitAnnotation(null, false);  // Parameters are unimportant
        genCompileTimeValue(null, value, visitor);
        visitor.visitEnd();
    }

    @Nullable
    private String genAnnotation(@NotNull AnnotationDescriptor annotationDescriptor) {
        ClassifierDescriptor classifierDescriptor = annotationDescriptor.getType().getConstructor().getDeclarationDescriptor();
        assert classifierDescriptor != null : "Annotation descriptor has no class: " + annotationDescriptor;
        RetentionPolicy rp = getRetentionPolicy(classifierDescriptor);
        if (rp == RetentionPolicy.SOURCE && typeMapper.getClassBuilderMode() != ClassBuilderMode.LIGHT_CLASSES) {
            return null;
        }

        String descriptor = typeMapper.mapType(annotationDescriptor.getType()).getDescriptor();
        AnnotationVisitor annotationVisitor = visitAnnotation(descriptor, rp == RetentionPolicy.RUNTIME);

        genAnnotationArguments(annotationDescriptor, annotationVisitor);
        annotationVisitor.visitEnd();

        return descriptor;
    }

    private void genAnnotationArguments(AnnotationDescriptor annotationDescriptor, AnnotationVisitor annotationVisitor) {
        for (Map.Entry<ValueParameterDescriptor, ConstantValue<?>> entry : annotationDescriptor.getAllValueArguments().entrySet()) {
            ValueParameterDescriptor descriptor = entry.getKey();
            String name = descriptor.getName().asString();
            genCompileTimeValue(name, entry.getValue(), annotationVisitor);
        }
    }

    private void genCompileTimeValue(
            @Nullable final String name,
            @NotNull ConstantValue<?> value,
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
                annotationVisitor.visitEnum(name, typeMapper.mapType(value.getType()).getDescriptor(), propertyName);
                return null;
            }

            @Override
            public Void visitArrayValue(ArrayValue value, Void data) {
                AnnotationVisitor visitor = annotationVisitor.visitArray(name);
                for (ConstantValue<?> argument : value.getValue()) {
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
            public Void visitKClassValue(KClassValue value, Void data) {
                annotationVisitor.visit(name, typeMapper.mapType(value.getValue()));
                return null;
            }

            private Void visitSimpleValue(ConstantValue<?> value) {
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

            private Void visitUnsupportedValue(ConstantValue<?> value) {
                ClassBuilderMode mode = typeMapper.getClassBuilderMode();
                switch (mode) {
                    case FULL:
                        throw new IllegalStateException("Don't know how to compile annotation value " + value);
                    case LIGHT_CLASSES:
                    case LIGHT_CLASSES_WITH_METADATA:
                        return null;
                    default:
                        throw new IllegalStateException("Unknown builder mode: " + mode);
                }
            }
        };

        value.accept(argumentVisitor, null);
    }

    private static final Map<KotlinRetention, RetentionPolicy> annotationRetentionMap =
            new EnumMap<KotlinRetention, RetentionPolicy>(KotlinRetention.class);

    static {
        annotationRetentionMap.put(KotlinRetention.SOURCE, RetentionPolicy.SOURCE);
        annotationRetentionMap.put(KotlinRetention.BINARY, RetentionPolicy.CLASS);
        annotationRetentionMap.put(KotlinRetention.RUNTIME, RetentionPolicy.RUNTIME);
    }

    @Nullable
    private Set<ElementType> getJavaTargetList(ClassDescriptor descriptor) {
        AnnotationDescriptor targetAnnotation = descriptor.getAnnotations().findAnnotation(new FqName(Target.class.getName()));
        if (targetAnnotation != null) {
            Collection<ConstantValue<?>> valueArguments = targetAnnotation.getAllValueArguments().values();
            if (!valueArguments.isEmpty()) {
                ConstantValue<?> compileTimeConstant = valueArguments.iterator().next();
                if (compileTimeConstant instanceof ArrayValue) {
                    List<? extends ConstantValue<?>> values = ((ArrayValue) compileTimeConstant).getValue();
                    Set<ElementType> result = EnumSet.noneOf(ElementType.class);
                    for (ConstantValue<?> value : values) {
                        if (value instanceof EnumValue) {
                            ClassDescriptor enumEntry = ((EnumValue) value).getValue();
                            KotlinType classObjectType = DescriptorUtilsKt.getClassValueType(enumEntry);
                            if (classObjectType != null) {
                                if ("java/lang/annotation/ElementType".equals(typeMapper.mapType(classObjectType).getInternalName())) {
                                    result.add(ElementType.valueOf(enumEntry.getName().asString()));
                                }
                            }
                        }
                    }
                    return result;
                }
            }
        }
        return null;
    }

    @NotNull
    private RetentionPolicy getRetentionPolicy(@NotNull Annotated descriptor) {
        KotlinRetention retention = DescriptorUtilsKt.getAnnotationRetention(descriptor);
        if (retention != null) {
            return annotationRetentionMap.get(retention);
        }
        AnnotationDescriptor retentionAnnotation = descriptor.getAnnotations().findAnnotation(new FqName(Retention.class.getName()));
        if (retentionAnnotation != null) {
            Collection<ConstantValue<?>> valueArguments = retentionAnnotation.getAllValueArguments().values();
            if (!valueArguments.isEmpty()) {
                ConstantValue<?> compileTimeConstant = valueArguments.iterator().next();
                if (compileTimeConstant instanceof EnumValue) {
                    ClassDescriptor enumEntry = ((EnumValue) compileTimeConstant).getValue();
                    KotlinType classObjectType = DescriptorUtilsKt.getClassValueType(enumEntry);
                    if (classObjectType != null) {
                        if ("java/lang/annotation/RetentionPolicy".equals(typeMapper.mapType(classObjectType).getInternalName())) {
                            return RetentionPolicy.valueOf(enumEntry.getName().asString());
                        }
                    }
                }
            }
        }

        return RetentionPolicy.RUNTIME;
    }

    @NotNull
    abstract AnnotationVisitor visitAnnotation(String descr, boolean visible);

    public static AnnotationCodegen forClass(final ClassVisitor cv, KotlinTypeMapper mapper) {
        return new AnnotationCodegen(mapper) {
            @NotNull
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return safe(cv.visitAnnotation(descr, visible));
            }
        };
    }

    public static AnnotationCodegen forMethod(final MethodVisitor mv, KotlinTypeMapper mapper) {
        return new AnnotationCodegen(mapper) {
            @NotNull
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return safe(mv.visitAnnotation(descr, visible));
            }
        };
    }

    public static AnnotationCodegen forField(final FieldVisitor fv, KotlinTypeMapper mapper) {
        return new AnnotationCodegen(mapper) {
            @NotNull
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return safe(fv.visitAnnotation(descr, visible));
            }
        };
    }

    public static AnnotationCodegen forParameter(final int parameter, final MethodVisitor mv, KotlinTypeMapper mapper) {
        return new AnnotationCodegen(mapper) {
            @NotNull
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return safe(mv.visitParameterAnnotation(parameter, descr, visible));
            }
        };
    }

    public static AnnotationCodegen forAnnotationDefaultValue(final MethodVisitor mv, KotlinTypeMapper mapper) {
        return new AnnotationCodegen(mapper) {
            @NotNull
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return safe(mv.visitAnnotationDefault());
            }
        };
    }

    @NotNull
    private static AnnotationVisitor safe(@Nullable AnnotationVisitor av) {
        return av == null ? NO_ANNOTATION_VISITOR : av;
    }
}

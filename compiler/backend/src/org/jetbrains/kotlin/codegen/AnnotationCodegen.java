/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.config.JvmTarget;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.*;
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.AnnotationChecker;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker;
import org.jetbrains.kotlin.resolve.constants.*;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt;
import org.jetbrains.kotlin.types.FlexibleType;
import org.jetbrains.kotlin.types.FlexibleTypesKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.org.objectweb.asm.*;

import java.lang.annotation.*;
import java.util.*;

import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt.getAnnotationClass;

public abstract class AnnotationCodegen {

    public static final class JvmFlagAnnotation {
        private final FqName fqName;
        private final int jvmFlag;

        public JvmFlagAnnotation(@NotNull String fqName, int jvmFlag) {
            this.fqName = new FqName(fqName);
            this.jvmFlag = jvmFlag;
        }

        public int getJvmFlag(@Nullable Annotated annotated) {
            return annotated != null && annotated.getAnnotations().hasAnnotation(fqName) ? jvmFlag : 0;
        }
    }

    public static final List<JvmFlagAnnotation> FIELD_FLAGS = Arrays.asList(
            new JvmFlagAnnotation(JvmAnnotationUtilKt.VOLATILE_ANNOTATION_FQ_NAME.asString(), Opcodes.ACC_VOLATILE),
            new JvmFlagAnnotation("kotlin.jvm.Transient", Opcodes.ACC_TRANSIENT)
    );

    public static final List<JvmFlagAnnotation> METHOD_FLAGS = Arrays.asList(
            new JvmFlagAnnotation(JvmAnnotationUtilKt.STRICTFP_ANNOTATION_FQ_NAME.asString(), Opcodes.ACC_STRICT),
            new JvmFlagAnnotation(JvmAnnotationUtilKt.SYNCHRONIZED_ANNOTATION_FQ_NAME.asString(), Opcodes.ACC_SYNCHRONIZED)
    );

    private static final AnnotationVisitor NO_ANNOTATION_VISITOR = new AnnotationVisitor(Opcodes.ASM5) {
        @Override
        public AnnotationVisitor visitAnnotation(String name, @NotNull String desc) {
            return safe(super.visitAnnotation(name, desc));
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            return safe(super.visitArray(name));
        }
    };

    private final InnerClassConsumer innerClassConsumer;
    private final KotlinTypeMapper typeMapper;
    private final ModuleDescriptor module;

    private AnnotationCodegen(@NotNull InnerClassConsumer innerClassConsumer, @NotNull GenerationState state) {
        this.innerClassConsumer = innerClassConsumer;
        this.typeMapper = state.getTypeMapper();
        this.module = state.getModule();
    }

    /**
     * @param returnType can be null if not applicable (e.g. {@code annotated} is a class)
     */
    public void genAnnotations(@Nullable Annotated annotated, @Nullable Type returnType) {
        if (annotated == null) return;

        Set<String> annotationDescriptorsAlreadyPresent = new HashSet<>();

        Annotations annotations = annotated.getAnnotations();

        for (AnnotationDescriptor annotation : annotations) {
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
        if (annotated instanceof CallableDescriptor) {
            generateAdditionalCallableAnnotations((CallableDescriptor) annotated, returnType, annotationDescriptorsAlreadyPresent);
        }
        else if (annotated instanceof FieldDescriptor) {
            generateAdditionalCallableAnnotations(
                    ((FieldDescriptor) annotated).getCorrespondingProperty(), returnType, annotationDescriptorsAlreadyPresent
            );
        }
        else if (annotated instanceof ClassDescriptor) {
            generateAdditionalClassAnnotations(annotationDescriptorsAlreadyPresent, (ClassDescriptor) annotated);
        }
    }

    private void generateAdditionalCallableAnnotations(
            @NotNull CallableDescriptor descriptor,
            @Nullable Type returnType,
            @NotNull Set<String> annotationDescriptorsAlreadyPresent
    ) {
        // No need to annotate privates, synthetic accessors and their parameters
        if (isInvisibleFromTheOutside(descriptor)) return;
        if (descriptor instanceof ValueParameterDescriptor && isInvisibleFromTheOutside(descriptor.getContainingDeclaration())) return;

        // No need to annotate annotation methods since they're always non-null
        if (descriptor instanceof PropertyGetterDescriptor &&
            DescriptorUtils.isAnnotationClass(descriptor.getContainingDeclaration())) {
            return;
        }

        if (returnType != null && !AsmUtil.isPrimitive(returnType)) {
            generateNullabilityAnnotation(descriptor.getReturnType(), annotationDescriptorsAlreadyPresent);
        }
    }

    private void generateAdditionalClassAnnotations(
            @NotNull Set<String> annotationDescriptorsAlreadyPresent,
            @NotNull ClassDescriptor descriptor
    ) {
        if (descriptor.getKind() == ClassKind.ANNOTATION_CLASS) {
            generateDocumentedAnnotation(descriptor, annotationDescriptorsAlreadyPresent);
            generateRetentionAnnotation(descriptor, annotationDescriptorsAlreadyPresent);
            generateTargetAnnotation(descriptor, annotationDescriptorsAlreadyPresent);
        }
    }

    private static boolean isInvisibleFromTheOutside(@Nullable DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableMemberDescriptor && KotlinTypeMapper.isAccessor((CallableMemberDescriptor) descriptor)) {
            return true;
        }
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
            FlexibleType flexibleType = FlexibleTypesKt.asFlexibleType(type);

            if (!TypeUtils.isNullableType(flexibleType.getLowerBound()) && TypeUtils.isNullableType(flexibleType.getUpperBound())) {
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

    private static final Map<JvmTarget, Map<KotlinTarget, ElementType>> annotationTargetMaps = new EnumMap<>(JvmTarget.class);

    static {
        Map<KotlinTarget, ElementType> jvm6 = new EnumMap<>(KotlinTarget.class);
        jvm6.put(KotlinTarget.CLASS, ElementType.TYPE);
        jvm6.put(KotlinTarget.ANNOTATION_CLASS, ElementType.ANNOTATION_TYPE);
        jvm6.put(KotlinTarget.CONSTRUCTOR, ElementType.CONSTRUCTOR);
        jvm6.put(KotlinTarget.LOCAL_VARIABLE, ElementType.LOCAL_VARIABLE);
        jvm6.put(KotlinTarget.FUNCTION, ElementType.METHOD);
        jvm6.put(KotlinTarget.PROPERTY_GETTER, ElementType.METHOD);
        jvm6.put(KotlinTarget.PROPERTY_SETTER, ElementType.METHOD);
        jvm6.put(KotlinTarget.FIELD, ElementType.FIELD);
        jvm6.put(KotlinTarget.VALUE_PARAMETER, ElementType.PARAMETER);

        Map<KotlinTarget, ElementType> jvm8 = new EnumMap<>(jvm6);
        jvm8.put(KotlinTarget.TYPE_PARAMETER, ElementType.TYPE_PARAMETER);
        jvm8.put(KotlinTarget.TYPE, ElementType.TYPE_USE);

        annotationTargetMaps.put(JvmTarget.JVM_1_6, jvm6);
        annotationTargetMaps.put(JvmTarget.JVM_1_8, jvm8);
    }

    private void generateTargetAnnotation(
            @NotNull ClassDescriptor classDescriptor, @NotNull Set<String> annotationDescriptorsAlreadyPresent
    ) {
        String descriptor = Type.getType(Target.class).getDescriptor();
        if (!annotationDescriptorsAlreadyPresent.add(descriptor)) return;

        Map<KotlinTarget, ElementType> annotationTargetMap = annotationTargetMaps.get(typeMapper.getJvmTarget());
        if (annotationTargetMap == null) throw new AssertionError("No annotation target map for JVM target " + typeMapper.getJvmTarget());

        Set<KotlinTarget> targets = AnnotationChecker.Companion.applicableTargetSet(classDescriptor);
        Set<ElementType> javaTargets;
        if (targets == null) {
            javaTargets = getJavaTargetList(classDescriptor);
            if (javaTargets == null) return;
        }
        else {
            javaTargets = EnumSet.noneOf(ElementType.class);
            for (KotlinTarget target : targets) {
                ElementType elementType = annotationTargetMap.get(target);
                if (elementType != null) {
                    javaTargets.add(elementType);
                }
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
        ClassDescriptor classDescriptor = getAnnotationClass(annotationDescriptor);
        assert classDescriptor != null : "Annotation descriptor has no class: " + annotationDescriptor;
        RetentionPolicy rp = getRetentionPolicy(classDescriptor);
        if (rp == RetentionPolicy.SOURCE && !typeMapper.getClassBuilderMode().generateSourceRetentionAnnotations) {
            return null;
        }

        // We do not generate annotations whose classes are optional (annotated with `@OptionalExpectation`) because if an annotation entry
        // is resolved to the expected declaration, this means that annotation has no actual class, and thus should not be generated.
        // (Otherwise we would've resolved the entry to the actual annotation class.)
        if (ExpectedActualDeclarationChecker.isOptionalAnnotationClass(classDescriptor)) {
            return null;
        }

        innerClassConsumer.addInnerClassInfoFromAnnotation(classDescriptor);

        String asmTypeDescriptor = typeMapper.mapType(annotationDescriptor.getType()).getDescriptor();
        AnnotationVisitor annotationVisitor = visitAnnotation(asmTypeDescriptor, rp == RetentionPolicy.RUNTIME);

        genAnnotationArguments(annotationDescriptor, annotationVisitor);
        annotationVisitor.visitEnd();

        return asmTypeDescriptor;
    }

    private void genAnnotationArguments(AnnotationDescriptor annotationDescriptor, AnnotationVisitor annotationVisitor) {
        ClassDescriptor annotationClass = DescriptorUtilsKt.getAnnotationClass(annotationDescriptor);
        for (Map.Entry<Name, ConstantValue<?>> entry : annotationDescriptor.getAllValueArguments().entrySet()) {
            genCompileTimeValue(getAnnotationArgumentJvmName(annotationClass, entry.getKey()), entry.getValue(), annotationVisitor);
        }
    }

    private String getAnnotationArgumentJvmName(@Nullable ClassDescriptor annotationClass, @NotNull Name parameterName) {
        if (annotationClass == null) return parameterName.asString();

        Collection<? extends PropertyDescriptor> variables =
                annotationClass.getUnsubstitutedMemberScope().getContributedVariables(parameterName, NoLookupLocation.FROM_BACKEND);
        if (variables.size() != 1) return parameterName.asString();

        return typeMapper.mapAnnotationParameterName(variables.iterator().next());
    }

    private void genCompileTimeValue(
            @Nullable String name,
            @NotNull ConstantValue<?> value,
            @NotNull AnnotationVisitor annotationVisitor
    ) {
        AnnotationArgumentVisitor<Void, Void> argumentVisitor = new AnnotationArgumentVisitor<Void, Void>() {
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
                String enumClassInternalName = AsmUtil.asmTypeByClassId(value.getEnumClassId()).getDescriptor();
                String enumEntryName = value.getEnumEntryName().asString();
                annotationVisitor.visitEnum(name, enumClassInternalName, enumEntryName);
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
                annotationVisitor.visit(name, typeMapper.mapType(value.getArgumentType(module)));
                return null;
            }

            @Override
            public Void visitUByteValue(UByteValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitUShortValue(UShortValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitUIntValue(UIntValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitULongValue(ULongValue value, Void data) {
                return visitSimpleValue(value);
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
                if (mode.generateBodies) {
                    throw new IllegalStateException("Don't know how to compile annotation value " + value);
                } else {
                    return null;
                }
            }
        };

        value.accept(argumentVisitor, null);
    }

    private static final Map<KotlinRetention, RetentionPolicy> annotationRetentionMap = new EnumMap<>(KotlinRetention.class);

    static {
        annotationRetentionMap.put(KotlinRetention.SOURCE, RetentionPolicy.SOURCE);
        annotationRetentionMap.put(KotlinRetention.BINARY, RetentionPolicy.CLASS);
        annotationRetentionMap.put(KotlinRetention.RUNTIME, RetentionPolicy.RUNTIME);
    }

    @Nullable
    private static Set<ElementType> getJavaTargetList(ClassDescriptor descriptor) {
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
                            FqName enumClassFqName = ((EnumValue) value).getEnumClassId().asSingleFqName();
                            if (ElementType.class.getName().equals(enumClassFqName.asString())) {
                                result.add(ElementType.valueOf(((EnumValue) value).getEnumEntryName().asString()));
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
    private static RetentionPolicy getRetentionPolicy(@NotNull Annotated descriptor) {
        KotlinRetention retention = DescriptorUtilsKt.getAnnotationRetention(descriptor);
        if (retention != null) {
            return annotationRetentionMap.get(retention);
        }
        AnnotationDescriptor retentionAnnotation = descriptor.getAnnotations().findAnnotation(new FqName(Retention.class.getName()));
        if (retentionAnnotation != null) {
            ConstantValue<?> value = CollectionsKt.firstOrNull(retentionAnnotation.getAllValueArguments().values());
            if (value instanceof EnumValue) {
                FqName enumClassFqName = ((EnumValue) value).getEnumClassId().asSingleFqName();
                if (RetentionPolicy.class.getName().equals(enumClassFqName.asString())) {
                    return RetentionPolicy.valueOf(((EnumValue) value).getEnumEntryName().asString());
                }
            }
        }

        return RetentionPolicy.RUNTIME;
    }

    @NotNull
    abstract AnnotationVisitor visitAnnotation(String descr, boolean visible);

    public static AnnotationCodegen forClass(
            @NotNull ClassVisitor cv,
            @NotNull InnerClassConsumer innerClassConsumer,
            @NotNull GenerationState state
    ) {
        return new AnnotationCodegen(innerClassConsumer, state) {
            @NotNull
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return safe(cv.visitAnnotation(descr, visible));
            }
        };
    }

    public static AnnotationCodegen forMethod(
            @NotNull MethodVisitor mv,
            @NotNull InnerClassConsumer innerClassConsumer,
            @NotNull GenerationState state
    ) {
        return new AnnotationCodegen(innerClassConsumer, state) {
            @NotNull
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return safe(mv.visitAnnotation(descr, visible));
            }
        };
    }

    public static AnnotationCodegen forField(
            @NotNull FieldVisitor fv,
            @NotNull InnerClassConsumer innerClassConsumer,
            @NotNull GenerationState state
    ) {
        return new AnnotationCodegen(innerClassConsumer, state) {
            @NotNull
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return safe(fv.visitAnnotation(descr, visible));
            }
        };
    }

    public static AnnotationCodegen forParameter(
            int parameter,
            @NotNull MethodVisitor mv,
            @NotNull InnerClassConsumer innerClassConsumer,
            @NotNull GenerationState state
    ) {
        return new AnnotationCodegen(innerClassConsumer, state) {
            @NotNull
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return safe(mv.visitParameterAnnotation(parameter, descr, visible));
            }
        };
    }

    public static AnnotationCodegen forAnnotationDefaultValue(
            @NotNull MethodVisitor mv,
            @NotNull InnerClassConsumer innerClassConsumer,
            @NotNull GenerationState state
    ) {
        return new AnnotationCodegen(innerClassConsumer, state) {
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

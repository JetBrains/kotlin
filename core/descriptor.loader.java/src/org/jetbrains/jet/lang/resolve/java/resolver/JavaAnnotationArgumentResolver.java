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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.resolve.constants.StringValue;
import org.jetbrains.jet.lang.resolve.java.structure.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeImpl;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getEnumEntriesScope;
import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;
import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.INCLUDE_KOTLIN_SOURCES;

public final class JavaAnnotationArgumentResolver {
    public static final FqName JL_CLASS_FQ_NAME = new FqName("java.lang.Class");

    private JavaAnnotationResolver annotationResolver;
    private JavaClassResolver classResolver;
    private JavaTypeTransformer typeTransformer;

    @Inject
    public void setAnnotationResolver(JavaAnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @Inject
    public void setTypeTransformer(JavaTypeTransformer typeTransformer) {
        this.typeTransformer = typeTransformer;
    }

    @Nullable
    public CompileTimeConstant<?> resolveAnnotationArgument(
            @NotNull FqName annotationFqName,
            @NotNull JavaAnnotationArgument argument,
            @NotNull PostponedTasks postponedTasks
    ) {
        if (argument instanceof JavaLiteralAnnotationArgument) {
            return resolveCompileTimeConstantValue(((JavaLiteralAnnotationArgument) argument).getValue(), null);
        }
        // Enum
        else if (argument instanceof JavaReferenceAnnotationArgument) {
            return resolveFromReference(((JavaReferenceAnnotationArgument) argument).resolve(), postponedTasks);
        }
        // Array
        else if (argument instanceof JavaArrayAnnotationArgument) {
            Name argumentName = argument.getName();
            return resolveFromArray(
                    annotationFqName,
                    argumentName == null ? JavaAnnotationResolver.DEFAULT_ANNOTATION_MEMBER_NAME : argumentName,
                    ((JavaArrayAnnotationArgument) argument).getElements(),
                    postponedTasks
            );
        }
        // Annotation
        else if (argument instanceof JavaAnnotationAsAnnotationArgument) {
            return resolveFromAnnotation(((JavaAnnotationAsAnnotationArgument) argument).getAnnotation(), postponedTasks);
        }
        // Class<?>
        else if (argument instanceof JavaClassObjectAnnotationArgument) {
            return resolveFromJavaClassObjectType(((JavaClassObjectAnnotationArgument) argument).getReferencedType());
        }

        return null;
    }

    @Nullable
    private CompileTimeConstant<?> resolveFromAnnotation(@NotNull JavaAnnotation value, @NotNull PostponedTasks taskList) {
        AnnotationDescriptor descriptor = annotationResolver.resolveAnnotation(value, taskList);
        return descriptor == null ? null : new AnnotationValue(descriptor);
    }

    @Nullable
    private CompileTimeConstant<?> resolveFromArray(
            @NotNull FqName annotationFqName,
            @NotNull Name argumentName,
            @NotNull List<JavaAnnotationArgument> elements,
            @NotNull PostponedTasks taskList
    ) {
        ClassDescriptor annotationClass = classResolver.resolveClass(annotationFqName, INCLUDE_KOTLIN_SOURCES, taskList);
        if (annotationClass == null) return null;

        //TODO: nullability issues
        ValueParameterDescriptor valueParameter = DescriptorResolverUtils.getAnnotationParameterByName(argumentName, annotationClass);
        if (valueParameter == null) return null;

        List<CompileTimeConstant<?>> values = new ArrayList<CompileTimeConstant<?>>(elements.size());
        for (JavaAnnotationArgument argument : elements) {
            CompileTimeConstant<?> value = resolveAnnotationArgument(annotationFqName, argument, taskList);
            values.add(value == null ? NullValue.NULL : value);
        }

        return new ArrayValue(values, valueParameter.getType());
    }

    @Nullable
    private CompileTimeConstant<?> resolveFromReference(@Nullable JavaElement element, @NotNull PostponedTasks taskList) {
        if (!(element instanceof JavaField)) return null;

        JavaField field = (JavaField) element;
        if (!field.isEnumEntry()) return null;

        FqName fqName = field.getContainingClass().getFqName();
        if (fqName == null) return null;

        ClassDescriptor enumClass = classResolver.resolveClass(fqName, INCLUDE_KOTLIN_SOURCES, taskList);
        if (enumClass == null) return null;

        for (VariableDescriptor variableDescriptor : getEnumEntriesScope(enumClass).getProperties(field.getName())) {
            if (variableDescriptor.getReceiverParameter() == null) {
                return new EnumValue((PropertyDescriptor) variableDescriptor);
            }
        }

        return null;
    }

    @Nullable
    private CompileTimeConstant<?> resolveFromJavaClassObjectType(@NotNull JavaType javaType) {
        JetType type = typeTransformer.transformToType(javaType, TypeVariableResolver.EMPTY);

        ClassDescriptor jlClass = classResolver.resolveClass(JL_CLASS_FQ_NAME, IGNORE_KOTLIN_SOURCES);
        if (jlClass == null) return null;

        List<TypeProjection> arguments = Collections.singletonList(new TypeProjection(type));
        JetTypeImpl javaClassType = new JetTypeImpl(
                jlClass.getAnnotations(),
                jlClass.getTypeConstructor(),
                false,
                arguments,
                jlClass.getMemberScope(arguments)
        );

        return new JavaClassValue(javaClassType);
    }

    @Nullable
    public static CompileTimeConstant<?> resolveCompileTimeConstantValue(@Nullable Object value, @Nullable JetType expectedType) {
        if (value instanceof String) {
            return new StringValue((String) value);
        }
        else if (value instanceof Byte) {
            return new ByteValue((Byte) value);
        }
        else if (value instanceof Short) {
            return new ShortValue((Short) value);
        }
        else if (value instanceof Character) {
            return new CharValue((Character) value);
        }
        else if (value instanceof Integer) {
            KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
            Integer integer = (Integer) value;
            if (builtIns.getShortType().equals(expectedType)) {
                return new ShortValue(integer.shortValue());
            }
            else if (builtIns.getByteType().equals(expectedType)) {
                return new ByteValue(integer.byteValue());
            }
            else if (builtIns.getCharType().equals(expectedType)) {
                return new CharValue((char) integer.intValue());
            }
            return new IntValue(integer);
        }
        else if (value instanceof Long) {
            return new LongValue((Long) value);
        }
        else if (value instanceof Float) {
            return new FloatValue((Float) value);
        }
        else if (value instanceof Double) {
            return new DoubleValue((Double) value);
        }
        else if (value instanceof Boolean) {
            return BooleanValue.valueOf((Boolean) value);
        }
        else if (value == null) {
            return NullValue.NULL;
        }
        return null;
    }
}

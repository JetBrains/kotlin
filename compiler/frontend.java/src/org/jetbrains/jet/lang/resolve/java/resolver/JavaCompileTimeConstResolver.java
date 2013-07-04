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

import com.google.common.collect.Lists;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.resolve.constants.StringValue;
import org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class JavaCompileTimeConstResolver {
    private JavaAnnotationResolver annotationResolver;
    private JavaClassResolver classResolver;

    public JavaCompileTimeConstResolver() {
    }

    @Inject
    public void setAnnotationResolver(JavaAnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @Nullable
    public CompileTimeConstant<?> getCompileTimeConstFromExpression(
            FqName annotationFqName, Name parameterName,
            PsiAnnotationMemberValue value, PostponedTasks postponedTasks
    ) {
        if (value instanceof PsiLiteralExpression) {
            return resolveCompileTimeConstantValue(((PsiLiteralExpression) value).getValue(), null);
        }
        // Enum
        else if (value instanceof PsiReferenceExpression) {
            return getCompileTimeConstFromReferenceExpression((PsiReferenceExpression) value, postponedTasks);
        }
        // Array
        else if (value instanceof PsiArrayInitializerMemberValue) {
            return getCompileTimeConstFromArrayExpression(annotationFqName, parameterName, (PsiArrayInitializerMemberValue) value,
                                                          postponedTasks);
        }
        // Annotation
        else if (value instanceof PsiAnnotation) {
            return getCompileTimeConstFromAnnotation((PsiAnnotation) value, postponedTasks);
        }
        return null;
    }

    @Nullable
    private CompileTimeConstant<?> getCompileTimeConstFromAnnotation(PsiAnnotation value, PostponedTasks taskList) {
        AnnotationDescriptor annotationDescriptor = annotationResolver.resolveAnnotation(value, taskList);
        if (annotationDescriptor != null) {
            return new AnnotationValue(annotationDescriptor);
        }
        return null;
    }

    @Nullable
    private CompileTimeConstant<?> getCompileTimeConstFromArrayExpression(
            FqName annotationFqName,
            Name valueName, PsiArrayInitializerMemberValue value,
            PostponedTasks taskList
    ) {
        PsiAnnotationMemberValue[] initializers = value.getInitializers();
        List<CompileTimeConstant<?>> values = getCompileTimeConstantForArrayValues(annotationFqName, valueName, taskList, initializers);

        ClassDescriptor classDescriptor =
                classResolver.resolveClass(annotationFqName, DescriptorSearchRule.INCLUDE_KOTLIN, taskList);

        //TODO: nullability issues
        ValueParameterDescriptor valueParameterDescriptor =
                DescriptorResolverUtils.getValueParameterDescriptorForAnnotationParameter(valueName, classDescriptor);
        if (valueParameterDescriptor == null) {
            return null;
        }
        JetType expectedArrayType = valueParameterDescriptor.getType();
        return new ArrayValue(values, expectedArrayType);
    }

    private List<CompileTimeConstant<?>> getCompileTimeConstantForArrayValues(
            FqName annotationQualifiedName,
            Name valueName,
            PostponedTasks taskList,
            PsiAnnotationMemberValue[] initializers
    ) {
        List<CompileTimeConstant<?>> values = new ArrayList<CompileTimeConstant<?>>();
        for (PsiAnnotationMemberValue initializer : initializers) {
            CompileTimeConstant<?> compileTimeConstant =
                    getCompileTimeConstFromExpression(annotationQualifiedName, valueName, initializer, taskList);
            if (compileTimeConstant == null) {
                compileTimeConstant = NullValue.NULL;
            }
            values.add(compileTimeConstant);
        }
        return values;
    }

    @Nullable
    private CompileTimeConstant<?> getCompileTimeConstFromReferenceExpression(PsiReferenceExpression value, PostponedTasks taskList) {
        PsiElement resolveElement = value.resolve();
        if (resolveElement instanceof PsiEnumConstant) {
            PsiElement psiElement = resolveElement.getParent();
            if (psiElement instanceof PsiClass) {
                PsiClass psiClass = (PsiClass) psiElement;
                String fqName = psiClass.getQualifiedName();
                if (fqName == null) {
                    return null;
                }

                JetScope scope;
                ClassDescriptor classDescriptor = classResolver.resolveClass(new FqName(fqName), DescriptorSearchRule.INCLUDE_KOTLIN, taskList);
                if (classDescriptor == null) {
                    return null;
                }
                ClassDescriptor classObjectDescriptor = classDescriptor.getClassObjectDescriptor();
                if (classObjectDescriptor == null) {
                    return null;
                }
                scope = classObjectDescriptor.getMemberScope(Lists.<TypeProjection>newArrayList());

                Name identifier = Name.identifier(((PsiEnumConstant) resolveElement).getName());
                Collection<VariableDescriptor> properties = scope.getProperties(identifier);
                for (VariableDescriptor variableDescriptor : properties) {
                    if (variableDescriptor.getReceiverParameter() == null) {
                        return new EnumValue((PropertyDescriptor) variableDescriptor);
                    }
                }
                return null;
            }
        }
        return null;
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
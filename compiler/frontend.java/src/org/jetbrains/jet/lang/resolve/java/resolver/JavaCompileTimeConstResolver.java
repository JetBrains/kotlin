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
import org.jetbrains.jet.lang.descriptors.*;
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
            PsiAnnotationMemberValue value
    ) {
        if (value instanceof PsiLiteralExpression) {
            return getCompileTimeConstFromLiteralExpression((PsiLiteralExpression) value);
        }
        // Enum
        else if (value instanceof PsiReferenceExpression) {
            return getCompileTimeConstFromReferenceExpression((PsiReferenceExpression) value);
        }
        // Array
        else if (value instanceof PsiArrayInitializerMemberValue) {
            return getCompileTimeConstFromArrayExpression(annotationFqName, parameterName, (PsiArrayInitializerMemberValue) value
            );
        }
        // Annotation
        else if (value instanceof PsiAnnotation) {
            return getCompileTimeConstFromAnnotation((PsiAnnotation) value);
        }
        return null;
    }

    @Nullable
    private CompileTimeConstant<?> getCompileTimeConstFromAnnotation(PsiAnnotation value) {
        AnnotationDescriptor annotationDescriptor = annotationResolver.resolveAnnotation(value);
        if (annotationDescriptor != null) {
            return new AnnotationValue(annotationDescriptor);
        }
        return null;
    }

    @Nullable
    private CompileTimeConstant<?> getCompileTimeConstFromArrayExpression(
            FqName annotationFqName,
            Name valueName, PsiArrayInitializerMemberValue value
    ) {
        PsiAnnotationMemberValue[] initializers = value.getInitializers();
        List<CompileTimeConstant<?>> values = getCompileTimeConstantForArrayValues(annotationFqName, valueName, initializers);

        ClassDescriptor classDescriptor =
                classResolver.resolveClass(annotationFqName, DescriptorSearchRule.INCLUDE_KOTLIN);

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
            PsiAnnotationMemberValue[] initializers
    ) {
        List<CompileTimeConstant<?>> values = new ArrayList<CompileTimeConstant<?>>();
        for (PsiAnnotationMemberValue initializer : initializers) {
            CompileTimeConstant<?> compileTimeConstant =
                    getCompileTimeConstFromExpression(annotationQualifiedName, valueName, initializer);
            if (compileTimeConstant == null) {
                compileTimeConstant = NullValue.NULL;
            }
            values.add(compileTimeConstant);
        }
        return values;
    }

    @Nullable
    private CompileTimeConstant<?> getCompileTimeConstFromReferenceExpression(PsiReferenceExpression value) {
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
                ClassDescriptor classDescriptor = classResolver.resolveClass(new FqName(fqName), DescriptorSearchRule.INCLUDE_KOTLIN);
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
    private static CompileTimeConstant<?> getCompileTimeConstFromLiteralExpression(PsiLiteralExpression value) {
        Object literalValue = value.getValue();
        if (literalValue instanceof String) {
            return new StringValue((String) literalValue);
        }
        else if (literalValue instanceof Byte) {
            return new ByteValue((Byte) literalValue);
        }
        else if (literalValue instanceof Short) {
            return new ShortValue((Short) literalValue);
        }
        else if (literalValue instanceof Character) {
            return new CharValue((Character) literalValue);
        }
        else if (literalValue instanceof Integer) {
            return new IntValue((Integer) literalValue);
        }
        else if (literalValue instanceof Long) {
            return new LongValue((Long) literalValue);
        }
        else if (literalValue instanceof Float) {
            return new FloatValue((Float) literalValue);
        }
        else if (literalValue instanceof Double) {
            return new DoubleValue((Double) literalValue);
        }
        else if (literalValue == null) {
            return NullValue.NULL;
        }
        return null;
    }
}
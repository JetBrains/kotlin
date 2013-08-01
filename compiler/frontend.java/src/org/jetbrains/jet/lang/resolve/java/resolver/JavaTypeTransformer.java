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
import com.google.common.collect.Sets;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.java.TypeUsage;
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.java.structure.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.INCLUDE_KOTLIN_SOURCES;
import static org.jetbrains.jet.lang.resolve.java.TypeUsage.*;
import static org.jetbrains.jet.lang.types.Variance.*;

public class JavaTypeTransformer {

    private static final Logger LOG = Logger.getInstance(JavaTypeTransformer.class);

    private JavaClassResolver classResolver;

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @NotNull
    private TypeProjection transformToTypeProjection(
            @NotNull JavaType type,
            @NotNull TypeParameterDescriptor typeParameterDescriptor,
            @NotNull TypeVariableResolver typeVariableResolver,
            @NotNull TypeUsage howThisTypeIsUsed
    ) {
        if (!(type instanceof JavaWildcardType)) {
            return new TypeProjection(transformToType(type, howThisTypeIsUsed, typeVariableResolver));
        }

        JavaWildcardType wildcardType = (JavaWildcardType) type;
        JavaType bound = wildcardType.getBound();
        if (bound == null) {
            return SubstitutionUtils.makeStarProjection(typeParameterDescriptor);
        }

        Variance variance = wildcardType.isExtends() ? OUT_VARIANCE : IN_VARIANCE;

        return new TypeProjection(variance, transformToType(bound, UPPER_BOUND, typeVariableResolver));
    }

    @NotNull
    public JetType transformToType(@NotNull JavaType type, @NotNull TypeVariableResolver typeVariableResolver) {
        return transformToType(type, TypeUsage.MEMBER_SIGNATURE_INVARIANT, typeVariableResolver);
    }

    @NotNull
    public JetType transformToType(
            @NotNull JavaType type,
            @NotNull TypeUsage howThisTypeIsUsed,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        if (type instanceof JavaClassifierType) {
            JavaClassifierType classifierType = (JavaClassifierType) type;
            JetType jetType = transformClassifierType(classifierType, howThisTypeIsUsed, typeVariableResolver);
            if (jetType == null) {
                return ErrorUtils.createErrorType("Unresolved java class: " + classifierType.getPresentableText());
            }
            return jetType;
        }
        else if (type instanceof JavaPrimitiveType) {
            String canonicalText = ((JavaPrimitiveType) type).getCanonicalText();
            JetType jetType = JavaToKotlinClassMap.getInstance().mapPrimitiveKotlinClass(canonicalText);
            assert jetType != null : "Primitive type is not found: " + canonicalText;
            return jetType;
        }
        else if (type instanceof JavaArrayType) {
            return transformArrayType((JavaArrayType) type, howThisTypeIsUsed, typeVariableResolver, false);
        }
        else {
            throw new UnsupportedOperationException("Unsupported type: " + type); // TODO
        }
    }

    @Nullable
    private JetType transformClassifierType(
            @NotNull JavaClassifierType classifierType,
            @NotNull TypeUsage howThisTypeIsUsed,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        JavaClassifier javaClassifier = classifierType.resolve();
        if (javaClassifier == null) {
            return null;
        }
        if (javaClassifier instanceof JavaTypeParameter) {
            return transformTypeParameter((JavaTypeParameter) javaClassifier, howThisTypeIsUsed, typeVariableResolver);
        }
        else if (javaClassifier instanceof JavaClass) {
            FqName fqName = ((JavaClass) javaClassifier).getFqName();
            assert fqName != null : "Class type should have a FQ name: " + javaClassifier;
            return transformClassType(fqName, classifierType, howThisTypeIsUsed, typeVariableResolver);
        }
        else {
            throw new UnsupportedOperationException("Unsupported classifier: " + javaClassifier);
        }
    }

    @Nullable
    private JetType transformTypeParameter(
            @NotNull JavaTypeParameter typeParameter,
            @NotNull TypeUsage howThisTypeIsUsed,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        JavaTypeParameterListOwner owner = typeParameter.getOwner();
        if (owner instanceof JavaMethod && ((JavaMethod) owner).isConstructor()) {
            Set<JetType> supertypesJet = Sets.newHashSet();
            for (JavaClassifierType supertype : typeParameter.getUpperBounds()) {
                supertypesJet.add(transformToType(supertype, UPPER_BOUND, typeVariableResolver));
            }
            return TypeUtils.intersect(JetTypeChecker.INSTANCE, supertypesJet);
        }

        TypeParameterDescriptor typeParameterDescriptor = typeVariableResolver.getTypeVariable(typeParameter.getName().asString());

        // In Java: ArrayList<T>
        // In Kotlin: ArrayList<T>, not ArrayList<T?>
        // nullability will be taken care of in individual member signatures
        boolean nullable = !EnumSet.of(TYPE_ARGUMENT, UPPER_BOUND, SUPERTYPE_ARGUMENT).contains(howThisTypeIsUsed);

        return TypeUtils.makeNullableIfNeeded(typeParameterDescriptor.getDefaultType(), nullable);
    }

    @Nullable
    private JetType transformClassType(
            @NotNull FqName fqName,
            @NotNull JavaClassifierType classifierType,
            @NotNull TypeUsage howThisTypeIsUsed,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        // 'L extends List<T>' in Java is a List<T> in Kotlin, not a List<T?>
        boolean nullable = !EnumSet.of(TYPE_ARGUMENT, SUPERTYPE_ARGUMENT, SUPERTYPE).contains(howThisTypeIsUsed);

        ClassDescriptor classData = JavaToKotlinClassMap.getInstance().mapKotlinClass(fqName, howThisTypeIsUsed);

        if (classData == null) {
            classData = classResolver.resolveClass(fqName, INCLUDE_KOTLIN_SOURCES);
        }
        if (classData == null) {
            return null;
        }

        List<TypeProjection> arguments = Lists.newArrayList();
        List<TypeParameterDescriptor> parameters = classData.getTypeConstructor().getParameters();
        if (isRaw(classifierType, !parameters.isEmpty())) {
            for (TypeParameterDescriptor parameter : parameters) {
                // not making a star projection because of this case:
                // Java:
                // class C<T extends C> {}
                // The upper bound is raw here, and we can't compute the projection: it would be infinite:
                // C<*> = C<out C<out C<...>>>
                // this way we loose some type information, even when the case is not so bad, but it doesn't seem to matter

                // projections are not allowed in immediate arguments of supertypes
                Variance projectionKind = parameter.getVariance() == OUT_VARIANCE || howThisTypeIsUsed == SUPERTYPE
                                          ? INVARIANT
                                          : OUT_VARIANCE;
                arguments.add(new TypeProjection(projectionKind, KotlinBuiltIns.getInstance().getNullableAnyType()));
            }
        }
        else {
            Collection<JavaType> javaTypeArguments = classifierType.getTypeArguments();

            if (parameters.size() != javaTypeArguments.size()) {
                // Most of the time this means there is an error in the Java code
                LOG.warn("parameters = " + parameters.size() + ", actual arguments = " + javaTypeArguments.size() +
                         " in " + classifierType.getPresentableText() + "\n fqName: \n" + fqName);

                for (TypeParameterDescriptor parameter : parameters) {
                    arguments.add(new TypeProjection(ErrorUtils.createErrorType(parameter.getName().asString())));
                }
            }
            else {
                int index = 0;
                for (JavaType typeArgument : javaTypeArguments) {
                    TypeParameterDescriptor typeParameterDescriptor = parameters.get(index);
                    index++;

                    TypeUsage howTheProjectionIsUsed = howThisTypeIsUsed == SUPERTYPE ? SUPERTYPE_ARGUMENT : TYPE_ARGUMENT;
                    TypeProjection typeProjection = transformToTypeProjection(typeArgument, typeParameterDescriptor, typeVariableResolver,
                            howTheProjectionIsUsed);

                    if (typeProjection.getProjectionKind() == typeParameterDescriptor.getVariance()) {
                        // remove redundant 'out' and 'in'
                        arguments.add(new TypeProjection(INVARIANT, typeProjection.getType()));
                    }
                    else {
                        arguments.add(typeProjection);
                    }
                }
            }
        }

        return new JetTypeImpl(
                Collections.<AnnotationDescriptor>emptyList(),
                classData.getTypeConstructor(),
                nullable,
                arguments,
                classData.getMemberScope(arguments));
    }

    @NotNull
    private JetType transformArrayType(
            @NotNull JavaArrayType arrayType,
            @NotNull TypeUsage howThisTypeIsUsed,
            @NotNull TypeVariableResolver typeVariableResolver,
            boolean vararg
    ) {
        JavaType componentType = arrayType.getComponentType();
        if (componentType instanceof JavaPrimitiveType) {
            JetType jetType = JavaToKotlinClassMap.getInstance().mapPrimitiveKotlinClass(
                    "[" + ((JavaPrimitiveType) componentType).getCanonicalText());
            if (jetType != null) {
                return TypeUtils.makeNullable(jetType);
            }
        }

        Variance projectionKind = arrayElementTypeProjectionKind(howThisTypeIsUsed, vararg);
        TypeUsage howArgumentTypeIsUsed = vararg ? MEMBER_SIGNATURE_CONTRAVARIANT : TYPE_ARGUMENT;

        JetType type = transformToType(componentType, howArgumentTypeIsUsed, typeVariableResolver);
        return TypeUtils.makeNullable(KotlinBuiltIns.getInstance().getArrayType(projectionKind, type));
    }

    @NotNull
    private static Variance arrayElementTypeProjectionKind(@NotNull TypeUsage howThisTypeIsUsed, boolean vararg) {
        if (howThisTypeIsUsed == MEMBER_SIGNATURE_CONTRAVARIANT && !vararg) {
            return OUT_VARIANCE;
        }
        else {
            return INVARIANT;
        }
    }

    @NotNull
    public JetType transformVarargType(
            @NotNull JavaArrayType type,
            @NotNull TypeUsage howThisTypeIsUsed,
            @NotNull TypeVariableResolver typeVariableResolver
    ) {
        return transformArrayType(type, howThisTypeIsUsed, typeVariableResolver, true);
    }

    private static boolean isRaw(@NotNull JavaClassifierType classifierType, boolean argumentsExpected) {
        // The second option is needed because sometimes we get weird versions of JDK classes in the class path,
        // such as collections with no generics, so the Java types are not raw, formally, but they don't match with
        // their Kotlin analogs, so we treat them as raw to avoid exceptions
        return classifierType.isRaw() || argumentsExpected && classifierType.getTypeArguments().isEmpty();
    }
}

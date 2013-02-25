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

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.rt.signature.JetSignatureReader;

import javax.inject.Inject;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.java.TypeUsage.*;

public class JavaTypeTransformer {

    private static final Logger LOG = Logger.getInstance(JavaTypeTransformer.class);

    private JavaSemanticServices javaSemanticServices;
    private JavaDescriptorResolver resolver;
    @Inject
    public void setJavaSemanticServices(JavaSemanticServices javaSemanticServices) {
        this.javaSemanticServices = javaSemanticServices;
    }

    @Inject
    public void setResolver(JavaDescriptorResolver resolver) {
        this.resolver = resolver;
    }

    @NotNull
    private TypeProjection transformToTypeProjection(@NotNull final PsiType javaType,
            @NotNull final TypeParameterDescriptor typeParameterDescriptor,
            @NotNull final TypeVariableResolver typeVariableByPsiResolver,
            @NotNull final TypeUsage howThisTypeIsUsed
    ) {
        TypeProjection result = javaType.accept(new PsiTypeVisitor<TypeProjection>() {

            @Override
            public TypeProjection visitCapturedWildcardType(PsiCapturedWildcardType capturedWildcardType) {
                throw new UnsupportedOperationException(); // TODO
            }

            @Override
            public TypeProjection visitWildcardType(PsiWildcardType wildcardType) {
                if (!wildcardType.isBounded()) {
                    return SubstitutionUtils.makeStarProjection(typeParameterDescriptor);
                }
                Variance variance = wildcardType.isExtends() ? Variance.OUT_VARIANCE : Variance.IN_VARIANCE;

                PsiType bound = wildcardType.getBound();
                assert bound != null;
                return new TypeProjection(variance, transformToType(bound, UPPER_BOUND, typeVariableByPsiResolver));
            }

            @Override
            public TypeProjection visitType(PsiType type) {
                return new TypeProjection(transformToType(type, howThisTypeIsUsed, typeVariableByPsiResolver));
            }
        });
        return result;
    }

    @NotNull
    public JetType transformToType(@NotNull String kotlinSignature, TypeVariableResolver typeVariableResolver) {
        final JetType[] r = new JetType[1];
        JetTypeJetSignatureReader reader = new JetTypeJetSignatureReader(javaSemanticServices, KotlinBuiltIns.getInstance(), typeVariableResolver) {
            @Override
            protected void done(@NotNull JetType jetType) {
                r[0] = jetType;
            }
        };
        new JetSignatureReader(kotlinSignature).acceptType(reader);
        return r[0];
    }

    @NotNull
    public JetType transformToType(@NotNull PsiType javaType,
                                   @NotNull final TypeVariableResolver typeVariableResolver) {
        return transformToType(javaType, TypeUsage.MEMBER_SIGNATURE_INVARIANT, typeVariableResolver);
    }

    @NotNull
    public JetType transformToType(@NotNull PsiType javaType, @NotNull final TypeUsage howThisTypeIsUsed,
            @NotNull final TypeVariableResolver typeVariableResolver) {
        JetType result = javaType.accept(new PsiTypeVisitor<JetType>() {
            @Override
            public JetType visitClassType(PsiClassType classType) {
                PsiClassType.ClassResolveResult classResolveResult = classType.resolveGenerics();
                PsiClass psiClass = classResolveResult.getElement();
                if (psiClass == null) {
                    return ErrorUtils.createErrorType("Unresolved java class: " + classType.getPresentableText());
                }

                if (psiClass instanceof PsiTypeParameter) {
                    PsiTypeParameter typeParameter = (PsiTypeParameter) psiClass;

                    PsiTypeParameterListOwner typeParameterListOwner = typeParameter.getOwner();
                    if (typeParameterListOwner instanceof PsiMethod) {
                        PsiMethod psiMethod = (PsiMethod) typeParameterListOwner;
                        if (psiMethod.isConstructor()) {
                            Set<JetType> supertypesJet = Sets.newHashSet();
                            for (PsiClassType supertype : typeParameter.getExtendsListTypes()) {
                                supertypesJet.add(transformToType(supertype, UPPER_BOUND, typeVariableResolver));
                            }
                            return TypeUtils.intersect(JetTypeChecker.INSTANCE, supertypesJet);
                        }
                    }

                    TypeParameterDescriptor typeParameterDescriptor = typeVariableResolver.getTypeVariable(typeParameter.getName());

                    // In Java: ArrayList<T>
                    // In Kotlin: ArrayList<T>, not ArrayList<T?>
                    // nullability will be taken care of in individual member signatures
                    boolean nullable = !EnumSet.of(TYPE_ARGUMENT, UPPER_BOUND, SUPERTYPE_ARGUMENT).contains(howThisTypeIsUsed);
                    if (nullable) {
                        return TypeUtils.makeNullable(typeParameterDescriptor.getDefaultType());
                    }
                    else {
                        return typeParameterDescriptor.getDefaultType();
                    }
                }
                else {
                    // 'L extends List<T>' in Java is a List<T> in Kotlin, not a List<T?>
                    boolean nullable = !EnumSet.of(TYPE_ARGUMENT, SUPERTYPE_ARGUMENT, SUPERTYPE).contains(howThisTypeIsUsed);

                    ClassDescriptor classData = JavaToKotlinClassMap.getInstance().mapKotlinClass(new FqName(psiClass.getQualifiedName()),
                                                                                                    howThisTypeIsUsed);

                    if (classData == null) {
                        classData = resolver.resolveClass(new FqName(psiClass.getQualifiedName()), DescriptorSearchRule.INCLUDE_KOTLIN);
                    }
                    if (classData == null) {
                        return ErrorUtils.createErrorType("Unresolved java class: " + classType.getPresentableText());
                    }

                    List<TypeProjection> arguments = Lists.newArrayList();
                    List<TypeParameterDescriptor> parameters = classData.getTypeConstructor().getParameters();
                    if (isRaw(classType, !parameters.isEmpty())) {
                        for (TypeParameterDescriptor parameter : parameters) {
                            TypeProjection starProjection = SubstitutionUtils.makeStarProjection(parameter);
                            if (howThisTypeIsUsed == SUPERTYPE) {
                                // projections are not allowed in immediate arguments of supertypes
                                arguments.add(new TypeProjection(starProjection.getType()));
                            }
                            else {
                                arguments.add(starProjection);
                            }
                        }
                    }
                    else {
                        PsiType[] psiArguments = classType.getParameters();
                        
                        if (parameters.size() != psiArguments.length) {
                            // Most of the time this means there is an error in the Java code
                            LOG.warn("parameters = " + parameters.size() + ", actual arguments = " + psiArguments.length +
                                     " in " + classType.getPresentableText() + "\n PsiClass: \n" + psiClass.getText());

                            for (TypeParameterDescriptor parameter : parameters) {
                                arguments.add(new TypeProjection(ErrorUtils.createErrorType(parameter.getName().getName())));
                            }
                        }
                        else {
                            for (int i = 0; i < parameters.size(); i++) {
                                PsiType psiArgument = psiArguments[i];
                                TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);

                                TypeUsage howTheProjectionIsUsed = howThisTypeIsUsed == SUPERTYPE ? SUPERTYPE_ARGUMENT : TYPE_ARGUMENT;
                                TypeProjection typeProjection = transformToTypeProjection(
                                        psiArgument, typeParameterDescriptor, typeVariableResolver, howTheProjectionIsUsed);

                                if (typeProjection.getProjectionKind() == typeParameterDescriptor.getVariance()) {
                                    // remove redundant 'out' and 'in'
                                    arguments.add(new TypeProjection(Variance.INVARIANT, typeProjection.getType()));
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
            }

            @Override
            public JetType visitPrimitiveType(PsiPrimitiveType primitiveType) {
                String canonicalText = primitiveType.getCanonicalText();
                JetType type = JavaToKotlinClassMap.getInstance().mapPrimitiveKotlinClass(canonicalText);
                assert type != null : canonicalText;
                return type;
            }

            @Override
            public JetType visitArrayType(PsiArrayType arrayType) {
                PsiType componentType = arrayType.getComponentType();
                if (componentType instanceof PsiPrimitiveType) {
                    JetType jetType = JavaToKotlinClassMap.getInstance().mapPrimitiveKotlinClass("[" + componentType.getCanonicalText());
                    if (jetType != null)
                        return TypeUtils.makeNullable(jetType);
                }

                boolean vararg = arrayType instanceof PsiEllipsisType;

                Variance projectionKind = arrayElementTypeProjectionKind(vararg);
                TypeUsage howArgumentTypeIsUsed = vararg ? MEMBER_SIGNATURE_CONTRAVARIANT : TYPE_ARGUMENT;

                JetType type = transformToType(componentType, howArgumentTypeIsUsed, typeVariableResolver);
                return TypeUtils.makeNullable(KotlinBuiltIns.getInstance().getArrayType(projectionKind, type));
            }

            private Variance arrayElementTypeProjectionKind(boolean vararg) {
                Variance variance;
                if (howThisTypeIsUsed == MEMBER_SIGNATURE_CONTRAVARIANT && !vararg) {
                    variance = Variance.OUT_VARIANCE;
                }
                else {
                    variance = Variance.INVARIANT;
                }
                return variance;
            }

            @Override
            public JetType visitType(PsiType type) {
                throw new UnsupportedOperationException("Unsupported type: " + type.getPresentableText()); // TODO
            }
        });
        return result;
    }

    private static boolean isRaw(@NotNull PsiClassType classType, boolean argumentsExpected) {
        // The second option is needed because sometimes we get weird versions of JDK classes in the class path,
        // such as collections with no generics, so the Java types are not raw, formally, but they don't match with
        // their Kotlin analogs, so we treat them as raw to avoid exceptions
        return classType.isRaw() || argumentsExpected && classType.getParameterCount() == 0;
    }

}

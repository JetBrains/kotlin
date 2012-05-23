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

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;
import org.jetbrains.jet.rt.signature.JetSignatureReader;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.java.JavaTypeTransformer.TypeUsage.*;

/**
 * @author abreslav
 */
public class JavaTypeTransformer {

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



    private Map<String, JetType> primitiveTypesMap;
    private Map<FqName, JetType> classTypesMap;
    private HashMap<FqName, ClassDescriptor> classDescriptorMap;



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
        JetTypeJetSignatureReader reader = new JetTypeJetSignatureReader(javaSemanticServices, JetStandardLibrary.getInstance(), typeVariableResolver) {
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
        return javaType.accept(new PsiTypeVisitor<JetType>() {
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
                    boolean nullable = !EnumSet.of(SUPERTYPE_ARGUMENT, SUPERTYPE).contains(howThisTypeIsUsed);

                    JetType jetAnalog = getKotlinAnalog(new FqName(psiClass.getQualifiedName()));
                    if (jetAnalog != null) {
                        return TypeUtils.makeNullableAsSpecified(jetAnalog, nullable);
                    }

                    final ClassDescriptor classData =
                            resolver.resolveClass(new FqName(psiClass.getQualifiedName()), DescriptorSearchRule.INCLUDE_KOTLIN);
                    if (classData == null) {
                        return ErrorUtils.createErrorType("Unresolve java class: " + classType.getPresentableText());
                    }

                    List<TypeProjection> arguments = Lists.newArrayList();
                    if (classType.isRaw()) {
                        List<TypeParameterDescriptor> parameters = classData.getTypeConstructor().getParameters();
                        for (TypeParameterDescriptor parameter : parameters) {
                            arguments.add(SubstitutionUtils.makeStarProjection(parameter));
                        }
                    }
                    else {
                        List<TypeParameterDescriptor> parameters = classData.getTypeConstructor().getParameters();
                        PsiType[] psiArguments = classType.getParameters();
                        
                        if (parameters.size() != psiArguments.length) {
                            throw new IllegalStateException(
                                    "parameters = " + parameters.size() + ", actual arguments = " + psiArguments.length
                                            + " in " + classType.getPresentableText());
                        }
                        
                        for (int i = 0; i < parameters.size(); i++) {
                            PsiType psiArgument = psiArguments[i];
                            TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);

                            TypeUsage howTheProjectionIsUsed = howThisTypeIsUsed == SUPERTYPE ? SUPERTYPE_ARGUMENT : TYPE_ARGUMENT;
                            arguments.add(transformToTypeProjection(psiArgument, typeParameterDescriptor, typeVariableResolver, howTheProjectionIsUsed));
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
                JetType type = getPrimitiveTypesMap().get(canonicalText);
                assert type != null : canonicalText;
                return type;
            }

            @Override
            public JetType visitArrayType(PsiArrayType arrayType) {
                PsiType componentType = arrayType.getComponentType();
                if(componentType instanceof PsiPrimitiveType) {
                    JetType jetType = getPrimitiveTypesMap().get("[" + componentType.getCanonicalText());
                    if(jetType != null)
                        return TypeUtils.makeNullable(jetType);
                }

                JetType type = transformToType(componentType, typeVariableResolver);
                return TypeUtils.makeNullable(JetStandardLibrary.getInstance().getArrayType(type));
            }

            @Override
            public JetType visitType(PsiType type) {
                throw new UnsupportedOperationException("Unsupported type: " + type.getPresentableText()); // TODO
            }
        });
    }

    public Map<String, JetType> getPrimitiveTypesMap() {
        if (primitiveTypesMap == null) {
            primitiveTypesMap = new HashMap<String, JetType>();
            for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
                PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
                primitiveTypesMap.put(jvmPrimitiveType.getName(), JetStandardLibrary.getInstance().getPrimitiveJetType(primitiveType));
                primitiveTypesMap.put("[" + jvmPrimitiveType.getName(), JetStandardLibrary.getInstance().getPrimitiveArrayJetType(primitiveType));
                primitiveTypesMap.put(jvmPrimitiveType.getWrapper().getFqName().getFqName(), JetStandardLibrary.getInstance().getNullablePrimitiveJetType(primitiveType));
            }
            primitiveTypesMap.put("void", JetStandardClasses.getUnitType());
        }
        return primitiveTypesMap;
    }

    public Map<FqName, JetType> getClassTypesMap() {
        if (classTypesMap == null) {
            classTypesMap = new HashMap<FqName, JetType>();
            for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
                PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
                classTypesMap.put(jvmPrimitiveType.getWrapper().getFqName(), JetStandardLibrary.getInstance().getNullablePrimitiveJetType(primitiveType));
            }
            classTypesMap.put(new FqName("java.lang.Object"), JetStandardClasses.getNullableAnyType());
            classTypesMap.put(new FqName("java.lang.String"), JetStandardLibrary.getInstance().getNullableStringType());
            classTypesMap.put(new FqName("java.lang.CharSequence"), JetStandardLibrary.getInstance().getNullableCharSequenceType());
            classTypesMap.put(new FqName("java.lang.Throwable"), JetStandardLibrary.getInstance().getNullableThrowableType());
        }
        return classTypesMap;
    }

    @Nullable
    public JetType getKotlinAnalog(@NotNull FqName fqName) {
        return getClassTypesMap().get(fqName);
    }

    private Map<FqName, ClassDescriptor> getPrimitiveWrappersClassDescriptorMap() {
        if (classDescriptorMap == null) {
            classDescriptorMap = new HashMap<FqName, ClassDescriptor>();
            for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
                PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
                classDescriptorMap.put(jvmPrimitiveType.getWrapper().getFqName(), JetStandardLibrary.getInstance().getPrimitiveClassDescriptor(primitiveType));
            }
            classDescriptorMap.put(new FqName("java.lang.String"), JetStandardLibrary.getInstance().getString());
            classDescriptorMap.put(new FqName("java.lang.CharSequence"), JetStandardLibrary.getInstance().getCharSequence());
            classDescriptorMap.put(new FqName("java.lang.Throwable"), JetStandardLibrary.getInstance().getThrowable());
        }
        return classDescriptorMap;
    }

    @Nullable
    public ClassDescriptor unwrapPrimitive(@NotNull FqName fqName) {
        return getPrimitiveWrappersClassDescriptorMap().get(fqName);
    }

    /**
     * We convert Java types differently, depending on where they occur in the Java code
     * This enum encodes the kinds of occurrences
     */
    public enum TypeUsage {
        // Type T occurs somewhere as a generic argument, e.g.: List<T> or List<? extends T>
        TYPE_ARGUMENT,
        UPPER_BOUND,
        MEMBER_SIGNATURE_COVARIANT,
        MEMBER_SIGNATURE_CONTRAVARIANT,
        MEMBER_SIGNATURE_INVARIANT,
        SUPERTYPE,
        SUPERTYPE_ARGUMENT
    }
}

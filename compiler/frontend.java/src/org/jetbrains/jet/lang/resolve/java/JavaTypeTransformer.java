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
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.lang.types.lang.PrimitiveType;
import org.jetbrains.jet.rt.signature.JetSignatureReader;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<String, JetType> classTypesMap;
    private Map<String, ClassDescriptor> classDescriptorMap;



    @NotNull
    public TypeProjection transformToTypeProjection(@NotNull final PsiType javaType,
            @NotNull final TypeParameterDescriptor typeParameterDescriptor,
            @NotNull final TypeVariableResolver typeVariableByPsiResolver) {
        TypeProjection result = javaType.accept(new PsiTypeVisitor<TypeProjection>() {

            @Override
            public TypeProjection visitCapturedWildcardType(PsiCapturedWildcardType capturedWildcardType) {
                throw new UnsupportedOperationException(); // TODO
            }

            @Override
            public TypeProjection visitWildcardType(PsiWildcardType wildcardType) {
                if (!wildcardType.isBounded()) {
                    return TypeUtils.makeStarProjection(typeParameterDescriptor);
                }
                Variance variance = wildcardType.isExtends() ? Variance.OUT_VARIANCE : Variance.IN_VARIANCE;

                PsiType bound = wildcardType.getBound();
                assert bound != null;
                return new TypeProjection(variance, transformToType(bound, typeVariableByPsiResolver));
            }

            @Override
            public TypeProjection visitType(PsiType type) {
                return new TypeProjection(transformToType(type, typeVariableByPsiResolver));
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
                    TypeParameterDescriptor typeParameterDescriptor = typeVariableResolver.getTypeVariable(typeParameter.getName());
//                    return TypeUtils.makeNullable(typeParameterDescriptor.getDefaultType());
                    return typeParameterDescriptor.getDefaultType();
                }
                else {
                    JetType jetAnalog = getClassTypesMap().get(psiClass.getQualifiedName());
                    if (jetAnalog != null) {
                        return jetAnalog;
                    }

                    final JavaDescriptorResolver.ResolverClassData classData = resolver.resolveClassData(psiClass, DescriptorSearchRule.INCLUDE_KOTLIN);
                    if (classData == null) {
                        return ErrorUtils.createErrorType("Unresolve java class: " + classType.getPresentableText());
                    }

                    List<TypeProjection> arguments = Lists.newArrayList();
                    if (classType.isRaw()) {
                        List<TypeParameterDescriptor> parameters = classData.getClassDescriptor().getTypeConstructor().getParameters();
                        for (TypeParameterDescriptor parameter : parameters) {
                            arguments.add(TypeUtils.makeStarProjection(parameter));
                        }
                    }
                    else {
                        List<TypeParameterDescriptor> parameters = classData.getClassDescriptor().getTypeConstructor().getParameters();
                        PsiType[] psiArguments = classType.getParameters();
                        
                        if (parameters.size() != psiArguments.length) {
                            throw new IllegalStateException();
                        }
                        
                        for (int i = 0; i < parameters.size(); i++) {
                            PsiType psiArgument = psiArguments[i];
                            TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);

                            arguments.add(transformToTypeProjection(psiArgument, typeParameterDescriptor, typeVariableResolver));
                        }
                    }
                    return new JetTypeImpl(
                            Collections.<AnnotationDescriptor>emptyList(),
                            classData.getClassDescriptor().getTypeConstructor(),
                            true,
                            arguments,
                            classData.getClassDescriptor().getMemberScope(arguments));
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
                primitiveTypesMap.put(jvmPrimitiveType.getWrapper().getFqName(), JetStandardLibrary.getInstance().getNullablePrimitiveJetType(primitiveType));
            }
            primitiveTypesMap.put("void", JetStandardClasses.getUnitType());
        }
        return primitiveTypesMap;
    }

    public Map<String, JetType> getClassTypesMap() {
        if (classTypesMap == null) {
            classTypesMap = new HashMap<String, JetType>();
            for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
                PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
                classTypesMap.put(jvmPrimitiveType.getWrapper().getFqName(), JetStandardLibrary.getInstance().getNullablePrimitiveJetType(primitiveType));
            }
            classTypesMap.put("java.lang.Object", JetStandardClasses.getNullableAnyType());
            classTypesMap.put("java.lang.String", JetStandardLibrary.getInstance().getNullableStringType());
            classTypesMap.put("java.lang.CharSequence", JetStandardLibrary.getInstance().getNullableCharSequenceType());
            classTypesMap.put("java.lang.Throwable", JetStandardLibrary.getInstance().getNullableThrowableType());
        }
        return classTypesMap;
    }

    public Map<String, ClassDescriptor> getPrimitiveWrappersClassDescriptorMap() {
        if (classDescriptorMap == null) {
            classDescriptorMap = new HashMap<String, ClassDescriptor>();
            for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
                PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
                classDescriptorMap.put(jvmPrimitiveType.getWrapper().getFqName(), JetStandardLibrary.getInstance().getPrimitiveClassDescriptor(primitiveType));
            }
            classDescriptorMap.put("java.lang.String", JetStandardLibrary.getInstance().getString());
            classDescriptorMap.put("java.lang.CharSequence", JetStandardLibrary.getInstance().getCharSequence());
            classDescriptorMap.put("java.lang.Throwable", JetStandardLibrary.getInstance().getThrowable());
        }
        return classDescriptorMap;
    }
}

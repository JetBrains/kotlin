package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Lists;
import com.intellij.psi.*;
import org.jetbrains.jet.rt.signature.JetSignatureReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class JavaTypeTransformer {

    private final JavaSemanticServices javaSemanticServices;
    private final JavaDescriptorResolver resolver;
    private final JetStandardLibrary standardLibrary;
    private Map<String, JetType> primitiveTypesMap;
    private Map<String, JetType> classTypesMap;
    private Map<String, ClassDescriptor> classDescriptorMap;

    public JavaTypeTransformer(JavaSemanticServices javaSemanticServices, JetStandardLibrary standardLibrary, JavaDescriptorResolver resolver) {
        this.javaSemanticServices = javaSemanticServices;
        this.resolver = resolver;
        this.standardLibrary = standardLibrary;
    }

    @NotNull
    public TypeProjection transformToTypeProjection(@NotNull final PsiType javaType,
            @NotNull final TypeParameterDescriptor typeParameterDescriptor,
            @NotNull final TypeVariableByPsiResolver typeVariableByPsiResolver) {
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
    public JetType transformToType(@NotNull String kotlinSignature, TypeVariableByNameResolver typeVariableByNameResolver) {
        final JetType[] r = new JetType[1];
        JetTypeJetSignatureReader reader = new JetTypeJetSignatureReader(javaSemanticServices, standardLibrary, typeVariableByNameResolver) {
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
            @NotNull final TypeVariableByPsiResolver typeVariableByPsiResolver) {
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
                    TypeParameterDescriptor typeParameterDescriptor = typeVariableByPsiResolver.getTypeVariable(typeParameter);
//                    return TypeUtils.makeNullable(typeParameterDescriptor.getDefaultType());
                    return typeParameterDescriptor.getDefaultType();
                }
                else {
                    JetType jetAnalog = getClassTypesMap().get(psiClass.getQualifiedName());
                    if (jetAnalog != null) {
                        return jetAnalog;
                    }

                    final JavaDescriptorResolver.ResolverClassData classData = resolver.resolveClassData(psiClass);
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
                            
                            TypeVariableResolver typeVariableByPsiResolver2 = new TypeVariableResolver() {
                                @NotNull
                                @Override
                                public TypeParameterDescriptor getTypeVariable(@NotNull String name) {
                                    throw new RuntimeException(); // TODO
                                }

                                @NotNull
                                @Override
                                public TypeParameterDescriptor getTypeVariable(@NotNull PsiTypeParameter psiTypeParameter) {
                                    if (classData instanceof JavaDescriptorResolver.ResolverSrcClassData) {
                                        // hack for TypeInfoImpl
                                        for (TypeParameterDescriptor typeParameter : classData.getClassDescriptor().getTypeConstructor().getParameters()) {
                                            if (psiTypeParameter.getName().equals(typeParameter.getName())) {
                                                // TODO?
                                                return typeParameter;
                                            }
                                        }
                                        throw new IllegalStateException();
                                    } else if (classData instanceof JavaDescriptorResolver.ResolverBinaryClassData) {
                                        return new TypeVariableByPsiResolverImpl(((JavaDescriptorResolver.ResolverBinaryClassData) classData).typeParameters, typeVariableByPsiResolver).getTypeVariable(psiTypeParameter);
                                    } else {
                                        throw new IllegalStateException();
                                    }
                                }
                            };
                            arguments.add(transformToTypeProjection(psiArgument, typeParameterDescriptor, typeVariableByPsiResolver2));
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

                JetType type = transformToType(componentType, typeVariableByPsiResolver);
                return TypeUtils.makeNullable(standardLibrary.getArrayType(type));
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
                primitiveTypesMap.put(jvmPrimitiveType.getName(), standardLibrary.getPrimitiveJetType(primitiveType));
                primitiveTypesMap.put("[" + jvmPrimitiveType.getName(), standardLibrary.getPrimitiveArrayJetType(primitiveType));
                primitiveTypesMap.put(jvmPrimitiveType.getWrapper().getFqName(), standardLibrary.getNullablePrimitiveJetType(primitiveType));
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
                classTypesMap.put(jvmPrimitiveType.getWrapper().getFqName(), standardLibrary.getNullablePrimitiveJetType(primitiveType));
            }
            classTypesMap.put("java.lang.Object", JetStandardClasses.getNullableAnyType());
            classTypesMap.put("java.lang.String", standardLibrary.getNullableStringType());
            classTypesMap.put("java.lang.CharSequence", standardLibrary.getNullableCharSequenceType());
        }
        return classTypesMap;
    }
    
    public Map<String, ClassDescriptor> getPrimitiveWrappersClassDescriptorMap() {
        if (classDescriptorMap == null) {
            classDescriptorMap = new HashMap<String, ClassDescriptor>();
            for (JvmPrimitiveType jvmPrimitiveType : JvmPrimitiveType.values()) {
                PrimitiveType primitiveType = jvmPrimitiveType.getPrimitiveType();
                classDescriptorMap.put(jvmPrimitiveType.getWrapper().getFqName(), standardLibrary.getPrimitiveClassDescriptor(primitiveType));
            }
            classDescriptorMap.put("java.lang.String", standardLibrary.getString());
            classDescriptorMap.put("java.lang.CharSequence", standardLibrary.getCharSequence());
        }
        return classDescriptorMap;
    }
}

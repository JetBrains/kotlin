package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Lists;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class JavaTypeTransformer {

    private final JavaDescriptorResolver resolver;
    private final JetStandardLibrary standardLibrary;
    private Map<String, JetType> primitiveTypesMap;
    private Map<String, JetType> classTypesMap;

    public JavaTypeTransformer(JetStandardLibrary standardLibrary, JavaDescriptorResolver resolver) {
        this.resolver = resolver;
        this.standardLibrary = standardLibrary;
    }

    @NotNull
    public JetType transform(PsiType javaType) {
        return javaType.accept(new PsiTypeVisitor<JetType>() {
            @Override
            public JetType visitClassType(PsiClassType classType) {
                PsiClassType.ClassResolveResult classResolveResult = classType.resolveGenerics();
                PsiClass psiClass = classResolveResult.getElement();
                if (psiClass == null) {
                    return ErrorUtils.createErrorType("Unresolved java class: " + classType.getPresentableText());
                }

                JetType jetAnalog = getClassTypesMap().get(psiClass.getQualifiedName());
                if (jetAnalog != null) {
                    return jetAnalog;
                }

                ClassDescriptor descriptor = resolver.resolveClass(psiClass);

                List<TypeProjection> arguments = Lists.newArrayList();
                if (classType.isRaw()) { // TODO
//                    List<TypeParameterDescriptor> parameters = descriptor.getTypeConstructor().getParameters();
//                    for (TypeParameterDescriptor parameter : parameters) {
//                        arguments.add(TypeUtils.makeStarProjection(parameter));
//                    }
                } else { // TODO
//                    PsiType[] psiArguments = classType.getParameters();
//                    for (PsiType psiArgument : psiArguments) {
//                        arguments.add(new TypeProjection(transform(psiArgument)));
//                    }
                }
                return new JetTypeImpl(
                        Collections.<Attribute>emptyList(),
                        descriptor.getTypeConstructor(),
                        true,
                        arguments,
                        descriptor.getMemberScope(arguments));
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
                JetType type = transform(arrayType.getComponentType());
                return TypeUtils.makeNullable(standardLibrary.getArrayType(type));
            }

            @Override
            public JetType visitWildcardType(PsiWildcardType wildcardType) {
                System.out.println("!!!");
                return JetStandardClasses.getNullableAnyType(); // TODO
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
            primitiveTypesMap.put("byte", standardLibrary.getByteType());
            primitiveTypesMap.put("short", standardLibrary.getShortType());
            primitiveTypesMap.put("char", standardLibrary.getCharType());
            primitiveTypesMap.put("int", standardLibrary.getIntType());
            primitiveTypesMap.put("long", standardLibrary.getLongType());
            primitiveTypesMap.put("float", standardLibrary.getFloatType());
            primitiveTypesMap.put("double", standardLibrary.getDoubleType());
            primitiveTypesMap.put("boolean", standardLibrary.getBooleanType());
            primitiveTypesMap.put("void", JetStandardClasses.getUnitType());
            primitiveTypesMap.put("java.lang.Byte", TypeUtils.makeNullable(standardLibrary.getByteType()));
            primitiveTypesMap.put("java.lang.Short", TypeUtils.makeNullable(standardLibrary.getShortType()));
            primitiveTypesMap.put("java.lang.Character", TypeUtils.makeNullable(standardLibrary.getCharType()));
            primitiveTypesMap.put("java.lang.Integer", TypeUtils.makeNullable(standardLibrary.getIntType()));
            primitiveTypesMap.put("java.lang.Long", TypeUtils.makeNullable(standardLibrary.getLongType()));
            primitiveTypesMap.put("java.lang.Float", TypeUtils.makeNullable(standardLibrary.getFloatType()));
            primitiveTypesMap.put("java.lang.Double", TypeUtils.makeNullable(standardLibrary.getDoubleType()));
            primitiveTypesMap.put("java.lang.Boolean", TypeUtils.makeNullable(standardLibrary.getBooleanType()));
        }
        return primitiveTypesMap;
    }

    public Map<String, JetType> getClassTypesMap() {
        if (classTypesMap == null) {
            classTypesMap = new HashMap<String, JetType>();
            classTypesMap.put("java.lang.Byte", TypeUtils.makeNullable(standardLibrary.getByteType()));
            classTypesMap.put("java.lang.Short", TypeUtils.makeNullable(standardLibrary.getShortType()));
            classTypesMap.put("java.lang.Character", TypeUtils.makeNullable(standardLibrary.getCharType()));
            classTypesMap.put("java.lang.Integer", TypeUtils.makeNullable(standardLibrary.getIntType()));
            classTypesMap.put("java.lang.Long", TypeUtils.makeNullable(standardLibrary.getLongType()));
            classTypesMap.put("java.lang.Float", TypeUtils.makeNullable(standardLibrary.getFloatType()));
            classTypesMap.put("java.lang.Double", TypeUtils.makeNullable(standardLibrary.getDoubleType()));
            classTypesMap.put("java.lang.Boolean", TypeUtils.makeNullable(standardLibrary.getBooleanType()));
            classTypesMap.put("java.lang.Object", JetStandardClasses.getNullableAnyType());
            classTypesMap.put("java.lang.String", standardLibrary.getNullableStringType());
        }
        return classTypesMap;
    }
}

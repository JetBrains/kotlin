package org.jetbrains.jet.lang.resolve.java;

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
    private Map<String, Type> primitiveTypesMap;

    public JavaTypeTransformer(JetStandardLibrary standardLibrary, JavaDescriptorResolver resolver) {
        this.resolver = resolver;
        this.standardLibrary = standardLibrary;
    }

    @NotNull
    public Type transform(PsiType javaType) {
        return javaType.accept(new PsiTypeVisitor<Type>() {
            @Override
            public Type visitClassType(PsiClassType classType) {
                PsiClass psiClass = classType.resolveGenerics().getElement();
                if (psiClass == null) {
                    return ErrorType.createErrorType("Unresolved java class: " + classType.getPresentableText());
                }

                if ("java.lang.Object".equals(psiClass.getQualifiedName())) {
                    return JetStandardClasses.getNullableAnyType();
                }

                ClassDescriptor descriptor = resolver.resolveClass(psiClass);
                // TODO : arguments & raw types
                List<TypeProjection> arguments = Collections.<TypeProjection>emptyList(); // TODO
                return new TypeImpl(
                        Collections.<Attribute>emptyList(),
                        descriptor.getTypeConstructor(),
                        true,
                        arguments,
                        descriptor.getMemberScope(arguments));
            }

            @Override
            public Type visitPrimitiveType(PsiPrimitiveType primitiveType) {
                String canonicalText = primitiveType.getCanonicalText();
                Type type = getPrimitiveTypesMap().get(canonicalText);
                assert type != null : canonicalText;
                return type;
            }

            @Override
            public Type visitArrayType(PsiArrayType arrayType) {
                Type type = transform(arrayType.getComponentType());
                return TypeUtils.makeNullable(standardLibrary.getArrayType(type));
            }

            @Override
            public Type visitType(PsiType type) {
                throw new UnsupportedOperationException("Unsupported type: " + type.getPresentableText()); // TODO
            }
        });
    }

    public Map<String, Type> getPrimitiveTypesMap() {
        if (primitiveTypesMap == null) {
            primitiveTypesMap = new HashMap<String, Type>();
            primitiveTypesMap.put("byte", standardLibrary.getByteType());
            primitiveTypesMap.put("short", standardLibrary.getShortType());
            primitiveTypesMap.put("char", standardLibrary.getCharType());
            primitiveTypesMap.put("int", standardLibrary.getIntType());
            primitiveTypesMap.put("long", standardLibrary.getLongType());
            primitiveTypesMap.put("float", standardLibrary.getFloatType());
            primitiveTypesMap.put("double", standardLibrary.getDoubleType());
            primitiveTypesMap.put("boolean", standardLibrary.getBooleanType());
            primitiveTypesMap.put("void", JetStandardClasses.getUnitType());
        }
        return primitiveTypesMap;
    }
}

package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

/**
 * @author abreslav
 */
public class ClassDescriptorResolver {
    public static final ClassDescriptorResolver INSTANCE = new ClassDescriptorResolver();

    private ClassDescriptorResolver() {}

    @Nullable
    public ClassDescriptor resolveClassDescriptor(@NotNull JetScope scope, @NotNull JetClass classElement) {
        TypeParameterExtensibleScope extensibleScope = new TypeParameterExtensibleScope(scope);

        // This call has side-effects on the extensibleScope (fills it in)
        List<TypeParameterDescriptor> typeParameters
                = resolveTypeParameters(extensibleScope, classElement.getTypeParameters());

        List<JetDelegationSpecifier> delegationSpecifiers = classElement.getDelegationSpecifiers();
        Collection<? extends Type> superclasses = delegationSpecifiers.isEmpty()
                ? Collections.singleton(JetStandardClasses.getAnyType())
                : resolveTypes(extensibleScope, delegationSpecifiers);
        boolean open = classElement.getModifierList().hasModifier(JetTokens.OPEN_KEYWORD);
        return new ClassDescriptor(
                AttributeResolver.INSTANCE.resolveAttributes(classElement.getModifierList()),
                !open,
                classElement.getName(),
                typeParameters,
                superclasses
        );
    }

    private static List<TypeParameterDescriptor> resolveTypeParameters(TypeParameterExtensibleScope extensibleScope, List<JetTypeParameter> typeParameters) {
        List<TypeParameterDescriptor> result = new ArrayList<TypeParameterDescriptor>();
        for (JetTypeParameter typeParameter : typeParameters) {
            result.add(resolveTypeParameter(extensibleScope, typeParameter));
        }
        return result;
    }

    private static TypeParameterDescriptor resolveTypeParameter(TypeParameterExtensibleScope extensibleScope, JetTypeParameter typeParameter) {
        JetTypeReference extendsBound = typeParameter.getExtendsBound();
        TypeParameterDescriptor typeParameterDescriptor = new TypeParameterDescriptor(
                AttributeResolver.INSTANCE.resolveAttributes(typeParameter.getModifierList()),
                typeParameter.getVariance(),
                typeParameter.getName(),
                extendsBound == null
                        ? Collections.<Type>singleton(JetStandardClasses.getAnyType())
                        : Collections.singleton(TypeResolver.INSTANCE.resolveType(extensibleScope, extendsBound))
        );
        extensibleScope.addTypeParameterDescriptor(typeParameterDescriptor);
        return typeParameterDescriptor;
    }

    private static Collection<? extends Type> resolveTypes(TypeParameterExtensibleScope extensibleScope, List<JetDelegationSpecifier> delegationSpecifiers) {
        if (delegationSpecifiers.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<Type> result = new ArrayList<Type>();
        for (JetDelegationSpecifier delegationSpecifier : delegationSpecifiers) {
            result.add(resolveType(extensibleScope, delegationSpecifier));
        }
        return result;
    }

    private static Type resolveType(JetScope scope, JetDelegationSpecifier delegationSpecifier) {
        JetTypeReference typeReference = delegationSpecifier.getTypeReference(); // TODO : make it not null
        return TypeResolver.INSTANCE.resolveType(scope, typeReference);
    }

    private static final class TypeParameterExtensibleScope extends JetScopeAdapter {

        private final Map<String, TypeParameterDescriptor> typeParameterDescriptors = new HashMap<String, TypeParameterDescriptor>();
        private TypeParameterExtensibleScope(JetScope scope) {
            super(scope);
        }

        public void addTypeParameterDescriptor(TypeParameterDescriptor typeParameterDescriptor) {
            String name = typeParameterDescriptor.getName();
            if (typeParameterDescriptors.containsKey(name)) {
                throw new UnsupportedOperationException(); // TODO
            }
            typeParameterDescriptors.put(name, typeParameterDescriptor);
        }

        @Override
        public TypeParameterDescriptor getTypeParameterDescriptor(String name) {
            TypeParameterDescriptor typeParameterDescriptor = typeParameterDescriptors.get(name);
            if (typeParameterDescriptor != null) {
                return typeParameterDescriptor;
            }
            return super.getTypeParameterDescriptor(name);
        }

    }
    @NotNull
    public PropertyDescriptor resolvePropertyDescriptor(@NotNull JetScope scope, @NotNull JetParameter parameter) {
        return new PropertyDescriptor(
                AttributeResolver.INSTANCE.resolveAttributes(parameter.getModifierList()),
                parameter.getName(),
                TypeResolver.INSTANCE.resolveType(scope, parameter.getTypeReference()));
    }

    public static PropertyDescriptor resolvePropertyDescriptor(@NotNull JetScope scope, JetProperty property) {
        // TODO : receiver?
        JetTypeReference propertyTypeRef = property.getPropertyTypeRef();

        Type type;
        if (propertyTypeRef == null) {
            JetExpression initializer = property.getInitializer();
            assert initializer != null;
            // TODO : ??? Fix-point here: what if we have something like "val a = foo {a.bar()}"
            type = JetTypeChecker.INSTANCE.getType(scope, initializer, false);
        } else {
            type = TypeResolver.INSTANCE.resolveType(scope, propertyTypeRef);
        }

        return new PropertyDescriptor(
                AttributeResolver.INSTANCE.resolveAttributes(property.getModifierList()),
                property.getName(),
                type);
    }
}

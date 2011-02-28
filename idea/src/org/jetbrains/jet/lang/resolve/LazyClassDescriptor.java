package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class LazyClassDescriptor implements ClassDescriptor {
    private final JetScope outerScope;
    private JetClass declaration;
    private TypeConstructor typeConstructor;
    private LazyScope unsubstitutedMemberScope;
    private List<Attribute> attributes;
    private WritableScope parameterScope;
    private List<TypeParameterDescriptor> typeParameterDescriptors;

    public LazyClassDescriptor(@NotNull JetScope scope, @NotNull JetClass klass) {
        this.declaration = klass;
        this.outerScope = scope;
    }

    @Override
    public List<Attribute> getAttributes() {
        if (attributes == null) {
            attributes = AttributeResolver.INSTANCE.resolveAttributes(declaration.getModifierList());
        }
        return attributes;
    }

    @Override
    public String getName() {
        return declaration.getName();
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        if (typeConstructor == null) {
            List<TypeParameterDescriptor> typeParameters = getTypeParameters();
            List<JetDelegationSpecifier> delegationSpecifiers = declaration.getDelegationSpecifiers();
            // TODO : assuming that the hierarchy is acyclic
            // TODO : cannot inherit from an inner class
            Collection<? extends Type> superclasses = delegationSpecifiers.isEmpty()
                    ? Collections.singleton(JetStandardClasses.getAnyType())
                    : ClassDescriptorResolver.resolveTypes(getTypeParameterScope(), delegationSpecifiers);
            boolean open = declaration.hasModifier(JetTokens.OPEN_KEYWORD);

            typeConstructor = new TypeConstructor(getAttributes(), !open, getName(), typeParameters, superclasses);
        }
        return typeConstructor;
    }

    public WritableScope getTypeParameterScope() {
        getTypeParameters();
        return parameterScope;
    }

    private List<TypeParameterDescriptor> getTypeParameters() {
        if (parameterScope == null) {
            parameterScope = new WritableScope(outerScope);
            typeParameterDescriptors = ClassDescriptorResolver.INSTANCE.resolveTypeParameters(parameterScope, declaration.getTypeParameters());
        }
        return typeParameterDescriptors;
    }

    @Override
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        if (unsubstitutedMemberScope == null) {
            unsubstitutedMemberScope = new LazyScope(getTypeParameterScope(), declaration.getDeclarations());
        }
        List<TypeParameterDescriptor> parameters = getTypeConstructor().getParameters();
        if (parameters.isEmpty()) {
            return unsubstitutedMemberScope;
        }
        Map<TypeConstructor,TypeProjection> substitutionContext = TypeSubstitutor.INSTANCE.buildSubstitutionContext(parameters, typeArguments);
        return new SubstitutingScope(unsubstitutedMemberScope, substitutionContext);
    }
}

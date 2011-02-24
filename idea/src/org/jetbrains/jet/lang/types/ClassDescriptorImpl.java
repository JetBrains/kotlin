package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.SubstitutingScope;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class ClassDescriptorImpl extends MemberDescriptorImpl implements ClassDescriptor {
    private final TypeConstructor typeConstructor;
    private final JetScope memberDeclarations;

    public ClassDescriptorImpl(
            List<Attribute> attributes, boolean sealed,
            String name, List<TypeParameterDescriptor> typeParameters,
            Collection<? extends Type> superclasses, JetScope memberDeclarations) {
        super(attributes, name);
        this.typeConstructor = new TypeConstructor(attributes, sealed, name, typeParameters, superclasses);
        this.memberDeclarations = memberDeclarations;
    }

    public ClassDescriptorImpl(String name, JetScope memberDeclarations) {
        this(Collections.<Attribute>emptyList(), true,
                name, Collections.<TypeParameterDescriptor>emptyList(),
                Collections.<Type>singleton(JetStandardClasses.getAnyType()), memberDeclarations);
    }

    @Override
    @NotNull
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @Override
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        Map<TypeConstructor,TypeProjection> substitutionContext = TypeSubstitutor.INSTANCE.buildSubstitutionContext(typeConstructor.getParameters(), typeArguments);
        return new SubstitutingScope(memberDeclarations, substitutionContext);
    }
}
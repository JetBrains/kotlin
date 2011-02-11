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
public class ClassDescriptor extends MemberDescriptorImpl {
    private final TypeConstructor typeConstructor;
    private final JetScope memberDeclarations;

    public ClassDescriptor(
            List<Attribute> attributes, boolean sealed,
            String name, List<TypeParameterDescriptor> typeParameters,
            Collection<? extends Type> superclasses, JetScope memberDeclarations) {
        super(attributes, name);
        this.typeConstructor = new TypeConstructor(attributes, sealed, name, typeParameters, superclasses);
        this.memberDeclarations = memberDeclarations;
    }

    public ClassDescriptor(String name, JetScope memberDeclarations) {
        this(Collections.<Attribute>emptyList(), true,
                name, Collections.<TypeParameterDescriptor>emptyList(),
                Collections.<Type>singleton(JetStandardClasses.getAnyType()), memberDeclarations);
    }

    @NotNull
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        Map<TypeConstructor,TypeProjection> substitutionContext = TypeSubstitutor.INSTANCE.buildSubstitutionContext(typeConstructor.getParameters(), typeArguments);
        return new SubstitutingScope(memberDeclarations, substitutionContext);
    }
}
package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class ClassDescriptor extends MemberDescriptorImpl {
    private final TypeConstructor typeConstructor;
    private final TypeMemberDomain memberDomain;

    public ClassDescriptor(
            List<Attribute> attributes, boolean sealed,
            String name, List<TypeParameterDescriptor> typeParameters,
            Collection<? extends Type> superclasses, TypeMemberDomain memberDomain) {
        super(attributes, name);
        this.typeConstructor = new TypeConstructor(attributes, sealed, name, typeParameters, superclasses);
        this.memberDomain = memberDomain;
    }

    public ClassDescriptor(String name, TypeMemberDomain memberDomain) {
        this(Collections.<Attribute>emptyList(), true,
                name, Collections.<TypeParameterDescriptor>emptyList(),
                Collections.<Type>singleton(JetStandardClasses.getAnyType()), memberDomain);
    }

    @NotNull
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    public TypeMemberDomain getMemberDomain() {
        return memberDomain;
    }

    public ClassDescriptor getClass(String referencedName) {
        throw new UnsupportedOperationException(); // TODO
    }

}
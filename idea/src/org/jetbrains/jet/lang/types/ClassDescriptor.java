package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.lang.modules.MemberDomain;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class ClassDescriptor extends MemberDescriptorImpl implements MemberDomain {
    private final TypeConstructor typeConstructor;

    public ClassDescriptor(
            List<Attribute> attributes,
            String name, List<TypeParameterDescriptor> typeParameters, Collection<? extends Type> superclasses) {
        super(attributes, name);
        this.typeConstructor = new TypeConstructor(attributes, name, typeParameters, superclasses);
    }

    public ClassDescriptor(String name) {
        this(Collections.<Attribute>emptyList(),
                name, Collections.<TypeParameterDescriptor>emptyList(),
                Collections.<Type>singleton(JetStandardClasses.getAnyType()));
    }

    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    public ClassDescriptor getClass(String referencedName) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public MethodDescriptor getMethods(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public PropertyDescriptor getProperty(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public ExtensionDescriptor getExtension(String name) {
        throw new UnsupportedOperationException(); // TODO
    }
}

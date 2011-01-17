package org.jetbrains.jet.lang.types;

import java.util.Collection;
import java.util.List;

/**
 * @author abreslav
 */
public class TypeConstructor extends AnnotatedImpl {
    private final List<TypeParameterDescriptor> parameters;
    private final Collection<Type> supertypes;
    private final String debugName;

    public TypeConstructor(List<Annotation> annotations, String debugName, List<TypeParameterDescriptor> parameters, Collection<Type> supertypes) {
        super(annotations);
        this.debugName = debugName;
        this.parameters = parameters;
        this.supertypes = supertypes;
    }

    public List<TypeParameterDescriptor> getParameters() {
        return parameters;
    }

    public Collection<Type> getSupertypes() {
        return supertypes;
    }

    @Override
    public String toString() {
        return debugName;
    }
}

package org.jetbrains.jet.lang.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author abreslav
 */
public class TypeConstructor extends AnnotatedImpl {
    private final List<TypeParameterDescriptor> parameters;
    private final Collection<? extends JetType> supertypes;
    private final String debugName;
    private final boolean sealed;

    public TypeConstructor(List<Attribute> attributes, boolean sealed, String debugName, List<TypeParameterDescriptor> parameters, Collection<? extends JetType> supertypes) {
        super(attributes);
        this.sealed = sealed;
        this.debugName = debugName;
        this.parameters = new ArrayList<TypeParameterDescriptor>(parameters);
        this.supertypes = supertypes;
    }

    public List<TypeParameterDescriptor> getParameters() {
        return parameters;
    }

    public Collection<? extends JetType> getSupertypes() {
        return supertypes;
    }

    @Override
    public String toString() {
        return debugName;
    }

    public boolean isSealed() {
        return sealed;
    }

}

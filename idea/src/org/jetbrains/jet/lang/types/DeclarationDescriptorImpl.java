package org.jetbrains.jet.lang.types;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class DeclarationDescriptorImpl extends AnnotatedImpl implements Named, DeclarationDescriptor {

    private final String name;

    public DeclarationDescriptorImpl(List<Attribute> attributes, String name) {
        super(attributes);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DeclarationDescriptor getOriginal() {
        return this;
    }
}

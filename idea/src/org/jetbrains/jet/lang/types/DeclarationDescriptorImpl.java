package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.resolve.DescriptorUtil;

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

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        accept(visitor, null);
    }

    @Override
    public String toString() {
        return DescriptorUtil.renderPresentableText(this) + "[" + getClass().getCanonicalName()+ "]";
    }
}

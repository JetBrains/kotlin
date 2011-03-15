package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author abreslav
 */
public class PropertyDescriptorImpl extends DeclarationDescriptorImpl implements PropertyDescriptor {
    private JetType type;

    public PropertyDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, List<Attribute> attributes, String name, JetType type) {
        super(containingDeclaration, attributes, name);
        this.type = type;
    }

    @Override
    public JetType getType() {
        return type;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyDescriptor(this, data);
    }
}

package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author abreslav
 */
public class PropertyDescriptorImpl extends DeclarationDescriptorImpl implements PropertyDescriptor {
    private final JetType inType;
    private final JetType outType;

    public PropertyDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<Attribute> attributes,
            @NotNull String name,
            @Nullable JetType inType,
            @Nullable JetType outType) {
        super(containingDeclaration, attributes, name);
        assert (inType != null) || (outType != null);

        this.inType = inType;
        this.outType = outType;
    }

    @NotNull
    @Override
    public JetType getOutType() {
        return outType;
    }

    @Override
    public JetType getInType() {
        return inType;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyDescriptor(this, data);
    }
}

package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class VariableDescriptorImpl extends DeclarationDescriptorImpl implements VariableDescriptor {
    private JetType inType;
    private JetType outType;

    public VariableDescriptorImpl(
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

    @Override
    public JetType getOutType() {
        return outType;
    }

    @Override
    public JetType getInType() {
        return inType;
    }

    protected void setInType(JetType inType) {
        this.inType = inType;
    }

    protected void setOutType(JetType outType) {
        this.outType = outType;
    }
}

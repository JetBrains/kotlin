package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.Attribute;
import org.jetbrains.jet.lang.types.DeclarationDescriptor;
import org.jetbrains.jet.lang.types.DeclarationDescriptorVisitor;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class MutableDeclarationDescriptor implements DeclarationDescriptor {
    private String name;
    private DeclarationDescriptor containingDeclaration;

    @Override
    public List<Attribute> getAttributes() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getOriginal() {
        return this;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    public void setContainingDeclaration(@Nullable DeclarationDescriptor containingDeclaration) {
        this.containingDeclaration = containingDeclaration;
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        accept(visitor, null);
    }
}

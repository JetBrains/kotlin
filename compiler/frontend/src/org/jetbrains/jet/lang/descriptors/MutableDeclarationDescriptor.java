package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public abstract class MutableDeclarationDescriptor implements DeclarationDescriptor {
    private String name;
    private final DeclarationDescriptor containingDeclaration;

    public MutableDeclarationDescriptor(DeclarationDescriptor containingDeclaration) {
        this.containingDeclaration = containingDeclaration;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        assert this.name == null : this.name;
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

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        accept(visitor, null);
    }
}

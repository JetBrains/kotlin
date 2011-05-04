package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.Annotation;
import org.jetbrains.jet.lang.types.DeclarationDescriptor;
import org.jetbrains.jet.lang.types.DeclarationDescriptorVisitor;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class MutableDeclarationDescriptor implements DeclarationDescriptor {
    private String name;
    private final DeclarationDescriptor containingDeclaration;

    public MutableDeclarationDescriptor(DeclarationDescriptor containingDeclaration) {
        this.containingDeclaration = containingDeclaration;
    }

    @Override
    public List<Annotation> getAnnotations() {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
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

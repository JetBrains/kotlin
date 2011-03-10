package org.jetbrains.jet.lang.resolve;

import org.jetbrains.jet.lang.types.Attribute;
import org.jetbrains.jet.lang.types.DeclarationDescriptor;

import java.util.List;

/**
 * @author abreslav
 */
public class MutableDeclarationDescriptor implements DeclarationDescriptor {
    private String name;

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

    @Override
    public DeclarationDescriptor getOriginal() {
        return this;
    }
}

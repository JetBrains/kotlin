package org.jetbrains.jet.lang.resolve;

import org.jetbrains.jet.lang.types.Annotated;
import org.jetbrains.jet.lang.types.Attribute;
import org.jetbrains.jet.lang.types.Named;

import java.util.List;

/**
 * @author abreslav
 */
public class MutableDeclarationDescriptor implements Annotated, Named {
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
}

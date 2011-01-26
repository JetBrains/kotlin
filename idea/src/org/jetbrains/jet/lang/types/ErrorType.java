package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.lang.psi.JetTypeReference;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class ErrorType extends VisitableTypeImpl implements Type {
    private final String debugLabel;

    public ErrorType(JetTypeReference typeReference) {
        super(Collections.<Attribute>emptyList());
        this.debugLabel = typeReference.getText();
        assert debugLabel != null;
    }

    @Override
    public TypeConstructor getConstructor() {
        throw new UnsupportedOperationException("Not found: " + debugLabel); // TODO
    }

    @Override
    public List<TypeProjection> getArguments() {
        throw new UnsupportedOperationException("Not found: " + debugLabel); // TODO
    }

    @Override
    public Collection<MemberDescriptor> getMembers() {
        throw new UnsupportedOperationException("Not found: " + debugLabel); // TODO
    }

    @Override
    public <R, D> R accept(TypeVisitor<R, D> visitor, D data) {
        throw new UnsupportedOperationException("Not found: " + debugLabel); // TODO
    }

    @Override
    public String toString() {
        return "!!!" + debugLabel + "!!!";
    }
}

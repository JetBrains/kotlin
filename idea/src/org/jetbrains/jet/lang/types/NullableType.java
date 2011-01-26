package org.jetbrains.jet.lang.types;

import java.util.Collection;
import java.util.List;

/**
 * @author abreslav
 */
public class NullableType extends VisitableTypeImpl {
    public NullableType(List<Attribute> attributes) {
        super(attributes);
    }

    @Override
    public TypeConstructor getConstructor() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public List<TypeProjection> getArguments() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Collection<MemberDescriptor> getMembers() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <R, D> R accept(TypeVisitor<R, D> visitor, D data) {
        return visitor.visitNullableType(this, data);
    }
}

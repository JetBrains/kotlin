package org.jetbrains.jet.lang.types;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class VisitableTypeImpl extends AnnotatedImpl implements Type {
    public VisitableTypeImpl(List<Attribute> attributes) {
        super(attributes);
    }

    @Override
    public <R> R acceptNoData(TypeVisitor<R, ?> visitor) {
        return accept(visitor, null);
    }

    @Override
    public void acceptVoid(TypeVisitor<?, ?> visitor) {
        accept(visitor, null);
    }
}

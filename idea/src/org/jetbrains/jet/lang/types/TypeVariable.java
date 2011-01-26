package org.jetbrains.jet.lang.types;


import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class TypeVariable extends TypeImpl {

    public TypeVariable(@NotNull List<Attribute> attributes, @NotNull TypeParameterDescriptor typeParameter) {
        super(attributes, typeParameter.getTypeConstructor(), Collections.<TypeProjection>emptyList());
    }

    @Override
    public Collection<MemberDescriptor> getMembers() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <R, D> R accept(TypeVisitor<R, D> visitor, D data) {
        return visitor.visitTypeVariable(this, data);
    }

    @Override
    public String toString() {
        return "&" + super.toString();
    }
}

package org.jetbrains.jet.lang.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class ThisType extends TypeImpl {
    public ThisType(List<Annotation> annotations, ClassDescriptor thisClass) {
        super(annotations, thisClass.getTypeConstructor(), toArguments(thisClass.getTypeConstructor().getParameters()));
    }

    private static List<TypeProjection> toArguments(List<TypeParameterDescriptor> parameters) {
        List<TypeProjection> result = new ArrayList<TypeProjection>();
        for (TypeParameterDescriptor parameter : parameters) {
            result.add(new TypeProjection(new TypeVariable(Collections.<Annotation>emptyList(), parameter)));
        }
        return result;
    }

    @Override
    public Collection<MemberDescriptor> getMembers() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <R, D> R accept(TypeVisitor<R, D> visitor, D data) {
        return visitor.visitThisType(this, data);
    }
}

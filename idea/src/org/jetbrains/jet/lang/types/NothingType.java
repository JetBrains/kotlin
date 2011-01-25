package org.jetbrains.jet.lang.types;

import java.util.*;

/**
 * @author abreslav
 */
public class NothingType extends TypeImpl {

    public static final TypeConstructor NOTHING = new TypeConstructor(
            Collections.<Annotation>emptyList(),
            "Nothing",
            Collections.<TypeParameterDescriptor>emptyList(),
            new AbstractCollection<Type>() {

                @Override
                public boolean contains(Object o) {
                    return o instanceof Type;
                }

                @Override
                public Iterator<Type> iterator() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int size() {
                    throw new UnsupportedOperationException();
                }
            });

    // TODO: annotations seem wrong here
    public NothingType(List<Annotation> annotations) {
        super(annotations, NOTHING, Collections.<TypeProjection>emptyList());
    }

    @Override
    public Collection<MemberDescriptor> getMembers() {
        return Collections.emptySet();
    }

    @Override
    public <R, D> R accept(TypeVisitor<R, D> visitor, D data) {
        return visitor.visitNothingType(this, data);
    }
}

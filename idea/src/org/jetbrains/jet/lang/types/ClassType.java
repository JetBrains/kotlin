package org.jetbrains.jet.lang.types;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class ClassType extends TypeImpl {
    private final ClassDescriptor classDescriptor;

    public ClassType(List<Annotation> annotations, ClassDescriptor classDescriptor, List<TypeProjection> arguments) {
        super(annotations, classDescriptor.getTypeConstructor(), arguments);
        assert classDescriptor.getTypeConstructor().getParameters().size() == arguments.size();

        this.classDescriptor = classDescriptor;
    }

    public ClassType(ClassDescriptor classDescriptor) {
        this(Collections.<Annotation>emptyList(), classDescriptor, Collections.<TypeProjection>emptyList());
        assert classDescriptor.getTypeConstructor().getParameters().size() == 0;
    }

    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }

    @Override
    public Collection<MemberDescriptor> getMembers() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <R, D> R accept(TypeVisitor<R, D> visitor, D data) {
        return visitor.visitClassType(this, data);
    }
}

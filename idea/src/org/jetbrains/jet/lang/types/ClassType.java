package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class ClassType extends TypeImpl {
    @NotNull
    private final ClassDescriptor classDescriptor;

    public ClassType(@NotNull List<Attribute> attributes, @NotNull ClassDescriptor classDescriptor, @NotNull List<TypeProjection> arguments) {
        super(attributes, classDescriptor.getTypeConstructor(), arguments);
        assert classDescriptor.getTypeConstructor().getParameters().size() == arguments.size() : classDescriptor.getTypeConstructor().getParameters().size() + " != " + arguments.size();

        this.classDescriptor = classDescriptor;
    }

    public ClassType(ClassDescriptor classDescriptor) {
        this(Collections.<Attribute>emptyList(), classDescriptor, Collections.<TypeProjection>emptyList());
        assert classDescriptor.getTypeConstructor().getParameters().size() == 0;
    }

    @NotNull
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

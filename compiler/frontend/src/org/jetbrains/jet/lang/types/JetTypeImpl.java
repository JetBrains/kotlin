package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotatedImpl;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author abreslav
 */
public final class JetTypeImpl extends AnnotatedImpl implements JetType {

    private final TypeConstructor constructor;
    private final List<TypeProjection> arguments;
    private final boolean nullable;
    private JetScope memberScope;

    public JetTypeImpl(List<AnnotationDescriptor> annotations, TypeConstructor constructor, boolean nullable, @NotNull List<TypeProjection> arguments, JetScope memberScope) {
        super(annotations);

        if (memberScope instanceof ErrorUtils.ErrorScope) {
            throw new IllegalStateException();
        }

        this.constructor = constructor;
        this.nullable = nullable;
        this.arguments = arguments;
        this.memberScope = memberScope;
    }

    public JetTypeImpl(TypeConstructor constructor, JetScope memberScope) {
        this(Collections.<AnnotationDescriptor>emptyList(), constructor, false, Collections.<TypeProjection>emptyList(), memberScope);
    }

    public JetTypeImpl(@NotNull ClassDescriptor classDescriptor) {
        this(Collections.<AnnotationDescriptor>emptyList(),
                classDescriptor.getTypeConstructor(),
                false,
                Collections.<TypeProjection>emptyList(),
                classDescriptor.getMemberScope(Collections.<TypeProjection>emptyList()));
    }

    @NotNull
    @Override
    public TypeConstructor getConstructor() {
        return constructor;
    }

    @NotNull
    @Override
    public List<TypeProjection> getArguments() {
        return arguments;
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        if (memberScope == null) {
            // TODO : this was supposed to mean something...
            throw new IllegalStateException(this.toString());
        }
        return memberScope;
    }

    @Override
    public String toString() {
        return constructor + (arguments.isEmpty() ? "" : "<" + argumentsToString() + ">") + (isNullable() ? "?" : "");
    }

    private StringBuilder argumentsToString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Iterator<TypeProjection> iterator = arguments.iterator(); iterator.hasNext();) {
            TypeProjection argument = iterator.next();
            stringBuilder.append(argument);
            if (iterator.hasNext()) {
                stringBuilder.append(", ");
            }
        }
        return stringBuilder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JetTypeImpl type = (JetTypeImpl) o;

        // TODO
        return nullable == type.nullable && JetTypeChecker.INSTANCE.equalTypes(this, type);
//        if (nullable != type.nullable) return false;
//        if (arguments != null ? !arguments.equals(type.arguments) : type.arguments != null) return false;
//        if (constructor != null ? !constructor.equals(type.constructor) : type.constructor != null) return false;
//        if (memberScope != null ? !memberScope.equals(type.memberScope) : type.memberScope != null) return false;

//        return true;
    }

    @Override
    public int hashCode() {
        int result = constructor != null ? constructor.hashCode() : 0;
        result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
        result = 31 * result + (nullable ? 1 : 0);
        return result;
    }


}

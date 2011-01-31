package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author abreslav
 */
public final class TypeImpl extends AnnotatedImpl implements Type {

    private final TypeConstructor constructor;
    private final List<TypeProjection> arguments;
    private final boolean nullable;
    private final TypeMemberDomain memberDomain;

    public TypeImpl(List<Attribute> attributes, TypeConstructor constructor, boolean nullable, List<TypeProjection> arguments, TypeMemberDomain memberDomain) {
        super(attributes);
        this.constructor = constructor;
        this.nullable = nullable;
        this.arguments = arguments;
        this.memberDomain = memberDomain;
    }

    public TypeImpl(TypeConstructor constructor, TypeMemberDomain memberDomain) {
        this(Collections.<Attribute>emptyList(), constructor, false, Collections.<TypeProjection>emptyList(), memberDomain);
    }

    @Override
    public TypeConstructor getConstructor() {
        return constructor;
    }

    @Override
    public List<TypeProjection> getArguments() {
        return arguments;
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    @Override
    public TypeMemberDomain getMemberDomain() {
        return memberDomain;
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

        TypeImpl type = (TypeImpl) o;

        // TODO
        return equalTypes(this, type);
//        if (nullable != type.nullable) return false;
//        if (arguments != null ? !arguments.equals(type.arguments) : type.arguments != null) return false;
//        if (constructor != null ? !constructor.equals(type.constructor) : type.constructor != null) return false;
//        if (memberDomain != null ? !memberDomain.equals(type.memberDomain) : type.memberDomain != null) return false;

//        return true;
    }

    @Override
    public int hashCode() {
        int result = constructor != null ? constructor.hashCode() : 0;
        result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
        result = 31 * result + (nullable ? 1 : 0);
        return result;
    }


    public static boolean equalTypes(@NotNull Type type1, @NotNull Type type2) {
        if (type1.isNullable() != type2.isNullable()) {
            return false;
        }
        if (!type1.getConstructor().equals(type2.getConstructor())) {
            return false;
        }
        List<TypeProjection> type1Arguments = type1.getArguments();
        List<TypeProjection> type2Arguments = type2.getArguments();
        if (type1Arguments.size() != type2Arguments.size()) {
            return false;
        }
        for (int i = 0; i < type1Arguments.size(); i++) {
            TypeProjection typeProjection1 = type1Arguments.get(i);
            TypeProjection typeProjection2 = type2Arguments.get(i);
            if (typeProjection1.getProjectionKind() != typeProjection2.getProjectionKind()) {
                return false;
            }
            if (typeProjection1.getProjectionKind() != ProjectionKind.NEITHER_OUT_NOR_IN) {
                if (!equalTypes(typeProjection1.getType(), typeProjection2.getType())) {
                    return false;
                }
            }
        }
        return true;
    }


}

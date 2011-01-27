package org.jetbrains.jet.lang.types;

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
    public Type getNullableVersion() {
        if (isNullable()) {
            return this;
        }
        // TODO: cache these?
        return new TypeImpl(getAttributes(), constructor, true, arguments, memberDomain);
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

}

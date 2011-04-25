package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.JetScope;

import java.util.List;

/**
 * This is a fake type assigned to namespace expressions. Only member lookup is
 * supposed to be done on these types.
 *
 * @author abreslav
 */
public class NamespaceType implements JetType {
    private final String name;
    @NotNull
    private final JetScope memberScope;

    public NamespaceType(@NotNull String name, @NotNull JetScope memberScope) {
        this.name = name;
        this.memberScope = memberScope;
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return memberScope;
    }

    @NotNull
    @Override
    public TypeConstructor getConstructor() {
        return throwException();
    }

    private TypeConstructor throwException() {
        throw new UnsupportedOperationException("Only member lookup is allowed on a namespace type " + name);
    }

    @NotNull
    @Override
    public List<TypeProjection> getArguments() {
        throwException();
        return null;
    }

    @Override
    public boolean isNullable() {
        throwException();
        return false;
    }

    @Override
    public List<Attribute> getAttributes() {
        throwException();
        return null;
    }
}

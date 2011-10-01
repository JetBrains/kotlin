package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

/**
 * @author abreslav
 */
public class CompositeTypeSubstitution implements TypeSubstitutor.TypeSubstitution {
    private final TypeSubstitutor.TypeSubstitution[] inner;

    public CompositeTypeSubstitution(@NotNull TypeSubstitutor.TypeSubstitution... inner) {
        this.inner = inner;
    }

    @Override
    public TypeProjection get(TypeConstructor key) {
        for (TypeSubstitutor.TypeSubstitution substitution : inner) {
            TypeProjection value = substitution.get(key);
            if (value != null) return value;
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        for (TypeSubstitutor.TypeSubstitution substitution : inner) {
            if (!substitution.isEmpty()) return false;
        }
        return true;
    }
}

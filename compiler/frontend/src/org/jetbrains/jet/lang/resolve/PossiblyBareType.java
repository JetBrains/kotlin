package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.*;

/**
 * Bare types are somewhat like raw types, but in Kotlin they are only allowed on the right-hand side of is/as.
 * For example:
 *
 *   fun foo(a: Any) {
 *     if (a is List) {
 *       // a is known to be List<*> here
 *     }
 *   }
 *
 * Another example:
 *
 *   fun foo(a: Collection<String>) {
 *     if (a is List) {
 *       // a is known to be List<String> here
 *     }
 *   }
 *
 * One can call reconstruct(supertype) to get an actual type from a bare type
 */
public class PossiblyBareType {

    @NotNull
    public static PossiblyBareType bare(@NotNull TypeConstructor bareTypeConstructor, boolean nullable) {
        return new PossiblyBareType(null, bareTypeConstructor, nullable);
    }

    @NotNull
    public static PossiblyBareType type(@NotNull JetType actualType) {
        return new PossiblyBareType(actualType, null, false);
    }

    private final JetType actualType;
    private final TypeConstructor bareTypeConstructor;
    private final boolean nullable;

    private PossiblyBareType(@Nullable JetType actualType, @Nullable TypeConstructor bareTypeConstructor, boolean nullable) {
        this.actualType = actualType;
        this.bareTypeConstructor = bareTypeConstructor;
        this.nullable = nullable;
    }

    public boolean isBare() {
        return actualType == null;
    }

    @NotNull
    public JetType getActualType() {
        //noinspection ConstantConditions
        return actualType;
    }

    @NotNull
    public TypeConstructor getBareTypeConstructor() {
        //noinspection ConstantConditions
        return bareTypeConstructor;
    }

    private boolean isBareTypeNullable() {
        return nullable;
    }

    public boolean isNullable() {
        if (isBare()) return isBareTypeNullable();
        return getActualType().isNullable();
    }

    public PossiblyBareType makeNullable() {
        if (isBare()) {
            return isBareTypeNullable() ? this : bare(getBareTypeConstructor(), true);
        }
        return type(TypeUtils.makeNullable(getActualType()));
    }

    @NotNull
    public JetType reconstruct(@NotNull JetType subjectType) {
        if (!isBare()) return getActualType();

        JetType type = CastDiagnosticsUtil.findStaticallyKnownSubtype(
                TypeUtils.makeNotNullable(subjectType),
                getBareTypeConstructor()
        );
        // No need to make an absent type nullable
        if (type == null) return type;

        return TypeUtils.makeNullableAsSpecified(type, isBareTypeNullable());
    }

    @Override
    public String toString() {
        return isBare() ? "bare " + bareTypeConstructor + (isBareTypeNullable() ? "?" : "") : getActualType().toString();
    }
}

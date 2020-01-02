// IGNORE_LIGHT_TREE
// Does not work in light tree mode due to lack of Java element finder there
// FILE: KotlinTypeChecker.java

public interface KotlinTypeChecker {

    interface TypeConstructorEquality {
        boolean equals(@NotNull TypeConstructor a, @NotNull TypeConstructor b);
    }

    KotlinTypeChecker DEFAULT = NewKotlinTypeChecker.Companion.getDefault();

    boolean isSubtypeOf(@NotNull KotlinType subtype, @NotNull KotlinType supertype);
    boolean equalTypes(@NotNull KotlinType a, @NotNull KotlinType b);
}

// FILE: OverridingUtil.java
public class OverridingUtil {
    public static OverridingUtil createWithEqualityAxioms(@NotNull KotlinTypeChecker.TypeConstructorEquality equalityAxioms) {
        return null;
    }
}

// FILE: main.kt

interface TypeConstructor {
    val x: String
}

fun main() {
    OverridingUtil.createWithEqualityAxioms l1@{ c1, c2 ->
        if (c1.x == c2.x) return@l1 true
        false
    }
}

// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// ENABLE_FOREIGN_ANNOTATIONS
// WITH_STDLIB
// FULL_JDK

// FILE: Unrelated.java
public interface Unrelated {
    public static Unrelated of(int x, String... y) {
        return null;
    }
}

// FILE: NullabilityMixed.java
import org.jspecify.annotations.*;

public interface NullabilityMixed {
    public static NullabilityMixed of(@NonNull String x, @Nullable String... y) {
        return null;
    }
}

// FILE: MultipleMixed.java
public interface MultipleMixed {
    public static MultipleMixed of(int x, double y, String... z) {
        return null;
    }
}

// FILE: OnlyNullability.java
import org.jspecify.annotations.*;

public interface OnlyNullability {
    public static OnlyNullability of(@Nullable String x, @NonNull String... y) {
        return null;
    }
}

// FILE: test.kt

fun <T> accept(t: T) {}

fun test() {
    accept<Unrelated>(<!UNRESOLVED_COLLECTION_LITERAL!>[1, "a", "b"]<!>)
    accept<Unrelated>(<!UNRESOLVED_COLLECTION_LITERAL!>[1]<!>)
    accept<Unrelated>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)

    accept<NullabilityMixed>(<!UNRESOLVED_COLLECTION_LITERAL!>["a", "b", null]<!>)
    accept<NullabilityMixed>(<!UNRESOLVED_COLLECTION_LITERAL!>["a"]<!>)
    accept<NullabilityMixed>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)

    accept<MultipleMixed>(<!UNRESOLVED_COLLECTION_LITERAL!>[1, 2.0, "a", "b"]<!>)
    accept<MultipleMixed>(<!UNRESOLVED_COLLECTION_LITERAL!>[1, 2.0]<!>)
    accept<MultipleMixed>(<!UNRESOLVED_COLLECTION_LITERAL!>[1]<!>)
    accept<MultipleMixed>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)

    accept<OnlyNullability>(<!UNRESOLVED_COLLECTION_LITERAL!>[null, "a", "b"]<!>)
    accept<OnlyNullability>(<!UNRESOLVED_COLLECTION_LITERAL!>["a", "b"]<!>)
    accept<OnlyNullability>(<!UNRESOLVED_COLLECTION_LITERAL!>[null]<!>)
    accept<OnlyNullability>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, javaType, nullableType, stringLiteral,
typeParameter */

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
    accept<Unrelated>(<!ARGUMENT_TYPE_MISMATCH!>[1, "a", "b"]<!>)
    accept<Unrelated>(<!ARGUMENT_TYPE_MISMATCH!>[1]<!>)
    accept<Unrelated>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)

    accept<NullabilityMixed>(<!ARGUMENT_TYPE_MISMATCH!>["a", "b", null]<!>)
    accept<NullabilityMixed>(<!ARGUMENT_TYPE_MISMATCH!>["a"]<!>)
    accept<NullabilityMixed>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)

    accept<MultipleMixed>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2.0, "a", "b"]<!>)
    accept<MultipleMixed>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2.0]<!>)
    accept<MultipleMixed>(<!ARGUMENT_TYPE_MISMATCH!>[1]<!>)
    accept<MultipleMixed>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)

    accept<OnlyNullability>(<!ARGUMENT_TYPE_MISMATCH!>[null, "a", "b"]<!>)
    accept<OnlyNullability>(<!ARGUMENT_TYPE_MISMATCH!>["a", "b"]<!>)
    accept<OnlyNullability>(<!ARGUMENT_TYPE_MISMATCH!>[null]<!>)
    accept<OnlyNullability>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, javaType, nullableType, stringLiteral,
typeParameter */

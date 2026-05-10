// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// ENABLE_FOREIGN_ANNOTATIONS
// WITH_STDLIB
// FULL_JDK

// FILE: Primitive.java

public interface Primitive {
    static Primitive of(int... args) {
        return null;
    }
}

// FILE: NonPrimitive.java

public interface NonPrimitive {
    static NonPrimitive of(String... args) {
        return null;
    }
}

// FILE: NullableNonPrimitive.java
import org.jspecify.annotations.Nullable;

public interface NullableNonPrimitive {
    static NullableNonPrimitive of(@Nullable String... args) {
        return null;
    }
}

// FILE: NotNullNonPrimitive.java
import org.jspecify.annotations.NonNull;

public interface NotNullNonPrimitive {
    static NotNullNonPrimitive of(@NonNull String... args) {
        return null;
    }
}

// FILE: ReturnNullableNonPrimitive.java
import org.jspecify.annotations.Nullable;

public interface ReturnNullableNonPrimitive {
    @Nullable
    static ReturnNullableNonPrimitive of(String... args) {
        return null;
    }
}

// FILE: ReturnNotNullNonPrimitive.java
import org.jspecify.annotations.NonNull;

public interface ReturnNotNullNonPrimitive {
    static class Impl implements ReturnNotNullNonPrimitive {}

    @NonNull
    static ReturnNotNullNonPrimitive of(String... args) {
        return new Impl();
    }
}

// FILE: test.kt

fun <T> accept(t: T) { }

fun test() {
    accept<Primitive>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    accept<Primitive>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)
    accept<Primitive>(<!ARGUMENT_TYPE_MISMATCH!>[1.toLong()]<!>)
    accept<Primitive>(<!ARGUMENT_TYPE_MISMATCH!>[null]<!>)

    accept<NonPrimitive>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    accept<NonPrimitive>(<!ARGUMENT_TYPE_MISMATCH!>["42"]<!>)
    accept<NonPrimitive>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)
    accept<NonPrimitive>(<!ARGUMENT_TYPE_MISMATCH!>[null]<!>)

    accept<NullableNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    accept<NullableNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH!>["42"]<!>)
    accept<NullableNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH!>[null]<!>)
    accept<NullableNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)

    accept<NotNullNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    accept<NotNullNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH!>["42"]<!>)
    accept<NotNullNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH!>[null]<!>)
    accept<NotNullNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)

    accept<ReturnNullableNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    accept<ReturnNullableNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH!>["42"]<!>)
    accept<ReturnNullableNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)
    accept<ReturnNullableNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH!>[null]<!>)

    accept<ReturnNotNullNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    accept<ReturnNotNullNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH!>["42"]<!>)
    accept<ReturnNotNullNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)
    accept<ReturnNotNullNonPrimitive>(<!ARGUMENT_TYPE_MISMATCH!>[null]<!>)
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, javaType, nullableType, stringLiteral,
typeParameter */

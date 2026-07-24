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
    accept<Primitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<Primitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)
    accept<Primitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[1.toLong()]<!>)
    accept<Primitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[null]<!>)

    accept<NonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<NonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>["42"]<!>)
    accept<NonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)
    accept<NonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[null]<!>)

    accept<NullableNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<NullableNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>["42"]<!>)
    accept<NullableNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[null]<!>)
    accept<NullableNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)

    accept<NotNullNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<NotNullNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>["42"]<!>)
    accept<NotNullNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[null]<!>)
    accept<NotNullNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)

    accept<ReturnNullableNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<ReturnNullableNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>["42"]<!>)
    accept<ReturnNullableNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)
    accept<ReturnNullableNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[null]<!>)

    accept<ReturnNotNullNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<ReturnNotNullNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>["42"]<!>)
    accept<ReturnNotNullNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)
    accept<ReturnNotNullNonPrimitive>(<!UNRESOLVED_COLLECTION_LITERAL!>[null]<!>)
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, javaType, nullableType, stringLiteral,
typeParameter */

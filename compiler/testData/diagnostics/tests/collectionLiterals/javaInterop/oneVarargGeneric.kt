// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// ENABLE_FOREIGN_ANNOTATIONS
// WITH_STDLIB
// FULL_JDK

// FILE: Generic.java
public interface Generic<T> {
    static <T> Generic<T> of(T... args) {
        return null;
    }

    static <U> void accept(Generic<U> arg) {
    }
}

// FILE: NullableGeneric.java
import org.jspecify.annotations.Nullable;

public interface NullableGeneric<T> {
    static <T> NullableGeneric<T> of(@Nullable T... args) {
        return null;
    }

    static <U> void accept(NullableGeneric<U> arg) {
    }
}

// FILE: NotNullGeneric.java
import org.jspecify.annotations.NonNull;

public interface NotNullGeneric<T> {
    static <T> NotNullGeneric<T> of(@NonNull T... args) {
        return null;
    }

    static <U> void accept(NotNullGeneric<U> arg) {
    }
}

// FILE: ReturnNullableGeneric.java
import org.jspecify.annotations.Nullable;

public interface ReturnNullableGeneric<T> {
    @Nullable
    static <T> ReturnNullableGeneric<T> of(T... args) {
        return null;
    }

    static <U> void accept(ReturnNullableGeneric<U> arg) {
    }
}

// FILE: ReturnNotNullGeneric.java
import org.jspecify.annotations.NonNull;

public interface ReturnNotNullGeneric<T> {
    static class Impl<T> implements ReturnNotNullGeneric<T> {}

    @NonNull
    static <T> ReturnNotNullGeneric<T> of(T... args) {
        return new Impl<T>();
    }

    static <U> void accept(ReturnNotNullGeneric<U> arg) {
    }
}

// FILE: test.kt

fun <T> accept(t: T) { }

fun <U> acceptGeneric(t: Generic<U>) { }
fun <U> acceptNullableGeneric(t: NullableGeneric<U>) { }
fun <U> acceptNotNullGeneric(t: NotNullGeneric<U>) { }
fun <U> acceptReturnNullableGeneric(t: ReturnNullableGeneric<U>) { }
fun <U> acceptReturnNotNullGeneric(t: ReturnNotNullGeneric<U>) { }

fun test() {
    accept<Generic<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<Generic<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>["42"]<!>)
    accept<Generic<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)
    accept<Generic<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[null]<!>)

    accept<NullableGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<NullableGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>["42"]<!>)
    accept<NullableGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[null]<!>)
    accept<NullableGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)

    accept<NotNullGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<NotNullGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>["42"]<!>)
    accept<NotNullGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[null]<!>)
    accept<NotNullGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)

    accept<ReturnNullableGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<ReturnNullableGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>["42"]<!>)
    accept<ReturnNullableGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)
    accept<ReturnNullableGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[null]<!>)

    accept<ReturnNotNullGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    accept<ReturnNotNullGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>["42"]<!>)
    accept<ReturnNotNullGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)
    accept<ReturnNotNullGeneric<String>>(<!UNRESOLVED_COLLECTION_LITERAL!>[null]<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGeneric<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    Generic.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGeneric<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)
    Generic.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>acceptNullableGeneric<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    NullableGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptNullableGeneric<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)
    NullableGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>acceptNotNullGeneric<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    NotNullGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptNotNullGeneric<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)
    NotNullGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>acceptReturnNullableGeneric<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    ReturnNullableGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptReturnNullableGeneric<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)
    ReturnNullableGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>acceptReturnNotNullGeneric<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    ReturnNotNullGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptReturnNotNullGeneric<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)
    ReturnNotNullGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!UNRESOLVED_COLLECTION_LITERAL!>[42]<!>)
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, javaFunction, javaType, nullableType,
stringLiteral, typeParameter */

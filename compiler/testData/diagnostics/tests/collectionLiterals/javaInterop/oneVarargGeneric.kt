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
    accept<Generic<String>>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    accept<Generic<String>>(<!ARGUMENT_TYPE_MISMATCH!>["42"]<!>)
    accept<Generic<String>>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)
    accept<Generic<String>>(<!ARGUMENT_TYPE_MISMATCH!>[null]<!>)

    accept<NullableGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    accept<NullableGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH!>["42"]<!>)
    accept<NullableGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH!>[null]<!>)
    accept<NullableGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)

    accept<NotNullGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    accept<NotNullGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH!>["42"]<!>)
    accept<NotNullGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH!>[null]<!>)
    accept<NotNullGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)

    accept<ReturnNullableGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    accept<ReturnNullableGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH!>["42"]<!>)
    accept<ReturnNullableGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)
    accept<ReturnNullableGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH!>[null]<!>)

    accept<ReturnNotNullGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    accept<ReturnNotNullGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH!>["42"]<!>)
    accept<ReturnNotNullGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)
    accept<ReturnNotNullGeneric<String>>(<!ARGUMENT_TYPE_MISMATCH!>[null]<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGeneric<!>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    Generic.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptGeneric<!>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)
    Generic.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>acceptNullableGeneric<!>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    NullableGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptNullableGeneric<!>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)
    NullableGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>acceptNotNullGeneric<!>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    NotNullGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptNotNullGeneric<!>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)
    NotNullGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>acceptReturnNullableGeneric<!>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    ReturnNullableGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptReturnNullableGeneric<!>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)
    ReturnNullableGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>acceptReturnNotNullGeneric<!>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    ReturnNotNullGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptReturnNotNullGeneric<!>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)
    ReturnNotNullGeneric.<!CANNOT_INFER_PARAMETER_TYPE!>accept<!>(<!ARGUMENT_TYPE_MISMATCH!>[42]<!>)
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, javaFunction, javaType, nullableType,
stringLiteral, typeParameter */

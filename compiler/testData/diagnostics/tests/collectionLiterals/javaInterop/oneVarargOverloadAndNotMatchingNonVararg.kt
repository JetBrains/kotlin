// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// ENABLE_FOREIGN_ANNOTATIONS
// WITH_STDLIB
// FULL_JDK

// FILE: JavaCollection.java
import org.jspecify.annotations.*;

public interface JavaCollection<T> {
    static class Impl<T> implements JavaCollection<T> {}

    // main overload
    static <T> JavaCollection<T> of(T... ts) {
        return null;
    }

    // OK, just T renamed to U
    static <U> JavaCollection<U> of() {
        return null;
    }

    // FAIL, return types are not identical
    static <T> JavaCollection<String> of(T t) {
        return null;
    }

    // FAIL, parameter type should be T
    static <T> JavaCollection<T> of(String s) {
        return null;
    }

    // FAIL, type parameter bounds don't match
    static <T extends Number> JavaCollection<T> of(T a, T b) {
        return null;
    }

    // FAIL or OK?
    @NonNull
    static <T> JavaCollection<T> of(T a, T b, T c) {
        return new Impl<T>();
    }

    // FAIL or OK?
    @Nullable
    static <T> JavaCollection<T> of(T a, T b, T c, T d) {
        return null;
    }

    // FAIL or OK?
    static <T> JavaCollection<T> of(@Nullable T a, T b, T c, T d, T e) {
        return null;
    }

    // FAIL or OK?
    static <T> JavaCollection<T> of(@NonNull T a, T b, T c, T d, T e, T f) {
        return null;
    }

    // OK, because we allow the same difference in Kotlin
    static <T> JavaCollection<@NonNull T> of(T a, T b, T c, T d, T e, T f, T g) {
        return null;
    }

    // OK, because we allow the same difference in Kotlin
    static <T> JavaCollection<@Nullable T> of(T a, T b, T c, T d, T e, T f, T g, T h) {
        return null;
    }
}

// FILE: CollectionOfNotNulls.java
import org.jspecify.annotations.*;

public interface CollectionOfNotNulls<T> {
    static class Impl<T> implements CollectionOfNotNulls<T> {}

    // main overload
    @NonNull
    static <T> CollectionOfNotNulls<@NonNull T> of(@NonNull T... ts) {
        return new Impl<T>();
    }

    // FAIL or OK?
    @Nullable
    static <T> CollectionOfNotNulls<@NonNull T> of() {
        return null;
    }

    // FAIL or OK?
    static <T> CollectionOfNotNulls<@NonNull T> of(@NonNull T t) {
        return null;
    }

    // FAIL or OK?
    @NonNull
    static <T> CollectionOfNotNulls<@NonNull T> of(T a, T b) {
        return new Impl<T>();
    }

    // FAIL or OK?
    /* If we allow it, it is not very good: in Kotlin, call `CollectionOfNotNulls.of(1, 2, 3)` resolves to vararg overload,
     * while collection literal `[1, 2, 3]` probably resolves to this one (depending on the approach we actually undertake --
     * we might want to pick the overload with `numberOfParameters == numberOfProvidedArguments` right away).
     */
    @NonNull
    static <T> CollectionOfNotNulls<@NonNull T> of(@Nullable T a, @Nullable T b, @Nullable T c) {
        return new Impl<T>();
    }

    // FAIL or OK?
    @NonNull
    static <T> CollectionOfNotNulls<T> of(@NonNull T a, @NonNull T b, @NonNull T c, @NonNull T d) {
        return new Impl<T>();
    }

    // FAIL or OK?
    @NonNull
    static <T> CollectionOfNotNulls<@Nullable T> of(@NonNull T a, @NonNull T b, @NonNull T c, @NonNull T d, @NonNull T e) {
        return new Impl<T>();
    }

    // OK
    @NonNull
    static <T> CollectionOfNotNulls<@NonNull T> of(@NonNull T a, @NonNull T b, @NonNull T c, @NonNull T d, @NonNull T e, @NonNull T f) {
        return new Impl<T>();
    }
}

// FILE: test.kt

fun <K> acceptJavaCollection(k: JavaCollection<K>) { }
fun <K> acceptJavaCollectionNullable(k: JavaCollection<K>?) { }
fun <K> acceptJavaCollectionWithDNN(k: JavaCollection<K & Any>) { }
fun <K> acceptJavaCollectionWithNullable(k: JavaCollection<K?>) { }
fun <K> acceptCollectionOfNotNulls(k: CollectionOfNotNulls<K>) { }

fun test() {
    acceptJavaCollection<String>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>) // with 0
    acceptJavaCollection<String>(<!ARGUMENT_TYPE_MISMATCH!>["42"]<!>) // vararg
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptJavaCollection<!>(<!ARGUMENT_TYPE_MISMATCH!>[42, 42]<!>) // vararg
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptJavaCollection<!>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3]<!>) // unclear
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptJavaCollectionNullable<!>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3, 4]<!>) // unclear
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptJavaCollection<!>(<!ARGUMENT_TYPE_MISMATCH!>[null, 2, 3, 4, 5]<!>) // unclear
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptJavaCollection<!>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3, 4, 5, 6]<!>) // unclear
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptJavaCollectionWithDNN<!>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3, 4, 5, 6, 7]<!>) // with 7 args
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptJavaCollection<!>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3, 4, 5, 6, 7]<!>) // with 7 args
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptJavaCollection<!>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3, 4, 5, 6, 7, 8]<!>) // with 8 args
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptJavaCollectionWithNullable<!>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3, 4, 5, 6, 7, 8]<!>) // with 8 args

    <!CANNOT_INFER_PARAMETER_TYPE!>acceptCollectionOfNotNulls<!>(<!ARGUMENT_TYPE_MISMATCH, CANNOT_INFER_PARAMETER_TYPE!>[]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptCollectionOfNotNulls<!>(<!ARGUMENT_TYPE_MISMATCH!>[1]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptCollectionOfNotNulls<!>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptCollectionOfNotNulls<!>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptCollectionOfNotNulls<!>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3, 4]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptCollectionOfNotNulls<!>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3, 4, 5]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptCollectionOfNotNulls<!>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3, 4, 5, 6]<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>acceptCollectionOfNotNulls<!>(<!ARGUMENT_TYPE_MISMATCH!>[1, 2, 3, 4, 5, 6, 7]<!>)
}

/* GENERATED_FIR_TAGS: collectionLiteral, dnnType, functionDeclaration, integerLiteral, javaType, nullableType,
stringLiteral, typeParameter */

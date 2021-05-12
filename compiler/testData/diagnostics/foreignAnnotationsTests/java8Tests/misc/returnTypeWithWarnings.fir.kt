// !LANGUAGE: +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT
// MUTE_FOR_PSI_CLASS_FILES_READING

// FILE: ReturnTypeWithWarnings.java
// We've already had errors in source mode, so it's relevant only for binaries for now
// INCLUDE_JAVA_AS_BINARY

import org.jetbrains.annotations.*;

public class ReturnTypeWithWarnings<T> {
    public interface A<T1, T2> {}

    public A<@Nullable String, @Nullable T> foo1() { return null; }
    public A<@Nullable String, @NotNull T> foo2() { return null; }
    public A<@NotNull String, @NotNull T> foo3 = null;
    public @NotNull T [] foo4 = null;
    public ReturnTypeWithWarnings<@Nullable String> foo41 = null;
    public T foo411 = null;
    public @Nullable String [] foo5() { return null; }
}

// FILE: main.kt
fun <K> takeNotNullStringAndKNullable(x: ReturnTypeWithWarnings.A<String, K?>) {}
fun <K> takeNullableStringAndKNullable(x: ReturnTypeWithWarnings.A<String?, K?>) {}
fun <K: Any> takeNotNullStringAndNotNullK(x: ReturnTypeWithWarnings.A<String, K>) {}
fun <K: Any> takeNullableStringAndNotNullK(x: ReturnTypeWithWarnings.A<String?, K>) {}
fun takeNotNullString(x: String) {}

fun takeArrayOfNotNullString(x: Array<String>) {}
fun takeArrayOfNullableString(x: Array<String?>) {}
fun <K: Any> takeArrayOfNotNullK(x: Array<K>) {}
fun <K> takeArrayOfNullableK(x: Array<K?>) {}

fun <R> main(a: ReturnTypeWithWarnings<R>) {
    val x1 = a.foo1()
    takeNotNullStringAndKNullable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>x1<!>)
    takeNullableStringAndKNullable(x1)
    takeNotNullStringAndNotNullK(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS, TYPE_MISMATCH, TYPE_MISMATCH!>x1<!>)
    takeNullableStringAndNotNullK(<!TYPE_MISMATCH, TYPE_MISMATCH!>x1<!>)
    takeNotNullString(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo41.foo411<!>)

    val x2 = a.foo2()
    takeNotNullStringAndKNullable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>x2<!>)
    takeNullableStringAndKNullable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>x2<!>)
    takeNotNullStringAndNotNullK(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS, TYPE_MISMATCH, TYPE_MISMATCH!>x2<!>)
    takeNullableStringAndNotNullK(<!TYPE_MISMATCH, TYPE_MISMATCH!>x2<!>)

    val x3 = a.foo3
    takeNotNullStringAndKNullable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>x3<!>)
    takeNullableStringAndKNullable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>x3<!>)
    takeNotNullStringAndNotNullK(<!TYPE_MISMATCH, TYPE_MISMATCH!>x3<!>)
    takeNullableStringAndNotNullK(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS, TYPE_MISMATCH, TYPE_MISMATCH!>x3<!>)

    val x4 = a.foo4
    takeArrayOfNotNullString(<!TYPE_MISMATCH!>x4<!>)
    takeArrayOfNullableString(<!TYPE_MISMATCH!>x4<!>)
    takeArrayOfNotNullK(<!TYPE_MISMATCH, TYPE_MISMATCH!>x4<!>)
    takeArrayOfNullableK(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>x4<!>)

    val x5 = a.foo5()
    takeArrayOfNotNullString(<!TYPE_MISMATCH!>x5<!>)
    takeArrayOfNullableString(<!TYPE_MISMATCH!>x5<!>)
    takeArrayOfNotNullK(<!TYPE_MISMATCH!>x5<!>)
    takeArrayOfNullableK(<!TYPE_MISMATCH!>x5<!>)
}

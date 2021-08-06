// !LANGUAGE: +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated, -TypeEnhancementImprovementsInStrictMode
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
    val x1 = <!DEBUG_INFO_EXPRESSION_TYPE("ReturnTypeWithWarnings.A<kotlin.String?, R..R?!>..ReturnTypeWithWarnings.A<kotlin.String?, R..R?!>?!")!>a.foo1()<!>
    takeNotNullStringAndKNullable(<!ARGUMENT_TYPE_MISMATCH!>x1<!>)
    takeNullableStringAndKNullable(x1)
    takeNotNullStringAndNotNullK(<!ARGUMENT_TYPE_MISMATCH!>x1<!>)
    takeNullableStringAndNotNullK(<!ARGUMENT_TYPE_MISMATCH!>x1<!>)
    takeNotNullString(a.foo41.foo411)

    val x2 = <!DEBUG_INFO_EXPRESSION_TYPE("ReturnTypeWithWarnings.A<kotlin.String, R..R?!>..ReturnTypeWithWarnings.A<kotlin.String, R..R?!>?!")!>a.foo2()<!>
    takeNotNullStringAndKNullable(x2)
    takeNullableStringAndKNullable(<!ARGUMENT_TYPE_MISMATCH!>x2<!>)
    takeNotNullStringAndNotNullK(<!ARGUMENT_TYPE_MISMATCH!>x2<!>)
    takeNullableStringAndNotNullK(<!ARGUMENT_TYPE_MISMATCH!>x2<!>)

    val x3 = <!DEBUG_INFO_EXPRESSION_TYPE("ReturnTypeWithWarnings.A<kotlin.String, R..R?!>..ReturnTypeWithWarnings.A<kotlin.String, R..R?!>?!")!>a.foo3<!>
    takeNotNullStringAndKNullable(x3)
    takeNullableStringAndKNullable(<!ARGUMENT_TYPE_MISMATCH!>x3<!>)
    takeNotNullStringAndNotNullK(<!ARGUMENT_TYPE_MISMATCH!>x3<!>)
    takeNullableStringAndNotNullK(<!ARGUMENT_TYPE_MISMATCH!>x3<!>)

    val x4 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<R..R?!>..kotlin.Array<out R..R?!>")!>a.foo4<!>
    takeArrayOfNotNullString(<!ARGUMENT_TYPE_MISMATCH!>x4<!>)
    takeArrayOfNullableString(<!ARGUMENT_TYPE_MISMATCH!>x4<!>)
    takeArrayOfNotNullK(<!ARGUMENT_TYPE_MISMATCH!>x4<!>)
    takeArrayOfNullableK(x4)

    val x5 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.String..kotlin.String?!>?..kotlin.Array<out kotlin.String..kotlin.String?!>??")!>a.foo5()<!>
    takeArrayOfNotNullString(<!ARGUMENT_TYPE_MISMATCH!>x5<!>)
    takeArrayOfNullableString(<!ARGUMENT_TYPE_MISMATCH!>x5<!>)
    takeArrayOfNotNullK(<!ARGUMENT_TYPE_MISMATCH!>x5<!>)
    takeArrayOfNullableK(<!ARGUMENT_TYPE_MISMATCH!>x5<!>)
}

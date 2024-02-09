// !LANGUAGE: +TypeEnhancementImprovementsInStrictMode +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT
// MUTE_FOR_PSI_CLASS_FILES_READING

// FILE: ReturnType.java

import org.jetbrains.annotations.*;

public class ReturnType<T> {
    public interface A<T1, T2> {}

    public A<@Nullable String, @Nullable T> foo1() { return null; }
    public A<@Nullable String, @NotNull T> foo2() { return null; }
    public A<@NotNull String, @NotNull T> foo3 = null;
    public @NotNull T [] foo4 = null;
    public ReturnType<@Nullable String> foo41 = null;
    public T foo411 = null;
    public @Nullable String [] foo5() { return null; }
}

// FILE: main.kt
fun <K> takeNotNullStringAndKNullable(x: ReturnType.A<String, K?>) {}
fun <K> takeNullableStringAndKNullable(x: ReturnType.A<String?, K?>) {}
fun <K: Any> takeNotNullStringAndNotNullK(x: ReturnType.A<String, K>) {}
fun <K: Any> takeNullableStringAndNotNullK(x: ReturnType.A<String?, K>) {}
fun takeNotNullString(x: String) {}

fun takeArrayOfNotNullString(x: Array<String>) {}
fun takeArrayOfNullableString(x: Array<String?>) {}
fun <K: Any> takeArrayOfNotNullK(x: Array<K>) {}
fun <K> takeArrayOfNullableK(x: Array<K?>) {}

fun <R> main(a: ReturnType<R>) {
    val x1 = <!DEBUG_INFO_EXPRESSION_TYPE("ReturnType.A<kotlin.String?, R?>..ReturnType.A<kotlin.String?, R?>?!")!>a.foo1()<!>
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNotNullStringAndKNullable<!>(<!ARGUMENT_TYPE_MISMATCH!>x1<!>)
    takeNullableStringAndKNullable(x1)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNotNullStringAndNotNullK<!>(<!ARGUMENT_TYPE_MISMATCH!>x1<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNullableStringAndNotNullK<!>(<!ARGUMENT_TYPE_MISMATCH!>x1<!>)
    takeNotNullString(<!ARGUMENT_TYPE_MISMATCH!>a.foo41.foo411<!>)

    val x2 = <!DEBUG_INFO_EXPRESSION_TYPE("ReturnType.A<kotlin.String?, R!!>..ReturnType.A<kotlin.String?, R!!>?!")!>a.foo2()<!>
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNotNullStringAndKNullable<!>(<!ARGUMENT_TYPE_MISMATCH!>x2<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNullableStringAndKNullable<!>(<!ARGUMENT_TYPE_MISMATCH!>x2<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNotNullStringAndNotNullK<!>(<!ARGUMENT_TYPE_MISMATCH!>x2<!>)
    takeNullableStringAndNotNullK(x2)

    val x3 = <!DEBUG_INFO_EXPRESSION_TYPE("ReturnType.A<kotlin.String, R!!>..ReturnType.A<kotlin.String, R!!>?!")!>a.foo3<!>
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNotNullStringAndKNullable<!>(<!ARGUMENT_TYPE_MISMATCH!>x3<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNullableStringAndKNullable<!>(<!ARGUMENT_TYPE_MISMATCH!>x3<!>)
    takeNotNullStringAndNotNullK(x3)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNullableStringAndNotNullK<!>(<!ARGUMENT_TYPE_MISMATCH!>x3<!>)

    val x4 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<R!!>..kotlin.Array<out R!!>?!")!>a.foo4<!>
    takeArrayOfNotNullString(<!ARGUMENT_TYPE_MISMATCH!>x4<!>)
    takeArrayOfNullableString(<!ARGUMENT_TYPE_MISMATCH!>x4<!>)
    takeArrayOfNotNullK(x4)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeArrayOfNullableK<!>(<!ARGUMENT_TYPE_MISMATCH!>x4<!>)

    val x5 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.String?>..kotlin.Array<out kotlin.String?>?!")!>a.foo5()<!>
    takeArrayOfNotNullString(<!ARGUMENT_TYPE_MISMATCH!>x5<!>)
    takeArrayOfNullableString(x5)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeArrayOfNotNullK<!>(<!ARGUMENT_TYPE_MISMATCH!>x5<!>)
    takeArrayOfNullableK(x5)
}

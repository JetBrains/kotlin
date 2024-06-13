// DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT
// MUTE_FOR_PSI_CLASS_FILES_READING
// ^ KT-68389

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

fun <R> main(rt: ReturnType<R>) {
    val x1 = <!DEBUG_INFO_EXPRESSION_TYPE("ReturnType.A<kotlin.String?, R?>..ReturnType.A<kotlin.String?, R?>?!")!>rt.foo1()<!>
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNotNullStringAndKNullable<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String, K?>; ReturnType.A<@Nullable() kotlin.String?, @Nullable() R?>!")!>x1<!>)
    takeNullableStringAndKNullable(x1)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNotNullStringAndNotNullK<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String, K>; ReturnType.A<@Nullable() kotlin.String?, @Nullable() R?>!")!>x1<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNullableStringAndNotNullK<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String?, K>; ReturnType.A<@Nullable() kotlin.String?, @Nullable() R?>!")!>x1<!>)
    takeNotNullString(<!ARGUMENT_TYPE_MISMATCH("kotlin.String; @Nullable() kotlin.String?")!>rt.foo41.foo411<!>)

    val x2 = <!DEBUG_INFO_EXPRESSION_TYPE("ReturnType.A<kotlin.String?, R!!>..ReturnType.A<kotlin.String?, R!!>?!")!>rt.foo2()<!>
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNotNullStringAndKNullable<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String, K?>; ReturnType.A<@Nullable() kotlin.String?, @NotNull() R & Any>!")!>x2<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNullableStringAndKNullable<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String?, K?>; ReturnType.A<@Nullable() kotlin.String?, @NotNull() R & Any>!")!>x2<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNotNullStringAndNotNullK<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String, K>; ReturnType.A<@Nullable() kotlin.String?, @NotNull() R & Any>!")!>x2<!>)
    takeNullableStringAndNotNullK(x2)

    val x3 = <!DEBUG_INFO_EXPRESSION_TYPE("ReturnType.A<kotlin.String, R!!>..ReturnType.A<kotlin.String, R!!>?!")!>rt.foo3<!>
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNotNullStringAndKNullable<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String, K?>; ReturnType.A<@NotNull() kotlin.String, @NotNull() R & Any>!")!>x3<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNullableStringAndKNullable<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String?, K?>; ReturnType.A<@NotNull() kotlin.String, @NotNull() R & Any>!")!>x3<!>)
    takeNotNullStringAndNotNullK(x3)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNullableStringAndNotNullK<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String?, K>; ReturnType.A<@NotNull() kotlin.String, @NotNull() R & Any>!")!>x3<!>)

    val x4 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<R!!>..kotlin.Array<out R!!>?!")!>rt.foo4<!>
    takeArrayOfNotNullString(<!ARGUMENT_TYPE_MISMATCH("kotlin.Array<kotlin.String>; kotlin.Array<(out) @NotNull() R & Any>!")!>x4<!>)
    takeArrayOfNullableString(<!ARGUMENT_TYPE_MISMATCH("kotlin.Array<kotlin.String?>; kotlin.Array<(out) @NotNull() R & Any>!")!>x4<!>)
    takeArrayOfNotNullK(x4)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeArrayOfNullableK<!>(<!ARGUMENT_TYPE_MISMATCH("kotlin.Array<K?>; kotlin.Array<(out) @NotNull() R & Any>!")!>x4<!>)

    val x5 = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Array<kotlin.String?>..kotlin.Array<out kotlin.String?>?!")!>rt.foo5()<!>
    takeArrayOfNotNullString(<!ARGUMENT_TYPE_MISMATCH("kotlin.Array<kotlin.String>; kotlin.Array<(out) @Nullable() kotlin.String?>!")!>x5<!>)
    takeArrayOfNullableString(x5)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeArrayOfNotNullK<!>(<!ARGUMENT_TYPE_MISMATCH("kotlin.Array<K>; kotlin.Array<(out) @Nullable() kotlin.String?>!")!>x5<!>)
    takeArrayOfNullableK(x5)
}

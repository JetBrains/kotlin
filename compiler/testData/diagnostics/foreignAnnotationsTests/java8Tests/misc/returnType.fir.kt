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
    val x1 = <!DEBUG_INFO_EXPRESSION_TYPE("(ReturnType.A<@Nullable() kotlin.String?, @Nullable() R?>..ReturnType.A<@Nullable() kotlin.String?, @Nullable() R?>?)")!>rt.foo1()<!>
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNotNullStringAndKNullable<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String, K? (of fun <K> takeNotNullStringAndKNullable)>; ReturnType.A<@Nullable() kotlin.String?, @Nullable() R? (of fun <R> main)>!")!>x1<!>)
    takeNullableStringAndKNullable(x1)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNotNullStringAndNotNullK<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String, K (of fun <K : Any> takeNotNullStringAndNotNullK)>; ReturnType.A<@Nullable() kotlin.String?, @Nullable() R? (of fun <R> main)>!")!>x1<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNullableStringAndNotNullK<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String?, K (of fun <K : Any> takeNullableStringAndNotNullK)>; ReturnType.A<@Nullable() kotlin.String?, @Nullable() R? (of fun <R> main)>!")!>x1<!>)
    takeNotNullString(<!ARGUMENT_TYPE_MISMATCH("kotlin.String; @Nullable() kotlin.String?")!>rt.foo41.foo411<!>)

    val x2 = <!DEBUG_INFO_EXPRESSION_TYPE("(ReturnType.A<@Nullable() kotlin.String?, @NotNull() R & Any>..ReturnType.A<@Nullable() kotlin.String?, @NotNull() R & Any>?)")!>rt.foo2()<!>
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNotNullStringAndKNullable<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String, K? (of fun <K> takeNotNullStringAndKNullable)>; ReturnType.A<@Nullable() kotlin.String?, @NotNull() R (of fun <R> main) & Any>!")!>x2<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNullableStringAndKNullable<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String?, K? (of fun <K> takeNullableStringAndKNullable)>; ReturnType.A<@Nullable() kotlin.String?, @NotNull() R (of fun <R> main) & Any>!")!>x2<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNotNullStringAndNotNullK<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String, K (of fun <K : Any> takeNotNullStringAndNotNullK)>; ReturnType.A<@Nullable() kotlin.String?, @NotNull() R (of fun <R> main) & Any>!")!>x2<!>)
    takeNullableStringAndNotNullK(x2)

    val x3 = <!DEBUG_INFO_EXPRESSION_TYPE("(ReturnType.A<@NotNull() kotlin.String, @NotNull() R & Any>..ReturnType.A<@NotNull() kotlin.String, @NotNull() R & Any>?)")!>rt.foo3<!>
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNotNullStringAndKNullable<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String, K? (of fun <K> takeNotNullStringAndKNullable)>; ReturnType.A<@NotNull() kotlin.String, @NotNull() R (of fun <R> main) & Any>!")!>x3<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNullableStringAndKNullable<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String?, K? (of fun <K> takeNullableStringAndKNullable)>; ReturnType.A<@NotNull() kotlin.String, @NotNull() R (of fun <R> main) & Any>!")!>x3<!>)
    takeNotNullStringAndNotNullK(x3)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeNullableStringAndNotNullK<!>(<!ARGUMENT_TYPE_MISMATCH("ReturnType.A<kotlin.String?, K (of fun <K : Any> takeNullableStringAndNotNullK)>; ReturnType.A<@NotNull() kotlin.String, @NotNull() R (of fun <R> main) & Any>!")!>x3<!>)

    val x4 = <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Array<@NotNull() R & Any>..kotlin.Array<out @NotNull() R & Any>?)")!>rt.foo4<!>
    takeArrayOfNotNullString(<!ARGUMENT_TYPE_MISMATCH("kotlin.Array<kotlin.String>; kotlin.Array<(out) @NotNull() R (of fun <R> main) & Any>!")!>x4<!>)
    takeArrayOfNullableString(<!ARGUMENT_TYPE_MISMATCH("kotlin.Array<kotlin.String?>; kotlin.Array<(out) @NotNull() R (of fun <R> main) & Any>!")!>x4<!>)
    takeArrayOfNotNullK(x4)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeArrayOfNullableK<!>(<!ARGUMENT_TYPE_MISMATCH("kotlin.Array<K? (of fun <K> takeArrayOfNullableK)>; kotlin.Array<(out) @NotNull() R (of fun <R> main) & Any>!")!>x4<!>)

    val x5 = <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Array<@Nullable() kotlin.String?>..kotlin.Array<out @Nullable() kotlin.String?>?)")!>rt.foo5()<!>
    takeArrayOfNotNullString(<!ARGUMENT_TYPE_MISMATCH("kotlin.Array<kotlin.String>; kotlin.Array<(out) @Nullable() kotlin.String?>!")!>x5<!>)
    takeArrayOfNullableString(x5)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeArrayOfNotNullK<!>(<!ARGUMENT_TYPE_MISMATCH("kotlin.Array<K (of fun <K : Any> takeArrayOfNotNullK)>; kotlin.Array<(out) @Nullable() kotlin.String?>!")!>x5<!>)
    takeArrayOfNullableK(x5)
}

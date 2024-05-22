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
    val x1 = <!DEBUG_INFO_EXPRESSION_TYPE("(ReturnType.A<@org.jetbrains.annotations.Nullable kotlin.String?, @org.jetbrains.annotations.Nullable R?>..ReturnType.A<@org.jetbrains.annotations.Nullable kotlin.String?, @org.jetbrains.annotations.Nullable R?>?)")!>rt.foo1()<!>
    takeNotNullStringAndKNullable(<!TYPE_MISMATCH("ReturnType.A<String, TypeVariable(K)?>; ReturnType.A<String?, R?>!")!>x1<!>)
    takeNullableStringAndKNullable(x1)
    takeNotNullStringAndNotNullK(<!TYPE_MISMATCH("ReturnType.A<String, TypeVariable(K)>; ReturnType.A<String?, R?>!")!>x1<!>)
    takeNullableStringAndNotNullK(<!TYPE_MISMATCH("Any; R?")!>x1<!>)
    takeNotNullString(<!TYPE_MISMATCH("String; String?")!>rt.foo41.foo411<!>)

    val x2 = <!DEBUG_INFO_EXPRESSION_TYPE("(ReturnType.A<@org.jetbrains.annotations.Nullable kotlin.String?, @org.jetbrains.annotations.NotNull R & Any>..ReturnType.A<@org.jetbrains.annotations.Nullable kotlin.String?, @org.jetbrains.annotations.NotNull R & Any>?)")!>rt.foo2()<!>
    takeNotNullStringAndKNullable(<!TYPE_MISMATCH("ReturnType.A<String, TypeVariable(K)?>; ReturnType.A<String?, R & Any>!")!>x2<!>)
    takeNullableStringAndKNullable(<!TYPE_MISMATCH("ReturnType.A<String?, TypeVariable(K)?>; ReturnType.A<String?, R & Any>!")!>x2<!>)
    takeNotNullStringAndNotNullK(<!TYPE_MISMATCH("ReturnType.A<String, TypeVariable(K)>; ReturnType.A<String?, R & Any>!")!>x2<!>)
    takeNullableStringAndNotNullK(x2)

    val x3 = <!DEBUG_INFO_EXPRESSION_TYPE("(ReturnType.A<@org.jetbrains.annotations.NotNull kotlin.String, @org.jetbrains.annotations.NotNull R & Any>..ReturnType.A<@org.jetbrains.annotations.NotNull kotlin.String, @org.jetbrains.annotations.NotNull R & Any>?)")!>rt.foo3<!>
    takeNotNullStringAndKNullable(<!TYPE_MISMATCH("ReturnType.A<String, TypeVariable(K)?>; ReturnType.A<String, R & Any>!")!>x3<!>)
    takeNullableStringAndKNullable(<!TYPE_MISMATCH("ReturnType.A<String?, TypeVariable(K)?>; ReturnType.A<String, R & Any>!")!>x3<!>)
    takeNotNullStringAndNotNullK(x3)
    takeNullableStringAndNotNullK(<!TYPE_MISMATCH("ReturnType.A<String?, TypeVariable(K)>; ReturnType.A<String, R & Any>!")!>x3<!>)

    val x4 = <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Array<@org.jetbrains.annotations.NotNull R & Any>..kotlin.Array<out @org.jetbrains.annotations.NotNull R & Any>?)")!>rt.foo4<!>
    takeArrayOfNotNullString(<!TYPE_MISMATCH("Array<String>; Array<(out) R & Any>!")!>x4<!>)
    takeArrayOfNullableString(<!TYPE_MISMATCH("Array<String?>; Array<(out) R & Any>!")!>x4<!>)
    takeArrayOfNotNullK(x4)
    takeArrayOfNullableK(<!TYPE_MISMATCH("Array<TypeVariable(K)?>; Array<(out) R & Any>!")!>x4<!>)

    val x5 = <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.Array<@org.jetbrains.annotations.Nullable kotlin.String?>..kotlin.Array<out @org.jetbrains.annotations.Nullable kotlin.String?>?)")!>rt.foo5()<!>
    takeArrayOfNotNullString(<!TYPE_MISMATCH("Array<String>; Array<(out) String?>!")!>x5<!>)
    takeArrayOfNullableString(x5)
    takeArrayOfNotNullK(<!TYPE_MISMATCH("Any; String?")!>x5<!>)
    takeArrayOfNullableK(x5)
}

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
    val x1 = a.foo1()
    takeNotNullStringAndKNullable(<!TYPE_MISMATCH!>x1<!>)
    takeNullableStringAndKNullable(x1)
    takeNotNullStringAndNotNullK(<!TYPE_MISMATCH!>x1<!>)
    takeNullableStringAndNotNullK(<!TYPE_MISMATCH, TYPE_MISMATCH!>x1<!>)
    takeNotNullString(<!TYPE_MISMATCH!>a.foo41.foo411<!>)

    val x2 = a.foo2()
    takeNotNullStringAndKNullable(<!TYPE_MISMATCH!>x2<!>)
    takeNullableStringAndKNullable(<!TYPE_MISMATCH!>x2<!>)
    takeNotNullStringAndNotNullK(<!TYPE_MISMATCH!>x2<!>)
    takeNullableStringAndNotNullK(x2)

    val x3 = a.foo3
    takeNotNullStringAndKNullable(<!TYPE_MISMATCH!>x3<!>)
    takeNullableStringAndKNullable(<!TYPE_MISMATCH!>x3<!>)
    takeNotNullStringAndNotNullK(x3)
    takeNullableStringAndNotNullK(<!TYPE_MISMATCH!>x3<!>)

    val x4 = a.foo4
    takeArrayOfNotNullString(<!TYPE_MISMATCH!>x4<!>)
    takeArrayOfNullableString(<!TYPE_MISMATCH!>x4<!>)
    takeArrayOfNotNullK(x4)
    takeArrayOfNullableK(<!TYPE_MISMATCH!>x4<!>)

    val x5 = a.foo5()
    takeArrayOfNotNullString(<!TYPE_MISMATCH!>x5<!>)
    takeArrayOfNullableString(x5)
    takeArrayOfNotNullK(<!TYPE_MISMATCH, TYPE_MISMATCH!>x5<!>)
    takeArrayOfNullableK(x5)
}

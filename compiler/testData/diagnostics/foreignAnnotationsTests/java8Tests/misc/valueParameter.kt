// !LANGUAGE: +TypeEnhancementImprovementsInStrictMode +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
// !DIAGNOSTICS: -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS
// SKIP_TXT
// MUTE_FOR_PSI_CLASS_FILES_READING

// FILE: ValueParameter.java

import org.jetbrains.annotations.*;

public class ValueParameter<T> {
    public interface A<T1, T2> {}

    public void foo1(A<@Nullable String, @Nullable T> x) { }
    public void foo2(A<@Nullable String, @NotNull T> x) { }
    public void foo3(A<@NotNull String, @NotNull T> x) { }
    public void foo4(@NotNull T [] x) { }
    public void foo41(@Nullable String x) { }
    public void foo411(T x) { }
    public void foo5(@Nullable String [] x) { }
}

// FILE: main.kt
fun <K> getNotNullStringAndKNullable() = null as ValueParameter.A<String, K?>
fun <K> getNullableStringAndKNullable() = null as ValueParameter.A<String?, K?>
fun <K: Any> getNotNullStringAndNotNullK() = null as ValueParameter.A<String, K>
fun <K: Any> getNullableStringAndNotNullK() = null as ValueParameter.A<String?, K>
fun getNotNullString() = null as String

fun getArrayOfNotNullString() = null as Array<String>
fun getArrayOfNullableString() = null as Array<String?>
fun <K: Any> getArrayOfNotNullK() = null as Array<K>
fun <K> getArrayOfNullableK() = null as Array<K?>

fun <R> main(a: ValueParameter<R>) {
    a.foo1(<!TYPE_MISMATCH!>getNotNullStringAndKNullable()<!>)
    a.foo1(getNullableStringAndKNullable())
    a.foo1(<!TYPE_MISMATCH!>getNotNullStringAndNotNullK()<!>)
    a.foo1(<!TYPE_MISMATCH!>getNullableStringAndNotNullK()<!>)

    a.foo2(<!TYPE_MISMATCH!>getNotNullStringAndKNullable()<!>)
    a.foo2(<!TYPE_MISMATCH!>getNullableStringAndKNullable()<!>)
    a.foo2(<!TYPE_MISMATCH!>getNotNullStringAndNotNullK()<!>)
    a.foo2(getNullableStringAndNotNullK())

    a.foo3(<!TYPE_MISMATCH!>getNotNullStringAndKNullable()<!>)
    a.foo3(<!TYPE_MISMATCH!>getNullableStringAndKNullable()<!>)
    a.foo3(getNotNullStringAndNotNullK())
    a.foo3(<!TYPE_MISMATCH!>getNullableStringAndNotNullK()<!>)

    a.foo4(<!TYPE_MISMATCH!>getArrayOfNotNullString()<!>)
    a.foo4(<!TYPE_MISMATCH!>getArrayOfNullableString()<!>)
    a.foo4(getArrayOfNotNullK())
    a.foo4(<!TYPE_MISMATCH!>getArrayOfNullableK()<!>)

    a.foo5(getArrayOfNotNullString())
    a.foo5(getArrayOfNullableString())
    a.foo5(getArrayOfNotNullK())
    a.foo5(getArrayOfNullableK())

    a.foo41(getNotNullString())
    a.foo411(<!TYPE_MISMATCH!>getNotNullString()<!>)
}

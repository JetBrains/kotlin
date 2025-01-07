// DIAGNOSTICS: -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS
// SKIP_TXT
// MUTE_FOR_PSI_CLASS_FILES_READING
// ^ KT-68389

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

fun <R> main(vp: ValueParameter<R>) {
    vp.foo1(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<String, K? (of fun <K> getNotNullStringAndKNullable)>; ValueParameter.A<@Nullable() String?, @Nullable() R? (of fun <R> main)>!")!><!CANNOT_INFER_PARAMETER_TYPE!>getNotNullStringAndKNullable<!>()<!>)
    vp.foo1(getNullableStringAndKNullable())
    vp.foo1(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<String, K (of fun <K : Any> getNotNullStringAndNotNullK)>; ValueParameter.A<@Nullable() String?, @Nullable() R? (of fun <R> main)>!")!><!CANNOT_INFER_PARAMETER_TYPE!>getNotNullStringAndNotNullK<!>()<!>)
    vp.foo1(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<String?, K (of fun <K : Any> getNullableStringAndNotNullK)>; ValueParameter.A<@Nullable() String?, @Nullable() R? (of fun <R> main)>!")!><!CANNOT_INFER_PARAMETER_TYPE!>getNullableStringAndNotNullK<!>()<!>)

    vp.foo2(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<String, K? (of fun <K> getNotNullStringAndKNullable)>; ValueParameter.A<@Nullable() String?, @NotNull() R (of fun <R> main) & Any>!")!><!CANNOT_INFER_PARAMETER_TYPE!>getNotNullStringAndKNullable<!>()<!>)
    vp.foo2(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<String?, K? (of fun <K> getNullableStringAndKNullable)>; ValueParameter.A<@Nullable() String?, @NotNull() R (of fun <R> main) & Any>!")!><!CANNOT_INFER_PARAMETER_TYPE!>getNullableStringAndKNullable<!>()<!>)
    vp.foo2(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<String, K (of fun <K : Any> getNotNullStringAndNotNullK)>; ValueParameter.A<@Nullable() String?, @NotNull() R (of fun <R> main) & Any>!")!><!CANNOT_INFER_PARAMETER_TYPE!>getNotNullStringAndNotNullK<!>()<!>)
    vp.foo2(getNullableStringAndNotNullK())

    vp.foo3(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<String, K? (of fun <K> getNotNullStringAndKNullable)>; ValueParameter.A<@NotNull() String, @NotNull() R (of fun <R> main) & Any>!")!><!CANNOT_INFER_PARAMETER_TYPE!>getNotNullStringAndKNullable<!>()<!>)
    vp.foo3(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<String?, K? (of fun <K> getNullableStringAndKNullable)>; ValueParameter.A<@NotNull() String, @NotNull() R (of fun <R> main) & Any>!")!><!CANNOT_INFER_PARAMETER_TYPE!>getNullableStringAndKNullable<!>()<!>)
    vp.foo3(getNotNullStringAndNotNullK())
    vp.foo3(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<String?, K (of fun <K : Any> getNullableStringAndNotNullK)>; ValueParameter.A<@NotNull() String, @NotNull() R (of fun <R> main) & Any>!")!><!CANNOT_INFER_PARAMETER_TYPE!>getNullableStringAndNotNullK<!>()<!>)

    vp.foo4(<!ARGUMENT_TYPE_MISMATCH("Array<String>; Array<(out) @NotNull() R (of fun <R> main) & Any>!")!>getArrayOfNotNullString()<!>)
    vp.foo4(<!ARGUMENT_TYPE_MISMATCH("Array<String?>; Array<(out) @NotNull() R (of fun <R> main) & Any>!")!>getArrayOfNullableString()<!>)
    vp.foo4(getArrayOfNotNullK())
    vp.foo4(<!ARGUMENT_TYPE_MISMATCH("Array<K? (of fun <K> getArrayOfNullableK)>; Array<(out) @NotNull() R (of fun <R> main) & Any>!")!><!CANNOT_INFER_PARAMETER_TYPE!>getArrayOfNullableK<!>()<!>)

    vp.foo5(getArrayOfNotNullString())
    vp.foo5(getArrayOfNullableString())
    vp.foo5(getArrayOfNotNullK())
    vp.foo5(getArrayOfNullableK())

    vp.foo41(getNotNullString())
    vp.foo411(<!ARGUMENT_TYPE_MISMATCH("String; R! (of fun <R> main)")!>getNotNullString()<!>)
}

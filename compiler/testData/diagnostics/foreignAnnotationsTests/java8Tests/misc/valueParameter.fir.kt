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
    vp.foo1(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<@Nullable() kotlin.String?, @Nullable() R?>!; ValueParameter.A<kotlin.String, K?>")!><!CANNOT_INFER_PARAMETER_TYPE!>getNotNullStringAndKNullable<!>()<!>)
    vp.foo1(getNullableStringAndKNullable())
    vp.foo1(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<@Nullable() kotlin.String?, @Nullable() R?>!; ValueParameter.A<kotlin.String, K>")!><!CANNOT_INFER_PARAMETER_TYPE!>getNotNullStringAndNotNullK<!>()<!>)
    vp.foo1(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<@Nullable() kotlin.String?, @Nullable() R?>!; ValueParameter.A<kotlin.String?, K>")!><!CANNOT_INFER_PARAMETER_TYPE!>getNullableStringAndNotNullK<!>()<!>)

    vp.foo2(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<@Nullable() kotlin.String?, @NotNull() R & Any>!; ValueParameter.A<kotlin.String, K?>")!><!CANNOT_INFER_PARAMETER_TYPE!>getNotNullStringAndKNullable<!>()<!>)
    vp.foo2(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<@Nullable() kotlin.String?, @NotNull() R & Any>!; ValueParameter.A<kotlin.String?, K?>")!><!CANNOT_INFER_PARAMETER_TYPE!>getNullableStringAndKNullable<!>()<!>)
    vp.foo2(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<@Nullable() kotlin.String?, @NotNull() R & Any>!; ValueParameter.A<kotlin.String, K>")!><!CANNOT_INFER_PARAMETER_TYPE!>getNotNullStringAndNotNullK<!>()<!>)
    vp.foo2(getNullableStringAndNotNullK())

    vp.foo3(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<@NotNull() kotlin.String, @NotNull() R & Any>!; ValueParameter.A<kotlin.String, K?>")!><!CANNOT_INFER_PARAMETER_TYPE!>getNotNullStringAndKNullable<!>()<!>)
    vp.foo3(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<@NotNull() kotlin.String, @NotNull() R & Any>!; ValueParameter.A<kotlin.String?, K?>")!><!CANNOT_INFER_PARAMETER_TYPE!>getNullableStringAndKNullable<!>()<!>)
    vp.foo3(getNotNullStringAndNotNullK())
    vp.foo3(<!ARGUMENT_TYPE_MISMATCH("ValueParameter.A<@NotNull() kotlin.String, @NotNull() R & Any>!; ValueParameter.A<kotlin.String?, K>")!><!CANNOT_INFER_PARAMETER_TYPE!>getNullableStringAndNotNullK<!>()<!>)

    vp.foo4(<!ARGUMENT_TYPE_MISMATCH("kotlin.Array<(out) @NotNull() R & Any>!; kotlin.Array<kotlin.String>")!>getArrayOfNotNullString()<!>)
    vp.foo4(<!ARGUMENT_TYPE_MISMATCH("kotlin.Array<(out) @NotNull() R & Any>!; kotlin.Array<kotlin.String?>")!>getArrayOfNullableString()<!>)
    vp.foo4(getArrayOfNotNullK())
    vp.foo4(<!ARGUMENT_TYPE_MISMATCH("kotlin.Array<(out) @NotNull() R & Any>!; kotlin.Array<K?>")!><!CANNOT_INFER_PARAMETER_TYPE!>getArrayOfNullableK<!>()<!>)

    vp.foo5(getArrayOfNotNullString())
    vp.foo5(getArrayOfNullableString())
    vp.foo5(getArrayOfNotNullK())
    vp.foo5(getArrayOfNullableK())

    vp.foo41(getNotNullString())
    vp.foo411(<!ARGUMENT_TYPE_MISMATCH("R!; kotlin.String")!>getNotNullString()<!>)
}

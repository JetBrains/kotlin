// !LANGUAGE: +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
// !DIAGNOSTICS: -UNUSED_PARAMETER -CAST_NEVER_SUCCEEDS
// SKIP_TXT
// MUTE_FOR_PSI_CLASS_FILES_READING

// FILE: ValueParameterWithWarnings.java
// We've already had errors in source mode, so it's relevant only for binaries for now
// INCLUDE_JAVA_AS_BINARY

import org.jetbrains.annotations.*;

public class ValueParameterWithWarnings<T> {
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
fun <K> getNotNullStringAndKNullable() = null as ValueParameterWithWarnings.A<String, K?>
fun <K> getNullableStringAndKNullable() = null as ValueParameterWithWarnings.A<String?, K?>
fun <K: Any> getNotNullStringAndNotNullK() = null as ValueParameterWithWarnings.A<String, K>
fun <K: Any> getNullableStringAndNotNullK() = null as ValueParameterWithWarnings.A<String?, K>
fun getNotNullString() = null as String

fun getArrayOfNotNullString() = null as Array<String>
fun getArrayOfNullableString() = null as Array<String?>
fun <K: Any> getArrayOfNotNullK() = null as Array<K>
fun <K> getArrayOfNullableK() = null as Array<K?>

fun <R> main(a: ValueParameterWithWarnings<R>) {
    a.foo1(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>getNotNullStringAndKNullable()<!>)
    a.foo1(getNullableStringAndKNullable())
    a.foo1(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS, TYPE_MISMATCH!>getNotNullStringAndNotNullK()<!>)
    a.foo1(<!TYPE_MISMATCH!>getNullableStringAndNotNullK()<!>)

    a.foo2(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>getNotNullStringAndKNullable()<!>)
    a.foo2(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>getNullableStringAndKNullable()<!>)
    a.foo2(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS, TYPE_MISMATCH!>getNotNullStringAndNotNullK()<!>)
    a.foo2(<!TYPE_MISMATCH!>getNullableStringAndNotNullK()<!>)

    a.foo3(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>getNotNullStringAndKNullable()<!>)
    a.foo3(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>getNullableStringAndKNullable()<!>)
    a.foo3(<!TYPE_MISMATCH!>getNotNullStringAndNotNullK()<!>)
    a.foo3(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS, TYPE_MISMATCH!>getNullableStringAndNotNullK()<!>)

    a.foo4(<!TYPE_MISMATCH!>getArrayOfNotNullString()<!>)
    a.foo4(<!TYPE_MISMATCH!>getArrayOfNullableString()<!>)
    a.foo4(getArrayOfNotNullK())
    a.foo4(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>getArrayOfNullableK()<!>)

    a.foo5(getArrayOfNotNullString())
    a.foo5(getArrayOfNullableString())
    a.foo5(getArrayOfNotNullK())
    a.foo5(getArrayOfNullableK())

    a.foo41(getNotNullString())
    a.foo411(<!TYPE_MISMATCH!>getNotNullString()<!>)
}

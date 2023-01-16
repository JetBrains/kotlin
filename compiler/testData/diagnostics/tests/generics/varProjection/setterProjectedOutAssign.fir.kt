// !DIAGNOSTICS: -UNREACHABLE_CODE -UNUSED_PARAMETER
// !CHECK_TYPE
// t is unused due to KT-4233

// FILE: test.kt

interface Tr<T> {
    var v: T
}

fun test(t: Tr<*>) {
    t.v = null!!
    t.v = <!ASSIGNMENT_TYPE_MISMATCH!>""<!>
    t.v = <!NULL_FOR_NONNULL_TYPE!>null<!>
    t.v checkType { _<Any?>() }
}

fun test2(t: JavaClass<*>) {
    t.v = null!!
    t.v = <!ASSIGNMENT_TYPE_MISMATCH!>""<!>
    t.v = null
    t.v checkType { _<Any?>() }
}

// FILE: JavaClass.java

public interface JavaClass<T> {
    public T getV();
    public void setV(T v);
}
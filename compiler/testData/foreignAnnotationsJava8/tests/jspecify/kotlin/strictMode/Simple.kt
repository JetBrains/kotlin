// JAVA_SOURCES: Simple.java

fun main(a: Simple, x: Derived): Unit {
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo(x, null)<!>.foo()
    // jspecify_nullness_mismatch
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>, x)<!>.foo()

    a.bar().foo()

    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.field<!>.foo()
}
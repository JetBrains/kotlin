// JAVA_SOURCES: NullnessUnspecifiedTypeParameter.java

fun main(a1: NullnessUnspecifiedTypeParameter<Any>, a2: NullnessUnspecifiedTypeParameter<Any?>, x: Test): Unit {
    a1.foo(null)
    a1.foo(1)

    a2.foo(null)
    a2.foo(1)

    // jspecify_nullness_mismatch
    a1.bar(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>, null)
    a1.bar(x, null)
    a1.bar(x, 1)

    // jspecify_nullness_mismatch
    a2.bar(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>, null)
    a2.bar(x, null)
    a2.bar(x, 1)
}
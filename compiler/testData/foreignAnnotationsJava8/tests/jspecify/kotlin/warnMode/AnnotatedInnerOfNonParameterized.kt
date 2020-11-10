// JAVA_SOURCES: AnnotatedInnerOfNonParameterized.java

fun main(o: AnnotatedInnerOfNonParameterized): Unit {
    // jspecify_nullness_mismatch
    o.takeNotNull(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>o.x4<!>)
    o.takeNotNull(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>o.x5<!>)
    // jspecify_nullness_mismatch
    o.takeNotNull(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>o.x6<!>)
    o.takeNotNull(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>o.x7<!>)
    o.takeNotNull(o.x8)
    // jspecify_nullness_mismatch
    o.takeNotNull(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>o.x9<!>)

    o.takeLibNotNull(<!TYPE_MISMATCH!>o.l1<!>)
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.l2<!>)
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.l3<!>)
}
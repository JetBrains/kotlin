// JAVA_SOURCES: AnnotatedInnerOfNonParameterized.java
// JSPECIFY_STATE strict

fun main(o: AnnotatedInnerOfNonParameterized): Unit {
    // jspecify_nullness_mismatch
    o.takeNotNull(<!TYPE_MISMATCH!>o.x4<!>)
    o.takeNotNull(<!TYPE_MISMATCH!>o.x5<!>)
    // jspecify_nullness_mismatch
    o.takeNotNull(<!TYPE_MISMATCH!>o.x6<!>)
    o.takeNotNull(<!TYPE_MISMATCH!>o.x7<!>)
    o.takeNotNull(o.x8)
    // jspecify_nullness_mismatch
    o.takeNotNull(<!TYPE_MISMATCH!>o.x9<!>)

    o.takeLibNotNull(<!TYPE_MISMATCH!>o.l1<!>)
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.l2<!>)
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.l3<!>)
}
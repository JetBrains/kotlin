// JAVA_SOURCES: AnnotatedWildcard.java
// JSPECIFY_STATE strict

fun main(o: AnnotatedWildcard): Unit {
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.x1<!>)
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.x2<!>)
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.x3<!>)
    // jspecify_nullness_mismatch
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.x4<!>)
    // jspecify_nullness_mismatch
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.x5<!>)
}
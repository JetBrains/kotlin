// JAVA_SOURCES: AnnotatedWildcardUnspec.java
// JSPECIFY_STATE strict

fun main(o: AnnotatedWildcardUnspec): Unit {
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.x1<!>)
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.x2<!>)
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.x3<!>)
    // jspecify_nullness_mismatch
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.x4<!>)
    // jspecify_nullness_mismatch
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.x5<!>)
}
// JAVA_SOURCES: AnnotatedWildcardUnspec.java

fun main(o: AnnotatedWildcardUnspec): Unit {
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.x1<!>)
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.x2<!>)
    o.takeLibNotNull(o.x3)
    // jspecify_nullness_mismatch
    o.takeLibNotNull(<!TYPE_MISMATCH!>o.x4<!>)
    // jspecify_nullness_mismatch
    o.takeLibNotNull(o.x5)
}
// JAVA_SOURCES: AnnotatedWildcardUnspec.java
// JSPECIFY_STATE strict

fun main(o: AnnotatedWildcardUnspec): Unit {
    o.takeLibExtendsNotNull(o.x1)
    o.takeLibExtendsNotNull(o.x2)
    // jspecify_nullness_mismatch
    o.takeLibSuperNullable(<!TYPE_MISMATCH!>o.x3<!>)
    // jspecify_nullness_mismatch
    o.takeLibExtendsNotNull(<!TYPE_MISMATCH!>o.x4<!>)
    o.takeLibSuperNullable(o.x5)
}
// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// SKIP_TXT
// JDK version is important, because we rely on @ValueBased annotation being present on LocalDate class
// JDK_KIND: FULL_JDK_21

typealias LocalDateAlias = java.time.LocalDate

fun test(p1: java.time.LocalDate, p2: LocalDateAlias) {
    synchronized(p1) { }
    synchronized(p2) { }
    <!FORBIDDEN_SYNCHRONIZED_BY_VALUE_CLASSES_OR_PRIMITIVES!>synchronized<!>(Integer.valueOf(1)) {}
}

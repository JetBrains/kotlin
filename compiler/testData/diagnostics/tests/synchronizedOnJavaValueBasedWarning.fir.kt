// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// SKIP_TXT
// JDK version is important, because we rely on @ValueBased annotation being present on LocalDate class
// JDK_KIND: FULL_JDK_21

typealias LocalDateAlias = java.time.LocalDate

fun test(p1: java.time.LocalDate, p2: LocalDateAlias) {
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>p1<!>) { }
    synchronized(<!SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS!>p2<!>) { }
    synchronized(Integer.valueOf(1)) {}
}

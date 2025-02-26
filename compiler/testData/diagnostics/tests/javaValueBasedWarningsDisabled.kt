// RUN_PIPELINE_TILL: BACKEND
// TARGET_BACKEND: JVM
// WITH_STDLIB
// SKIP_TXT
// FIR_IDENTICAL
// LANGUAGE: +DisableWarningsForValueBasedJavaClasses
// JDK version is important, because we rely on @ValueBased annotation being present on LocalDate class
// JDK_KIND: FULL_JDK_21

fun test(p1: java.time.LocalDate) {
    synchronized(p1) { }
}
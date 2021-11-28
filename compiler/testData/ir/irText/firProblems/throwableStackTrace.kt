// TARGET_BACKEND: JVM
// FIR_IDENTICAL
// WITH_STDLIB
// FULL_JDK

fun foo(t: Throwable) {
    t.stackTrace = t.stackTrace
}
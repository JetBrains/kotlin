// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

fun foo(t: Throwable) {
    t.stackTrace = t.stackTrace
}

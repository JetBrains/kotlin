// WITH_RUNTIME
// FULL_JDK

fun foo(t: Throwable) {
    t.stackTrace = t.stackTrace
}
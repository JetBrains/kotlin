// IGNORE_BACKEND: JS
// Check that local variables for inline functions and inline default lambdas start
// after they are initialized.

inline fun spray() {
    val a = Any()
    val b = Any()
    val c = Any()
    val d = Any()
    val e = Any()
}

inline fun f(block: () -> String = { "OK" }): String = block()

fun box(): String {
    // On the JVM, this call adds some locals with reference types to the LVT
    // which end after the call returns.
    spray()
    // When inlining `f` we'll reuse the same slots that previously contained
    // locals with reference types for the $i$f$f and $i$a$-f-... variables.
    // Since these locals have integer types D8 would produce a warning if they
    // started before being initialized, which would cause the test to fail.
    return f()
}

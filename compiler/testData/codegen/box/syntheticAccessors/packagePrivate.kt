// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// FILE: x.kt
package x

internal class C {
    // `foo$default` generated as package-private (not protected):
    private fun foo(result: String = "OK") = result
    // this needs an accessor:
    internal inline fun bar() = foo()
}

// FILE: y.kt
import x.*

fun box() = C().bar()

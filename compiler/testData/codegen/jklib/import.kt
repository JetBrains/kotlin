// IGNORE_BACKEND: JVM_IR
// FILE: klib.kt
package fromKlib

class C {
    val inClass = "O"
}

val toplevel get() = "K"

fun referByDescriptor(s: String) = s.length

inline fun foo() {
    println()
}

// FILE: test.kt
import fromKlib.C
import fromKlib.referByDescriptor
import fromKlib.toplevel
import fromKlib.foo

fun box(): String {
    referByDescriptor("")
    foo()
    return C().inClass + toplevel
}

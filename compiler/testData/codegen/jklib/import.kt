
// FILE: foo.kt
package foo

class C {
    val inClass = "O"
}

val toplevel get() = "K"

fun referByDescriptor(s: String) = s.length

inline fun foo() {
    println()
}

// FILE: test.kt
import foo.C
import foo.referByDescriptor
import foo.toplevel
import foo.foo

fun box(): String {
    referByDescriptor("")
    foo()
    return C().inClass + toplevel
}

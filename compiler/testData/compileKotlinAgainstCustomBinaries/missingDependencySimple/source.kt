package c

import b.B

fun bar(b: B) {
    // Implicit usage of (unavailable) a.A, return value is not used. It should still be an error as in Java
    b.foo()

    // Return value is used but the type is incorrect, also an error
    val x: String = b.foo()
}

// FILE: klib.kt
package fromKlib

class C {
    val x = "OK"
}
fun foo(): String {
    return C().x
}

// FILE: test.kt
import fromKlib.foo

fun box(): String {
    return foo()
}
// FILE: A.kt

package aaa

fun hello() = 17

// FILE: B.kt

fun box(): String {
    val h = aaa.hello()
    if (h != 17) {
        throw Exception()
    }
    return "OK"
}

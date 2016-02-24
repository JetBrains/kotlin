// FILE: A.kt

package aaa

fun hello() = 17

// FILE: B.kt

fun main(args: Array<String>) {
    val h = aaa.hello()
    if (h != 17) {
        throw Exception()
    }
}

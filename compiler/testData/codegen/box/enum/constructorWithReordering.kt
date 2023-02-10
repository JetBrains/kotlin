// WITH_STDLIB
// IGNORE_BACKEND: JS
fun f(): String = "O"
fun g(): String = "K"

enum class E(val x: String, val y: String) {
    A(y = g(), x = f())
}

fun box(): String = E.A.x + E.A.y

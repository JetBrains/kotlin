// IGNORE_BACKEND: JS

var result = "OK"

object A {
    val x = "O${foo()}"
    fun foo() = y
    const val y = "K"
}

fun box(): String {
    return A.x
}
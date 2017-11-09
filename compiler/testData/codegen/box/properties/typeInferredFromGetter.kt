val x get() = "O"

class A {
    val y get() = "K"
}

fun box() = x + A().y

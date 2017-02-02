object A {
    val a = "OK"
    val b = A.a
}

fun box() = A.b

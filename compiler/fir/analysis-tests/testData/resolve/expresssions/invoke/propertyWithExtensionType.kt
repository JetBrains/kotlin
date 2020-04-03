class A(val x: (String.() -> Unit)?, val y: (String.() -> Int))

fun test(a: A) {
    if (a.x != null) {
        val b = a.x
        "".b()
    }
    val c = a.y
    val d = "".c()
}
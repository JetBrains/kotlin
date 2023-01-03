fun foo() {
    val a: dynamic = Any()
    println(a in setOf(1, 2))
    println(1 in a)
    println(1 !in a)
    when (2) {
        in a -> println("ok")
    }
    when (3) {
        !in a -> println("ok")
    }
}

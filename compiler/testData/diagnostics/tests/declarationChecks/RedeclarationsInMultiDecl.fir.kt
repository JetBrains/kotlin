class A {
    operator fun component1() : Int = 1
    operator fun component2() : Int = 2
}

fun a() {
    val (a, a) = A()
    val (x, y) = A();
    val b = 1
    use(b)
    val (b, y) = A();
}


fun use(a: Any): Any = a

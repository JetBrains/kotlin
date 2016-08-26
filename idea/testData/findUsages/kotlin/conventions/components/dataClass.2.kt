import pack.A

fun test(javaClass: JavaClass, javaClass2: JavaClass2) {
    val a = A(1, "2", Any())
    a.n
    a.component1()

    val (x, y, z) = a
    val (x1, y1, z1) = f()
    val (x2, y2, z2) = g()
    val (x3, y3, z3) = h()

    val (x4, y4, z4) = listOfA()[0]

    val (x5, y5, z5) = javaClass.getA()[0]
}

fun f(): A = TODO()
fun g() = A()
fun h() = g()

fun listOfA() = listOf<A>(A(1, "", ""))

fun x(o: Any) {
    if (o is A) {
        val (x, y) = o
        val (x1, y1) = A(1, "", "")
    }
}

fun y(o: Any) {
    val list = o as List<A>
    val (x, y) = list[0]
}

fun when1(o: Any) {
    when (o) {
        is A -> {
            val (x, y) = o
        }

        is String -> TODO()

        else -> return
    }
}

fun when2(o: Any) {
    when (o) {
        !is A -> { }

        else -> {
            val (x, y) = o
        }
    }
}

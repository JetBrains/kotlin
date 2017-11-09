class A {
    operator fun component1() = 1
    operator fun component2() = 2
}

fun box() : String {
    val (_, b) = A()

    val (a, _) = A()

    val (`_`, c) = A()

    return if (a == 1 && b == 2 && `_` == 1 && c == 2) "OK" else "fail"
}

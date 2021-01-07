interface IC1 {
    operator fun component1(): String
}

interface IC2 {
    operator fun component2(): String
}

class A : IC1, IC2 {
    override fun component1(): String = "O"
    override fun component2(): String = "K"
}

fun test(x: Any): String {
    if (x is IC1 && x is IC2) {
        val (x1, x2) = x
        return "$x1$x2"
    }
    return "FAIL"
}

fun box(): String = test(A())
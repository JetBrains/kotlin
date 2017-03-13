class A(
        val i : Int,
        val j : Int = i
)

fun box() = if (A(1).j == 1) "OK" else "fail"

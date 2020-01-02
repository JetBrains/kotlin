// !LANGUAGE: -SingleUnderscoreForParameterName

data class A(val x: Int, val y: Int)

fun foo(a: Array<A>) {
    val (_, y) = A(1, 2)
    y.hashCode()

    val q1: (Int, String) -> Unit = {
        _, s -> s.hashCode()
    }
    q1(1, "")

    val q2: (Int, String) -> Unit = fun(_: Int, s: String) {
        s.hashCode()
    }
    q2(1, "")

    val q3: (A) -> Unit = {
        (_, y) -> y.hashCode()
    }
    q3(A(2, 3))

    for ((_, z) in a) {
        z.hashCode()
    }
}

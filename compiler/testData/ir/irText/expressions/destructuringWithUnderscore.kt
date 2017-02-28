object A

object B {
    operator fun A.component1() = 1
    operator fun A.component2() = 2
    operator fun A.component3() = 3
}

fun B.test() {
    val (x, _, z) = A
}
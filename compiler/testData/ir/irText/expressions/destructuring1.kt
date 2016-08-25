object A

object B {
    operator fun A.component1() = 1
    operator fun A.component2() = 2
}

fun B.test() { // <<< destructuring1.txt
    val (x, y) = A
}
package a

class A() {
}

fun A.component1() = 1
fun A.component2() = 1

fun main(args: Array<String>) {
    val (a, <caret>b) = A()
}

// REF: (for A in a).component2()

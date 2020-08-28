package a

class MyPair {
    operator fun component1() = 1
    operator fun component2() = 2
}

fun main(args: Array<String>) {
    val p = MyPair()
    val (a, <caret>b) = p
}

// REF: (in a.MyPair).component2()
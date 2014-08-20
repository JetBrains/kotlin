package a

class MyPair {
    fun component1() = 1
    fun component2() = 2
}

fun main(args: Array<String>) {
    val p = MyPair()
    val (a, b<caret>) = p
}

// MULTIRESOLVE
// REF: (in a.MyPair).component1()
// REF: (in a.MyPair).component2()
package a

fun main(args: Array<String>) {
    val p = Pair(1, 2)
    val (a, b<caret>) = p
}

// MULTIRESOLVE
// REF: (in kotlin.Pair).component1()
// REF: (in kotlin.Pair).component2()
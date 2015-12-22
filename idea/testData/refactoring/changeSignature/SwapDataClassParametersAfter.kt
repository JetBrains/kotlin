data class <caret>X(val c: Int, val a: Int, val b: Int) {

}

fun test() {
    val (c, a, b) = X(3, 1, 2)
    val aa = X(3, 1, 2).component2()
    val bb = X(3, 1, 2).component3()
    val cc = X(3, 1, 2).component1()
    for ((c, a, b) in listOf(X(3, 1, 2))) {}
}
data class <caret>X(val a: Int, val c: Int, val b: Int) {

}

fun test() {
    val (a, c1, b) = X(1, 3, 2)
    val aa = X(1, 3, 2).component1()
    val bb = X(1, 3, 2).component3()
    for ((a, c, b) in listOf(X(1, 3, 2))) {}
}
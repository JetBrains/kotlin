data class <caret>X(val d: Int, val c: Int, val e: Int) {

}

fun test() {
    val () = X(4, 3, 5)
    val aa = X(4, 3, 5).component1()
    val bb = X(4, 3, 5).component2()
    val cc = X(4, 3, 5).component2()
    for (() in listOf(X(4, 3, 5))) {}
}
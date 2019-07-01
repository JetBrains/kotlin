// WITH_RUNTIME
// PROBLEM: none
fun test(args: Array<Int>) {
    val x = arrayOf<String>()
    for (index in ar<caret>gs) {
        val out = x[index]
    }
}

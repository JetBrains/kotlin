// WITH_RUNTIME
// PROBLEM: none
fun test(args: Array<String>) {
    val x = arrayOf<String>()
    for (index in args) {
        val out = x[index]
    }
}

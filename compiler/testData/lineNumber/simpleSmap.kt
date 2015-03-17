inline fun inlineFun(s: () -> Unit) {
    s()
}

fun main(args: Array<String>) {
    inlineFun {
        test.lineNumber()
    }
}

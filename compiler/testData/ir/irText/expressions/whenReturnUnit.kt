// FIR_IDENTICAL

fun run(block: () -> Unit) {}

fun branch(x: Int) = run {
    when (x) {
        1 -> TODO("1")
        2 -> TODO("2")
    }
}

// FIR_IDENTICAL
fun foo() {
    when {}
    val x = 0
    when (x) {
        else -> {}
    }
    val z = when (x) {
        else -> {}
    }
}

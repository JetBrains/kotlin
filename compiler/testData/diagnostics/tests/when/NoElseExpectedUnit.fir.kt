fun foo(x: Int) {
    val y: Unit = when (x) {
        2 -> {}
        3 -> {}
    }
    return y
}
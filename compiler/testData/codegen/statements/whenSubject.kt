fun z() {}

fun foo(x: Int) {
    when (x) {
        21 -> z()
        42 -> z()
        else -> {}
    }
}

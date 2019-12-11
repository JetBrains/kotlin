fun bar(x: Int): Int = x + 1

fun foo() {
    val x: Int? = null

    if (x != null) {
        when (x) {
            0 -> bar(x)
            else -> {}
        }
    }

    when (x) {
        0 -> { if (x == null) return }
        else -> { if (x == null) return }
    }
    bar(x)
}

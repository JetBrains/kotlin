// ISSUE: KT-48708

fun test(b: Boolean) {
    val x = if (b) {
        3
    } else {
        throw Exception()
        0
    }
    takeInt(x)
}

fun takeInt(x: Int) {}

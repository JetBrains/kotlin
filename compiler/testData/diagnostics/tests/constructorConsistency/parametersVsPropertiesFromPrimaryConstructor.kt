// FIR_IDENTICAL
// ISSUE: KT-58135

class Test(
    val x: Int, // (1)
    y: Int // (2)
) {
    val String.x: String get() = this // (3)
    val String.y: String get() = this // (4)

    val y: Int = y // (5)

    fun test(s: String) {
        with(s) {
            x.length // (3)
            y.length // (4)
        }
    }

    val test = with("hello") {
        x.length // (3)
        y.inc() // (2)
    }

    init {
        with("hello") {
            x.length // (3)
            y.inc() // (2)
        }
    }
}

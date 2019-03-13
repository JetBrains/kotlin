// PROBLEM: none

class <caret>A(val i: Int) {
    constructor(): this(42)

    companion object {
        const val B = 1
    }
}

fun main() {
    val test = A()
}
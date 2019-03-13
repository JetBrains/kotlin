// PROBLEM: none

typealias T = A

class <caret>A(val i: Int) {
    companion object {
        const val B = 1
    }
}

fun test() {
    val t = T(42)
}
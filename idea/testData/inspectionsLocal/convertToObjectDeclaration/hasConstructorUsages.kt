// PROBLEM: none

class <caret>A {
    companion object {
        fun check() {}
    }
}

fun test() {
    val a = A()
    A.check()
}

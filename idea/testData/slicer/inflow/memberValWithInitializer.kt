// FLOW: IN

class A {
    val <caret>x: Int = 1
    val y = x

    fun test() {
        val y = x
    }
}
// FLOW: IN

class A {
    val <caret>x: Int

    init {
        x = 1
    }

    val y = x

    fun test() {
        val y = x
    }
}
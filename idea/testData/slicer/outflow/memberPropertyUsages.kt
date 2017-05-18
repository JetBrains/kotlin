// FLOW: OUT

class A {
    val <caret>x = 1

    val y = x

    val z: Int

    init {
        z = x

        bar(x)
    }

    fun bar(m: Int) {

    }
}
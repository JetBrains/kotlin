// FLOW: IN

open class A(var <caret>n: Int) {
    val x = n

    val y: Int

    init {
        y = n

        bar(n)
    }

    fun bar(m: Int) {
        val z = n
        n = 1
    }
}

class B : A(1)

fun test() {
    val z = A(2).n
    A(3).n = 2
}
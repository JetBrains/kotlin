class A(
        a: Int
<caret>): Base(1) {
    val c = 1

    init {
        val d = 1
        val e = 1
    }
}

open class Base(i: Int)
interface A {
    val s: String
}

interface B {
    val x: Int
}

abstract class C(open val d: Double)

class D(<caret>open val y: Int, override val d: Double) :  A, C(d), B {
    override val s = "$y -> $d"

    override val x = y * y
}
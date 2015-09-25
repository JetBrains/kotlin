open class B {
    constructor(vararg x: Int) {}
}

class A : B {
    <caret>constructor(vararg x: Int): super(*x, *intArrayOf(1, 2, 3), 4) {}
}

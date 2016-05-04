open class <caret>A(n: Int) {
    constructor(b: Boolean, n: Int): this(n)
}

open class B: A {
    constructor(n: Int) : super(n)
    constructor(b: Boolean, n: Int): super(n)
}

open class C(b: Boolean, n: Int): A(n)

fun test(n: Int) {
    A(n)
    A(false, n)
    B(n)
    B(false, n)
    C(false, n)
    J(n)
    J(false, n)
}
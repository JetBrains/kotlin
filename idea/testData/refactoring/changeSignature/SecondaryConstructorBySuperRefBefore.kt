open class A {
    constructor(a: Int) {

    }

    constructor(): this(1) {

    }
}

open class B: A {
    constructor(a: Int): <caret>super(a) {

    }
}

fun test() {
    A(1)
}
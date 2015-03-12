class A {
    constructor(x: Int) : () {
        x = 1
    }
    fun foo() = 2
}

class B {
    constructor(x: Int) : () {
        x = 3
    }
}

fun foo() = 4

open class A internal <caret>constructor() {
    constructor(a: Int) : this() {

    }
}

open class B : A {
    constructor() : super() {

    }
}

open class C : A {
    constructor() {

    }
}

class D : A() {

}

fun test() {
    A()
}
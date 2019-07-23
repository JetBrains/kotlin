open class A internal constructor(s: String) {
    constructor(a: Int) : this("foo") {

    }
}

open class B : A {
    constructor() : super("foo") {

    }
}

open class C : A {
    constructor() : super("foo") {

    }
}

class D : A("foo") {

}

fun test() {
    A("foo")
}
class A {
    constructor(x: Int) {

    }
    constructor(x: Double, y: String = "abc") {

    }
}

val v = <caret>A(1.0)

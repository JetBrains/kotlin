class A {
    constructor(x: Int) {

    }
    constructor(x: Double, y: String = "abc") {

    }
}

val v = <caret>A(x=1.0)

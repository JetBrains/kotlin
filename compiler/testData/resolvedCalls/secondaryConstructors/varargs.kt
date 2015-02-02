class A {
    constructor(vararg x: Int) {}
}

val y = <caret>A(0, *intArray(1, 2, 3), 4))
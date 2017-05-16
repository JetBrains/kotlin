package foo

val <caret>a: Int get() {
    a
    val b: B = B()
    return 0
}

class B {
    val a: A = A()
    val b: B = B()
}

package foo

class A {
    val a: <caret>A = A()
    val b: B = B()
}

class B {
    val a: A = A()
    val b: B = B()
}

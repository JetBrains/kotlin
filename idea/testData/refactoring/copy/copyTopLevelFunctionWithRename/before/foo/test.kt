package foo

fun <caret>a() {
    a()
    val b: B = B()
}

class B {
    val a: A = A()
    val b: B = B()
}

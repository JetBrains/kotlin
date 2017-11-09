package foo

fun a() {
    a()
    val b: B = B()
}

class B {
    val a: A = A()
    val b: B = B()
}

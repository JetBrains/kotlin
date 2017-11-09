package foo

class A {
    init {
        val a: A = A()
        val x: X = X()
        b()
        c
    }
}

class X {
    init {
        val a: A = A()
        val x: X = X()
        b()
        c
    }
}

fun b() {
    val a: A = A()
    val x: X = X()
    b()
    c
}

val c: Int get() {
    val a: A = A()
    val x: X = X()
    b()
    c
}
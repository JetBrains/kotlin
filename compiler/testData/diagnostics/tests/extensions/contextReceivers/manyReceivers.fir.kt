class A {
    val a = 1
}

class B {
    val b = 2
}

class C {
    val c = 3
}

context(A, B) fun C.f() {}

fun main(a: A, b: B, c: C) {
    with(a) {
        with(b) {
            with(c) {
                f()
            }
        }
    }
    with(b) {
        with(c) {
            with(a) {
                f()
            }
        }
    }
    with(a) {
        with(c) {
            f()
        }
    }
}
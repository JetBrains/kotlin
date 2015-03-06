class A {
    default object {
        val i1 = 1
        val i2 = 2
    }
}

class B {
    default object Named {
        val i1 = 3
        val i2 = 4
    }
}

fun box(): String {
    return if (J.f() == A.i1 + A.i2 + B.i1 + B.i2) "OK" else "Fail: ${J.f()}"
}

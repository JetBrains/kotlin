fun intBinEq() {
    var x = 0
    <!UNRESOLVED_REFERENCE!>x += 'a'<!>
    x += 1.toByte()
    x += 1.toShort()
    x += 1L
    x += 1f
    x += 1.0
    <!UNRESOLVED_REFERENCE!>x *= 'a'<!>
    x *= 1.toByte()
    x *= 1.toShort()
    x *= 1L
    x *= 1f
    x *= 1.0
}

fun shortBinEq() {
    var x = 0.toShort()
    <!UNRESOLVED_REFERENCE!>x += 'a'<!>
    x += 1.toByte()
    x += 1.toShort()
    x += 1L
    x += 1f
    x += 1.0

    <!UNRESOLVED_REFERENCE!>x *= 'a'<!>
    x *= 1.toByte()
    x *= 1.toShort()
    x *= 1L
    x *= 1f
    x *= 1.0
}

class A {
    operator fun plus(x : A) : A { return x }
}

class B {
    operator fun plus(x : A) : A { return x }
}

fun overloading() {
    var x = A()
    var y = A()
    x += y
    var z = B()
    z += x
}

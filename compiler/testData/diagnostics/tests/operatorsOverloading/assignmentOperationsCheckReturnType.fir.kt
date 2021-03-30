fun intBinEq() {
    var x = 0
    x <!UNRESOLVED_REFERENCE!>+=<!> 'a'
    x += 1.toByte()
    x += 1.toShort()
    x += 1L
    x += 1f
    x += 1.0
    x <!UNRESOLVED_REFERENCE!>*=<!> 'a'
    x *= 1.toByte()
    x *= 1.toShort()
    x *= 1L
    x *= 1f
    x *= 1.0
}

fun shortBinEq() {
    var x = 0.toShort()
    x <!UNRESOLVED_REFERENCE!>+=<!> 'a'
    x += 1.toByte()
    x += 1.toShort()
    x += 1L
    x += 1f
    x += 1.0

    x <!UNRESOLVED_REFERENCE!>*=<!> 'a'
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

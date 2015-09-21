fun intBinEq() {
    var x = 0
    x <!NONE_APPLICABLE!>+=<!> 'a'
    x += 1.toByte()
    x += 1.toShort()
    <!TYPE_MISMATCH!>x += 1L<!>
    <!TYPE_MISMATCH!>x += 1f<!>
    <!TYPE_MISMATCH!>x += 1.0<!>
    x <!NONE_APPLICABLE!>*=<!> 'a'
    x *= 1.toByte()
    x *= 1.toShort()
    <!TYPE_MISMATCH!>x *= 1L<!>
    <!TYPE_MISMATCH!>x *= 1f<!>
    <!TYPE_MISMATCH!>x *= 1.0<!>
}

fun shortBinEq() {
    var x = 0.toShort()
    x <!NONE_APPLICABLE!>+=<!> 'a'
    <!TYPE_MISMATCH!>x += 1.toByte()<!>
    <!TYPE_MISMATCH!>x += 1.toShort()<!>
    <!TYPE_MISMATCH!>x += 1L<!>
    <!TYPE_MISMATCH!>x += 1f<!>
    <!TYPE_MISMATCH!>x += 1.0<!>

    x <!NONE_APPLICABLE!>*=<!> 'a'
    <!TYPE_MISMATCH!>x *= 1.toByte()<!>
    <!TYPE_MISMATCH!>x *= 1.toShort()<!>
    <!TYPE_MISMATCH!>x *= 1L<!>
    <!TYPE_MISMATCH!>x *= 1f<!>
    <!TYPE_MISMATCH!>x *= 1.0<!>
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
    <!TYPE_MISMATCH!>z += x<!>
}

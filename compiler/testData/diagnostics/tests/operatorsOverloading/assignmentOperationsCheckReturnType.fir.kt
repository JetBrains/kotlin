fun intBinEq() {
    var x = 0
    x <!NONE_APPLICABLE!>+=<!> 'a'
    x += 1.toByte()
    x += 1.toShort()
    <!ASSIGNMENT_TYPE_MISMATCH!>x += 1L<!>
    <!ASSIGNMENT_TYPE_MISMATCH!>x += 1f<!>
    <!ASSIGNMENT_TYPE_MISMATCH!>x += 1.0<!>
    x <!NONE_APPLICABLE!>*=<!> 'a'
    x *= 1.toByte()
    x *= 1.toShort()
    <!ASSIGNMENT_TYPE_MISMATCH!>x *= 1L<!>
    <!ASSIGNMENT_TYPE_MISMATCH!>x *= 1f<!>
    <!ASSIGNMENT_TYPE_MISMATCH!>x *= 1.0<!>
}

fun shortBinEq() {
    var x = 0.toShort()
    x <!NONE_APPLICABLE!>+=<!> 'a'
    <!ASSIGNMENT_TYPE_MISMATCH!>x += 1.toByte()<!>
    <!ASSIGNMENT_TYPE_MISMATCH!>x += 1.toShort()<!>
    <!ASSIGNMENT_TYPE_MISMATCH!>x += 1L<!>
    <!ASSIGNMENT_TYPE_MISMATCH!>x += 1f<!>
    <!ASSIGNMENT_TYPE_MISMATCH!>x += 1.0<!>

    x <!NONE_APPLICABLE!>*=<!> 'a'
    <!ASSIGNMENT_TYPE_MISMATCH!>x *= 1.toByte()<!>
    <!ASSIGNMENT_TYPE_MISMATCH!>x *= 1.toShort()<!>
    <!ASSIGNMENT_TYPE_MISMATCH!>x *= 1L<!>
    <!ASSIGNMENT_TYPE_MISMATCH!>x *= 1f<!>
    <!ASSIGNMENT_TYPE_MISMATCH!>x *= 1.0<!>
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
    <!ASSIGNMENT_TYPE_MISMATCH!>z += x<!>
}

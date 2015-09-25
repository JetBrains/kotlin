//KT-1571 Frontend fails to check val reassigment for operator overloading.
package kt1571

var c0 = 0
var c1 = 0
var c2 = 0

class A() {
    var p = 0
    operator fun divAssign(<!UNUSED_PARAMETER!>a<!> : Int) {
        c1++;
    }
    operator fun times(<!UNUSED_PARAMETER!>a<!> : Int) : A {
        c2++;
        return this;
    }
}

val a : A = A()
get() {
    c0++
    return field
}

fun box() : String {

    a /= 3
    if (c0 != 1) {
        return "1"
    }
    if (c1 != 1) {
        return "2"
    }
    <!VAL_REASSIGNMENT!>a<!> *= 3 // a = a * 3, shouldn't be able to do this on val
    if (c0 != 2) {
        return "3"
    }
    if (c2 != 1) {
        return "4"
    }
    return "OK"
}

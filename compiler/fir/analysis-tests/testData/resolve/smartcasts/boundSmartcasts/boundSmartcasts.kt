// !DUMP_CFG
interface A {
    fun foo()
}

interface B {
    fun bar()
}

fun test_1(x: Any) {
    val y = x
    if (x is A) {
        x.foo()
        y.foo()
    }
}

fun test_2(x: Any) {
    val y = x
    if (y is A) {
        x.foo()
        y.foo()
    }
}

fun test_3(x: Any, y: Any) {
    var z = x
    if (x is A) {
        z.foo()
    }
    z = y
    if (y is B) {
        z.<!UNRESOLVED_REFERENCE!>foo<!>()
        z.bar()
    }
}

fun test_4(y: Any) {
    var x: Any = 1
    x as Int
    x.inc()
    x = y
    x.<!UNRESOLVED_REFERENCE!>inc<!>()
    if (y is A) {
        x.foo()
        y.foo()
    }
}

class D(val any: Any?)

fun Any.baz() {}

fun test_5(d: D) {
    // Elvis operator is converted into == function call
    val a = d.any ?: return
    a.baz() // should be OK
    d.any.baz() // should be OK
    a as A
    a.foo() // should be OK
}

fun test_6(d1: D) {
    val a = d1.any
    a as A
    a.foo() // should be OK
    d1.any.foo() // should be OK
    d1.any.baz() // should be OK
}

fun test_7(d1: D, d2: D) {
    val a = d1?.any
    val b = d2?.any
    a as A
    a.foo() // should be OK
    b as B
    b.bar() // should be OK
}
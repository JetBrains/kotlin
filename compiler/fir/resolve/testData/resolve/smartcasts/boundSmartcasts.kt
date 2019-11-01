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
        z.<!UNRESOLVED_REFERENCE!>bar<!>()
    }
}

fun test_4(y: Any) {
    var x: Any = 1
    x as Int
    x.inc()
    x = y
    x.inc()
    if (y is A) {
        x.<!UNRESOLVED_REFERENCE!>foo<!>()
        y.foo()
    }
}
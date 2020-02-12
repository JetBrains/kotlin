interface A {
    fun foo()

    fun bool(): Boolean
}

interface B : A {
    fun bar()
}

interface C : A {
    fun baz()
}

fun test_1(x: Any) {
    if (x is B && x is C) {
        x.foo()
        x.bar()
        x.baz()
    }
}

fun test_2(x: Any) {
    if (x is B || x is C) {
        x.foo()
        x.<!UNRESOLVED_REFERENCE!>bar<!>()
        x.<!UNRESOLVED_REFERENCE!>baz<!>()
    }
}

fun test_3(x: Any) {
    if (!(x !is A)) {
        x.foo()
    }
}

fun test_4(x: Any) {
    if (x !is String || x.length == 0) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test_5(x: Any) {
    if (x is A && x.bool()) {
        x.foo()
    }
}

fun test_6(x: Any) {
    if ((x !is A).not()) {
        x.foo()
    }
}

//  || and const

fun test_7(x: Any) {
    if (x is A || false) {
        // TODO: should be smartcast
        x.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}

fun test_8(x: Any) {
    if (false || x is A) {
        // TODO: should be smartcast
        x.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}

fun test_9(x: Any) {
    if (x is A || true) {
        x.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}

fun test_10(x: Any) {
    if (true || x is A) {
        x.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}

//  && and const

fun test_11(x: Any, b: Boolean) {
    if (false && x is A) {
        x.foo()
    }
}

fun test_12(x: Any, b: Boolean) {
    if (x is A && false) {
        x.foo()
    }
}

fun test_13(x: Any, b: Boolean) {
    if (true && x is A) {
        x.foo()
    }
}

fun test_14(x: Any, b: Boolean) {
    if (x is A && false) {
        x.foo()
    }
}

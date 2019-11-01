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

fun test_5(x: A?) {
    if (x != null || false) {
        x.<!INAPPLICABLE_CANDIDATE!>foo<!>()
    }
}

fun test_6(x: A?) {
    if (false || x != null) {
        x.<!INAPPLICABLE_CANDIDATE!>foo<!>()
    }
}

fun test_7(x: Any) {
    if (x is A && x.bool()) {
        x.foo()
    }
}
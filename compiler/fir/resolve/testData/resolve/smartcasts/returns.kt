fun test_0(x: Any) {
    if (x is String) {
        x.length
    } else {

    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun test_1(x: Any) {
    if (x is String) {
        x.length
    } else {
        return
    }
    x.length
}

interface A {
    fun foo()
}

interface B : A {
    fun bar()
}

interface C : A {
    fun baz()
}

fun test_2(x: Any) {
    when {
        x is B -> x.bar()
        x is C -> x.baz()
        else -> return
    }
    x.foo()
    x.<!UNRESOLVED_REFERENCE!>bar<!>()
    x.<!UNRESOLVED_REFERENCE!>baz<!>()
}

fun test_3(x: Any) {
    when {
        x is B -> x.bar()
        x is C -> x.baz()
    }
    x.<!UNRESOLVED_REFERENCE!>foo<!>()
    x.<!UNRESOLVED_REFERENCE!>bar<!>()
    x.<!UNRESOLVED_REFERENCE!>baz<!>()
}
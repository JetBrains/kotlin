interface A {
    fun foo(): Int

    val x: Int

    fun bar()
}

fun test_1(a: A?) {
    val x = a?.x
    if (x != null) {
        a.<!INAPPLICABLE_CANDIDATE!>bar<!>()
    }
}

fun test_2(a: A?) {
    val x = a?.foo()
    if (x != null) {
        a.<!INAPPLICABLE_CANDIDATE!>bar<!>()
    }
}

// ----------------------------------------------------------------

fun test_3(x: Any?) {
    val a = x as? A ?: return
    a.foo()
    x.<!UNRESOLVED_REFERENCE!>foo<!>()
}
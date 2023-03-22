// IGNORE_REVERSED_RESOLVE
// !DUMP_CFG

interface A {
    fun foo()
}

// ------------------------- Should work -------------------------

fun test_1(a: A?) {
    a != null || throw Exception()
    a.foo()
}

fun teat_2(a: A?) {
    a == null && throw Exception()
    a.foo()
}

fun test_3(a: A?) {
    if (a != null || throw Exception()) {
        a.foo()
    }
    a.foo()
}

fun test_4(a: A?) {
    if (a == null && throw Exception()) {
        a.foo()
    }
    a.foo()
}

// ------------------------- Shouldn't work -------------------------

fun test_5(a: A?) {
    a == null || throw Exception()
    a<!UNSAFE_CALL!>.<!>foo()
}

fun teat_6(a: A?) {
    a != null && throw Exception()
    a<!UNSAFE_CALL!>.<!>foo()
}

fun test_7(a: A?) {
    if (a == null || throw Exception()) {
        a<!UNSAFE_CALL!>.<!>foo()
    }
    a<!UNSAFE_CALL!>.<!>foo()
}

fun test_8(a: A?) {
    if (a != null && throw Exception()) {
        a<!UNSAFE_CALL!>.<!>foo()
    }
    a<!UNSAFE_CALL!>.<!>foo()
}

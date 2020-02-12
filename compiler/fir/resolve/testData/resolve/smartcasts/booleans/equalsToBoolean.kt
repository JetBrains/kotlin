interface A {
    fun foo()
    val b: Boolean
}

fun test_1(b: Boolean?) {
    if ((b == true) == true) {
        b.not() // OK
    } else {
        b.<!INAPPLICABLE_CANDIDATE!>not<!>() // Bad
    }
}

fun test_2(b: Boolean?) {
    if ((b == true) != true) {
        b.<!INAPPLICABLE_CANDIDATE!>not<!>() // Bad
    } else {
        b.not() // OK
    }
}

fun test_3(b: Boolean?) {
    if ((b == true) == false) {
        b.<!INAPPLICABLE_CANDIDATE!>not<!>() // Bad
    } else {
        b.not() // OK
    }
}

fun test_4(b: Boolean?) {
    if ((b == true) != false) {
        b.not() // OK
    } else {
        b.<!INAPPLICABLE_CANDIDATE!>not<!>() // Bad
    }
}

fun test_5(b: Boolean?) {
    if ((b != true) == true) {
        b.<!INAPPLICABLE_CANDIDATE!>not<!>() // Bad
    } else {
        b.not() // OK
    }
}

fun test_6(b: Boolean?) {
    if ((b != true) != true) {
        b.not() // OK
    } else {
        b.<!INAPPLICABLE_CANDIDATE!>not<!>() // Bad
    }
}

fun test_7(b: Boolean?) {
    if ((b != true) == false) {
        b.not() // OK
    } else {
        b.<!INAPPLICABLE_CANDIDATE!>not<!>() // Bad
    }
}

fun test_8(b: Boolean?) {
    if ((b != true) != false) {
        b.<!INAPPLICABLE_CANDIDATE!>not<!>() // Bad
    } else {
        b.not() // OK
    }
}